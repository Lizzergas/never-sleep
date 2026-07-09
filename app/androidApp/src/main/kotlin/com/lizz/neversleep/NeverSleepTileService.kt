package com.lizz.neversleep

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class NeverSleepTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        // Toggle using the shared controller (handles permission checks internally)
        val wasEnabled = NeverSleepController.isEnabled(this)
        val success = NeverSleepController.toggle(this)
        if (!success && !NeverSleepController.isEnabled(this)) {
            // Permission missing - open the settings screen
            val intent = android.content
                .Intent(
                    android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:$packageName"),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = NeverSleepController.isEnabled(this)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (enabled) "Never Sleep" else "Normal Sleep"
        // Use a simple icon (we'll add drawables)
        tile.icon = Icon.createWithResource(this, if (enabled) R.drawable.ic_moon else R.drawable.ic_sun)
        tile.updateTile()
    }
}
