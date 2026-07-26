package com.lizz.neversleep

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

class NeverSleepWidgetReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE = "com.lizz.neversleep.ACTION_TOGGLE_WIDGET"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_TOGGLE) {
            val desired = !NeverSleepController.isEnabled(context)
            val success = if (desired) {
                NeverSleepController.setNeverSleep(context)
            } else {
                NeverSleepController.setNormal(context)
            }
            if (!success && !Settings.System.canWrite(context)) {
                // Explain the permission in-app and preserve the user's intended state.
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_REQUEST_WRITE_SETTINGS, true)
                    putExtra(MainActivity.EXTRA_DESIRED_ENABLED, desired)
                }
                context.startActivity(launchIntent)
            }
            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NeverSleepWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val provider = NeverSleepWidgetProvider()
            provider.onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
