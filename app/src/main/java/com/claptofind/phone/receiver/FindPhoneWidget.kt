package com.claptofind.phone.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.R
import com.claptofind.phone.service.DetectionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FindPhoneWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleToggle(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun handleToggle(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val app = context.applicationContext as ClapToFindApp
        runBlocking {
            val current = app.prefsManager.isDetectionEnabled.first()
            val newState = !current
            app.prefsManager.setDetectionEnabled(newState)
            if (newState) DetectionService.start(context) else DetectionService.stop(context)
        }
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val app = context.applicationContext as ClapToFindApp
        val isEnabled = runBlocking { app.prefsManager.isDetectionEnabled.first() }

        val views = RemoteViews(context.packageName, R.layout.widget_find_phone)
        views.setTextViewText(
            R.id.widget_status,
            if (isEnabled) "Status: Activated" else "Status: Deactivated"
        )
        views.setTextViewText(R.id.widget_title, "Clap To Find")

        val toggleIntent = Intent(context, FindPhoneWidget::class.java).apply {
            action = ACTION_WIDGET_TOGGLE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.claptofind.phone.WIDGET_TOGGLE"
    }
}
