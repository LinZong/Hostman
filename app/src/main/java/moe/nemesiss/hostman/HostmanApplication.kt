package moe.nemesiss.hostman

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import io.sentry.Sentry
import io.sentry.SentryLevel
import moe.nemesiss.hostman.model.viewmodel.ShizukuStateModel
import moe.nemesiss.hostman.service.NetService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui

class HostmanApplication : Application() {

    val sui: Boolean

    init {
        sui = Sui.init(BuildConfig.APPLICATION_ID)
        Log.w("HostmanApplication", "Sui had been initialized. isSui: $sui")
    }

    override fun onCreate() {
        super.onCreate()
        Sentry.configureScope { scope -> scope.level = SentryLevel.WARNING }
        Shizuku.addBinderReceivedListenerSticky(ShizukuStateModel)
        Shizuku.addBinderDeadListener(ShizukuStateModel)
        Shizuku.addRequestPermissionResultListener(ShizukuStateModel)
    }

    override fun onTerminate() {
        Shizuku.removeBinderReceivedListener(ShizukuStateModel)
        Shizuku.removeBinderDeadListener(ShizukuStateModel)
        Shizuku.removeRequestPermissionResultListener(ShizukuStateModel)
        shutdownServices()
        super.onTerminate()
    }


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
    }

    private fun shutdownServices() {
        runCatching {
            NetService.close()
        }
    }
}