package moe.nemesiss.hostman.debug.activity

import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import moe.nemesiss.hostman.PostmortemActivity
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
            val ctx = this
            NetTrafficService.ensurePermissionAndStart(ctx)
        }

        binding.stopNetworkMonitor.setOnClickListener {
            NetTrafficService.stop(this)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyNotification.handlePostNotificationsPermissionResult(this, requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}