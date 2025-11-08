package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()
    
    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Settings
    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Heutiger Eintrag
    private val todayDate = DateUtils.today()
    val todayEntry: StateFlow<TimeEntry?> = timeEntryDao.getEntryByDateFlow(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Letzte 7 Tage
    val weekEntries: StateFlow<List<TimeEntry>> = flow {
        val today = LocalDate.now()
        val weekDays = DateUtils.getDaysOfWeek(today)
        val startDate = DateUtils.dateToString(weekDays.first())
        val endDate = DateUtils.dateToString(weekDays.last())
        
        emitAll(timeEntryDao.getEntriesByDateRangeFlow(startDate, endDate))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        // Erstelle heute-Eintrag falls nicht vorhanden
        viewModelScope.launch {
            ensureTodayEntryExists()
        }
    }
    
    /**
     * Stellt sicher dass für heute ein Eintrag existiert
     */
    private suspend fun ensureTodayEntryExists() {
        val existing = timeEntryDao.getEntryByDate(todayDate)
        if (existing == null) {
            val today = LocalDate.now()
            val settings = settingsDao.getSettings()
            
            val entry = TimeEntry(
                datum = todayDate,
                wochentag = DateUtils.getWeekdayShort(today),
                kalenderwoche = DateUtils.getWeekOfYear(today),
                jahr = DateUtils.getWeekBasedYear(today),
                startZeit = null,
                endZeit = null,
                pauseMinuten = 0,
                sollMinuten = calculateSollMinuten(today, settings),
                typ = TimeEntry.TYP_NORMAL
            )
            
            timeEntryDao.insert(entry)
        }
    }
    
    /**
     * Berechnet Soll-Minuten für einen Tag basierend auf Settings
     */
    private fun calculateSollMinuten(date: LocalDate, settings: UserSettings?): Int {
        if (settings == null) return 0

        // Wochenende = 0 (außer individuelle Zeiten sind gesetzt)
        if (DateUtils.isWeekend(date)) {
            val dayOfWeek = date.dayOfWeek.value
            return settings.getSollMinutenForDay(dayOfWeek) ?: 0
        }

        // Prüfe ob individuelle Tages-Soll-Zeiten definiert sind
        val dayOfWeek = date.dayOfWeek.value
        val individualSollMinuten = settings.getSollMinutenForDay(dayOfWeek)

        // Wenn individuelle Zeit vorhanden, verwende diese
        if (individualSollMinuten != null) {
            return individualSollMinuten
        }

        // Sonst: Wochenstunden / Arbeitstage
        return settings.wochenStundenMinuten / settings.arbeitsTageProWoche
    }
    
    /**
     * Schnell-Stempel: Setzt Start oder Ende automatisch
     */
    fun quickStamp() {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate)
            val currentTime = TimeUtils.currentTimeInMinutes()
            
            if (entry == null) {
                // Neuer Eintrag mit Start
                ensureTodayEntryExists()
                val newEntry = timeEntryDao.getEntryByDate(todayDate)
                newEntry?.let {
                    timeEntryDao.update(it.copy(
                        startZeit = currentTime,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            } else {
                // Entscheide: Start oder Ende?
                if (entry.startZeit == null) {
                    // Noch kein Start -> setze Start
                    timeEntryDao.update(entry.copy(
                        startZeit = currentTime,
                        updatedAt = System.currentTimeMillis()
                    ))
                } else if (entry.endZeit == null) {
                    // Start vorhanden, Ende fehlt -> setze Ende
                    timeEntryDao.update(entry.copy(
                        endZeit = currentTime,
                        updatedAt = System.currentTimeMillis()
                    ))
                } else {
                    // Beide vorhanden -> überschreibe Ende
                    timeEntryDao.update(entry.copy(
                        endZeit = currentTime,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
        }
    }
    
    /**
     * Setzt Arbeitsbeginn
     */
    fun setStartTime(minutes: Int) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate) ?: return@launch
            timeEntryDao.update(entry.copy(
                startZeit = minutes,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
    
    /**
     * Setzt Arbeitsende
     */
    fun setEndTime(minutes: Int) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate) ?: return@launch
            timeEntryDao.update(entry.copy(
                endZeit = minutes,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
    
    /**
     * Setzt Pause
     */
    fun setPause(minutes: Int) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate) ?: return@launch
            timeEntryDao.update(entry.copy(
                pauseMinuten = minutes,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
    
    /**
     * Setzt Typ (NORMAL, U, K, F, AB)
     */
    fun setTyp(typ: String) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate) ?: return@launch
            
            // Bei Typ != NORMAL: Zeiten löschen
            if (typ != TimeEntry.TYP_NORMAL) {
                timeEntryDao.update(entry.copy(
                    typ = typ,
                    startZeit = null,
                    endZeit = null,
                    pauseMinuten = 0,
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                timeEntryDao.update(entry.copy(
                    typ = typ,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }
    
    /**
     * Berechnet Wochen-Statistik
     */
    fun getWeekSummary(): WeekSummary {
        val entries = weekEntries.value
        
        val totalSoll = entries.sumOf { it.sollMinuten }
        val totalIst = entries.sumOf { it.getIstMinuten() }
        val totalDifferenz = totalIst - totalSoll
        
        return WeekSummary(
            sollMinuten = totalSoll,
            istMinuten = totalIst,
            differenzMinuten = totalDifferenz,
            completedDays = entries.count { it.isComplete() },
            totalDays = entries.size
        )
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WeekSummary(
    val sollMinuten: Int,
    val istMinuten: Int,
    val differenzMinuten: Int,
    val completedDays: Int,
    val totalDays: Int
)
