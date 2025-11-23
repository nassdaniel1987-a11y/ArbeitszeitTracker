package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val settingsDao = database.userSettingsDao()
    private val timeEntryDao = database.timeEntryDao()
    
    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    /**
     * Aktualisiert oder erstellt Benutzereinstellungen
     */
    fun updateSettings(
        name: String,
        einrichtung: String,
        arbeitsumfangProzent: Int,
        wochenStundenMinuten: Int,
        arbeitsTageProWoche: Int,
        ferienbetreuung: Boolean,
        ueberstundenVorjahrMinuten: Int,
        ersterMontagImJahr: String? = null
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings()

            val settings = UserSettings(
                id = 1,
                name = name,
                einrichtung = einrichtung,
                arbeitsumfangProzent = arbeitsumfangProzent,
                wochenStundenMinuten = wochenStundenMinuten,
                arbeitsTageProWoche = arbeitsTageProWoche,
                ferienbetreuung = ferienbetreuung,
                ueberstundenVorjahrMinuten = ueberstundenVorjahrMinuten,
                letzterUebertragMinuten = existing?.letzterUebertragMinuten ?: 0,
                ersterMontagImJahr = ersterMontagImJahr,
                // WICHTIG: Geofencing-Einstellungen beibehalten!
                geofencingEnabled = existing?.geofencingEnabled ?: false,
                geofencingStartHour = existing?.geofencingStartHour ?: 6,
                geofencingEndHour = existing?.geofencingEndHour ?: 20,
                geofencingActiveDays = existing?.geofencingActiveDays ?: "12345",
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            settingsDao.insertOrUpdate(settings)

            // Aktualisiere sollMinuten für alle bestehenden Einträge
            updateSollMinutenForAllEntries(settings)

            // Aktualisiere Kalenderwochen, wenn sich der erste Montag geändert hat
            updateKalenderwochenForAllEntries(settings)
        }
    }

    /**
     * Aktualisiert nur Stammdaten (Name, Einrichtung)
     * Alle anderen Felder werden beibehalten
     */
    fun updateStammdaten(
        name: String,
        einrichtung: String
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings() ?: return@launch

            settingsDao.insertOrUpdate(existing.copy(
                name = name,
                einrichtung = einrichtung,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert nur Arbeitszeiteinstellungen
     * Alle anderen Felder werden beibehalten
     */
    fun updateArbeitszeit(
        arbeitsumfangProzent: Int,
        wochenStundenMinuten: Int,
        arbeitsTageProWoche: Int,
        ferienbetreuung: Boolean,
        ersterMontagImJahr: String?,
        workingDays: String = "12345"
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings() ?: return@launch

            val updated = existing.copy(
                arbeitsumfangProzent = arbeitsumfangProzent,
                wochenStundenMinuten = wochenStundenMinuten,
                arbeitsTageProWoche = arbeitsTageProWoche,
                ferienbetreuung = ferienbetreuung,
                ersterMontagImJahr = ersterMontagImJahr,
                workingDays = workingDays,
                updatedAt = System.currentTimeMillis()
            )

            settingsDao.insertOrUpdate(updated)

            // Aktualisiere sollMinuten für alle bestehenden Einträge
            updateSollMinutenForAllEntries(updated)

            // Aktualisiere Kalenderwochen, wenn sich der erste Montag geändert hat
            updateKalenderwochenForAllEntries(updated)
        }
    }


    /**
     * Aktualisiert die SollMinuten für alle bestehenden Zeiteinträge
     * basierend auf den neuen Einstellungen
     */
    private suspend fun updateSollMinutenForAllEntries(settings: UserSettings) {
        val allEntries = timeEntryDao.getAllEntriesFlow().first()

        allEntries.forEach { entry ->
            val date = java.time.LocalDate.parse(entry.datum)
            val dayOfWeek = date.dayOfWeek.value

            // Berechne neue Sollminuten
            val newSollMinuten = if (entry.typ != com.arbeitszeit.tracker.data.entity.TimeEntry.TYP_NORMAL) {
                // Bei Urlaub, Krank, etc. -> Soll bleibt
                entry.sollMinuten
            } else {
                // Standardberechnung: Prüfe ob Arbeitstag
                if (settings.isWorkingDay(dayOfWeek)) {
                    settings.wochenStundenMinuten / settings.arbeitsTageProWoche
                } else {
                    0  // Kein Arbeitstag
                }
            }

            // Aktualisiere nur, wenn sich die Sollminuten geändert haben
            if (entry.sollMinuten != newSollMinuten) {
                timeEntryDao.update(entry.copy(
                    sollMinuten = newSollMinuten,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }
    
    /**
     * Aktualisiert die Kalenderwochen für alle bestehenden Zeiteinträge
     * basierend auf der benutzerdefinierten Berechnung
     */
    private suspend fun updateKalenderwochenForAllEntries(settings: UserSettings) {
        val allEntries = timeEntryDao.getAllEntriesFlow().first()

        allEntries.forEach { entry ->
            val date = java.time.LocalDate.parse(entry.datum)

            // Berechne KW mit benutzerdefinierter Methode
            val newKW = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekOfYear(
                date,
                settings.ersterMontagImJahr
            )
            val newJahr = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekBasedYear(
                date,
                settings.ersterMontagImJahr
            )

            // Aktualisiere nur, wenn sich KW oder Jahr geändert haben
            if (entry.kalenderwoche != newKW || entry.jahr != newJahr) {
                timeEntryDao.update(entry.copy(
                    kalenderwoche = newKW,
                    jahr = newJahr,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    /**
     * Aktualisiert nur den Übertrag vom letzten Blatt
     */
    fun updateLetzterUebertrag(minuten: Int) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.insertOrUpdate(settings.copy(
                letzterUebertragMinuten = minuten,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert nur den Dark Mode
     */
    fun updateDarkMode(mode: String) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.insertOrUpdate(settings.copy(
                darkMode = mode,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert das Bundesland für Feiertags-Berechnung
     */
    fun updateBundesland(bundeslandCode: String?) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.insertOrUpdate(settings.copy(
                bundesland = bundeslandCode,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert den Urlaubsanspruch in Tagen
     */
    fun updateUrlaubsanspruch(tage: Int) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.insertOrUpdate(settings.copy(
                urlaubsanspruchTage = tage,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Löscht alle Zeiteinträge (ACHTUNG: Kann nicht rückgängig gemacht werden!)
     */
    fun deleteAllTimeEntries() {
        viewModelScope.launch {
            timeEntryDao.deleteAllEntries()
        }
    }
}
