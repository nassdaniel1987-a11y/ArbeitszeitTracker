package com.arbeitszeit.tracker.wear.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.wear.data.WearDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class HomeUiState(
    val todayEntry: TimeEntry? = null,
    val isWorking: Boolean = false,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val settings: UserSettings? = null,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WearDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()

    private val today = LocalDate.now()
    private val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load settings
                val settings = settingsDao.getSettings()

                // Load today's entry
                val entry = timeEntryDao.getEntryByDate(dateString)

                // Calculate week minutes
                val weekField = WeekFields.of(Locale.GERMANY)
                val currentWeek = today.get(weekField.weekOfWeekBasedYear())
                val currentYear = today.get(weekField.weekBasedYear())
                val weekEntries = timeEntryDao.getEntriesByWeek(currentYear, currentWeek)
                val weekMinutes = weekEntries.sumOf { it.getIstMinuten() }

                _uiState.value = HomeUiState(
                    todayEntry = entry,
                    isWorking = entry?.startZeit != null && entry?.endZeit == null,
                    todayMinutes = entry?.getIstMinuten() ?: 0,
                    weekMinutes = weekMinutes,
                    settings = settings,
                    isLoading = false
                )
            } catch (e: Exception) {
                // Fallback on error
                _uiState.value = HomeUiState(
                    isLoading = false
                )
            }
        }
    }

    fun onCheckIn() {
        viewModelScope.launch {
            try {
                val now = java.time.LocalTime.now()
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
                    timeEntryDao.update(entry.copy(startZeit = minutesSinceMidnight, endZeit = null))
                }
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onCheckOut() {
        viewModelScope.launch {
            try {
                val now = java.time.LocalTime.now()
                val minutesSinceMidnight = now.hour * 60 + now.minute

                val entry = timeEntryDao.getEntryByDate(dateString)

                if (entry != null && entry.startZeit != null) {
                    timeEntryDao.update(entry.copy(endZeit = minutesSinceMidnight))
                }
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onCancelWork() {
        viewModelScope.launch {
            try {
                val entry = timeEntryDao.getEntryByDate(dateString)

                if (entry != null) {
                    timeEntryDao.update(
                        entry.copy(
                            startZeit = null,
                            endZeit = null
                        )
                    )
                }
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
