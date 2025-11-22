package com.arbeitszeit.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Widget für Arbeitszeit-Statistiken
 * Zeigt: Heute, Woche, Überstunden
 */
class StatistikWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_STATS = "com.arbeitszeit.tracker.ACTION_REFRESH_STATS"
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
            ACTION_REFRESH_STATS -> {
                refreshWidget(context)
            }
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, StatistikWidget::class.java)
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

            // Hole Wocheneinträge (Montag bis Sonntag)
            val currentDate = LocalDate.now()
            val monday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)

            val weekEntries = timeEntryDao.getEntriesInRange(
                DateUtils.formatDate(monday),
                DateUtils.formatDate(sunday)
            )

            // Berechne Statistiken
            val todayMinutes = todayEntry?.getIstMinuten() ?: 0
            val weekMinutes = weekEntries.sumOf { it.getIstMinuten() }

            // Hole Überstunden
            val settings = settingsDao.getSettings()
            val allEntries = timeEntryDao.getAllEntries()

            val currentYear = LocalDate.now().year
            val currentYearEntries = allEntries.filter {
                val date = LocalDate.parse(it.datum)
                date.year == currentYear
            }

            val laufendesJahrUeberstunden = currentYearEntries.sumOf { it.getDifferenzMinuten() }
            val gesamtUeberstunden = laufendesJahrUeberstunden + (settings?.ueberstundenVorjahrMinuten ?: 0)

            // Update UI
            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.widget_statistik)

                // Setze Daten
                views.setTextViewText(
                    R.id.widget_stats_today,
                    minutesToHoursString(todayMinutes)
                )

                views.setTextViewText(
                    R.id.widget_stats_week,
                    minutesToHoursString(weekMinutes)
                )

                val overtimeText = minutesToHoursString(gesamtUeberstunden)
                val overtimeFormatted = if (gesamtUeberstunden >= 0) "+$overtimeText" else overtimeText
                views.setTextViewText(
                    R.id.widget_stats_overtime,
                    overtimeFormatted
                )

                // Farbe für Überstunden
                val overtimeColor = when {
                    gesamtUeberstunden > 0 -> context.getColor(R.color.widget_status_active) // Grün
                    gesamtUeberstunden < 0 -> context.getColor(R.color.widget_error) // Rot
                    else -> context.getColor(R.color.widget_text) // Neutral
                }
                views.setTextColor(R.id.widget_stats_overtime, overtimeColor)

                // Datum anzeigen
                val dateText = DateUtils.formatForDisplay(LocalDate.now())
                views.setTextViewText(R.id.widget_stats_date, dateText)

                // Refresh Button
                val refreshIntent = Intent(context, StatistikWidget::class.java).apply {
                    action = ACTION_REFRESH_STATS
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_stats_refresh_button, refreshPendingIntent)

                // Öffne App beim Klick auf Widget
                val appIntent = Intent(context, MainActivity::class.java)
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_stats_today, appPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_stats_week, appPendingIntent)

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
