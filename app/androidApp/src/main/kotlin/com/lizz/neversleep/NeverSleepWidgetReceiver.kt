package com.lizz.neversleep

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class NeverSleepWidgetReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE = "com.lizz.neversleep.ACTION_TOGGLE_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val success = NeverSleepController.toggle(context)
            if (!success && !NeverSleepController.isEnabled(context)) {
                // Permission issue - open main activity (user can grant from there)
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(launchIntent)
            }
            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NeverSleepWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_root)
            }
            val provider = NeverSleepWidgetProvider()
            provider.onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}