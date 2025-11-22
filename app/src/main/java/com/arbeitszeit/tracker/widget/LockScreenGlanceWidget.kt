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
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Lock Screen Glance Widget - Kompaktes Widget optimiert für den Sperrbildschirm
 * Zeigt Status, Heute-Zeit und Überstunden auf einen Blick
 */
class LockScreenGlanceWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_LOCKSCREEN = "com.arbeitszeit.tracker.ACTION_REFRESH_LOCKSCREEN"
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
            ACTION_REFRESH_LOCKSCREEN -> {
                refreshWidget(context)
            }
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, LockScreenGlanceWidget::class.java)
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
            val settingsDao = database.userSettingsDao()

            // Hole heutigen Eintrag
            val today = DateUtils.today()
            val todayEntry = timeEntryDao.getEntryByDate(today)

            // Prüfe ob es einen laufenden Eintrag gibt
            val isRunning = todayEntry != null && todayEntry.endZeit == null

            // Berechne Statistiken
            val todayMinutes = todayEntry?.getIstMinuten() ?: 0

            // Hole Überstunden
            val settings = settingsDao.getSettings()
            val allEntries = timeEntryDao.getAllEntriesFlow().first()

            val currentYear = LocalDate.now().year
            val currentYearEntries = allEntries.filter {
                val date = LocalDate.parse(it.datum)
                date.year == currentYear
            }

            val laufendesJahrUeberstunden = currentYearEntries.sumOf { it.getDifferenzMinuten() }
            val gesamtUeberstunden = laufendesJahrUeberstunden + (settings?.ueberstundenVorjahrMinuten ?: 0)

            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.widget_lockscreen_glance)

                // Status anzeigen
                if (isRunning && todayEntry != null) {
                    // Arbeitszeit läuft
                    views.setTextViewText(R.id.widget_lockscreen_status_icon, "▶")
                    views.setTextViewText(R.id.widget_lockscreen_status_text, "Arbeitszeit läuft")

                    val startTime = TimeUtils.formatTimeForDisplay(todayEntry.startZeit)
                    views.setTextViewText(R.id.widget_lockscreen_time_info, "seit $startTime")
                } else if (todayEntry != null && todayEntry.endZeit != null) {
                    // Arbeitszeit beendet
                    views.setTextViewText(R.id.widget_lockscreen_status_icon, "✓")
                    views.setTextViewText(R.id.widget_lockscreen_status_text, "Arbeitszeit beendet")

                    val endTime = TimeUtils.formatTimeForDisplay(todayEntry.endZeit)
                    views.setTextViewText(R.id.widget_lockscreen_time_info, "um $endTime")
                } else {
                    // Noch nicht gestartet
                    views.setTextViewText(R.id.widget_lockscreen_status_icon, "⏸")
                    views.setTextViewText(R.id.widget_lockscreen_status_text, "Noch nicht gestartet")
                    views.setTextViewText(R.id.widget_lockscreen_time_info, "Heute")
                }

                // Heute Arbeitszeit
                val todayText = minutesToHoursString(todayMinutes)
                views.setTextViewText(R.id.widget_lockscreen_today_hours, todayText)

                // Überstunden
                val overtimeText = minutesToHoursString(gesamtUeberstunden)
                val overtimeFormatted = if (gesamtUeberstunden >= 0) "+$overtimeText" else overtimeText
                views.setTextViewText(R.id.widget_lockscreen_overtime, overtimeFormatted)

                // Farbe für Überstunden
                val overtimeColor = when {
                    gesamtUeberstunden > 0 -> context.getColor(R.color.widget_status_active) // Grün
                    gesamtUeberstunden < 0 -> context.getColor(R.color.widget_error) // Rot
                    else -> context.getColor(R.color.widget_text) // Neutral
                }
                views.setTextColor(R.id.widget_lockscreen_overtime, overtimeColor)

                // Öffne App beim Klick auf Widget
                val appIntent = Intent(context, MainActivity::class.java)
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_lockscreen_status_text, appPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_lockscreen_today_hours, appPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    /**
     * Konvertiert Minuten in Stunden-String (z.B. "8:30" oder "-2:15")
     */
    private fun minutesToHoursString(minutes: Int): String {
        val isNegative = minutes < 0
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val sign = if (isNegative) "-" else ""
        return String.format("%s%d:%02d", sign, hours, mins)
    }
}
