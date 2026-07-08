package com.lizz.neversleep

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class NeverSleepWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.never_sleep_widget)
        val enabled = NeverSleepController.isEnabled(context)
        views.setImageViewResource(
            R.id.widget_icon,
            if (enabled) R.drawable.ic_moon else R.drawable.ic_sun
        )
        // Click handled by the receiver
        val intent = android.content.Intent(context, NeverSleepWidgetReceiver::class.java).apply {
            action = NeverSleepWidgetReceiver.ACTION_TOGGLE
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}