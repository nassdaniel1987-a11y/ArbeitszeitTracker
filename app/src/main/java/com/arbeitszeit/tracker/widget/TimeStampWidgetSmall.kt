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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Kleines Widget (2x1) - Nur Quick-Stempel Button
 */
class TimeStampWidgetSmall : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_STAMP = "com.arbeitszeit.tracker.ACTION_QUICK_STAMP_SMALL"
        const val ACTION_MIDNIGHT_RESET = "com.arbeitszeit.tracker.ACTION_MIDNIGHT_RESET_SMALL"
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 1002
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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
                handleQuickStamp(context)
            }
            ACTION_MIDNIGHT_RESET -> {
                refreshWidget(context)
                scheduleMidnightReset(context)
            }
        }
    }

    /**
     * Quick Stamp Logic:
     * - Wenn keine Start-Zeit: Setze Start-Zeit
     * - Wenn Start-Zeit aber keine End-Zeit: Setze End-Zeit
     * - Wenn beides gesetzt: Setze neue Start-Zeit (neuer Tag)
     */
    private fun handleQuickStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()
            val currentLocalTime = java.time.LocalTime.now()
            val tracker = RunningTimeTracker(context)

            if (entry != null) {
                when {
                    entry.startZeit == null -> {
                        // Set start time
                        timeEntryDao.update(entry.copy(
                            startZeit = currentTime,
                            updatedAt = System.currentTimeMillis()
                        ))

                        tracker.startTracking(
                            startTime = currentLocalTime,
                            isAutoStart = false,
                            date = today
                        )
                    }
                    entry.endZeit == null -> {
                        // Statt im Hintergrund zu stoppen, App öffnen mit Dialog
                        // Da dies hier eine Coroutine ist und handleQuickStamp durch einen Intent aufgerufen wird,
                        // ist es etwas komplizierter.
                        // Aber Moment: handleQuickStamp wird durch ACTION_QUICK_STAMP aufgerufen.
                        // Wir müssen updateAppWidget ändern, damit der Button direkt die App öffnet wenn es läuft.
                    }
                    else -> {
                        // Reset to new start (new session)
                        timeEntryDao.update(entry.copy(
                            startZeit = currentTime,
                            endZeit = null,
                            updatedAt = System.currentTimeMillis()
                        ))

                        tracker.startTracking(
                            startTime = currentLocalTime,
                            isAutoStart = false,
                            date = today
                        )
                    }
                }
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
        try {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)

            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp_small)

            // Calculate duration
            val istMinuten = entry?.getIstMinuten() ?: 0
            val durationHours = istMinuten / 60
            val durationMinutes = istMinuten % 60
            val durationText = String.format("%d:%02dh", durationHours, durationMinutes)

            // Determine if work is running
            val isRunning = entry?.startZeit != null && entry.endZeit == null

            if (isRunning) {
                // Versuche exakten Start-Zeitpunkt aus SharedPreferences zu holen
                val prefs = context.getSharedPreferences("running_time_tracker", Context.MODE_PRIVATE)
                val startedAt = prefs.getLong("startedAt", 0L)

                if (startedAt > 0) {
                    views.setChronometer(
                        R.id.widget_chronometer_small,
                        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAt),
                        null,
                        true
                    )
                } else {
                    // Fallback auf DB
                    val startMinutes = entry?.startZeit ?: 0
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                        set(Calendar.MINUTE, startMinutes % 60)
                        set(Calendar.SECOND, 0)
                    }
                    val startTimeMillis = calendar.timeInMillis

                    views.setChronometer(
                        R.id.widget_chronometer_small,
                        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTimeMillis),
                        null,
                        true
                    )
                }

                views.setViewVisibility(R.id.widget_chronometer_small, View.VISIBLE)
                views.setViewVisibility(R.id.widget_duration_small, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_chronometer_small, View.GONE)
                views.setViewVisibility(R.id.widget_duration_small, View.VISIBLE)
            }

            // Set duration text
            views.setTextViewText(R.id.widget_duration_small, durationText)

            // Set quick stamp button intent
            if (isRunning) {
                // Wenn läuft -> App öffnen zum Stoppen
                val stopIntent = Intent(context, MainActivity::class.java).apply {
                    action = "com.arbeitszeit.tracker.ACTION_STOP_FROM_WIDGET"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val stopPendingIntent = PendingIntent.getActivity(
                    context, 0, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_quick_stamp_button, stopPendingIntent)
            } else {
                // Wenn nicht läuft -> Normaler QuickStamp (Start)
                val quickStampIntent = Intent(context, TimeStampWidgetSmall::class.java).apply {
                    action = ACTION_QUICK_STAMP
                }
                val quickStampPendingIntent = PendingIntent.getBroadcast(
                    context, 0, quickStampIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_quick_stamp_button, quickStampPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            // Fallback: Zeige Fehler-Widget
            val views = RemoteViews(context.packageName, R.layout.widget_time_stamp_small)
            views.setTextViewText(R.id.widget_duration_small, "ERROR")
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
