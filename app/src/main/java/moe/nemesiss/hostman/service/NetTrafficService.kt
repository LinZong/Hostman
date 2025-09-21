package moe.nemesiss.hostman.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.AtomicReference
import kotlinx.coroutines.*
import moe.nemesiss.hostman.boost.EasyNotification
import moe.nemesiss.hostman.databinding.NetworkTrafficFloatingViewBinding
import moe.nemesiss.hostman.ui.NetworkSpeed


class NetTrafficService : Service() {

    private class NetTrafficStatsModel {
        private var wlan: Long? = null
        private var mobile: Long? = null
        private var prevWlan: Long? = null
        private var prevMobile: Long? = null


        suspend fun refresh(): Boolean {
            return withContext(Dispatchers.IO) {
                try {

                    prevWlan = wlan
                    wlan = TrafficStats.getTxBytes(WLAN_IF_NAME) + TrafficStats.getRxBytes(WLAN_IF_NAME)

                    prevMobile = mobile

                    mobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()

                    true
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to refresh network stats.", t)
                    false
                }
            }
        }

        fun getWlanRxTxBytes(): Long? {
            val current = wlan ?: return null
            val prev = prevWlan ?: return null
            return current - prev
        }

        fun getMobileDataRxTxBytes(): Long? {
            val current = mobile ?: return null
            val prev = prevMobile ?: return null
            return current - prev
        }

        fun reset() {
            wlan = null
            mobile = null
            prevWlan = null
            prevMobile = null
        }
    }

    enum class State {
        IDLE,
        STARTING,
        STARTED,
        STOPPING,
    }

    companion object {
        const val WLAN_IF_NAME = "wlan0"
        const val TAG = "NetTrafficService"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val REFRESH_INTERVALS = 2000L
        fun start(ctx: Context) {
            ctx.startService(createIntent(ctx))
        }

        fun stop(ctx: Context) {
            ctx.startService(createIntent(ctx, false))
        }

        private fun createIntent(ctx: Context, start: Boolean = true): Intent {
            val intent = Intent(ctx, NetTrafficService::class.java)
            if (start) {
                intent.action = ACTION_START
            } else {
                intent.action = ACTION_STOP
            }
            return intent
        }
    }


    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var refreshJob: Job? = null

    private var windowManager: WindowManager? = null

    private var floatingViewBinding: NetworkTrafficFloatingViewBinding? = null

    private val state = AtomicReference<State>(State.IDLE)


    private val model = NetTrafficStatsModel()


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        EasyNotification.ensureNetTrafficNotificationChannel(this)
        this.windowManager = getSystemService<WindowManager>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            startNetTrafficMonitor()
        }
        if (intent?.action == ACTION_STOP) {
            stopNetTrafficMonitor()
        }
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }


    private fun startNetTrafficMonitor() {
        if (state.compareAndSet(State.IDLE, State.STARTING)) {
            Toast.makeText(this, "Starting Net Traffic Monitor", Toast.LENGTH_SHORT).show()
            onNetTrafficMonitorStarted()
        }
    }

    private fun stopNetTrafficMonitor() {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            Toast.makeText(this, "Stopping Net Traffic Monitor", Toast.LENGTH_SHORT).show()
            onNetTrafficMonitorStopped()
        }
    }

    private fun onNetTrafficMonitorStarted() {
        createFloatingView()
        startRefreshJob()
        state.set(State.STARTED)
        EasyNotification.notifyNetTrafficServiceRunning(this)
    }

    private fun onNetTrafficMonitorStopped() {
        cleanup()
        state.set(State.IDLE)
        stopSelf()
    }


    private fun startRefreshJob() {
        cancelPreviousJob()
        model.reset()
        this.refreshJob = scope.launch {
            while (true) {
                delay(REFRESH_INTERVALS)
                if (model.refresh()) {
                    updateWlanStats(model.getWlanRxTxBytes())
                    updateMobileDataStats(model.getMobileDataRxTxBytes())
                }
            }
        }
    }

    private fun updateWlanStats(rxtxBytes: Long?) {
        Log.w(TAG, "Update wlan stats: $rxtxBytes")
        val wlan = floatingViewBinding?.wlanSpeed ?: return
        if (rxtxBytes == null) {
            wlan.text = ""
        } else {
            wlan.text = NetworkSpeed.format(rxtxBytes / (REFRESH_INTERVALS / 1000))
        }
    }

    private fun updateMobileDataStats(rxtxBytes: Long?) {
        Log.w(TAG, "Update mobile data stats: $rxtxBytes")
        val mobileData = floatingViewBinding?.mobileSpeed ?: return
        if (rxtxBytes == null) {
            mobileData.text = ""
        } else {
            mobileData.text = NetworkSpeed.format(rxtxBytes / (REFRESH_INTERVALS / 1000))
        }
    }


    private fun createFloatingView() {
        val wm = this.windowManager ?: return
        removeViewIfNecessary()

        val params = createLayoutParams()
        val binding = NetworkTrafficFloatingViewBinding.inflate(LayoutInflater.from(this))
        val view = binding.root
        wm.addView(view, params)
        this.floatingViewBinding = binding

        // Enable drag to move behavior
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchStartX).toInt()
                        val dy = (event.rawY - touchStartY).toInt()
                        params.x = initialX + dx
                        params.y = initialY + dy
                        wm.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        val closeWindow = view.findViewById<ImageView>(moe.nemesiss.hostman.R.id.close_window)
        closeWindow?.setOnClickListener { stopNetTrafficMonitor() }
    }

    private fun cleanup() {
        cancelPreviousJob()
        removeViewIfNecessary()
        EasyNotification.removeNetTrafficServiceRunningNotification(this)
    }

    private fun cancelPreviousJob() {
        if (this.refreshJob != null && this.refreshJob?.isActive == true) {
            this.refreshJob?.cancel()
            this.refreshJob = null
        }
    }

    private fun removeViewIfNecessary() {
        if (this.floatingViewBinding != null) {
            this.windowManager?.removeView(this.floatingViewBinding?.root)
            this.floatingViewBinding = null
        }
    }


    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.START
        return params
    }
}