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
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Schnell-Stempel Widget (1x1) - Ein Klick zum Stempeln
 *
 * Features:
 * - Einzelner Button zum Starten der Zeiterfassung
 * - Zeigt Status durch Farbe (Grün = läuft, Grau = gestoppt)
 * - Dark Mode Unterstützung
 */
@Keep
class QuickStampWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "QuickStampWidget"
        const val ACTION_STAMP = "com.arbeitszeit.tracker.ACTION_QUICK_STAMP"
        const val ACTION_REFRESH = "com.arbeitszeit.tracker.ACTION_REFRESH_QUICK"

        // Dark Mode Farben
        private const val COLOR_BG_LIGHT = 0xFFFFFFFF.toInt()
        private const val COLOR_BG_DARK = 0xFF1C1B1F.toInt()
        private const val COLOR_RUNNING = 0xFF4CAF50.toInt()   // Grün
        private const val COLOR_STOPPED = 0xFF9E9E9E.toInt()   // Grau

        fun isDarkMode(context: Context): Boolean {
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, QuickStampWidget::class.java).apply {
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
            ACTION_STAMP -> handleStamp(context)
            ACTION_REFRESH -> refreshWidget(context)
        }
    }

    private fun handleStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val timeEntryDao = database.timeEntryDao()
                val today = DateUtils.today()
                val entry = timeEntryDao.getEntryByDate(today)
                val currentTime = TimeUtils.currentTimeInMinutes()
                val currentLocalTime = java.time.LocalTime.now()
                val tracker = RunningTimeTracker(context)

                if (entry != null) {
                    val isRunning = entry.startZeit != null && entry.endZeit == null

                    if (!isRunning) {
                        // Start oder Neustart
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
                    // Wenn schon läuft: nichts tun (Stop nur über App)
                }

                refreshWidget(context)
                // Auch andere Widgets aktualisieren
                TimeStampWidgetSmall.updateAllWidgets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling stamp", e)
            }
        }
    }

    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuickStampWidget::class.java)
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
            val entry = database.timeEntryDao().getEntryByDate(DateUtils.today())
            val isRunning = entry?.startZeit != null && entry.endZeit == null
            val darkMode = isDarkMode(context)

            val views = RemoteViews(context.packageName, R.layout.widget_quick_stamp)

            // Hintergrund setzen
            val bgColor = if (darkMode) COLOR_BG_DARK else COLOR_BG_LIGHT
            views.setInt(R.id.widget_background, "setBackgroundColor", bgColor)

            // Button-Farbe nach Status
            val btnColor = if (isRunning) COLOR_RUNNING else COLOR_STOPPED
            views.setInt(R.id.widget_btn_stamp, "setBackgroundColor", btnColor)

            // Button-Text: Play wenn gestoppt, Haken wenn läuft
            val btnText = if (isRunning) "✓" else "▶"
            views.setTextViewText(R.id.widget_btn_stamp, btnText)

            // Click Intent
            val stampIntent = Intent(context, QuickStampWidget::class.java).apply {
                action = ACTION_STAMP
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_stamp,
                PendingIntent.getBroadcast(
                    context, 0, stampIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget updated: running=$isRunning, darkMode=$darkMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId", e)
        }
    }
}
