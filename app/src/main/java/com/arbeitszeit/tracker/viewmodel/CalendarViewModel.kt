package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()
    
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    private val _monthEntries = MutableStateFlow<List<TimeEntry>>(emptyList())
    val monthEntries: StateFlow<List<TimeEntry>> = _monthEntries.asStateFlow()
    
    private val _selectedEntry = MutableStateFlow<TimeEntry?>(null)
    val selectedEntry: StateFlow<TimeEntry?> = _selectedEntry.asStateFlow()
    
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
     */
    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        loadMonthEntries()
    }
    
    /**
     * Wechselt zum nächsten Monat
     */
    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        loadMonthEntries()
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
        notiz: String
    ) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(date) ?: return@launch
            
            timeEntryDao.update(entry.copy(
                startZeit = if (typ == TimeEntry.TYP_NORMAL) startZeit else null,
                endZeit = if (typ == TimeEntry.TYP_NORMAL) endZeit else null,
                pauseMinuten = if (typ == TimeEntry.TYP_NORMAL) pauseMinuten else 0,
                typ = typ,
                notiz = notiz,
                isManualEntry = true,
                updatedAt = System.currentTimeMillis()
            ))
            
            loadMonthEntries()
        }
    }
    
    /**
     * Erstellt fehlende Einträge für einen Monat
     */
    private suspend fun ensureMonthEntriesExist(month: YearMonth) {
        val settings = settingsDao.getSettings()
        val daysInMonth = month.lengthOfMonth()
        
        for (day in 1..daysInMonth) {
            val date = month.atDay(day)
            val dateString = DateUtils.dateToString(date)
            val existing = timeEntryDao.getEntryByDate(dateString)
            
            if (existing == null) {
                val entry = TimeEntry(
                    datum = dateString,
                    wochentag = DateUtils.getWeekdayShort(date),
                    kalenderwoche = DateUtils.getWeekOfYear(date),
                    jahr = DateUtils.getWeekBasedYear(date),
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
     */
    private fun calculateSollMinuten(date: LocalDate, settings: com.arbeitszeit.tracker.data.entity.UserSettings?): Int {
        if (settings == null) return 0
        if (DateUtils.isWeekend(date)) return 0
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
