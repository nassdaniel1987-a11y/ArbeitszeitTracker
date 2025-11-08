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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
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
        montagSollMinuten: Int? = null,
        dienstagSollMinuten: Int? = null,
        mittwochSollMinuten: Int? = null,
        donnerstagSollMinuten: Int? = null,
        freitagSollMinuten: Int? = null,
        samstagSollMinuten: Int? = null,
        sonntagSollMinuten: Int? = null
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
                montagSollMinuten = montagSollMinuten,
                dienstagSollMinuten = dienstagSollMinuten,
                mittwochSollMinuten = mittwochSollMinuten,
                donnerstagSollMinuten = donnerstagSollMinuten,
                freitagSollMinuten = freitagSollMinuten,
                samstagSollMinuten = samstagSollMinuten,
                sonntagSollMinuten = sonntagSollMinuten,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            settingsDao.insertOrUpdate(settings)

            // Aktualisiere sollMinuten für alle bestehenden Einträge
            updateSollMinutenForAllEntries(settings)
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
                // Individuelle Zeit für diesen Wochentag?
                val individualSoll = settings.getSollMinutenForDay(dayOfWeek)
                if (individualSoll != null) {
                    individualSoll
                } else {
                    // Standardberechnung
                    if (com.arbeitszeit.tracker.utils.DateUtils.isWeekend(date)) {
                        0
                    } else {
                        settings.wochenStundenMinuten / settings.arbeitsTageProWoche
                    }
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
     * Aktualisiert nur den Übertrag vom letzten Blatt
     */
    fun updateLetzterUebertrag(minuten: Int) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings() ?: return@launch
            settingsDao.update(settings.copy(
                letzterUebertragMinuten = minuten,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Löscht alle Zeiteinträge (ACHTUNG: Kann nicht rückgängig gemacht werden!)
     */
    fun deleteAllTimeEntries() {
        viewModelScope.launch {
            database.clearAllTables()
        }
    }
}
