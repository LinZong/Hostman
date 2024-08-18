package moe.nemesiss.hostman

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import moe.nemesiss.hostman.model.viewmodel.ShizukuState
import moe.nemesiss.hostman.model.viewmodel.ShizukuStateModel
import moe.nemesiss.hostman.ui.theme.HostmanTheme


class MainActivity : ComponentActivity() {

    private val viewModel = ShizukuStateModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HostmanTheme {
                App()
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect {
                    if (it.looksGoodToMe) {
                        proceedToHostman()
                    }
                }
            }
        }
    }


    @Composable
    fun ShizukuStatePanel(state: ShizukuState) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        Image(painter = painterResource(id = if (state.connected) R.mipmap.ic_launcher else R.mipmap.ic_launcher_gray),
                              contentDescription = "Shizuku Icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Shizuku State", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Divider(modifier = Modifier.padding(horizontal = 8.dp))

                    Row(modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!state.permissionGranted) {
                                Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                                       onClick = { viewModel.requestShizukuPermission() }) {
                                    Text(text = "Request Shizuku Permission")
                                }
                            } else if (state.connected) {
                                Text(text = "UID: ${state.uid}")
                                Text(text = "Version: ${state.version}")
                                Text(text = "SELinux Context: ${state.selinuxContext}")
                            }

                            if (state.getStateMessage().isNotEmpty()) {
                                Text(text = state.getStateMessage())
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun App() {
        val state by viewModel.state.collectAsState()
        HostmanTheme {
            Box(modifier = Modifier.fillMaxSize()) {

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ShizukuStatePanel(state)
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewApp() {
        HostmanTheme {
            Box(modifier = Modifier.fillMaxSize()) {

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ShizukuStatePanel(ShizukuState())
                    }
                }
            }
        }
    }

    private fun proceedToHostman() {
        startActivity(Intent(this, HostmanActivity::class.java))
        finish()
    }
}

