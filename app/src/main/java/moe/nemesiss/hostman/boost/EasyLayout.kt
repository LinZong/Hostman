package moe.nemesiss.hostman.boost

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.WindowInsets
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
}