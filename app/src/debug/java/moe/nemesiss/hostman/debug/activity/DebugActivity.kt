package moe.nemesiss.hostman.debug.activity

import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import io.noties.markwon.Markwon
import moe.nemesiss.hostman.PostmortemActivity
import moe.nemesiss.hostman.boost.EasyMarkdown
import moe.nemesiss.hostman.boost.EasyNotification
import moe.nemesiss.hostman.boost.EasyProcess
import moe.nemesiss.hostman.databinding.ActivityDebugBinding
import moe.nemesiss.hostman.service.NetTrafficService

class DebugActivity : AppCompatActivity() {
    private val TAG = "DebugActivity"

    private val binding by lazy { ActivityDebugBinding.inflate(layoutInflater) }


    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Log.w(TAG, "My Process ID: ${Process.myPid()}")
        markwon = EasyMarkdown.createMarkwon(this)

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

        markwon.setMarkdown(binding.markdownPreview,
                            "Hostman 1.0.9 introduces a new feature: Net Traffic Monitor. It can monitor the network traffic currently on device.\r\n<p float=\"left\">\r\n  <img width=\"270\" height=\"600\" alt=\"screenshot\" src=\"https://github.com/user-attachments/assets/4e117033-9cc2-455c-94a1-92123c442cd9\" />\r\n  <img width=\"270\" height=\"600\" alt=\"screenshot\" src=\"https://github.com/user-attachments/assets/c177361f-770d-42f8-9b2e-f897b79e3f46\" />\r\n</p>\r\n")
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyNotification.handlePostNotificationsPermissionResult(this, requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}