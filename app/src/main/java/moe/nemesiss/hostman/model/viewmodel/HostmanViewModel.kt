package moe.nemesiss.hostman.model.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.google.firebase.perf.metrics.AddTrace
import io.netty.resolver.HostsFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.nemesiss.hostman.BuildConfig
import moe.nemesiss.hostman.IFileProvider
import moe.nemesiss.hostman.boost.EasyDebug
import moe.nemesiss.hostman.boost.update
import moe.nemesiss.hostman.model.FileOperationResult
import moe.nemesiss.hostman.model.HostEntries
import moe.nemesiss.hostman.model.ServiceRef
import moe.nemesiss.hostman.service.FileProviderService
import moe.nemesiss.hostman.ui.compose.HostEntry
import moe.nemesiss.hostman.ui.compose.RemovePromise
import rikka.shizuku.Shizuku
import java.io.StringReader
import kotlin.time.Duration.Companion.milliseconds

class HostmanViewModel : ViewModel(), ServiceConnection, DefaultLifecycleObserver {

    val TAG = "HostmanViewModel"

    val loading = MutableLiveData(true)
    val hostFileEntries = MutableLiveData(HostEntries(emptyMap(), emptyMap()))
    val showEditDialog = MutableLiveData(false)
    val editingHostEntry = MutableLiveData<HostEntry?>()
    val savingContent = MutableLiveData(false)
    val fileProviderConnected = MutableStateFlow(false)

    private val componentName = ComponentName(BuildConfig.APPLICATION_ID, FileProviderService::class.java.name)
    private val serviceArgs = createFileProviderServiceArgs()
    private var fileProviderRef: ServiceRef<IFileProvider>? = null

    fun prepare(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        EasyDebug.info(TAG) { "Shutting down FileProvider service..." }
        unbindFileProviderService()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        bindFileProviderServiceIfNecessary()
    }

    private fun bindFileProviderServiceIfNecessary() {
        if (checkServiceAlive()) {
            this.fileProviderConnected.value = true
            EasyDebug.info(TAG) { "FileProvider service is alive." }
        } else {
            EasyDebug.info(TAG) { "FileProvide service was died. Perform a new bind." }
            onServiceDisconnected(componentName)
            viewModelScope.launch {
                ShizukuStateModel
                    .state
                    .filter { it.looksGoodToMe }
                    .collect {
                        bindFileProviderService()
                    }
            }
        }
    }


    fun startEditingHostEntry(entry: HostEntry) {
        showEditDialog.value = true
        editingHostEntry.value = entry
    }

    fun cleanEditingHostEntry() {
        showEditDialog.value = false
        editingHostEntry.value = null
    }

    fun loadHostFileEntries() {
        val fileProvider = fileProviderRef?.service ?: return
        viewModelScope.launch {
            loading.value = true
            val begin = System.currentTimeMillis()
            val hostEntries = withContext(Dispatchers.IO) { getHostEntries(fileProvider) }
            EasyDebug.info(TAG) { "Got host file entries: $hostEntries" }
            val end = System.currentTimeMillis()
            val cost = ((end - begin).toInt()).coerceAtLeast(500).milliseconds
            delay(cost)
            hostFileEntries.value = hostEntries
            loading.value = false
        }
    }

    fun updateHostEntry(context: Context,
                        previousHostEntry: HostEntry?,
                        newHostEntry: HostEntry) {
        val fileProvider = fileProviderRef?.service ?: return

        EasyDebug.info(TAG) { "Host entry was edited: $newHostEntry" }
        var entries: HostEntries? = hostFileEntries.value ?: return
        if (previousHostEntry != null) {
            entries = entries?.removeHostEntries(previousHostEntry)
        }
        entries = entries?.updateHostEntries(newHostEntry) ?: return
        viewModelScope.launch {
            val result = writeHostEntriesToHostFile(context, fileProvider, entries)
            if (result.success) {
                hostFileEntries.update { entries }
            }
            cleanEditingHostEntry()
        }
    }

    fun removeHostEntry(context: Context,
                        removingHostEntry: HostEntry,
                        removePromise: RemovePromise) {
        val fileProvider = fileProviderRef?.service ?: return

        val newEntries =
            hostFileEntries.value?.removeHostEntries(removingHostEntry) ?: return
        viewModelScope.launch {
            val result = writeHostEntriesToHostFile(context, fileProvider, newEntries)
            if (result.success) {
                removePromise.resolve()
                hostFileEntries.update { newEntries }
            } else {
                removePromise.reject()
            }
        }
    }

    @AddTrace(name = "load_host_file_entries")
    private fun getHostEntries(fileProvider: IFileProvider): HostEntries {
        // FIX: https://nemesisslin.sentry.io/issues/6908404397/events/latest/?query=is%3Aunresolved&referrer=latest-event
        val hostContent = fileProvider.getFileTextContent(HostEntries.HOST_FILE_PATH) ?: ""
        EasyDebug.info(TAG) { "Got host file content: $hostContent" }
        val nettyEntries = HostsFileParser.parse(StringReader(hostContent))
        return HostEntries.fromNettyHostFileEntries(nettyEntries)
    }

    @AddTrace(name = "write_host_entries_to_host_file")
    private suspend fun writeHostEntriesToHostFile(context: Context,
                                                   fileProvider: IFileProvider,
                                                   hostsFileEntries: HostEntries): FileOperationResult {
        savingContent.value = true
        val hostFileContent = hostsFileEntries.generateHostFileContent()
        val fileOperationResult = withContext(Dispatchers.IO) {
            EasyDebug.info(TAG) { "Saving content: $hostFileContent to host file: ${HostEntries.HOST_FILE_PATH}" }
            val result = fileProvider.writeFileBytes(HostEntries.HOST_FILE_PATH,
                                                     hostFileContent.toByteArray(Charsets.UTF_8))
            val fileOperationResult = FileOperationResult.deserialize(result)
            fileOperationResult
        }
        savingContent.value = false
        if (!fileOperationResult.success) {
            Toast.makeText(context,
                           "Failed to write host file. ${fileOperationResult.message}",
                           Toast.LENGTH_SHORT).show()

            EasyDebug.error(TAG) {
                "Failed to save content to host file. error: ${fileOperationResult.message}, stack: ${fileOperationResult.exceptionStack}"
            }
        }
        return fileOperationResult
    }

    private fun bindFileProviderService() {
        Shizuku.bindUserService(serviceArgs, this)
    }

    private fun unbindFileProviderService() {
        Shizuku.unbindUserService(serviceArgs, this, true)
    }

    private fun createFileProviderServiceArgs(): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(componentName)
            .daemon(false)
            .tag("FileProviderService")
            .processNameSuffix("fileProvider")
            .debuggable(true)
            .version(BuildConfig.VERSION_CODE)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        if (service.pingBinder()) {
            Log.e(TAG, "Got a valid binder from onServiceConnected: $name")
            this.fileProviderRef = ServiceRef(service, IFileProvider.Stub.asInterface(service))
            this.fileProviderConnected.value = true
        } else {
            Log.e(TAG, "Got a broken binder from onServiceConnected: $name")
            onServiceDisconnected(name)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        this.fileProviderRef = null
        this.fileProviderConnected.value = false
    }

    private fun checkServiceAlive(): Boolean {
        val fp = fileProviderRef ?: return false
        return fp.pingBinder()
    }
}