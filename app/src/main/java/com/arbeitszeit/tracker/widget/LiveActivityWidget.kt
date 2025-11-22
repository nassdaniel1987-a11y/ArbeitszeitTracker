package com.arbeitszeit.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Live Activity Widget - Zeigt laufende Arbeitszeit
 * Update alle 60 Sekunden um Elapsed-Zeit zu aktualisieren
 */
class LiveActivityWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_STOP_WORK = "com.arbeitszeit.tracker.ACTION_STOP_WORK"
        const val ACTION_REFRESH_LIVE = "com.arbeitszeit.tracker.ACTION_REFRESH_LIVE"
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
            ACTION_STOP_WORK -> {
                stopCurrentWork(context)
            }
            ACTION_REFRESH_LIVE -> {
                refreshWidget(context)
            }
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, LiveActivityWidget::class.java)
        )
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun stopCurrentWork(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            // Hole heutigen Eintrag
            val today = DateUtils.today()
            val todayEntry = timeEntryDao.getEntryByDate(today)

            // Wenn es einen laufenden Eintrag gibt, beende ihn
            if (todayEntry != null && todayEntry.endezeit.isNullOrEmpty()) {
                val currentTime = LocalTime.now()
                val updatedEntry = todayEntry.copy(
                    endezeit = DateUtils.formatTime(currentTime)
                )
                timeEntryDao.insertOrUpdate(updatedEntry)

                // Widget aktualisieren
                withContext(Dispatchers.Main) {
                    refreshWidget(context)
                }
            }
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

            // Hole heutigen Eintrag
            val today = DateUtils.today()
            val todayEntry = timeEntryDao.getEntryByDate(today)

            // Prüfe ob es einen laufenden Eintrag gibt
            val isRunning = todayEntry != null && todayEntry.endezeit.isNullOrEmpty()

            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.widget_live_activity)

                if (isRunning && todayEntry != null) {
                    // Es läuft eine Arbeitszeit - Zeige aktiven Status

                    // Verstecke "Inaktiv"-Container, zeige aktive Elemente
                    views.setViewVisibility(R.id.widget_live_inactive_container, View.GONE)
                    views.setViewVisibility(R.id.widget_live_status, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_live_start_time, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_live_elapsed_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_live_stop_button, View.VISIBLE)

                    // Status-Indikator
                    views.setTextViewText(R.id.widget_live_status, "● AKTIV")
                    views.setTextColor(R.id.widget_live_status, context.getColor(R.color.widget_status_active))

                    // Start-Zeit anzeigen
                    val startTime = todayEntry.startzeit ?: "--:--"
                    views.setTextViewText(R.id.widget_live_start_time, startTime)

                    // Verstrichene Zeit berechnen
                    val elapsedMinutes = calculateElapsedMinutes(todayEntry.startzeit)
                    val elapsedText = minutesToHoursString(elapsedMinutes)
                    views.setTextViewText(R.id.widget_live_elapsed_time, elapsedText)

                    // Stop Button
                    val stopIntent = Intent(context, LiveActivityWidget::class.java).apply {
                        action = ACTION_STOP_WORK
                    }
                    val stopPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_live_stop_button, stopPendingIntent)

                } else {
                    // Keine aktive Arbeitszeit - Zeige inaktiven Status

                    // Verstecke aktive Elemente, zeige "Inaktiv"-Container
                    views.setViewVisibility(R.id.widget_live_inactive_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_live_status, View.GONE)
                    views.setViewVisibility(R.id.widget_live_start_time, View.GONE)
                    views.setViewVisibility(R.id.widget_live_elapsed_container, View.GONE)
                    views.setViewVisibility(R.id.widget_live_stop_button, View.GONE)
                }

                // Öffne App beim Klick auf Widget (Header)
                val appIntent = Intent(context, MainActivity::class.java)
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_live_elapsed_container, appPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    /**
     * Berechnet die verstrichenen Minuten seit der Startzeit
     */
    private fun calculateElapsedMinutes(startzeit: String?): Int {
        if (startzeit.isNullOrEmpty()) return 0

        return try {
            val start = LocalTime.parse(startzeit)
            val now = LocalTime.now()
            ChronoUnit.MINUTES.between(start, now).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Konvertiert Minuten in Stunden-String (z.B. "8:30")
     */
    private fun minutesToHoursString(minutes: Int): String {
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        return String.format("%d:%02d", hours, mins)
    }
}
