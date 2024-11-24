package moe.nemesiss.hostman

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import moe.nemesiss.hostman.model.HostEntries
import moe.nemesiss.hostman.model.viewmodel.HostmanViewModel
import moe.nemesiss.hostman.model.viewmodel.ShizukuStateModel
import moe.nemesiss.hostman.service.FileProviderService
import moe.nemesiss.hostman.ui.compose.EditHostEntryDialog
import moe.nemesiss.hostman.ui.compose.HostEntryItem
import moe.nemesiss.hostman.ui.theme.HostEntriesGroupNameColor
import moe.nemesiss.hostman.ui.theme.HostmanTheme
import rikka.shizuku.Shizuku
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
class HostmanActivity : ComponentActivity(), ServiceConnection {

    companion object {

        const val TAG = "HostmanActivity"
    }

    private val vm by viewModels<HostmanViewModel>()

    private var fileProvider: IFileProvider? = null

    private val fileProviderConnected = MutableStateFlow(false)

    private var shuttingdown = false

    private val serviceArgs = createFileProviderServiceArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HostmanTheme {
                App()
            }
        }
        subscribeStates()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App() {
        val loading = vm.loading.observeAsState(true).value
        val savingContent = vm.savingContent.observeAsState(false).value
        val connected = fileProviderConnected.collectAsStateWithLifecycle().value
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(BuildConfig.APPLICATION_NAME, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        actions = {
                            IconButton(enabled = connected,
                                       onClick = {
                                           shutdown()
                                       }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = "Shutdown"
                                )
                            }

                            IconButton(enabled = !loading,
                                       onClick = {
                                           vm.showEditDialog.value = true
                                           vm.editingHostEntry.value = null
                                       }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add a new host entry"
                                )
                            }
                        }
                    )
                    if (savingContent) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            content = { paddingValues ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {
                    HostEntriesList()
                }
            }
        )
    }


    @Composable
    fun HostEntriesList() {
        val loading = vm.loading.observeAsState().value ?: true
        val entries = vm.hostFileEntries.observeAsState().value ?: HostEntries()
        val showEditDialog = vm.showEditDialog.observeAsState().value ?: false
        val editingHostEntry = vm.editingHostEntry.observeAsState().value
        val context = LocalContext.current

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {
                fileProvider?.let { vm.loadHostFileEntries(it) }
            },
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)) {

            if (showEditDialog) {
                EditHostEntryDialog(entry = editingHostEntry,
                                    dismiss = {
                                        vm.cleanEditingHostEntry()
                                    },
                                    confirmation = { prev, curr ->
                                        fileProvider?.let { fp ->
                                            vm.updateHostEntry(context, fp, prev, curr)
                                        }
                                    })
            }

            // Ensure 'Swipe-to-refresh' gesture is also available at the blank space.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                listOf("IPV4", "IPV6").zip(listOf(entries.ipv4, entries.ipv6))
                    .forEach { (key, entries) ->
                        if (entries.isNotEmpty()) {
                            item(key = key) {
                                ListItem(headlineContent = {
                                    Text(text = key,
                                         color = HostEntriesGroupNameColor,
                                         fontSize = 18.sp)
                                })
                            }

                            for ((index, entry) in entries.values.withIndex()) {
                                item(key = key + "-" + entry.key + "-" + index) {
                                    Box(modifier = Modifier
                                        .clickable {
                                            if (!loading) {
                                                vm.startEditingHostEntry(entry)
                                            }
                                        }
                                    ) {
                                        HostEntryItem(entryData = entry,
                                                      onRemoving = { removingEntry, promise ->
                                                          fileProvider?.let { fp ->
                                                              vm.removeHostEntry(context,
                                                                                 fp,
                                                                                 removingEntry,
                                                                                 promise)
                                                          }
                                                      })
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }


    private fun subscribeStates() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ShizukuStateModel
                    .state
                    .combine(fileProviderConnected) { v1, v2 -> v1.looksGoodToMe && !v2 && fileProvider == null }
                    .filter { it }
                    .collect {
                        bindFileProviderService()
                    }
            }

            lifecycle.repeatOnLifecycle(Lifecycle.State.DESTROYED) {
                unbindFileProviderService()
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fileProviderConnected
                    .filter { it }
                    .collect {
                        fileProvider?.let { provider -> vm.loadHostFileEntries(provider) }
                    }
            }
        }
    }


    private fun bindFileProviderService() {
        Shizuku.bindUserService(serviceArgs, this)
    }

    private fun unbindFileProviderService() {
        Shizuku.unbindUserService(serviceArgs, this, true)
    }


    private fun createFileProviderServiceArgs(): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, FileProviderService::class.java.name)
        ).daemon(false)
            .tag("FileProviderService")
            .processNameSuffix("fileProvider")
            .debuggable(true)
            .version(BuildConfig.VERSION_CODE)
    }

    private fun shutdown() {
        shuttingdown = true
        unbindFileProviderService()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        if (service.pingBinder()) {
            Log.e(TAG, "Got a valid binder from onServiceConnected: $name")
            this.fileProvider = IFileProvider.Stub.asInterface(service)
            this.fileProviderConnected.value = true
        } else {
            Log.e(TAG, "Got a broken binder from onServiceConnected: $name")
            onServiceDisconnected(name)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        this.fileProvider = null
        this.fileProviderConnected.value = false
        if (this.shuttingdown) {
            finish()
            exitProcess(0)
        }
    }
}