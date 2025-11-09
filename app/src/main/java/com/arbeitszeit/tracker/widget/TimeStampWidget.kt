package com.arbeitszeit.tracker.widget

import android.app.AlarmManager
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
import java.util.Calendar

/**
 * Widget für schnelles Zeit-Stempeln ohne App zu öffnen
 */
class TimeStampWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "com.arbeitszeit.tracker.ACTION_START"
        const val ACTION_END = "com.arbeitszeit.tracker.ACTION_END"
        const val ACTION_PAUSE = "com.arbeitszeit.tracker.ACTION_PAUSE"
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH"
        const val ACTION_MIDNIGHT_RESET = "com.arbeitszeit.tracker.ACTION_MIDNIGHT_RESET"
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 1001
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Schedule midnight reset (in case it wasn't set up or got cancelled)
        scheduleMidnightReset(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is added
        super.onEnabled(context)
        scheduleMidnightReset(context)
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
        super.onDisabled(context)
        cancelMidnightReset(context)
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
            ACTION_PAUSE -> {
                handlePauseStamp(context)
            }
            ACTION_REFRESH -> {
                refreshWidget(context)
            }
            ACTION_MIDNIGHT_RESET -> {
                // Reset widget for new day
                refreshWidget(context)
                // Schedule next midnight reset
                scheduleMidnightReset(context)
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

    private fun handlePauseStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)

            if (entry != null) {
                val currentPause = entry.pauseMinuten ?: 0
                // Add 30 minutes pause
                timeEntryDao.update(entry.copy(
                    pauseMinuten = currentPause + 30,
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
            val userSettingsDao = database.userSettingsDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val settings = userSettingsDao.getUserSettings()

            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp)

            // Calculate times
            val startText = entry?.startZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"
            val endText = entry?.endZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"

            // Calculate duration
            val istMinuten = entry?.getIstMinuten() ?: 0
            val durationHours = istMinuten / 60
            val durationMinutes = istMinuten % 60
            val durationText = String.format("%d:%02dh", durationHours, durationMinutes)

            // Calculate progress (to Soll-Zeit)
            val sollMinuten = entry?.getSollMinuten(settings) ?: 480 // Default 8h
            val progress = if (sollMinuten > 0) {
                ((istMinuten.toFloat() / sollMinuten.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Determine if work is running
            val isRunning = entry?.startZeit != null && entry.endZeit == null

            // Set views
            views.setTextViewText(R.id.widget_start_time, startText)
            views.setTextViewText(R.id.widget_end_time, endText)
            views.setTextViewText(R.id.widget_duration, durationText)
            views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

            // Set status indicator color
            val statusColor = if (isRunning) {
                context.getColor(R.color.widget_status_active)
            } else {
                context.getColor(R.color.widget_status_inactive)
            }
            views.setInt(R.id.widget_status_indicator, "setBackgroundColor", statusColor)

            // Set button intents
            val startIntent = Intent(context, TimeStampWidget::class.java).apply {
                action = ACTION_START
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context, 0, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)

            val pauseIntent = Intent(context, TimeStampWidget::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_pause_button, pausePendingIntent)

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

    /**
     * Schedules a daily alarm to reset the widget at midnight
     */
    private fun scheduleMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeStampWidget::class.java).apply {
            action = ACTION_MIDNIGHT_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next midnight
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If it's already past midnight today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Use setRepeating for daily reset at midnight
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Cancels the midnight reset alarm
     */
    private fun cancelMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeStampWidget::class.java).apply {
            action = ACTION_MIDNIGHT_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
