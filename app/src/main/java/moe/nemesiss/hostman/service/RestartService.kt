package moe.nemesiss.hostman.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.Log
import moe.nemesiss.hostman.MainActivity
import moe.nemesiss.hostman.boost.EasyProcess

class RestartService : Service() {

    private val TAG = "RestartService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val hostPid = intent?.getIntExtra(HOST_PID, -1)
        if (hostPid != null && hostPid > 0) {
            Log.w(TAG, "Killing previous process: $hostPid")
            Process.killProcess(hostPid)
        }

        EasyProcess.logPid(TAG)

        restartApp()

        prepareToKillMySelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val HOST_PID = "hostPid"

        fun buildIntent(context: Context, hostPid: Int): Intent {
            val intent = Intent(context, RestartService::class.java)
            intent.putExtra(HOST_PID, hostPid)
            return intent
        }
    }

    private fun restartApp() {
        val restartIntent = Intent(this, MainActivity::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(restartIntent)
    }

    private fun prepareToKillMySelf() {
        val looper = getLooper()
        Handler(looper)
            .postDelayed({ EasyProcess.killMySelf() }, 1000L)
    }

    private fun getLooper(): Looper {
        val looper = Looper.myLooper()
        if (looper != null) {
            return looper
        }
        Looper.prepare()
        return getLooper()
    }
}