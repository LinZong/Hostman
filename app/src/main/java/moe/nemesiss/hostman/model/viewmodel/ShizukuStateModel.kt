package moe.nemesiss.hostman.model.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.nemesiss.hostman.BuildConfig
import rikka.shizuku.Shizuku

object ShizukuStateModel :
        Shizuku.OnBinderReceivedListener,
        Shizuku.OnBinderDeadListener,
        Shizuku.OnRequestPermissionResultListener {

    private const val TAG = "ShizukuStateModel"

    private val shizukuPermissionCode = "SHIZUKU".hashCode()

    private val _state = MutableStateFlow(ShizukuState())

    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    override fun onBinderReceived() {
        nextValue(state.value.copy(
            permissionGranted = checkShizukuPermission(),
            connected = true,
            uid = Shizuku.getUid(),
            version = Shizuku.getVersion(),
            selinuxContext = Shizuku.getSELinuxContext()
        ))
    }

    override fun onBinderDead() {
        nextValue(ShizukuState(permissionGranted = state.value.permissionGranted,
                               connected = false))
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == shizukuPermissionCode) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                nextValue(state.value.copy(permissionGranted = true))
            } else {
                nextValue(state.value.copy(permissionGranted = false))
            }
        }
    }

    private fun nextValue(shizukuState: ShizukuState) {
        _state.value = shizukuState
    }

    fun checkShizukuPermission(): Boolean {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return false
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            return false
        } else {
            // Request the permission
            return false
        }
    }

    fun requestShizukuPermission(context: Context) {
        try {
            Shizuku.requestPermission(shizukuPermissionCode)
        } catch (t: Throwable) {
            Toast.makeText(context, "Failed to request shizuku permission. ${t.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Shizuku error", t)
        }
    }
}


data class ShizukuState(
    val connected: Boolean = false,
    val permissionGranted: Boolean = false,
    val uid: Int = -1,
    val version: Int = -1,
    val selinuxContext: String = ""
) {
    val looksGoodToMe: Boolean get() = connected && permissionGranted && runningMode == ShizukuRunningMode.ROOT
    val runningMode: ShizukuRunningMode
        get() {
            return when {
                uid == 0 -> {
                    ShizukuRunningMode.ROOT
                }

                uid > 0 -> {
                    ShizukuRunningMode.ADB
                }

                else -> {
                    ShizukuRunningMode.UNKNOWN
                }
            }
        }

    fun getStateMessage(): String {
        if (!connected) {
            return "Waiting for Shizuku Service"
        }
        if (!permissionGranted) {
            return "Please authorize ${BuildConfig.APPLICATION_NAME} to use Shizuku in Shizuku Manager App."
        }
        if (runningMode == ShizukuRunningMode.UNKNOWN) {
            return "Shizuku is running with a bad uid: $uid."
        }
        if (runningMode == ShizukuRunningMode.ADB) {
            return "${BuildConfig.APPLICATION_NAME} requires Shizuku to be running in root mode."
        }
        return ""
    }
}

enum class ShizukuRunningMode {
    ROOT,
    ADB,
    UNKNOWN
}