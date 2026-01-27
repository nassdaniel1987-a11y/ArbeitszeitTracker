package com.arbeitszeit.tracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.Keep
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Statistik-Widget (4x2) - Zeigt Heute, Woche und Überstunden
 *
 * Features:
 * - Heutige Arbeitszeit mit Status-Indikator
 * - Wöchentliche Arbeitszeit mit Ziel
 * - Überstunden-Anzeige (positiv/negativ)
 * - Dark Mode Unterstützung
 */
@Keep
class StatistikWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "StatistikWidget"
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH_STATISTIK"

        // Dark Mode Farben
        private const val COLOR_BG_LIGHT = 0xFFFFFFFF.toInt()
        private const val COLOR_BG_DARK = 0xFF1C1B1F.toInt()
        private const val COLOR_PRIMARY_LIGHT = 0xFF6750A4.toInt()
        private const val COLOR_PRIMARY_DARK = 0xFFD0BCFF.toInt()
        private const val COLOR_TEXT_LIGHT = 0xFF666666.toInt()
        private const val COLOR_TEXT_DARK = 0xFFAAAAAA.toInt()
        private const val COLOR_SUBTEXT_LIGHT = 0xFF999999.toInt()
        private const val COLOR_SUBTEXT_DARK = 0xFF777777.toInt()
        private const val COLOR_DIVIDER_LIGHT = 0xFFCCCCCC.toInt()
        private const val COLOR_DIVIDER_DARK = 0xFF444444.toInt()
        private const val COLOR_POSITIVE = 0xFF4CAF50.toInt()
        private const val COLOR_NEGATIVE = 0xFFF44336.toInt()
        private const val COLOR_RUNNING = 0xFF4CAF50.toInt()
        private const val COLOR_STOPPED = 0xFF9E9E9E.toInt()

        fun isDarkMode(context: Context): Boolean {
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, StatistikWidget::class.java).apply {
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
        Log.i(TAG, "onUpdate() called for ${appWidgetIds.size} widgets")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onUpdate", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> refreshWidget(context)
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StatistikWidget::class.java)
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
            val settingsDao = database.userSettingsDao()

            val today = DateUtils.today()
            val todayEntry = timeEntryDao.getEntryByDate(today)
            val settings = settingsDao.getSettings()

            // Wochenberechnung
            val now = LocalDate.now()
            val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

            val weekEntries = timeEntryDao.getEntriesInRange(
                startOfWeek.toString(),
                endOfWeek.toString()
            )

            // Heute
            val todayMinutes = todayEntry?.getIstMinuten() ?: 0
            val isRunning = todayEntry?.startZeit != null && todayEntry.endZeit == null

            // Diese Woche
            val weekMinutes = weekEntries.sumOf { it.getIstMinuten() }
            val weekTargetMinutes = (settings?.weeklyHours ?: 40f) * 60

            // Überstunden berechnen (vereinfacht: Woche - Ziel)
            val overtimeMinutes = weekMinutes - weekTargetMinutes.toInt()

            val darkMode = isDarkMode(context)
            val views = RemoteViews(context.packageName, R.layout.widget_statistik)

            // Hintergrundfarbe
            val bgColor = if (darkMode) COLOR_BG_DARK else COLOR_BG_LIGHT
            views.setInt(R.id.widget_background, "setBackgroundColor", bgColor)

            // Textfarben
            val primaryColor = if (darkMode) COLOR_PRIMARY_DARK else COLOR_PRIMARY_LIGHT
            val textColor = if (darkMode) COLOR_TEXT_DARK else COLOR_TEXT_LIGHT
            val subtextColor = if (darkMode) COLOR_SUBTEXT_DARK else COLOR_SUBTEXT_LIGHT

            // Titel
            views.setTextColor(R.id.widget_title, primaryColor)

            // Heute
            views.setTextColor(R.id.widget_today_label, textColor)
            views.setTextColor(R.id.widget_today_value, primaryColor)
            views.setTextViewText(R.id.widget_today_value, formatTime(todayMinutes))

            // Status-Indikator
            val statusColor = if (isRunning) COLOR_RUNNING else COLOR_STOPPED
            views.setInt(R.id.widget_status_indicator, "setBackgroundColor", statusColor)

            // Woche
            views.setTextColor(R.id.widget_week_label, textColor)
            views.setTextColor(R.id.widget_week_value, primaryColor)
            views.setTextColor(R.id.widget_week_target, subtextColor)
            views.setTextViewText(R.id.widget_week_value, formatTime(weekMinutes))
            views.setTextViewText(R.id.widget_week_target, "/ ${formatTime(weekTargetMinutes.toInt())}")

            // Überstunden
            views.setTextColor(R.id.widget_overtime_label, textColor)
            val overtimeColor = if (overtimeMinutes >= 0) COLOR_POSITIVE else COLOR_NEGATIVE
            views.setTextColor(R.id.widget_overtime_value, overtimeColor)
            val sign = if (overtimeMinutes >= 0) "+" else ""
            views.setTextViewText(R.id.widget_overtime_value, "$sign${formatTime(overtimeMinutes)}")

            // Click Intent - öffnet App
            val openAppIntent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.widget_background,
                PendingIntent.getActivity(
                    context, 0, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget updated: today=$todayMinutes, week=$weekMinutes, overtime=$overtimeMinutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId", e)
        }
    }

    private fun formatTime(minutes: Int): String {
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        return String.format("%d:%02dh", hours, mins)
    }
}
