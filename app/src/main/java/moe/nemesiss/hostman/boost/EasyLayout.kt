package moe.nemesiss.hostman.boost

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.view.WindowInsets
import androidx.core.net.toUri
import androidx.core.util.TypedValueCompat

object EasyLayout {


    fun getSystemBarTopHeight(insets: WindowInsets): Int {
        return if (VERSION.SDK_INT >= VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.systemBars()).top
        } else {
            insets.systemWindowInsetTop
        }
    }

    fun Context.dpToPx(dp: Float): Int {
        return TypedValueCompat.dpToPx(dp, resources.displayMetrics).toInt()
    }

    fun openOverlayPermissionSetting(ctx: Context): Boolean {
        if (!(Settings.canDrawOverlays(ctx))) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${ctx.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            EasyIntent.startActivity(ctx, intent)
            return true
        }
        return false
    }
}