package moe.nemesiss.hostman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import moe.nemesiss.hostman.boost.EasyDebug
import moe.nemesiss.hostman.model.HostEntries
import moe.nemesiss.hostman.model.viewmodel.CheckUpdateViewModel
import moe.nemesiss.hostman.model.viewmodel.HostmanViewModel
import moe.nemesiss.hostman.ui.compose.EditHostEntryDialog
import moe.nemesiss.hostman.ui.compose.HostEntryItem
import moe.nemesiss.hostman.ui.compose.NewVersionDialog
import moe.nemesiss.hostman.ui.theme.HostEntriesGroupNameColor
import moe.nemesiss.hostman.ui.theme.HostmanTheme

@OptIn(ExperimentalMaterial3Api::class)
class HostmanActivity : ComponentActivity() {

    companion object {

        const val TAG = "HostmanActivity"
    }

    private val viewModel by viewModels<HostmanViewModel>()

    private val checkUpdateModel by viewModels<CheckUpdateViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HostmanTheme {
                App()
            }
        }
        viewModel.prepare(this)
        subscribeStates()
    }

    override fun onPause() {
        EasyDebug.info(TAG) { "onPause" }
        super.onPause()
    }

    override fun onDestroy() {
        EasyDebug.info(TAG) { "onDestroy" }
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App() {
        val loading = viewModel.loading.observeAsState(true).value
        val savingContent = viewModel.savingContent.observeAsState(false).value
        val connected = viewModel.fileProviderConnected.collectAsStateWithLifecycle().value

        val latestVersionAvailable = checkUpdateModel.hasLatestVersion.observeAsState(false).value
        val latestVersion = checkUpdateModel.latestVersion.observeAsState().value
        val showNewVersionDialog = checkUpdateModel.newVersionAvailableDialogShown.observeAsState(false).value

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Box(modifier = Modifier.wrapContentSize()) {
                                var textModifier = Modifier.padding(2.dp)
                                if (latestVersionAvailable) {
                                    textModifier = textModifier.clickable {
                                        checkUpdateModel.showNewVersionDialog()
                                    }
                                }
                                Text(BuildConfig.APPLICATION_NAME,
                                     maxLines = 1,
                                     overflow = TextOverflow.Ellipsis,
                                     modifier = textModifier)
                                if (latestVersionAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp) // Size of the red dot
                                            .background(Color.Red, shape = CircleShape) // Red circle
                                            .align(Alignment.TopEnd) // Align the dot to the top-right corner
                                            .offset(x = (-4).dp, y = (4).dp) // Adjust the position slightly for overlap
                                    )
                                }
                            }
                        },
                        actions = {

                            IconButton(enabled = !loading,
                                       onClick = {
                                           viewModel.showEditDialog.value = true
                                           viewModel.editingHostEntry.value = null
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

                    if (showNewVersionDialog && latestVersion != null) {
                        NewVersionDialog(latestVersion = latestVersion,
                                         dismiss = { checkUpdateModel.hideNewVersionDialog() })
                    }
                    HostEntriesList()
                }
            }
        )
    }


    @Composable
    fun HostEntriesList() {
        val loading = viewModel.loading.observeAsState().value ?: true
        val entries = viewModel.hostFileEntries.observeAsState().value ?: HostEntries()
        val showEditDialog = viewModel.showEditDialog.observeAsState().value ?: false
        val editingHostEntry = viewModel.editingHostEntry.observeAsState().value
        val context = LocalContext.current

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {
                viewModel.loadHostFileEntries(this)
            },
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)) {

            if (showEditDialog) {
                EditHostEntryDialog(entry = editingHostEntry,
                                    dismiss = {
                                        viewModel.cleanEditingHostEntry()
                                    },
                                    confirmation = { prev, curr ->
                                        viewModel.updateHostEntry(context, prev, curr)
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
                                                viewModel.startEditingHostEntry(entry)
                                            }
                                        }
                                    ) {
                                        HostEntryItem(entryData = entry,
                                                      onRemoving = { removingEntry, promise ->
                                                          viewModel.removeHostEntry(context,
                                                                                    removingEntry,
                                                                                    promise)
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
        // Check for updates.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Check new version in individual coroutine scope.
                lifecycleScope.launch {
                    checkUpdateModel.checkNewVersionAvailable()
                }
            }
        }

        // Watch and react for various fileProvider's state.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileProviderConnected
                    .filter { it }
                    .collect {
                        viewModel.loadHostFileEntries(this@HostmanActivity)
                    }

            }
        }
    }
}