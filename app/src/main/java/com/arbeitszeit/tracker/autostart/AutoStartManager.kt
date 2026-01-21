package com.arbeitszeit.tracker.autostart

import android.content.Context
import android.util.Log
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.geofencing.GeofencingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Manager für Auto-Start der Arbeitszeiterfassung
 *
 * Features:
 * - Prüft ob Auto-Start möglich ist
 * - Startet Zeiterfassung automatisch
 * - Berücksichtigt Geofencing
 * - Verhindert Duplikate
 * - Start-Zeiten werden aus UserSettings geladen
 */
class AutoStartManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val settingsDao = database.userSettingsDao()
    private val timeEntryDao = database.timeEntryDao()
    private val runningTimeTracker = RunningTimeTracker(context)
    private val geofencingManager = GeofencingManager(context)

    companion object {
        private const val TAG = "AutoStartManager"
    }

    /**
     * Prüft ob Auto-Start durchgeführt werden soll
     *
     * Bedingungen:
     * - Auto-Start aktiviert in Settings
     * - Noch kein Eintrag für heute
     * - Zeiterfassung läuft noch nicht
     * - Start-Zeit für heute in Settings konfiguriert
     * - Geofencing erfüllt (wenn aktiviert)
     *
     * @return true wenn Auto-Start möglich
     */
    suspend fun shouldAutoStart(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = settingsDao.getSettings() ?: run {
                Log.d(TAG, "Keine Settings vorhanden")
                return@withContext false
            }

            // Auto-Start aktiviert?
            if (!settings.autoStartEnabled) {
                Log.d(TAG, "Auto-Start nicht aktiviert")
                return@withContext false
            }

            // Bereits am Laufen?
            if (runningTimeTracker.isTracking()) {
                Log.d(TAG, "Zeiterfassung läuft bereits")
                return@withContext false
            }

            // Heute schon Eintrag mit Start-Zeit?
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingEntry = timeEntryDao.getEntryByDate(today)
            if (existingEntry?.startZeit != null) {
                Log.d(TAG, "Heute bereits gestartet (startZeit vorhanden)")
                return@withContext false
            }

            // Start-Zeit für heute in Settings konfiguriert?
            val dayOfWeek = LocalDate.now().dayOfWeek.value
            val startZeit = settings.getAutoStartZeitForDay(dayOfWeek)
            if (startZeit == null) {
                Log.d(TAG, "Keine Start-Zeit für heute (Tag $dayOfWeek) in den Einstellungen konfiguriert")
                return@withContext false
            }

            // Geofencing-Prüfung (wenn erforderlich)
            if (settings.autoStartRequiresGeofencing) {
                // Aktive Standortprüfung statt statischer SharedPreferences-Wert
                val isAtWorkLocation = geofencingManager.checkActiveLocationAtWork()
                if (!isAtWorkLocation) {
                    Log.d(TAG, "Nicht am Arbeitsort (aktive Geofencing-Prüfung)")
                    return@withContext false
                }
                Log.d(TAG, "Am Arbeitsort bestätigt (aktive Geofencing-Prüfung)")
            }

            Log.d(TAG, "Auto-Start möglich!")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei shouldAutoStart: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Führt den Auto-Start durch
     *
     * @return true wenn erfolgreich
     */
    suspend fun performAutoStart(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "performAutoStart() - Starte Prüfung")

            if (!shouldAutoStart()) {
                Log.w(TAG, "Auto-Start nicht möglich (shouldAutoStart = false)")
                return@withContext false
            }

            val settings = settingsDao.getSettings() ?: run {
                Log.e(TAG, "Keine Settings vorhanden")
                return@withContext false
            }

            val dayOfWeek = LocalDate.now().dayOfWeek.value
            Log.d(TAG, "Heute ist Tag: $dayOfWeek")

            val startZeit = settings.getAutoStartZeitForDay(dayOfWeek) ?: run {
                Log.e(TAG, "Keine Start-Zeit für heute (Tag $dayOfWeek) in den Einstellungen")
                return@withContext false
            }
            Log.d(TAG, "Start-Zeit aus Einstellungen: $startZeit Minuten")

            val startTime = minutesToLocalTime(startZeit)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            Log.d(TAG, "Starte Tracking für $today um $startTime")

            // Update TimeEntry in Datenbank
            val entry = timeEntryDao.getEntryByDate(today)
            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    startZeit = startZeit,
                    updatedAt = System.currentTimeMillis()
                ))
                Log.d(TAG, "TimeEntry updated mit startZeit: $startZeit")
            } else {
                Log.w(TAG, "Kein TimeEntry für heute gefunden - kann nicht starten")
                return@withContext false
            }

            // Starte Tracking
            runningTimeTracker.startTracking(
                startTime = startTime,
                isAutoStart = true,
                date = today
            )

            Log.i(TAG, "Auto-Start erfolgreich: $startTime")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei performAutoStart: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Gibt die Start-Zeit für heute zurück (falls vorhanden)
     * Lädt die Start-Zeit aus den UserSettings
     */
    suspend fun getStartTimeForToday(): LocalTime? = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettings() ?: return@withContext null
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val startZeit = settings.getAutoStartZeitForDay(dayOfWeek)
        return@withContext startZeit?.let { minutesToLocalTime(it) }
    }

    /**
     * Konvertiert Minuten seit Mitternacht zu LocalTime
     */
    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val hours = (minutes / 60).coerceIn(0, 23)
        val mins = (minutes % 60).coerceIn(0, 59)
        return LocalTime.of(hours, mins)
    }
}
