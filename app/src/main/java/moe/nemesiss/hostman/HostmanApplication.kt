package moe.nemesiss.hostman

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import moe.nemesiss.hostman.boost.EasyProcess
import moe.nemesiss.hostman.crashhandler.HostmanCrashHandler
import moe.nemesiss.hostman.model.viewmodel.ShizukuStateModel
import moe.nemesiss.hostman.service.NetService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui

open class HostmanApplication : Application(), SingletonImageLoader.Factory {

    private val TAG = "HostmanApplication"

    val sui: Boolean

    init {
        sui = Sui.init(BuildConfig.APPLICATION_ID)
        Log.w("HostmanApplication", "Sui had been initialized. isSui: $sui")
    }

    override fun onCreate() {
        super.onCreate()
        Shizuku.addBinderReceivedListenerSticky(ShizukuStateModel)
        Shizuku.addBinderDeadListener(ShizukuStateModel)
        Shizuku.addRequestPermissionResultListener(ShizukuStateModel)
        EasyProcess.logPid("HostmanApplication")
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

        val appContext = base ?: return

        registerUncaughtExceptionHandler(appContext)
    }


    private fun shutdownServices() {
        runCatching {
            NetService.close()
        }
    }

    private fun registerUncaughtExceptionHandler(appContext: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Log.w(TAG, "Default uncaught handler is: ${defaultHandler?.javaClass?.simpleName}")
        Thread.setDefaultUncaughtExceptionHandler(HostmanCrashHandler(appContext, defaultHandler))
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}