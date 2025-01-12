package moe.nemesiss.hostman.debug

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import io.sentry.android.core.performance.ActivityLifecycleCallbacksAdapter
import moe.nemesiss.hostman.R
import moe.nemesiss.hostman.boost.EasyLayout.dpToPx
import moe.nemesiss.hostman.debug.activity.DebugActivity

class DebugEntranceRegisterCallback : ActivityLifecycleCallbacksAdapter() {

    private val TAG = "DebugEntranceRegisterCallback"

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPostCreated(activity, savedInstanceState)
        attachDebugEntrance(activity)
    }

    private fun attachDebugEntrance(activity: Activity) {

        if (activity is DebugActivity) {
            return
        }

        val decorView = activity.window.decorView as? FrameLayout ?: return


        if (decorView.findViewById<View>(R.id.debug_entrance) != null) {
            return
        }

        val debugEntranceView = LayoutInflater.from(activity).inflate(R.layout.debug_entrance, decorView, false)


        val params = FrameLayout.LayoutParams(activity.dpToPx(48f),
                                              activity.dpToPx(48f),
                                              Gravity.BOTTOM or Gravity.END)

        val baseMarginBottom = activity.dpToPx(96f)
        val baseMarginEnd = activity.dpToPx(16f)
        params.setMargins(0, 0, baseMarginEnd, baseMarginBottom)
        Log.i(TAG, "Attaching debug entrance to activity: ${activity.javaClass.simpleName}")

        decorView.addView(debugEntranceView, params)


        debugEntranceView.setOnClickListener {
            activity.startDebugActivity()
        }
    }

    private fun Context.startDebugActivity() {
        startActivity(Intent(this, DebugActivity::class.java))
    }
}