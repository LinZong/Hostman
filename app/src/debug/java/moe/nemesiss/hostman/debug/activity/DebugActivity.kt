package moe.nemesiss.hostman.debug.activity

import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import moe.nemesiss.hostman.PostmortemActivity
import moe.nemesiss.hostman.boost.EasyProcess
import moe.nemesiss.hostman.databinding.ActivityDebugBinding

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
    }
}