package moe.nemesiss.hostman

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui

class HostmanApplication : Application() {

    val sui: Boolean

    init {
        sui = Sui.init(BuildConfig.APPLICATION_ID)
        Log.w("HostmanApplication", "Sui had been initialized. isSui: $sui")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
    }
}