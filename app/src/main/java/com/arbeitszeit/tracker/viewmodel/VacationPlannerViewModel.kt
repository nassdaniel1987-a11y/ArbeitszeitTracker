package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.ai.GeminiClient
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.ClosingDay
import com.arbeitszeit.tracker.data.entity.SchoolHoliday
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.vacation.SchoolHolidayManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel für die KI-gestützte Urlaubsplanung
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VacationPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val settingsDao = database.userSettingsDao()
    private val timeEntryDao = database.timeEntryDao()
    private val closingDayDao = database.closingDayDao()
    private val schoolHolidayDao = database.schoolHolidayDao()
    private val yearSettingsDao = database.yearSettingsDao()

    private val geminiClient = GeminiClient()
    private val schoolHolidayManager = SchoolHolidayManager(schoolHolidayDao)

    // User Settings
    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Active Year Settings
    private val activeYear = yearSettingsDao.getActiveYearFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // UI State - selectedYear wird vom aktiven Jahr überschrieben
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = activeYear
        .map { it?.year ?: LocalDate.now().year }
        .stateIn(viewModelScope, SharingStarted.Lazily, LocalDate.now().year)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Schließtage für das ausgewählte Jahr
     */
    val closingDays: StateFlow<List<ClosingDay>> = selectedYear
        .flatMapLatest { year ->
            closingDayDao.getClosingDaysByYearFlow(year)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Schulferien für das ausgewählte Jahr
     */
    val schoolHolidays: StateFlow<List<SchoolHoliday>> = combine(
        selectedYear,
        userSettings
    ) { year, settings ->
        Pair(year, settings?.bundesland)
    }
        .flatMapLatest { (year, bundesland) ->
            if (bundesland != null) {
                // Initialisiere Schulferien falls nötig
                viewModelScope.launch {
                    schoolHolidayManager.initializeHolidays(bundesland, year)
                }
                schoolHolidayDao.getHolidaysByBundeslandAndYearFlow(bundesland, year)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Urlaubstage im ausgewählten Jahr
     * Wichtig: Berücksichtigt auch Resturlaub (urlaubsJahr-Feld)
     */
    val vacationDays: StateFlow<List<TimeEntry>> = selectedYear
        .flatMapLatest { year ->
            timeEntryDao.getAllEntriesFlow()
                .map { allEntries ->
                    // Filtere Urlaubstage die FÜR dieses Jahr zählen (auch wenn Kalenderjahr anders)
                    // Beispiel: Urlaub am 2.1.2026 mit urlaubsJahr=2025 zählt für 2025!
                    allEntries.filter { entry ->
                        entry.typ == TimeEntry.TYP_URLAUB && entry.getUrlaubsJahr() == year
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Verfügbare Urlaubstage (Anspruch - Genommen)
     */
    val availableVacationDays: StateFlow<Int> = combine(
        activeYear,
        vacationDays
    ) { yearSettings, taken ->
        val anspruch = yearSettings?.urlaubsanspruchTage ?: 30
        val genommen = taken.size
        (anspruch - genommen).coerceAtLeast(0)
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, 30)

    /**
     * Wechselt das Jahr
     */
    fun setYear(year: Int) {
        _selectedYear.value = year
        _aiSuggestion.value = null  // Reset AI suggestion
        _errorMessage.value = null
    }

    /**
     * Startet KI-Optimierung
     */
    fun optimizeVacation(
        preferences: String,
        customStartDate: String? = null,
        customEndDate: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val settings = userSettings.value
                if (settings == null) {
                    _errorMessage.value = "Bitte erst Einstellungen ausfüllen"
                    return@launch
                }

                if (settings.bundesland.isNullOrEmpty()) {
                    _errorMessage.value = "Bitte Bundesland in Einstellungen auswählen"
                    return@launch
                }

                if (!geminiClient.isConfigured()) {
                    _errorMessage.value = "Gemini API Key nicht konfiguriert. Bitte in local.properties eintragen."
                    return@launch
                }

                // Bereite Schließtage für Prompt vor
                val closingDayStrings = closingDays.value.map { closingDay ->
                    "${closingDay.getFormattedDateRange()}: ${closingDay.title}"
                }

                // Bereite genommene Urlaubstage für Prompt vor
                val takenVacationStrings = vacationDays.value.map { entry ->
                    "${entry.datum}: ${entry.notiz.ifEmpty { "Urlaub" }}"
                }

                // Rufe Gemini API auf
                val result = geminiClient.optimizeVacation(
                    availableVacationDays = availableVacationDays.value,
                    bundesland = settings.bundesland!!,
                    closingDays = closingDayStrings,
                    takenVacationDays = takenVacationStrings,
                    preferences = preferences,
                    year = selectedYear.value,
                    customStartDate = customStartDate,
                    customEndDate = customEndDate
                )

                result.fold(
                    onSuccess = { suggestion ->
                        _aiSuggestion.value = suggestion
                    },
                    onFailure = { error ->
                        _errorMessage.value = "KI-Fehler: ${error.message}"
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Fehler: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Löscht Fehlermeldung
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Löscht KI-Vorschlag
     */
    fun clearAiSuggestion() {
        _aiSuggestion.value = null
    }

    /**
     * Fügt einen Urlaubstag hinzu oder entfernt ihn wenn schon vorhanden
     */
    fun toggleVacationDay(date: LocalDate, note: String = "") {
        viewModelScope.launch {
            try {
                val dateString = com.arbeitszeit.tracker.utils.DateUtils.dateToString(date)

                // Prüfe ob schon ein Eintrag existiert
                val existingEntry = timeEntryDao.getEntryByDate(dateString)

                if (existingEntry != null && existingEntry.typ == TimeEntry.TYP_URLAUB) {
                    // Urlaub entfernen: Setze Typ zurück auf NORMAL
                    timeEntryDao.update(
                        existingEntry.copy(
                            typ = TimeEntry.TYP_NORMAL,
                            notiz = "",
                            startZeit = null,
                            endZeit = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    val settings = userSettings.value ?: return@launch
                    val sollMinuten = if (settings.isWorkingDay(date.dayOfWeek.value)) {
                        settings.wochenStundenMinuten / settings.arbeitsTageProWoche
                    } else {
                        0
                    }

                    if (existingEntry != null) {
                        // Update existierenden Eintrag zu Urlaub
                        timeEntryDao.update(
                            existingEntry.copy(
                                typ = TimeEntry.TYP_URLAUB,
                                notiz = note,
                                startZeit = null,
                                endZeit = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // Neuen Urlaubs-Eintrag erstellen
                        val entry = TimeEntry(
                            datum = dateString,
                            wochentag = com.arbeitszeit.tracker.utils.DateUtils.getWeekdayShort(date),
                            kalenderwoche = com.arbeitszeit.tracker.utils.DateUtils.getWeekOfYear(date),
                            jahr = date.year,
                            startZeit = null,
                            endZeit = null,
                            sollMinuten = sollMinuten,
                            typ = TimeEntry.TYP_URLAUB,
                            notiz = note
                        )
                        timeEntryDao.insert(entry)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Fehler beim Speichern: ${e.message}"
            }
        }
    }

    /**
     * Berechnet gesamte Urlaubsstatistik
     */
    data class VacationStats(
        val anspruch: Int,
        val genommen: Int,
        val verfuegbar: Int,
        val closingDayCount: Int,
        val holidayPeriods: Int
    )

    val vacationStats: StateFlow<VacationStats> = combine(
        activeYear,
        vacationDays,
        closingDays,
        schoolHolidays
    ) { yearSettings, taken, closing, holidays ->
        VacationStats(
            anspruch = yearSettings?.urlaubsanspruchTage ?: 30,
            genommen = taken.size,
            verfuegbar = (yearSettings?.urlaubsanspruchTage ?: 30) - taken.size,
            closingDayCount = closing.sumOf { it.getDurationDays() },
            holidayPeriods = holidays.size
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, VacationStats(30, 0, 30, 0, 0))
}
