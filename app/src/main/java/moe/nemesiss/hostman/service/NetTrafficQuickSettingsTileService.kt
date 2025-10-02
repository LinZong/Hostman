package moe.nemesiss.hostman.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import moe.nemesiss.hostman.boost.EasyDebug

class NetTrafficQuickSettingsTileService : TileService() {

    private val TAG = "NetTrafficQsTS"

    private var receiverRegistered = false
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != NetTrafficService.ACTION_NET_TRAFFIC_STATE) return
            val running = intent.getBooleanExtra(NetTrafficService.EXTRA_RUNNING, false)
            val tile = qsTile ?: return
            tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
            EasyDebug.warn(TAG) { "Receiver updated tile. running=$running" }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        EasyDebug.info(TAG) { "onTileAdded" }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        EasyDebug.warn(TAG) { "onTileRemoved" }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(this,
                                           stateReceiver,
                                           IntentFilter(NetTrafficService.ACTION_NET_TRAFFIC_STATE),
                                           ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        val running = NetTrafficService.isRunning()
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, moe.nemesiss.hostman.R.drawable.speed_24px)
        tile.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isActive = tile.state == Tile.STATE_ACTIVE
        if (isActive) {
            NetTrafficService.stop(this)
        } else {
            NetTrafficService.ensurePermissionAndStart(this)
        }
        tile.updateTile()
    }
}
