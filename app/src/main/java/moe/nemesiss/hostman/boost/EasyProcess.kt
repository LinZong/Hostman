package moe.nemesiss.hostman.boost

import android.content.Context
import android.os.Process
import android.util.Log
import moe.nemesiss.hostman.service.RestartService

object EasyProcess {

    private const val TAG = "EasyProcess"

    fun logPid(component: String) {
        Log.w(TAG, "$component, my pid is: ${Process.myPid()}")
    }

    fun killMySelf() {
        val myPid = Process.myPid()
        Log.w(TAG, "Killing process: $myPid")
        Process.killProcess(myPid)
    }

    fun restartApp(context: Context) {
        val intent = RestartService.buildIntent(context, Process.myPid())
        context.startService(intent)
    }

}