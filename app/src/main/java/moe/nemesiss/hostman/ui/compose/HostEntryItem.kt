package moe.nemesiss.hostman.ui.compose

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.nemesiss.hostman.R
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress


data class HostEntry(val hostName: String, val address: InetAddress) {
    val ipv4: Boolean get() = address is Inet4Address
    val ipv6: Boolean get() = address is Inet6Address
    val created = System.currentTimeMillis()
    val key get() = "$hostName-$created"
}

@Composable
fun DismissBackgroundContent(targetState: SwipeToDismissBoxValue) {
    val color by
    animateColorAsState(
        when (targetState) {
            SwipeToDismissBoxValue.Settled -> Color.LightGray
            SwipeToDismissBoxValue.StartToEnd -> Color.Red
            SwipeToDismissBoxValue.EndToStart -> Color.Red
        }, label = "DismissBackgroundColor"
    )
    Row(modifier = Modifier
        .fillMaxSize()
        .background(color)
        .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "delete"
        )
    }
}

private fun SwipeToDismissBoxState.animatedToEnd(): Boolean {
    return progress >= 0.99f
}

data class RemovePromise(val resolve: () -> Unit = {}, val reject: () -> Unit)

@Composable
fun HostEntryItem(entryData: HostEntry, onRemoving: (HostEntry, RemovePromise) -> Unit = { _, _ -> }) {
    val currentItem by rememberUpdatedState(entryData)
    val scope = rememberCoroutineScope { Dispatchers.Main }
    val dismissStateRef = Ref<SwipeToDismissBoxState>()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            Log.w("HostEntryItem", "swipe value: $it")
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    Log.w("HostEntryItem", "swipe progress: ${dismissStateRef.value?.progress}")
                    if (dismissStateRef.value?.animatedToEnd() == true) {
                        scope.launch {
                            onRemoving(currentItem, RemovePromise(resolve = {},
                                                                  reject = {
                                                                      scope.launch {
                                                                          Log.w("HostEntryItem",
                                                                                "resetting dismissState")
                                                                          dismissStateRef.value?.reset()
                                                                          Log.w("HostEntryItem",
                                                                                "reset dismissState done")
                                                                      }
                                                                  }))
                        }
                        return@rememberSwipeToDismissBoxState true
                    }
                    return@rememberSwipeToDismissBoxState true
                }
                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState true
                SwipeToDismissBoxValue.StartToEnd -> return@rememberSwipeToDismissBoxState false
            }
        },
        // positional threshold of 25%
        positionalThreshold = { it * .25f }
    )
    dismissStateRef.value = dismissState

    SwipeToDismissBox(state = dismissState,
                      backgroundContent = { DismissBackgroundContent(targetState = dismissState.targetValue) },
                      enableDismissFromEndToStart = true,
                      enableDismissFromStartToEnd = false) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painter = painterResource(id = R.drawable.link_24dp_5f6368_fill0_wght400_grad0_opsz24),
                         contentDescription = "Host name")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = entryData.hostName)
                }
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painter = painterResource(id = R.drawable.dns_24dp_5f6368_fill0_wght400_grad0_opsz24),
                         contentDescription = "IP Address")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = entryData.address.hostAddress ?: "")
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit",
                )
            },
        )
    }
}

