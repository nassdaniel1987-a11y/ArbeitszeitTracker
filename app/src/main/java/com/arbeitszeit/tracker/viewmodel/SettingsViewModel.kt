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
    private val yearSettingsDao = database.yearSettingsDao()

    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Aktives Jahr (für jahr-spezifische Einstellungen wie ersterMontag)
    val activeYear = yearSettingsDao.getActiveYearFlow()
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
     * Aktualisiert nur Stammdaten (Name, Einrichtung, Überstunden vom Vorjahr)
     * Alle anderen Felder werden beibehalten
     */
    fun updateStammdaten(
        name: String,
        einrichtung: String,
        ueberstundenVorjahrMinuten: Int? = null
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings() ?: return@launch

            settingsDao.insertOrUpdate(existing.copy(
                name = name,
                einrichtung = einrichtung,
                ueberstundenVorjahrMinuten = ueberstundenVorjahrMinuten ?: existing.ueberstundenVorjahrMinuten,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert nur Arbeitszeiteinstellungen
     * Alle anderen Felder werden beibehalten
     *
     * WICHTIG: ersterMontagImJahr wird NICHT mehr hier gespeichert!
     * Nutze stattdessen updateErsterMontag() (speichert in YearSettings)
     */
    fun updateArbeitszeit(
        arbeitsumfangProzent: Int,
        wochenStundenMinuten: Int,
        arbeitsTageProWoche: Int,
        ferienbetreuung: Boolean,
        workingDays: String = "12345"
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings() ?: return@launch

            val updated = existing.copy(
                arbeitsumfangProzent = arbeitsumfangProzent,
                wochenStundenMinuten = wochenStundenMinuten,
                arbeitsTageProWoche = arbeitsTageProWoche,
                ferienbetreuung = ferienbetreuung,
                workingDays = workingDays,
                updatedAt = System.currentTimeMillis()
            )

            settingsDao.insertOrUpdate(updated)

            // Aktualisiere sollMinuten für alle bestehenden Einträge
            updateSollMinutenForAllEntries(updated)
        }
    }


    /**
     * Aktualisiert die SollMinuten für alle bestehenden Zeiteinträge
     * basierend auf den neuen Einstellungen
     *
     * WICHTIG: Einträge mit sollZeitVorlageName (z.B. "IMPORT", "Normal", "Ferienbetreuung")
     * werden übersprungen, da ihre sollMinuten manuell gesetzt wurden und nicht
     * überschrieben werden sollen
     */
    private suspend fun updateSollMinutenForAllEntries(settings: UserSettings) {
        val allEntries = timeEntryDao.getAllEntriesFlow().first()

        allEntries.forEach { entry ->
            // Überspringe Einträge mit Vorlagennamen (Import oder Wochenvorlagen)
            // Diese haben bewusst gesetzte sollMinuten und sollen nicht überschrieben werden
            if (!entry.sollZeitVorlageName.isNullOrEmpty()) {
                android.util.Log.d("SettingsViewModel", "Überspringe Eintrag mit Vorlage '${entry.sollZeitVorlageName}': ${entry.datum}")
                return@forEach
            }

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
     * Aktualisiert die Auto-Switch-Einstellung für Jahreswechsel
     */
    fun updateAutoSwitchYear(enabled: Boolean) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.insertOrUpdate(settings.copy(
                autoSwitchYear = enabled,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert den ersten Montag für das aktive Jahr
     *
     * WICHTIG: Speichert den Wert in YearSettings (jahr-spezifisch),
     * NICHT in UserSettings!
     */
    fun updateErsterMontag(ersterMontag: String) {
        viewModelScope.launch {
            val activeYear = yearSettingsDao.getActiveYear() ?: return@launch
            yearSettingsDao.update(activeYear.copy(
                ersterMontagImJahr = ersterMontag
            ))

            // Aktualisiere KW-Nummern für alle Einträge des aktiven Jahres
            updateKalenderwochenForYear(activeYear.year, ersterMontag)
        }
    }

    /**
     * Aktualisiert Kalenderwochen für alle Einträge eines bestimmten Jahres
     */
    private suspend fun updateKalenderwochenForYear(year: Int, ersterMontag: String) {
        val entries = timeEntryDao.getEntriesByYear(year)
        entries.forEach { entry ->
            val date = java.time.LocalDate.parse(entry.datum)
            val newKW = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekOfYear(date, ersterMontag)
            val newJahr = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekBasedYear(date, ersterMontag)

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
     * Löscht alle Zeiteinträge (ACHTUNG: Kann nicht rückgängig gemacht werden!)
     */
    fun deleteAllTimeEntries() {
        viewModelScope.launch {
            timeEntryDao.deleteAllEntries()
        }
    }

    /**
     * Aktualisiert Auto-Start Einstellungen
     */
    fun updateAutoStartSettings(
        autoStartEnabled: Boolean,
        autoStartRequiresGeofencing: Boolean,
        autoStartReminderMinutes: Int,
        autoStartDefaultPauseMinutes: Int,
        autoStartZeiten: Map<Int, Int?> = emptyMap()  // Tag (1-7) -> Minuten oder null zum Löschen
    ) {
        viewModelScope.launch {
            val existing = settingsDao.getSettings()
            if (existing != null) {
                val updated = existing.copy(
                    autoStartEnabled = autoStartEnabled,
                    autoStartRequiresGeofencing = autoStartRequiresGeofencing,
                    autoStartReminderMinutes = autoStartReminderMinutes,
                    autoStartDefaultPauseMinutes = autoStartDefaultPauseMinutes,
                    autoStartMontagZeit = if (autoStartZeiten.containsKey(1)) autoStartZeiten[1] else existing.autoStartMontagZeit,
                    autoStartDienstagZeit = if (autoStartZeiten.containsKey(2)) autoStartZeiten[2] else existing.autoStartDienstagZeit,
                    autoStartMittwochZeit = if (autoStartZeiten.containsKey(3)) autoStartZeiten[3] else existing.autoStartMittwochZeit,
                    autoStartDonnerstagZeit = if (autoStartZeiten.containsKey(4)) autoStartZeiten[4] else existing.autoStartDonnerstagZeit,
                    autoStartFreitagZeit = if (autoStartZeiten.containsKey(5)) autoStartZeiten[5] else existing.autoStartFreitagZeit,
                    autoStartSamstagZeit = if (autoStartZeiten.containsKey(6)) autoStartZeiten[6] else existing.autoStartSamstagZeit,
                    autoStartSonntagZeit = if (autoStartZeiten.containsKey(7)) autoStartZeiten[7] else existing.autoStartSonntagZeit,
                    updatedAt = System.currentTimeMillis()
                )
                settingsDao.insertOrUpdate(updated)

                // AlarmManager-basierten Auto-Start planen/abbrechen
                if (autoStartEnabled) {
                    com.arbeitszeit.tracker.worker.AutoStartAlarmManager.scheduleNextAlarm(getApplication())
                } else {
                    com.arbeitszeit.tracker.worker.AutoStartAlarmManager.cancelAlarm(getApplication())
                }
            }
        }
    }
}
