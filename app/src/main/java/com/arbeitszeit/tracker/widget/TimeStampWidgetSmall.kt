package com.arbeitszeit.tracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.Keep
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Kleines Widget (2x1) - Quick-Stempel mit Start/Pause/Stop Buttons
 *
 * Features:
 * - Live-Timer wenn Arbeit läuft (Chronometer)
 * - Status-Indikator (grün = läuft, grau = gestoppt)
 * - Start-Button: Startet Zeiterfassung
 * - Pause-Button: Fügt 30 Minuten Pause hinzu
 * - Stop-Button: Öffnet App zum Beenden
 */
@Keep
class TimeStampWidgetSmall : AppWidgetProvider() {

    companion object {
        private const val TAG = "TimeStampWidgetSmall"
        const val ACTION_QUICK_STAMP = "com.arbeitszeit.tracker.ACTION_QUICK_STAMP_SMALL"
        const val ACTION_START = "com.arbeitszeit.tracker.ACTION_START_SMALL"
        const val ACTION_PAUSE = "com.arbeitszeit.tracker.ACTION_PAUSE_SMALL"
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH_SMALL"
        const val ACTION_MIDNIGHT_RESET = "com.arbeitszeit.tracker.ACTION_MIDNIGHT_RESET_SMALL"
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 1002

        /**
         * Statische Hilfsmethode um alle Widgets zu aktualisieren
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TimeStampWidgetSmall::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i(TAG, "onUpdate() called for ${appWidgetIds.size} widgets: ${appWidgetIds.toList()}")
        scheduleMidnightReset(context)

        // Use goAsync() to allow async operations
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onUpdate coroutine", e)
            } finally {
                pendingResult.finish()
            }
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
            ACTION_QUICK_STAMP -> {
                // Deprecated single button logic, keeping for potential fallback
                handleStart(context)
            }
            ACTION_START -> {
                handleStart(context)
            }
            ACTION_PAUSE -> {
                handlePause(context)
            }
            ACTION_REFRESH, ACTION_MIDNIGHT_RESET -> {
                refreshWidget(context)
                if (intent.action == ACTION_MIDNIGHT_RESET) {
                    scheduleMidnightReset(context)
                }
            }
        }
    }

    private fun handleStart(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()
            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()
            val currentLocalTime = java.time.LocalTime.now()
            val tracker = RunningTimeTracker(context)

            if (entry != null) {
                // Wenn noch kein Start -> Start setzen
                // Wenn Ende schon gesetzt -> Neuer Start (Reset)
                // Wenn Start aber kein Ende -> Nichts tun (läuft schon)
                if (entry.startZeit == null || (entry.startZeit != null && entry.endZeit != null)) {
                     timeEntryDao.update(entry.copy(
                        startZeit = currentTime,
                        endZeit = null, // Reset End if exists
                        updatedAt = System.currentTimeMillis()
                    ))

                    tracker.startTracking(
                        startTime = currentLocalTime,
                        isAutoStart = false,
                        date = today
                    )
                }
            }
            refreshWidget(context)
        }
    }

    private fun handlePause(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val today = DateUtils.today()
            val entry = database.timeEntryDao().getEntryByDate(today)

            if (entry != null) {
                val currentPause = entry.pauseMinuten
                database.timeEntryDao().update(entry.copy(
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
            android.content.ComponentName(context, TimeStampWidgetSmall::class.java)
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
        Log.d(TAG, "updateAppWidget() called for widgetId: $appWidgetId")

        try {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            Log.d(TAG, "Today: $today, Entry: $entry")

            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp_small)
            Log.d(TAG, "RemoteViews created successfully")

            // Calculate duration
            val istMinuten = entry?.getIstMinuten() ?: 0
            val durationHours = istMinuten / 60
            val durationMinutes = istMinuten % 60
            val durationText = String.format("%d:%02dh", durationHours, durationMinutes)

            // Determine if work is running
            val isRunning = entry?.startZeit != null && entry.endZeit == null

            // 1. Status Indicator
            if (isRunning) {
                views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_active)
            } else {
                views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_inactive)
            }

            // 2. Chronometer vs Static Text
            if (isRunning) {
                val prefs = context.getSharedPreferences("running_time_tracker", Context.MODE_PRIVATE)
                val startedAt = prefs.getLong("startedAt", 0L)
                val baseTime = if (startedAt > 0) SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAt) else SystemClock.elapsedRealtime()

                views.setViewVisibility(R.id.widget_chronometer_small, View.VISIBLE)
                views.setViewVisibility(R.id.widget_duration_small, View.GONE)
                views.setChronometer(R.id.widget_chronometer_small, baseTime, null, true)
            } else {
                views.setViewVisibility(R.id.widget_chronometer_small, View.GONE)
                views.setViewVisibility(R.id.widget_duration_small, View.VISIBLE)
                views.setTextViewText(R.id.widget_duration_small, durationText)
            }

            // 3. Button Intents
            // Start
            val startIntent = Intent(context, TimeStampWidgetSmall::class.java).apply { action = ACTION_START }
            views.setOnClickPendingIntent(R.id.widget_btn_start, PendingIntent.getBroadcast(
                context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            // Pause
            val pauseIntent = Intent(context, TimeStampWidgetSmall::class.java).apply { action = ACTION_PAUSE }
            views.setOnClickPendingIntent(R.id.widget_btn_pause, PendingIntent.getBroadcast(
                context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            // Stop (Open App Dialog)
            if (isRunning) {
                val stopIntent = Intent(context, MainActivity::class.java).apply {
                    action = "com.arbeitszeit.tracker.ACTION_STOP_FROM_WIDGET"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                views.setOnClickPendingIntent(R.id.widget_btn_stop, PendingIntent.getActivity(
                    context, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
            } else {
                // If not running, stop button just opens app normally
                val openIntent = Intent(context, MainActivity::class.java)
                views.setOnClickPendingIntent(R.id.widget_btn_stop, PendingIntent.getActivity(
                    context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $appWidgetId", e)
            // Fallback: Versuche Fehler-Widget anzuzeigen
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_time_stamp_small)
                views.setTextViewText(R.id.widget_duration_small, "Fehler")
                views.setViewVisibility(R.id.widget_chronometer_small, View.GONE)
                views.setViewVisibility(R.id.widget_duration_small, View.VISIBLE)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e2: Exception) {
                Log.e(TAG, "Even fallback widget failed", e2)
            }
        }
    }

    private fun scheduleMidnightReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeStampWidgetSmall::class.java).apply {
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
        val intent = Intent(context, TimeStampWidgetSmall::class.java).apply {
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
