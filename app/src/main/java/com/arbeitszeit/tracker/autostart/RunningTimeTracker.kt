package com.arbeitszeit.tracker.autostart

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arbeitszeit.tracker.widget.TimeStampWidgetSmall
import com.arbeitszeit.tracker.widget.QuickStampWidget
import com.arbeitszeit.tracker.widget.StatistikWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Verwaltet den Status der laufenden Arbeitszeiterfassung
 *
 * Features:
 * - Speichert laufende Arbeitszeit (auch über App-Neustart hinweg)
 * - Auto-Start vs. Manuell-Start Tracking
 * - StateFlow für UI-Updates
 * - Globaler Broadcast für Synchronisierung zwischen App, Widget und Benachrichtigungen
 */
class RunningTimeTracker(private val context: Context) {

    companion object {
        const val ACTION_TRACKING_STATE_CHANGED = "com.arbeitszeit.tracker.ACTION_TRACKING_STATE_CHANGED"
        const val EXTRA_IS_TRACKING = "is_tracking"
        private const val PREFS_NAME = "running_time_tracker"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _runningState = MutableStateFlow<RunningTimeState?>(null)
    val runningState: StateFlow<RunningTimeState?> = _runningState.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "startedAt" || key == "startTime" || key == "date") {
            // Wenn sich Daten in SharedPreferences ändern (z.B. durch Widget),
            // lade den State neu
            val currentState = _runningState.value
            val startedAt = prefs.getLong("startedAt", 0L)

            if (startedAt == 0L && currentState != null) {
                // Tracking wurde gestoppt (extern)
                _runningState.value = null
            } else if (startedAt > 0 && (currentState == null || currentState.startedAt != startedAt)) {
                // Tracking wurde gestartet oder geändert (extern)
                loadState()
            }
        }
    }

    init {
        // Listener registrieren
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // Lade gespeicherten State beim Start
        loadState()
    }

    /**
     * Startet die Zeiterfassung
     */
    fun startTracking(
        startTime: LocalTime,
        isAutoStart: Boolean,
        date: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    ) {
        val state = RunningTimeState(
            date = date,
            startTime = startTime,
            isAutoStart = isAutoStart,
            startedAt = System.currentTimeMillis()
        )

        // Speichern
        saveState(state)
        _runningState.value = state

        // Broadcast senden für Synchronisierung
        sendTrackingStateChangedBroadcast(true)

        // Widgets aktualisieren
        updateWidgets()
    }

    /**
     * Beendet die Zeiterfassung
     */
    fun stopTracking(): RunningTimeState? {
        val currentState = _runningState.value
        clearState()

        // Broadcast senden für Synchronisierung
        sendTrackingStateChangedBroadcast(false)

        // Widgets aktualisieren
        updateWidgets()

        return currentState
    }

    /**
     * Prüft ob gerade eine Zeiterfassung läuft
     */
    fun isTracking(): Boolean {
        return _runningState.value != null
    }

    /**
     * Prüft statisch ob gerade eine Zeiterfassung läuft (ohne StateFlow)
     */
    fun isTrackingStatic(): Boolean {
        val startedAt = prefs.getLong("startedAt", 0L)
        return startedAt > 0
    }

    /**
     * Gibt die laufende Dauer in Minuten zurück
     */
    fun getRunningDurationMinutes(): Int {
        val state = _runningState.value ?: return 0
        val elapsedMillis = System.currentTimeMillis() - state.startedAt
        return (elapsedMillis / 1000 / 60).toInt()
    }

    /**
     * Speichert den aktuellen State in SharedPreferences
     */
    private fun saveState(state: RunningTimeState) {
        prefs.edit().apply {
            putString("date", state.date)
            putString("startTime", state.startTime.format(DateTimeFormatter.ISO_LOCAL_TIME))
            putBoolean("isAutoStart", state.isAutoStart)
            putLong("startedAt", state.startedAt)
            apply()
        }
    }

    /**
     * Lädt den State aus SharedPreferences
     */
    private fun loadState() {
        val date = prefs.getString("date", null)
        val startTimeStr = prefs.getString("startTime", null)
        val isAutoStart = prefs.getBoolean("isAutoStart", false)
        val startedAt = prefs.getLong("startedAt", 0L)

        if (date != null && startTimeStr != null && startedAt > 0) {
            val startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_TIME)
            _runningState.value = RunningTimeState(
                date = date,
                startTime = startTime,
                isAutoStart = isAutoStart,
                startedAt = startedAt
            )
        } else {
            _runningState.value = null
        }
    }

    /**
     * Lädt den State neu aus SharedPreferences (öffentlich für externe Synchronisierung)
     */
    fun refreshState() {
        loadState()
    }

    /**
     * Löscht den gespeicherten State
     */
    private fun clearState() {
        // Explizit startedAt auf 0 setzen damit der Listener getriggert wird
        prefs.edit().apply {
            putLong("startedAt", 0L)
            remove("date")
            remove("startTime")
            remove("isAutoStart")
            apply()
        }
        _runningState.value = null
    }

    /**
     * Sendet einen lokalen Broadcast um alle Komponenten zu synchronisieren
     */
    private fun sendTrackingStateChangedBroadcast(isTracking: Boolean) {
        val intent = Intent(ACTION_TRACKING_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_TRACKING, isTracking)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Sendet Update-Broadcast an alle Widgets
     */
    fun updateWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // Update Small Widget
            val smallIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TimeStampWidgetSmall::class.java))
            if (smallIds.isNotEmpty()) {
                val intent = Intent(context, TimeStampWidgetSmall::class.java)
                intent.action = TimeStampWidgetSmall.ACTION_REFRESH
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, smallIds)
                context.sendBroadcast(intent)
            }

            // Update Quick Stamp Widget
            val quickIds = appWidgetManager.getAppWidgetIds(ComponentName(context, QuickStampWidget::class.java))
            if (quickIds.isNotEmpty()) {
                val intent = Intent(context, QuickStampWidget::class.java)
                intent.action = QuickStampWidget.ACTION_REFRESH
                context.sendBroadcast(intent)
            }

            // Update Statistik Widget
            val statIds = appWidgetManager.getAppWidgetIds(ComponentName(context, StatistikWidget::class.java))
            if (statIds.isNotEmpty()) {
                val intent = Intent(context, StatistikWidget::class.java)
                intent.action = StatistikWidget.ACTION_REFRESH
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("RunningTimeTracker", "Fehler beim Aktualisieren der Widgets", e)
        }
    }
}

/**
 * State-Klasse für laufende Zeiterfassung
 */
data class RunningTimeState(
    val date: String,                    // Datum (yyyy-MM-dd)
    val startTime: LocalTime,            // Start-Zeit
    val isAutoStart: Boolean,            // Wurde automatisch gestartet?
    val startedAt: Long                  // Timestamp wann gestartet (für Dauer-Berechnung)
) {
    /**
     * Berechnet die End-Zeit basierend auf aktueller Zeit
     */
    fun calculateEndTime(): LocalTime {
        return LocalTime.now()
    }

    /**
     * Berechnet die Dauer in Minuten
     */
    fun calculateDurationMinutes(): Int {
        val elapsedMillis = System.currentTimeMillis() - startedAt
        return (elapsedMillis / 1000 / 60).toInt()
    }

    /**
     * Formatiert die Start-Zeit für Anzeige
     */
    fun getStartTimeFormatted(): String {
        return startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    /**
     * Formatiert die Dauer für Anzeige (z.B. "8h 45min")
     */
    fun getDurationFormatted(currentTimeMillis: Long = System.currentTimeMillis()): String {
        val elapsedMillis = currentTimeMillis - startedAt
        val totalSeconds = elapsedMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%dh %02dmin %02ds".format(hours, minutes, seconds)
        } else {
            "%02dmin %02ds".format(minutes, seconds)
        }
    }
}
