package moe.nemesiss.hostman.debug.activity

import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import moe.nemesiss.hostman.PostmortemActivity
import moe.nemesiss.hostman.boost.EasyLayout
import moe.nemesiss.hostman.boost.EasyNotification
import moe.nemesiss.hostman.boost.EasyProcess
import moe.nemesiss.hostman.databinding.ActivityDebugBinding
import moe.nemesiss.hostman.service.NetTrafficService

class DebugActivity : AppCompatActivity() {
    private val TAG = "DebugActivity"

    private val binding by lazy { ActivityDebugBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Log.w(TAG, "My Process ID: ${Process.myPid()}")

        binding.restartApp.setOnClickListener {
            EasyProcess.restartApp(this)
        }

        binding.startPostmortemActivity.setOnClickListener {
            val intent = PostmortemActivity.buildIntent(this, Throwable())
            startActivity(intent)
            finish()
        }

        binding.crashApp.setOnClickListener {
            throw RuntimeException("Crash is awesome!!!")
        }

        binding.imePicker.setOnClickListener {
            val imm = getSystemService<InputMethodManager>() ?: return@setOnClickListener
            imm.showInputMethodPicker()
        }

        binding.startNetworkMonitor.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                if (EasyNotification.checkPostNotificationPermission(this)) {
                    NetTrafficService.start(this)
                } else {
                    EasyNotification.ensurePostNotificationsPermission(this)
                }
            } else {
                EasyLayout.openOverlayPermissionSetting(this)
            }
        }

        binding.stopNetworkMonitor.setOnClickListener {
            NetTrafficService.stop(this)
        }
    }


    private fun showRxTxTemplateCode() {
        val nsm = getSystemService<NetworkStatsManager>() ?: return
        val wifiSummary =
            nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, 0L, System.currentTimeMillis())

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyNotification.handlePostNotificationsPermissionResult(this, requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}