package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.year.NewYearSuggestion
import com.arbeitszeit.tracker.year.YearManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class YearUiState(
    val activeYear: YearSettings? = null,
    val allYears: List<YearSettings> = emptyList(),
    val isLoading: Boolean = false,
    val showNewYearDialog: Boolean = false,
    val newYearSuggestion: NewYearSuggestion? = null,
    val error: String? = null,
    val success: String? = null
)

class YearViewModel(application: Application) : AndroidViewModel(application) {

    private val yearManager = YearManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val yearSettingsDao = database.yearSettingsDao()

    private val _uiState = MutableStateFlow(YearUiState())
    val uiState: StateFlow<YearUiState> = _uiState.asStateFlow()

    init {
        loadYears()
        observeActiveYear()
    }

    /**
     * Lädt alle Jahre und das aktive Jahr
     */
    private fun loadYears() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val activeYear = yearManager.getActiveYear()
                val allYears = yearManager.getAllYears()

                _uiState.value = _uiState.value.copy(
                    activeYear = activeYear,
                    allYears = allYears,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden der Jahre: ${e.message}"
                )
            }
        }
    }

    /**
     * Beobachtet das aktive Jahr (für Live-Updates)
     */
    private fun observeActiveYear() {
        viewModelScope.launch {
            yearSettingsDao.getActiveYearFlow().collect { activeYear ->
                _uiState.value = _uiState.value.copy(activeYear = activeYear)
            }
        }
    }

    /**
     * Wechselt zu einem anderen Jahr
     */
    fun switchToYear(year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = yearManager.switchToYear(year)

            if (success) {
                loadYears()
                _uiState.value = _uiState.value.copy(
                    success = "Jahr $year aktiviert",
                    isLoading = false
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Wechseln zu Jahr $year"
                )
            }
        }
    }

    /**
     * Zeigt den "Neues Jahr anlegen" Dialog
     */
    fun showNewYearDialog() {
        viewModelScope.launch {
            val currentYear = LocalDate.now().year
            val nextYear = currentYear + 1

            // Prüfe ob nächstes Jahr bereits existiert
            if (yearManager.yearExists(nextYear)) {
                _uiState.value = _uiState.value.copy(
                    error = "Jahr $nextYear existiert bereits"
                )
                return@launch
            }

            // Lade Vorschlag
            val suggestion = yearManager.suggestNewYear(nextYear)

            _uiState.value = _uiState.value.copy(
                showNewYearDialog = true,
                newYearSuggestion = suggestion
            )
        }
    }

    /**
     * Schließt den "Neues Jahr" Dialog
     */
    fun dismissNewYearDialog() {
        _uiState.value = _uiState.value.copy(
            showNewYearDialog = false,
            newYearSuggestion = null
        )
    }

    /**
     * Erstellt ein neues Jahr
     *
     * @param year Jahr
     * @param ersterMontagImJahr Erster Montag (yyyy-MM-dd)
     * @param urlaubsanspruch Urlaubstage
     * @param uebertragUeberstunden Überstunden übertragen?
     * @param uebertragResturlaub Resturlaub übertragen?
     */
    fun createNewYear(
        year: Int,
        ersterMontagImJahr: String,
        urlaubsanspruch: Int,
        uebertragUeberstunden: Boolean,
        uebertragResturlaub: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = yearManager.createNewYear(
                year = year,
                ersterMontagImJahr = ersterMontagImJahr,
                urlaubsanspruch = urlaubsanspruch,
                uebertragUeberstunden = uebertragUeberstunden,
                uebertragResturlaub = uebertragResturlaub
            )

            if (result.isSuccess) {
                // Jahr erfolgreich erstellt
                loadYears()

                // Automatisch zu neuem Jahr wechseln
                switchToYear(year)

                _uiState.value = _uiState.value.copy(
                    showNewYearDialog = false,
                    newYearSuggestion = null,
                    isLoading = false,
                    success = "Jahr $year erfolgreich angelegt!"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()

            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Fehler beim Erstellen des Jahres"
                )
            }
        }
    }

    /**
     * Löscht ein Jahr
     */
    fun deleteYear(year: Int) {
        viewModelScope.launch {
            val success = yearManager.deleteYear(year)

            if (success) {
                loadYears()
                _uiState.value = _uiState.value.copy(
                    success = "Jahr $year gelöscht"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Jahr $year konnte nicht gelöscht werden (ist es aktiv?)"
                )
            }
        }
    }

    /**
     * Archiviert ein Jahr
     */
    fun archiveYear(year: Int) {
        viewModelScope.launch {
            yearManager.archiveYear(year)
            loadYears()

            _uiState.value = _uiState.value.copy(
                success = "Jahr $year archiviert"
            )

            // Clear success message after 3 seconds
            kotlinx.coroutines.delay(3000)
            clearMessages()
        }
    }

    /**
     * Löscht Erfolgs-/Fehlermeldungen
     */
    private fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            success = null
        )
    }

    /**
     * Aktualisiert das Excel-Template-Flag
     */
    fun updateExcelTemplateFlag(year: Int) {
        viewModelScope.launch {
            yearManager.updateExcelTemplateFlag(year)
            loadYears()
        }
    }
}
