package com.safelivebridge.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.safelivebridge.app.data.BridgePreferences

class LiveBridgeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = BridgePreferences.getInstance(applicationContext)
        val newState = !prefs.isBridgeEnabled.value
        prefs.setBridgeEnabled(newState)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = BridgePreferences.getInstance(applicationContext)
        val isEnabled = prefs.isBridgeEnabled.value

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "LiveBridge"
        tile.subtitle = if (isEnabled) "Активен" else "Выключен"
        tile.updateTile()
    }
}
