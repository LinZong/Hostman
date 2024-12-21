package moe.nemesiss.hostman.model.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.perf.metrics.AddTrace
import com.google.firebase.perf.performance
import io.netty.resolver.HostsFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.nemesiss.hostman.HostmanActivity
import moe.nemesiss.hostman.IFileProvider
import moe.nemesiss.hostman.boost.EasyDebug
import moe.nemesiss.hostman.boost.update
import moe.nemesiss.hostman.model.FileOperationResult
import moe.nemesiss.hostman.model.HostEntries
import moe.nemesiss.hostman.ui.compose.HostEntry
import moe.nemesiss.hostman.ui.compose.RemovePromise
import java.io.StringReader
import kotlin.time.Duration.Companion.milliseconds

class HostmanViewModel : ViewModel() {

    val TAG = "HostmanViewModel"

    val loading = MutableLiveData(true)
    val hostFileEntries = MutableLiveData(HostEntries(emptyMap(), emptyMap()))
    val showEditDialog = MutableLiveData(false)
    val editingHostEntry = MutableLiveData<HostEntry?>()
    val savingContent = MutableLiveData(false)

    fun startEditingHostEntry(entry: HostEntry) {
        showEditDialog.value = true
        editingHostEntry.value = entry
    }

    fun cleanEditingHostEntry() {
        showEditDialog.value = false
        editingHostEntry.value = null
    }

    fun loadHostFileEntries(fileProvider: IFileProvider) {
        viewModelScope.launch {
            loading.value = true
            val trace = Firebase.performance.newTrace("load_host_file_entries")
            trace.start()
            val begin = System.currentTimeMillis()
            val hostEntries = withContext(Dispatchers.IO) {
                val hostContent = fileProvider.getFileTextContent(HostEntries.HOST_FILE_PATH)
                EasyDebug.info(TAG) { "Got host file content: $hostContent" }
                val nettyEntries = HostsFileParser.parse(StringReader(hostContent))
                HostEntries.fromNettyHostFileEntries(nettyEntries)
            }
            EasyDebug.info(TAG) { "Got host file entries: $hostEntries" }
            val end = System.currentTimeMillis()
            trace.stop()
            val cost = ((end - begin).toInt()).coerceAtLeast(500).milliseconds
            delay(cost)
            hostFileEntries.value = hostEntries
            loading.value = false
        }
    }

    fun updateHostEntry(context: Context,
                        fileProvider: IFileProvider,
                        previousHostEntry: HostEntry?,
                        newHostEntry: HostEntry) {
        EasyDebug.info(HostmanActivity.TAG) { "Host entry was edited: $newHostEntry" }
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
                        fileProvider: IFileProvider,
                        removingHostEntry: HostEntry,
                        removePromise: RemovePromise) {
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


    @AddTrace(name = "write_host_entries_to_host_file")
    private suspend fun writeHostEntriesToHostFile(context: Context,
                                                   fileProvider: IFileProvider,
                                                   hostsFileEntries: HostEntries): FileOperationResult {
        savingContent.value = true
        val hostFileContent = hostsFileEntries.generateHostFileContent()
        val fileOperationResult = withContext(Dispatchers.IO) {
            EasyDebug.info(HostmanActivity.TAG) { "Saving content: $hostFileContent to host file: ${HostEntries.HOST_FILE_PATH}" }
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

            EasyDebug.error(HostmanActivity.TAG) {
                "Failed to save content to host file. error: ${fileOperationResult.message}, stack: ${fileOperationResult.exceptionStack}"
            }
        }
        return fileOperationResult
    }
}