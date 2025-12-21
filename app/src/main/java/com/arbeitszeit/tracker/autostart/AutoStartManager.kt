package com.arbeitszeit.tracker.autostart

import android.content.Context
import android.util.Log
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.SollZeitVorlage
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
 */
class AutoStartManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val settingsDao = database.userSettingsDao()
    private val sollZeitVorlageDao = database.sollZeitVorlageDao()
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
     * - Wochenvorlage existiert mit Start-Zeit
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

            // Heute schon Eintrag?
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingEntry = timeEntryDao.getEntryByDate(today)
            if (existingEntry != null) {
                Log.d(TAG, "Eintrag für heute existiert bereits")
                return@withContext false
            }

            // SollZeitVorlage mit Start-Zeit vorhanden?
            val dayOfWeek = LocalDate.now().dayOfWeek.value
            val vorlage = getActiveVorlage()
            val startZeit = vorlage?.getStartZeitForDay(dayOfWeek)
            if (startZeit == null) {
                Log.d(TAG, "Keine Start-Zeit für heute in der Vorlage konfiguriert")
                return@withContext false
            }

            // Geofencing-Prüfung (wenn erforderlich)
            if (settings.autoStartRequiresGeofencing) {
                val isAtWorkLocation = geofencingManager.isCurrentlyAtWorkLocation()
                if (!isAtWorkLocation) {
                    Log.d(TAG, "Nicht am Arbeitsort (Geofencing)")
                    return@withContext false
                }
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
            if (!shouldAutoStart()) {
                Log.w(TAG, "Auto-Start nicht möglich")
                return@withContext false
            }

            val dayOfWeek = LocalDate.now().dayOfWeek.value
            val vorlage = getActiveVorlage() ?: run {
                Log.e(TAG, "Keine Vorlage gefunden")
                return@withContext false
            }

            val startZeit = vorlage.getStartZeitForDay(dayOfWeek) ?: run {
                Log.e(TAG, "Keine Start-Zeit für heute gefunden")
                return@withContext false
            }

            val startTime = minutesToLocalTime(startZeit)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

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
     */
    suspend fun getStartTimeForToday(): LocalTime? = withContext(Dispatchers.IO) {
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val vorlage = getActiveVorlage()
        val startZeit = vorlage?.getStartZeitForDay(dayOfWeek)
        return@withContext startZeit?.let { minutesToLocalTime(it) }
    }

    /**
     * Lädt die aktive SollZeitVorlage
     *
     * Verwendet die Standard-Vorlage (isDefault = true).
     * Falls keine Standard-Vorlage existiert, wird die erste Vorlage verwendet.
     */
    private suspend fun getActiveVorlage(): SollZeitVorlage? {
        // Lade alle Vorlagen
        val vorlagen = sollZeitVorlageDao.getAllVorlagen()
        if (vorlagen.isEmpty()) {
            return null
        }

        // Nimm die Standard-Vorlage, oder die erste Vorlage
        return vorlagen.firstOrNull { it.isDefault } ?: vorlagen.firstOrNull()
    }

    /**
     * Konvertiert Minuten seit Mitternacht zu LocalTime
     */
    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val hours = minutes / 60
        val mins = minutes % 60
        return LocalTime.of(hours, mins)
    }
}
