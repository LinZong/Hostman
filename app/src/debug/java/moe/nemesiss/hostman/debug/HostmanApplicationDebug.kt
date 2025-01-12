package moe.nemesiss.hostman.debug

import android.util.Log
import moe.nemesiss.hostman.HostmanApplication

class HostmanApplicationDebug : HostmanApplication() {

    private val TAG = "HostmanApplicationDebug"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Launching HostmanApplicationDebug Application")
        registerActivityLifecycleCallbacks(DebugEntranceRegisterCallback())
    }

}