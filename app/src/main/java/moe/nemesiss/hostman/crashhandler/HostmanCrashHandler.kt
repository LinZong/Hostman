package moe.nemesiss.hostman.crashhandler

import android.content.Context
import moe.nemesiss.hostman.PostmortemActivity

class HostmanCrashHandler(private val applicationContext: Context,
                          private val nextHandler: Thread.UncaughtExceptionHandler? = null) :
        Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val intent = PostmortemActivity.buildIntent(applicationContext, e)
        applicationContext.startActivity(intent)
        nextHandler?.uncaughtException(t, e)
    }
}