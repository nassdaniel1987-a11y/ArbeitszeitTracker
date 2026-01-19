package com.arbeitszeit.tracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import android.util.Log
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
        Log.d("TimeStampWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        // Schedule midnight reset (in case it wasn't set up or got cancelled)
        scheduleMidnightReset(context)

        // Use goAsync() to allow async operations
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
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
        Log.d("TimeStampWidget", "onReceive: ${intent.action}")

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
            val currentLocalTime = java.time.LocalTime.now()

            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    startZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))

                // RunningTimeTracker aktualisieren
                val tracker = RunningTimeTracker(context)
                tracker.startTracking(
                    startTime = currentLocalTime,
                    isAutoStart = false,
                    date = today
                )
            }

            refreshWidget(context)
        }
    }

    private fun handleEndStamp(context: Context) {
        // Nicht mehr im Hintergrund stoppen, sondern App öffnen mit Dialog
        // Die Logik wird jetzt durch den PendingIntent auf dem Button gesteuert
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

        CoroutineScope(Dispatchers.IO).launch {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()
            val userSettingsDao = database.userSettingsDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val settings = userSettingsDao.getSettings()

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
            val sollMinuten = entry?.sollMinuten ?: 480 // Default 8h
            val progress = if (sollMinuten > 0) {
                ((istMinuten.toFloat() / sollMinuten.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Determine if work is running
            val isRunning = entry?.startZeit != null && entry.endZeit == null

            // LIVE Anzeige Logik
            if (isRunning) {
                // Versuche exakten Start-Zeitpunkt aus SharedPreferences zu holen
                val prefs = context.getSharedPreferences("running_time_tracker", Context.MODE_PRIVATE)
                val startedAt = prefs.getLong("startedAt", 0L)

                if (startedAt > 0) {
                    // Verwende Chronometer für exakte Live-Anzeige
                    // Base ist die Zeit, bei der der Timer auf 0 wäre
                    views.setChronometer(
                        R.id.widget_chronometer,
                        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAt),
                        null,
                        true
                    )
                } else {
                    // Fallback auf Datenbank-Zeit (Minuten-genau)
                    val startMinutes = entry?.startZeit ?: 0
                    // Berechne Millisekunden für heute Startzeit
                    // Achtung: Das ist ungenau, da Sekunden fehlen
                    // Aber besser als nichts
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                        set(Calendar.MINUTE, startMinutes % 60)
                        set(Calendar.SECOND, 0)
                    }
                    val startTimeMillis = calendar.timeInMillis

                    views.setChronometer(
                        R.id.widget_chronometer,
                        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTimeMillis),
                        null,
                        true
                    )
                }

                // UI umschalten: Chronometer AN, Statischer Text AUS
                views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_duration, View.GONE)
            } else {
                // UI umschalten: Chronometer AUS, Statischer Text AN
                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_duration, View.VISIBLE)
            }

            // Set views
            views.setTextViewText(R.id.widget_start_time, startText)
            views.setTextViewText(R.id.widget_end_time, endText)
            views.setTextViewText(R.id.widget_duration, durationText)
            views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

            // Set status indicator image
            if (isRunning) {
                views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_active)
            } else {
                views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_inactive)
            }

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

            // Ende-Button Logik:
            // Wenn Tracking läuft -> App öffnen mit Stop-Dialog
            // Wenn Tracking nicht läuft -> Button deaktiviert oder normaler Intent (hier: normaler End-Stempel Logic für manuellen Nachtrag?)
            // Der User möchte "wenn ich stop drücke geht fenster auf".
            // Also immer App öffnen wenn Stop gedrückt wird, sofern es Sinn macht (d.h. Tracking läuft).

            if (isRunning) {
                val stopIntent = Intent(context, MainActivity::class.java).apply {
                    action = "com.arbeitszeit.tracker.ACTION_STOP_FROM_WIDGET"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val stopPendingIntent = PendingIntent.getActivity(
                    context, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_end_button, stopPendingIntent)
            } else {
                // Fallback: Wenn nicht läuft, vielleicht manuell Ende setzen?
                // Lassen wir die alte Logik für Konsistenz (Nachtrag)
                val endIntent = Intent(context, TimeStampWidget::class.java).apply {
                    action = ACTION_END
                }
                val endPendingIntent = PendingIntent.getBroadcast(
                    context, 1, endIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_end_button, endPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            // Fallback: Zeige Fehler-Widget
            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp)
            views.setTextViewText(R.id.widget_start_time, "ERROR")
            views.setTextViewText(R.id.widget_end_time, "ERROR")
            views.setTextViewText(R.id.widget_duration, "0:00h")
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
