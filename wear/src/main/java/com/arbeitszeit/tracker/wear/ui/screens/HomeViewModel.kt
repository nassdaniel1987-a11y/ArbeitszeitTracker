package com.arbeitszeit.tracker.wear.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.wear.data.WearDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class HomeUiState(
    val todayEntry: TimeEntry? = null,
    val isWorking: Boolean = false,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val currentTime: String = "",
    val settings: UserSettings? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WearDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        updateCurrentTime()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load settings
            val settings = settingsDao.getSettings()
            _uiState.value = _uiState.value.copy(settings = settings)

            // Load today's entry
            val today = LocalDate.now()
            val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val entry = timeEntryDao.getEntryByDate(dateString)

            // Check if currently working
            val isWorking = entry?.startZeit != null && entry.endZeit == null

            // Calculate today's minutes
            val todayMinutes = entry?.getIstMinuten() ?: 0

            // Calculate week's minutes
            val weekField = WeekFields.of(Locale.GERMANY)
            val currentWeek = today.get(weekField.weekOfWeekBasedYear())
            val currentYear = today.get(weekField.weekBasedYear())
            val weekEntries = timeEntryDao.getEntriesByWeek(currentYear, currentWeek)
            val weekMinutes = weekEntries.sumOf { it.getIstMinuten() }

            _uiState.value = _uiState.value.copy(
                todayEntry = entry,
                isWorking = isWorking,
                todayMinutes = todayMinutes,
                weekMinutes = weekMinutes
            )
        }
    }

    private fun updateCurrentTime() {
        viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                _uiState.value = _uiState.value.copy(currentTime = now.format(formatter))
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    fun onCheckIn() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val now = LocalTime.now()
            val minutesSinceMidnight = now.hour * 60 + now.minute

            val entry = timeEntryDao.getEntryByDate(dateString)

            if (entry == null) {
                // Create new entry
                val weekField = WeekFields.of(Locale.GERMANY)
                val currentWeek = today.get(weekField.weekOfWeekBasedYear())
                val currentYear = today.get(weekField.weekBasedYear())
                val dayOfWeek = today.dayOfWeek.value

                val wochentag = when (dayOfWeek) {
                    1 -> "Mo"
                    2 -> "Di"
                    3 -> "Mi"
                    4 -> "Do"
                    5 -> "Fr"
                    6 -> "Sa"
                    7 -> "So"
                    else -> ""
                }

                val settings = settingsDao.getSettings()
                val sollMinuten = if (settings?.isWorkingDay(dayOfWeek) == true) {
                    settings.wochenStundenMinuten / settings.arbeitsTageProWoche
                } else {
                    0
                }

                val newEntry = TimeEntry(
                    datum = dateString,
                    wochentag = wochentag,
                    kalenderwoche = currentWeek,
                    jahr = currentYear,
                    startZeit = minutesSinceMidnight,
                    endZeit = null,
                    sollMinuten = sollMinuten,
                    sollZeitVorlageName = "Normal"
                )

                timeEntryDao.insert(newEntry)
            } else {
                // Update existing entry
                timeEntryDao.update(entry.copy(startZeit = minutesSinceMidnight))
            }

            loadData()
        }
    }

    fun onCheckOut() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val now = LocalTime.now()
            val minutesSinceMidnight = now.hour * 60 + now.minute

            val entry = timeEntryDao.getEntryByDate(dateString)

            if (entry != null && entry.startZeit != null) {
                timeEntryDao.update(entry.copy(endZeit = minutesSinceMidnight))
                loadData()
            }
        }
    }

    fun onCancelWork() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val entry = timeEntryDao.getEntryByDate(dateString)

            if (entry != null) {
                timeEntryDao.update(
                    entry.copy(
                        startZeit = null,
                        endZeit = null
                    )
                )
                loadData()
            }
        }
    }
}
