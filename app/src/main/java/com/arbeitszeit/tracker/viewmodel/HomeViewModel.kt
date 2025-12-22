package com.arbeitszeit.tracker.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()
    private val yearSettingsDao = database.yearSettingsDao()
    private val workLocationDao = database.workLocationDao()
    private val sollZeitVorlageDao = database.sollZeitVorlageDao()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Running Time Tracker für Auto-Start
    private val runningTimeTracker = RunningTimeTracker(application)
    val runningTimeState = runningTimeTracker.runningState

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Ausgewählte Woche (Referenzdatum)
    private val _selectedWeekDate = MutableStateFlow(LocalDate.now())
    val selectedWeekDate: StateFlow<LocalDate> = _selectedWeekDate.asStateFlow()

    // Geofencing Status
    private val _locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Unknown)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    // Für Undo-Funktion: Speichert gelöschten Eintrag temporär
    private val _deletedEntry = MutableStateFlow<TimeEntry?>(null)
    val deletedEntry: StateFlow<TimeEntry?> = _deletedEntry.asStateFlow()

    // Settings
    val userSettings: StateFlow<UserSettings?> = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Active Year Settings
    private val activeYear = yearSettingsDao.getActiveYearFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Alle verfügbaren Arbeitszeitvorlagen
    val vorlagen = sollZeitVorlageDao.getAllVorlagenFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Standard-Vorlage
    val defaultVorlage = sollZeitVorlageDao.getDefaultVorlageFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Heutiger Eintrag
    private val todayDate = DateUtils.today()
    val todayEntry: StateFlow<TimeEntry?> = timeEntryDao.getEntryByDateFlow(todayDate)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Einträge der ausgewählten Woche - nur Arbeitstage
    val weekEntries: StateFlow<List<TimeEntry>> = _selectedWeekDate
        .flatMapLatest { weekDate ->
            val weekDays = DateUtils.getDaysOfWeek(weekDate)
            val startDate = DateUtils.dateToString(weekDays.first())
            val endDate = DateUtils.dateToString(weekDays.last())

            // Verwende Flow aus Datenbank für Live-Updates!
            timeEntryDao.getEntriesByDateRangeFlow(startDate, endDate)
        }
        .combine(userSettings) { entries, settings ->
            // Filtere nur Arbeitstage
            if (settings != null) {
                entries.filter { entry ->
                    val date = LocalDate.parse(entry.datum)
                    val dayOfWeek = date.dayOfWeek.value // 1=Mo, 7=So
                    settings.isWorkingDay(dayOfWeek)
                }
            } else {
                entries
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        // Erstelle heute-Eintrag falls nicht vorhanden
        viewModelScope.launch {
            ensureTodayEntryExists()
            checkLocationStatus()
        }
    }

    /**
     * Prüft ob der Nutzer an einem Arbeitsort ist
     */
    fun checkLocationStatus() {
        viewModelScope.launch {
            try {
                val settings = settingsDao.getSettings()
                if (settings?.geofencingEnabled != true) {
                    _locationStatus.value = LocationStatus.GeofencingDisabled
                    return@launch
                }

                val locations = workLocationDao.getEnabledLocations()
                if (locations.isEmpty()) {
                    _locationStatus.value = LocationStatus.NoLocations
                    return@launch
                }

                // Prüfe Berechtigung
                if (ContextCompat.checkSelfPermission(
                        getApplication(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    _locationStatus.value = LocationStatus.NoPermission
                    return@launch
                }

                // Hole aktuelle Position
                val currentLocation = fusedLocationClient.lastLocation.await()
                if (currentLocation == null) {
                    _locationStatus.value = LocationStatus.LocationUnavailable
                    return@launch
                }

                // Prüfe ob in einem Geofence (unterstützt Kreis und Polygon)
                val atWorkLocation = locations.firstOrNull { location ->
                    location.containsPoint(currentLocation.latitude, currentLocation.longitude)
                }

                _locationStatus.value = if (atWorkLocation != null) {
                    LocationStatus.AtWork(atWorkLocation)
                } else {
                    LocationStatus.NotAtWork
                }

            } catch (e: Exception) {
                _locationStatus.value = LocationStatus.Error(e.message ?: "Unbekannter Fehler")
            }
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
            val yearSettings = yearSettingsDao.getActiveYear()
            val defaultVorlage = sollZeitVorlageDao.getDefaultVorlage()

            // Berechne Sollminuten: Entweder aus Default-Vorlage oder aus Settings
            val dayOfWeek = today.dayOfWeek.value
            val sollMinuten = if (defaultVorlage != null) {
                defaultVorlage.getSollMinutenForDay(dayOfWeek)
            } else {
                calculateSollMinuten(today, settings)
            }

            val entry = TimeEntry(
                datum = todayDate,
                wochentag = DateUtils.getWeekdayShort(today),
                kalenderwoche = DateUtils.getCustomWeekOfYear(today, yearSettings?.ersterMontagImJahr),
                jahr = DateUtils.getCustomWeekBasedYear(today, yearSettings?.ersterMontagImJahr),
                startZeit = null,
                endZeit = null,
                pauseMinuten = 0,
                sollMinuten = sollMinuten,
                sollZeitVorlageName = defaultVorlage?.name,
                typ = TimeEntry.TYP_NORMAL
            )

            timeEntryDao.insert(entry)
        }
    }
    
    /**
     * Berechnet Soll-Minuten für einen Tag basierend auf Settings
     * Verwendet die Standardberechnung (Wochenstunden / Arbeitstage)
     * Individuelle Sollzeiten werden über SollZeitVorlagen direkt in TimeEntry gesetzt
     */
    private fun calculateSollMinuten(date: LocalDate, settings: UserSettings?): Int {
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
     * Zeiten bleiben erhalten, auch bei U/K/F/AB
     */
    fun setTyp(typ: String) {
        viewModelScope.launch {
            val entry = timeEntryDao.getEntryByDate(todayDate) ?: return@launch

            // Zeiten werden jetzt bei ALLEN Typen beibehalten (auch U/K/F/AB)
            timeEntryDao.update(entry.copy(
                typ = typ,
                updatedAt = System.currentTimeMillis()
            ))
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
                isManualEntry = false,
                updatedAt = System.currentTimeMillis()
            ))
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
        }
    }

    /**
     * Löscht den gespeicherten Eintrag (nach Timeout oder wenn User abbricht)
     */
    fun clearDeletedEntry() {
        _deletedEntry.value = null
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

    /**
     * Navigiert zur vorherigen Woche
     * Begrenzt auf den ersten Montag des aktiven Arbeitsjahres
     */
    fun previousWeek() {
        val yearSettings = activeYear.value
        if (yearSettings != null) {
            val firstMonday = LocalDate.parse(yearSettings.ersterMontagImJahr)
            val newWeekDate = _selectedWeekDate.value.minusWeeks(1)

            // Nur navigieren wenn wir nicht vor den Jahresbeginn gehen
            if (newWeekDate >= firstMonday) {
                _selectedWeekDate.value = newWeekDate
            }
        } else {
            _selectedWeekDate.value = _selectedWeekDate.value.minusWeeks(1)
        }
    }

    /**
     * Navigiert zur nächsten Woche
     * Begrenzt auf den letzten Freitag vor dem ersten Montag des nächsten Jahres
     */
    fun nextWeek() {
        val yearSettings = activeYear.value
        if (yearSettings != null) {
            val nextYear = yearSettings.year + 1
            val firstMondayNextYear = findFirstMonday(nextYear)
            val lastFriday = firstMondayNextYear.minusDays(3)

            val newWeekDate = _selectedWeekDate.value.plusWeeks(1)

            // Nur navigieren wenn wir nicht über das Jahresende hinaus gehen
            if (newWeekDate <= lastFriday) {
                _selectedWeekDate.value = newWeekDate
            }
        } else {
            _selectedWeekDate.value = _selectedWeekDate.value.plusWeeks(1)
        }
    }

    /**
     * Findet den ersten Montag eines Jahres
     */
    private fun findFirstMonday(year: Int): LocalDate {
        var date = LocalDate.of(year, 1, 1)
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            date = date.plusDays(1)
        }
        return date
    }

    /**
     * Springt zurück zur aktuellen Woche
     */
    fun goToCurrentWeek() {
        _selectedWeekDate.value = LocalDate.now()
    }

    /**
     * Prüft ob die aktuelle Woche angezeigt wird
     */
    fun isCurrentWeek(): Boolean {
        val yearSettings = activeYear.value
        val firstMonday = yearSettings?.ersterMontagImJahr

        val selectedWeek = DateUtils.getCustomWeekOfYear(_selectedWeekDate.value, firstMonday)
        val selectedYear = DateUtils.getCustomWeekBasedYear(_selectedWeekDate.value, firstMonday)
        val currentWeek = DateUtils.getCustomWeekOfYear(LocalDate.now(), firstMonday)
        val currentYear = DateUtils.getCustomWeekBasedYear(LocalDate.now(), firstMonday)

        return selectedWeek == currentWeek && selectedYear == currentYear
    }

    /**
     * Wendet eine Arbeitszeitvorlage auf die gesamte Woche an
     * Berechnet die Soll-Zeiten für alle Tage basierend auf der Vorlage
     */
    fun applyVorlageToWeek(vorlageId: Long) {
        viewModelScope.launch {
            val vorlage = sollZeitVorlageDao.getVorlageById(vorlageId) ?: return@launch
            val weekDays = DateUtils.getDaysOfWeek(_selectedWeekDate.value)

            for (day in weekDays) {
                val datum = DateUtils.dateToString(day)
                val entry = timeEntryDao.getEntryByDate(datum)

                if (entry != null) {
                    val dayOfWeek = day.dayOfWeek.value // 1=Mo, 7=So
                    val sollMinuten = vorlage.getSollMinutenForDay(dayOfWeek)

                    timeEntryDao.update(entry.copy(
                        sollMinuten = sollMinuten,
                        sollZeitVorlageName = vorlage.name,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
        }
    }

    /**
     * Wendet eine Arbeitszeitvorlage auf einen einzelnen Tag an
     */
    fun applyVorlageToDay(datum: String, vorlageId: Long) {
        viewModelScope.launch {
            val vorlage = sollZeitVorlageDao.getVorlageById(vorlageId) ?: return@launch
            val entry = timeEntryDao.getEntryByDate(datum) ?: return@launch

            val date = LocalDate.parse(datum)
            val dayOfWeek = date.dayOfWeek.value
            val sollMinuten = vorlage.getSollMinutenForDay(dayOfWeek)

            timeEntryDao.update(entry.copy(
                sollMinuten = sollMinuten,
                sollZeitVorlageName = vorlage.name,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Beendet die laufende Zeiterfassung und speichert den Eintrag
     */
    fun stopRunningTimeTracking(pauseMinutes: Int) {
        viewModelScope.launch {
            val runningState = runningTimeTracker.stopTracking() ?: return@launch

            // Berechne End-Zeit und Dauer
            val endTime = runningState.calculateEndTime()
            val endMinutes = endTime.hour * 60 + endTime.minute

            // Hole Settings für Soll-Zeit
            val settings = settingsDao.getSettings()
            val dayOfWeek = LocalDate.now().dayOfWeek.value
            val sollMinuten = if (settings?.isWorkingDay(dayOfWeek) == true) {
                settings.wochenStundenMinuten / settings.arbeitsTageProWoche
            } else {
                0
            }

            // Erstelle TimeEntry
            val today = DateUtils.today()
            val existingEntry = timeEntryDao.getEntryByDate(today)

            if (existingEntry != null) {
                // Update existing entry
                timeEntryDao.update(existingEntry.copy(
                    startZeit = runningState.startTime.hour * 60 + runningState.startTime.minute,
                    endZeit = endMinutes,
                    pauseMinuten = pauseMinutes,
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                // Create new entry
                timeEntryDao.insert(TimeEntry(
                    datum = today,
                    wochentag = DateUtils.getWeekdayShort(LocalDate.now()),
                    kalenderwoche = DateUtils.getCustomWeekOfYear(LocalDate.now(), settings?.ersterMontagImJahr),
                    jahr = LocalDate.now().year,
                    startZeit = runningState.startTime.hour * 60 + runningState.startTime.minute,
                    endZeit = endMinutes,
                    pauseMinuten = pauseMinutes,
                    sollMinuten = sollMinuten,
                    typ = TimeEntry.TYP_NORMAL,
                    notiz = if (runningState.isAutoStart) "Auto-Start" else "",
                    isManualEntry = false
                ))
            }
        }
    }

    /**
     * Verwirft die laufende Zeiterfassung ohne zu speichern
     */
    fun discardRunningTimeTracking() {
        runningTimeTracker.stopTracking()
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

sealed class LocationStatus {
    object Unknown : LocationStatus()
    object GeofencingDisabled : LocationStatus()
    object NoLocations : LocationStatus()
    object NoPermission : LocationStatus()
    object LocationUnavailable : LocationStatus()
    object NotAtWork : LocationStatus()
    data class AtWork(val location: WorkLocation) : LocationStatus()
    data class Error(val message: String) : LocationStatus()
}
