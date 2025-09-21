package moe.nemesiss.hostman.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class NetTrafficQuickSettingsTileService : TileService() {

    private val TAG = "NetTrafficQsTS"

    override fun onTileAdded() {
        super.onTileAdded()
        Log.w(TAG, "onTileAdded")
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.w(TAG, "onTileRemoved")
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.w(TAG, "onStartListening begin")
        val running = getSharedPreferences(NetTrafficService.PREF_NAME,
                                           MODE_PRIVATE).getBoolean(NetTrafficService.PREF_KEY_RUNNING, false)
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // Optional: set icon/label for better UX in the tile panel
        tile.icon = Icon.createWithResource(this, moe.nemesiss.hostman.R.drawable.speed_24px)
        tile.updateTile()
        Log.w(TAG, "onStartListening end")
    }

    override fun onClick() {
        super.onClick()
        Log.w(TAG, "onClick begin")
        val tile = qsTile ?: return
        val isActive = tile.state == Tile.STATE_ACTIVE
        if (isActive) {
            NetTrafficService.stop(this)
            tile.state = Tile.STATE_INACTIVE
        } else {
            NetTrafficService.start(this)
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()
        Log.w(TAG, "onClick end")
    }
}
