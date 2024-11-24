package moe.nemesiss.hostman.boost

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

object EasyDebug {


    fun info(tag: String, block: () -> String) {
        val msg = block()
        Log.i(tag, msg)
        Firebase.crashlytics.log("[INFO] $tag $msg")
    }

    fun warn(tag: String, block: () -> String) {
        val msg = block()
        Log.w(tag, msg)
        Firebase.crashlytics.log("[WARN] $tag $msg")
    }

    fun warn(tag: String, t: Throwable, block: () -> String) {
        val msg = block()
        Log.w(tag, msg, t)
        Firebase.crashlytics.log("[WARN] $tag $msg")
    }


    fun error(tag: String, block: () -> String) {
        val msg = block()
        Log.e(tag, msg)
        Firebase.crashlytics.log("[ERROR] $tag $msg")
    }

    fun error(tag: String, t: Throwable, block: () -> String) {
        val msg = block()
        Log.e(tag, msg, t)
        Firebase.crashlytics.log("[ERROR] $tag $msg \n${t.stackTraceToString()}")
    }

    fun toast(context: Context, block: () -> String) {
        Toast.makeText(context, block(), Toast.LENGTH_SHORT).show()
    }
}