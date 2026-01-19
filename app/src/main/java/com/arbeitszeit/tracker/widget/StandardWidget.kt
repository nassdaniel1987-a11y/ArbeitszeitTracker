package com.arbeitszeit.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Standard Widget (4x2)
 * Bietet Start/Pause/Stop Funktionalität und Live-Zeitanzeige.
 */
class StandardWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH_STANDARD"
        const val ACTION_START = "com.arbeitszeit.tracker.ACTION_START_STANDARD"
        const val ACTION_PAUSE = "com.arbeitszeit.tracker.ACTION_PAUSE_STANDARD"
        // Stop wird über Intent direkt zur App gehandhabt
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Initiales Update
        performUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle Custom Actions
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, StandardWidget::class.java))
                performUpdate(context, appWidgetManager, ids)
            }
            ACTION_START -> {
                handleStart(context)
            }
            ACTION_PAUSE -> {
                handlePause(context)
            }
        }
    }

    private fun handleStart(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val today = DateUtils.today()
            val entry = database.timeEntryDao().getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()

            // Logik: Startzeit setzen wenn noch nicht vorhanden
            if (entry != null) {
                database.timeEntryDao().update(entry.copy(
                    startZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))

                // Running Tracker starten
                val tracker = RunningTimeTracker(context)
                tracker.startTracking(
                    startTime = java.time.LocalTime.now(),
                    isAutoStart = false,
                    date = today
                )
            }

            // Trigger Refresh
            val intent = Intent(context, StandardWidget::class.java)
            intent.action = ACTION_REFRESH
            context.sendBroadcast(intent)
        }
    }

    private fun handlePause(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val today = DateUtils.today()
            val entry = database.timeEntryDao().getEntryByDate(today)

            if (entry != null) {
                // Pause um 30 min erhöhen (einfache Logik wie zuvor)
                val currentPause = entry.pauseMinuten
                database.timeEntryDao().update(entry.copy(
                    pauseMinuten = currentPause + 30,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            // Trigger Refresh
            val intent = Intent(context, StandardWidget::class.java)
            intent.action = ACTION_REFRESH
            context.sendBroadcast(intent)
        }
    }

    private fun performUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Asynchron Daten laden
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val today = DateUtils.today()
            val entry = database.timeEntryDao().getEntryByDate(today)

            // Prüfen ob Tracking läuft (Start gesetzt, Ende noch nicht)
            val isRunning = entry?.startZeit != null && entry.endZeit == null

            // Startzeit für Chronometer berechnen
            val prefs = context.getSharedPreferences("running_time_tracker", Context.MODE_PRIVATE)
            val startedAt = prefs.getLong("startedAt", 0L)

            val baseTime = if (isRunning && startedAt > 0) {
                SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAt)
            } else {
                0L
            }

            // RemoteViews bauen
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_standard)

                // 1. Status Indicator
                if (isRunning) {
                    views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_active)
                } else {
                    views.setInt(R.id.widget_status_indicator, "setBackgroundResource", R.drawable.widget_status_inactive)
                }

                // 2. Zeiten setzen
                val startText = entry?.startZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"
                val endText = entry?.endZeit?.let { TimeUtils.minutesToTimeString(it) } ?: "--:--"

                views.setTextViewText(R.id.widget_start_time, startText)
                views.setTextViewText(R.id.widget_end_time, endText)

                // 3. Chronometer vs Static Text
                if (isRunning) {
                    views.setViewVisibility(R.id.widget_duration_text, View.GONE)
                    views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    views.setChronometer(R.id.widget_chronometer, baseTime, null, true)
                } else {
                    views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_duration_text, View.VISIBLE)

                    val istMinuten = entry?.getIstMinuten() ?: 0
                    val hours = istMinuten / 60
                    val minutes = istMinuten % 60
                    views.setTextViewText(R.id.widget_duration_text, String.format("%d:%02dh", hours, minutes))
                }

                // 4. Buttons Intents
                // Start
                val startIntent = Intent(context, StandardWidget::class.java).apply { action = ACTION_START }
                views.setOnClickPendingIntent(R.id.widget_btn_start, PendingIntent.getBroadcast(
                    context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))

                // Pause
                val pauseIntent = Intent(context, StandardWidget::class.java).apply { action = ACTION_PAUSE }
                views.setOnClickPendingIntent(R.id.widget_btn_pause, PendingIntent.getBroadcast(
                    context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))

                // Stop (Öffnet App mit Dialog)
                if (isRunning) {
                    val stopIntent = Intent(context, MainActivity::class.java).apply {
                        action = "com.arbeitszeit.tracker.ACTION_STOP_FROM_WIDGET"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    views.setOnClickPendingIntent(R.id.widget_btn_stop, PendingIntent.getActivity(
                        context, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ))
                } else {
                    // Wenn nicht läuft -> Button macht nichts oder öffnet App normal
                    val openIntent = Intent(context, MainActivity::class.java)
                    views.setOnClickPendingIntent(R.id.widget_btn_stop, PendingIntent.getActivity(
                        context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ))
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
