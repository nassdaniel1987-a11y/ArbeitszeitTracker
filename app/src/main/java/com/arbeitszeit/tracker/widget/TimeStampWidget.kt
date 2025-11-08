package com.arbeitszeit.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget für schnelles Zeit-Stempeln ohne App zu öffnen
 */
class TimeStampWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "com.arbeitszeit.tracker.ACTION_START"
        const val ACTION_END = "com.arbeitszeit.tracker.ACTION_END"
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_START -> {
                handleStartStamp(context)
            }
            ACTION_END -> {
                handleEndStamp(context)
            }
            ACTION_REFRESH -> {
                refreshWidget(context)
            }
        }
    }

    private fun handleStartStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()

            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    startZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            refreshWidget(context)
        }
    }

    private fun handleEndStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()

            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    endZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            refreshWidget(context)
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, TimeStampWidget::class.java)
        )

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)

            val startText = entry?.startZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"
            val endText = entry?.endZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"

            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp)

            // Setze Zeitanzeigen
            views.setTextViewText(R.id.widget_start_time, startText)
            views.setTextViewText(R.id.widget_end_time, endText)

            // Setze Button-Intents
            val startIntent = Intent(context, TimeStampWidget::class.java).apply {
                action = ACTION_START
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context, 0, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)

            val endIntent = Intent(context, TimeStampWidget::class.java).apply {
                action = ACTION_END
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context, 1, endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_end_button, endPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
