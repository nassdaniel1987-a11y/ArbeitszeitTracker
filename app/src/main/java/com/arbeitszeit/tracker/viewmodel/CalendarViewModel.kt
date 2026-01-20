package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()
    private val yearSettingsDao = database.yearSettingsDao()

    // YearBoundaryService für Jahr-Berechnungen
    private val yearBoundaryService = com.arbeitszeit.tracker.year.YearBoundaryService(application)

    // User Settings mit Bundesland für Feiertage
    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Year Settings für Arbeitsjahr-Grenzen
    val yearSettings = yearSettingsDao.getActiveYearFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    private val _monthEntries = MutableStateFlow<List<TimeEntry>>(emptyList())
    val monthEntries: StateFlow<List<TimeEntry>> = _monthEntries.asStateFlow()
    
    private val _selectedEntry = MutableStateFlow<TimeEntry?>(null)
    val selectedEntry: StateFlow<TimeEntry?> = _selectedEntry.asStateFlow()

    // Für Undo-Funktion: Speichert gelöschten Eintrag temporär
    private val _deletedEntry = MutableStateFlow<TimeEntry?>(null)
    val deletedEntry: StateFlow<TimeEntry?> = _deletedEntry.asStateFlow()
    
    init {
        loadMonthEntries()
    }
    
    /**
     * Lädt alle Einträge für den aktuellen Monat
     */
    fun loadMonthEntries() {
        viewModelScope.launch {
            val month = _currentMonth.value
            val startDate = DateUtils.dateToString(month.atDay(1))
            val endDate = DateUtils.dateToString(month.atEndOfMonth())
            
            val entries = timeEntryDao.getEntriesByDateRange(startDate, endDate)
            _monthEntries.value = entries
            
            // Erstelle fehlende Einträge für den Monat
            ensureMonthEntriesExist(month)
        }
    }
    
    /**
     * Wechselt zum vorherigen Monat
     * Begrenzt auf den ersten Montag des aktiven Arbeitsjahres
     */
    fun previousMonth() {
        viewModelScope.launch {
            val newMonth = _currentMonth.value.minusMonths(1)

            // Prüfe über YearBoundaryService ob Navigation erlaubt ist
            if (yearBoundaryService.canNavigateToMonth(newMonth)) {
                _currentMonth.value = newMonth
                loadMonthEntries()
            }
        }
    }

    /**
     * Wechselt zum nächsten Monat
     * Begrenzt auf den letzten Freitag vor dem ersten Montag des nächsten Jahres
     */
    fun nextMonth() {
        viewModelScope.launch {
            val newMonth = _currentMonth.value.plusMonths(1)

            // Prüfe über YearBoundaryService ob Navigation erlaubt ist
            if (yearBoundaryService.canNavigateToMonth(newMonth)) {
                _currentMonth.value = newMonth
                loadMonthEntries()
            }
        }
    }

    /**
     * Setzt einen spezifischen Monat (z.B. für Pager-Navigation oder "Heute"-Button)
     */
    fun setMonth(month: YearMonth) {
        viewModelScope.launch {
            if (yearBoundaryService.canNavigateToMonth(month)) {
                _currentMonth.value = month
                loadMonthEntries()
            }
        }
    }

    /**
     * Wählt einen Eintrag aus (für Detail-Ansicht)
     */
    fun selectEntry(date: String) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(date)
            _selectedEntry.value = entry
        }
    }
    
    /**
     * Aktualisiert einen Eintrag
     */
    fun updateEntry(
        date: String,
        startZeit: Int?,
        endZeit: Int?,
        pauseMinuten: Int,
        typ: String,
        notiz: String,
        urlaubsJahr: Int? = null
    ) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(date) ?: return@launch

            // Zeiten werden jetzt bei ALLEN Typen gespeichert (auch U/K/F/AB)
            timeEntryDao.update(entry.copy(
                startZeit = startZeit,
                endZeit = endZeit,
                pauseMinuten = pauseMinuten,
                typ = typ,
                notiz = notiz,
                urlaubsJahr = urlaubsJahr,
                isManualEntry = true,
                updatedAt = System.currentTimeMillis()
            ))

            loadMonthEntries()
        }
    }

    /**
     * Löscht einen Eintrag (setzt alle Werte zurück auf leer)
     * Speichert den Eintrag für Undo-Funktion
     */
    fun deleteEntry(date: String) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(date) ?: return@launch

            // Speichere Original-Eintrag für Undo
            _deletedEntry.value = entry

            // Setze alle Werte zurück auf Standard (leerer Eintrag)
            timeEntryDao.update(entry.copy(
                startZeit = null,
                endZeit = null,
                pauseMinuten = 0,
                typ = TimeEntry.TYP_NORMAL,
                notiz = "",
                urlaubsJahr = null,
                isManualEntry = false,
                updatedAt = System.currentTimeMillis()
            ))

            loadMonthEntries()
        }
    }

    /**
     * Stellt einen gelöschten Eintrag wieder her
     */
    fun undoDeleteEntry() {
        viewModelScope.launch {
            val entry = _deletedEntry.value ?: return@launch

            timeEntryDao.update(entry)
            _deletedEntry.value = null
            loadMonthEntries()
        }
    }

    /**
     * Löscht den gespeicherten Eintrag (nach Timeout oder wenn User abbricht)
     */
    fun clearDeletedEntry() {
        _deletedEntry.value = null
    }
    
    /**
     * Erstellt fehlende Einträge für einen Monat
     * Erstellt Einträge bis zu 3 Monate in die Zukunft (für Planung & Urlaubseintragungen)
     */
    private suspend fun ensureMonthEntriesExist(month: YearMonth) {
        val settings = settingsDao.getSettings()
        val daysInMonth = month.lengthOfMonth()
        val today = LocalDate.now()
        val futureLimit = today.plusMonths(3)  // Erlaube Einträge bis +3 Monate

        for (day in 1..daysInMonth) {
            val date = month.atDay(day)

            // Überspringe Tage, die mehr als 3 Monate in der Zukunft liegen
            if (date.isAfter(futureLimit)) {
                continue
            }

            val dateString = DateUtils.dateToString(date)
            val existing = timeEntryDao.getEntryByDate(dateString)

            if (existing == null) {
                // Sichere Berechnung von Wochennummer und Jahr mit Fallback
                val weekNumber = try {
                    DateUtils.getCustomWeekOfYear(date, settings?.ersterMontagImJahr)
                } catch (e: Exception) {
                    DateUtils.getWeekOfYear(date)
                }

                val year = try {
                    if (settings?.ersterMontagImJahr != null) {
                        DateUtils.getCustomWeekBasedYear(date, settings.ersterMontagImJahr)
                    } else {
                        DateUtils.getWeekBasedYear(date)
                    }
                } catch (e: Exception) {
                    date.year
                }

                val entry = TimeEntry(
                    datum = dateString,
                    wochentag = DateUtils.getWeekdayShort(date),
                    kalenderwoche = weekNumber,
                    jahr = year,
                    startZeit = null,
                    endZeit = null,
                    pauseMinuten = 0,
                    sollMinuten = calculateSollMinuten(date, settings),
                    typ = TimeEntry.TYP_NORMAL
                )

                timeEntryDao.insert(entry)
            }
        }

        // Lade Einträge neu
        val startDate = DateUtils.dateToString(month.atDay(1))
        val endDate = DateUtils.dateToString(month.atEndOfMonth())
        val entries = timeEntryDao.getEntriesByDateRange(startDate, endDate)
        _monthEntries.value = entries
    }
    
    /**
     * Berechnet Soll-Minuten für einen Tag
     * Verwendet die Standardberechnung (Wochenstunden / Arbeitstage)
     * Individuelle Sollzeiten werden über SollZeitVorlagen direkt in TimeEntry gesetzt
     */
    private fun calculateSollMinuten(date: LocalDate, settings: com.arbeitszeit.tracker.data.entity.UserSettings?): Int {
        if (settings == null) return 0

        // Prüfe ob der Tag ein Arbeitstag ist
        val dayOfWeek = date.dayOfWeek.value
        if (!settings.isWorkingDay(dayOfWeek)) {
            return 0  // Kein Arbeitstag = 0 Sollminuten
        }

        // Standard: Wochenstunden / Arbeitstage
        return settings.wochenStundenMinuten / settings.arbeitsTageProWoche
    }
    
    /**
     * Gibt Status-Farbe für einen Tag zurück
     */
    fun getEntryStatus(entry: TimeEntry?): EntryStatus {
        return when {
            entry == null -> EntryStatus.MISSING
            entry.typ != TimeEntry.TYP_NORMAL -> EntryStatus.SPECIAL
            entry.isComplete() -> EntryStatus.COMPLETE
            entry.startZeit != null || entry.endZeit != null -> EntryStatus.PARTIAL
            else -> EntryStatus.EMPTY
        }
    }
}

enum class EntryStatus {
    COMPLETE,   // Grün
    PARTIAL,    // Gelb
    EMPTY,      // Rot
    MISSING,    // Grau
    SPECIAL     // Blau (U/K/F/AB)
}
