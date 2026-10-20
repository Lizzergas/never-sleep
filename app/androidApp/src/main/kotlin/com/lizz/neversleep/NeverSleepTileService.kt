package com.lizz.neversleep

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class NeverSleepTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val desired = !NeverSleepController.isEnabled(this)
        val success = if (desired) {
            NeverSleepController.setNeverSleep(this)
        } else {
            NeverSleepController.setNormal(this)
        }
        if (!success && !Settings.System.canWrite(this)) {
            // Explain the special access in-app before opening Android Settings.
            val intent = Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_REQUEST_WRITE_SETTINGS, true)
                .putExtra(MainActivity.EXTRA_DESIRED_ENABLED, desired)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivityAndCollapseCompat(intent)
        }
        updateTile()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = NeverSleepController.isEnabled(this)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.keep_screen_on)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (enabled) R.string.mode_on else R.string.mode_off)
        }
        tile.contentDescription = getString(
            if (enabled) R.string.mode_on_description else R.string.mode_off_description,
        )
        tile.icon = Icon.createWithResource(this, if (enabled) R.drawable.ic_sun else R.drawable.ic_moon)
        tile.updateTile()
    }
}
