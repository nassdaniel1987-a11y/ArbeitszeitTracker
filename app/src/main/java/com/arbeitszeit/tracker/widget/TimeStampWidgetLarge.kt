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
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

/**
 * Großes Widget (4x2) - Mit Wochenübersicht
 */
class TimeStampWidgetLarge : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "com.arbeitszeit.tracker.ACTION_START_LARGE"
        const val ACTION_END = "com.arbeitszeit.tracker.ACTION_END_LARGE"
        const val ACTION_PAUSE = "com.arbeitszeit.tracker.ACTION_PAUSE_LARGE"
        const val ACTION_MIDNIGHT_RESET = "com.arbeitszeit.tracker.ACTION_MIDNIGHT_RESET_LARGE"
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 1003
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scheduleMidnightReset(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMidnightReset(context)
    }

    override fun onDisabled(context: Context) {
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
            ACTION_MIDNIGHT_RESET -> {
                refreshWidget(context)
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
            android.content.ComponentName(context, TimeStampWidgetLarge::class.java)
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

            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp_large)

            // === TODAY DATA ===
            val startText = entry?.startZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"
            val endText = entry?.endZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"

            val istMinuten = entry?.getIstMinuten() ?: 0
            val durationHours = istMinuten / 60
            val durationMinutes = istMinuten % 60
            val durationText = String.format("%d:%02dh", durationHours, durationMinutes)

            val sollMinuten = entry?.sollMinuten ?: 480
            val progress = if (sollMinuten > 0) {
                ((istMinuten.toFloat() / sollMinuten.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            val isRunning = entry?.startZeit != null && entry.endZeit == null

            views.setTextViewText(R.id.widget_start_time_large, startText)
            views.setTextViewText(R.id.widget_end_time_large, endText)
            views.setTextViewText(R.id.widget_duration_large, durationText)
            views.setProgressBar(R.id.widget_progress_bar_large, 100, progress, false)

            val statusColor = if (isRunning) {
                context.getColor(R.color.widget_status_active)
            } else {
                context.getColor(R.color.widget_status_inactive)
            }
            views.setInt(R.id.widget_status_indicator_large, "setBackgroundColor", statusColor)

            // === WEEK DATA ===
            val currentDate = LocalDate.parse(today)
            val weekFields = WeekFields.of(Locale.GERMANY)
            val weekOfYear = currentDate.get(weekFields.weekOfWeekBasedYear())
            val year = currentDate.year

            // Get all entries for the current week
            val startOfWeek = currentDate.with(weekFields.dayOfWeek(), 1)
            val endOfWeek = currentDate.with(weekFields.dayOfWeek(), 7)

            val weekEntries = timeEntryDao.getEntriesByDateRange(
                startOfWeek.toString(),
                endOfWeek.toString()
            )

            val weekSoll = weekEntries.sumOf { it.sollMinuten }
            val weekIst = weekEntries.sumOf { it.getIstMinuten() }
            val weekDiff = weekIst - weekSoll

            val weekSollHours = weekSoll / 60
            val weekSollMinutes = weekSoll % 60
            val weekSollText = String.format("%d:%02dh", weekSollHours, weekSollMinutes)

            val weekIstHours = weekIst / 60
            val weekIstMinutes = weekIst % 60
            val weekIstText = String.format("%d:%02dh", weekIstHours, weekIstMinutes)

            val weekDiffText = TimeUtils.formatDifferenz(weekDiff)
            val weekDiffColor = when {
                weekDiff > 0 -> context.getColor(R.color.widget_progress)
                weekDiff < 0 -> context.getColor(R.color.widget_error)
                else -> context.getColor(R.color.widget_text)
            }

            views.setTextViewText(R.id.widget_week_soll, weekSollText)
            views.setTextViewText(R.id.widget_week_ist, weekIstText)
            views.setTextViewText(R.id.widget_week_diff, weekDiffText)
            views.setTextColor(R.id.widget_week_diff, weekDiffColor)

            // === BUTTON INTENTS ===
            val startIntent = Intent(context, TimeStampWidgetLarge::class.java).apply {
                action = ACTION_START
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context, 0, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button_large, startPendingIntent)

            val pauseIntent = Intent(context, TimeStampWidgetLarge::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_pause_button_large, pausePendingIntent)

            val endIntent = Intent(context, TimeStampWidgetLarge::class.java).apply {
                action = ACTION_END
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context, 1, endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_end_button_large, endPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun scheduleMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeStampWidgetLarge::class.java).apply {
            action = ACTION_MIDNIGHT_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeStampWidgetLarge::class.java).apply {
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
