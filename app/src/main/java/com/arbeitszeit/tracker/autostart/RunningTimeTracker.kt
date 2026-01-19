package com.arbeitszeit.tracker.autostart

import android.content.Context
import android.content.SharedPreferences
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
 */
class RunningTimeTracker(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "running_time_tracker",
        Context.MODE_PRIVATE
    )

    private val _runningState = MutableStateFlow<RunningTimeState?>(null)
    val runningState: StateFlow<RunningTimeState?> = _runningState.asStateFlow()

    init {
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
    }

    /**
     * Beendet die Zeiterfassung
     */
    fun stopTracking(): RunningTimeState? {
        val currentState = _runningState.value
        clearState()
        return currentState
    }

    /**
     * Prüft ob gerade eine Zeiterfassung läuft
     */
    fun isTracking(): Boolean {
        return _runningState.value != null
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
        }
    }

    /**
     * Löscht den gespeicherten State
     */
    private fun clearState() {
        prefs.edit().clear().apply()
        _runningState.value = null
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
