package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.template.TemplateManager
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
    val showEditYearDialog: Boolean = false,
    val editYear: YearSettings? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val deleteYear: YearSettings? = null,
    val availableTemplates: List<Int> = emptyList(),  // Jahre mit Templates
    val error: String? = null,
    val success: String? = null
)

class YearViewModel(application: Application) : AndroidViewModel(application) {

    private val yearManager = YearManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val yearSettingsDao = database.yearSettingsDao()
    private val userSettingsDao = database.userSettingsDao()
    private val templateManager = TemplateManager(application)

    private val _uiState = MutableStateFlow(YearUiState())
    val uiState: StateFlow<YearUiState> = _uiState.asStateFlow()

    init {
        loadYears()
        observeActiveYear()
        loadAvailableTemplates()
    }

    /**
     * Prüft und führt Auto-Switch zum neuen Jahr aus (wenn aktiviert)
     *
     * Wird von MainActivity beim Start aufgerufen.
     * Logik:
     * 1. Prüfe ob Auto-Switch aktiviert ist
     * 2. Prüfe ob heute >= erster Montag des nächsten Jahres
     * 3. Falls ja:
     *    - Wenn nächstes Jahr existiert: Automatisch wechseln
     *    - Wenn nächstes Jahr NICHT existiert: Dialog anzeigen
     */
    suspend fun checkAndPerformAutoSwitch() {
        try {
            // 1. Prüfe ob Auto-Switch aktiviert ist
            val settings = userSettingsDao.getSettings()
            if (settings?.autoSwitchYear != true) {
                return // Auto-Switch deaktiviert
            }

            // 2. Hole aktuelles aktives Jahr
            val activeYear = yearManager.getActiveYear() ?: return
            val today = LocalDate.now()

            // 3. Berechne erster Montag des aktuellen Jahres
            val firstMondayOfActiveYear = LocalDate.parse(activeYear.ersterMontagImJahr)

            // 4. Berechne erster Montag des NÄCHSTEN Jahres
            val nextYearNumber = activeYear.year + 1
            var firstMondayOfNextYear = LocalDate.of(nextYearNumber, 1, 1)
            while (firstMondayOfNextYear.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                firstMondayOfNextYear = firstMondayOfNextYear.plusDays(1)
            }

            // 5. Prüfe ob heute >= erster Montag des nächsten Jahres
            if (today >= firstMondayOfNextYear) {
                // Es ist Zeit für einen Jahreswechsel!

                // 6. Prüfe ob nächstes Jahr bereits existiert
                if (yearManager.yearExists(nextYearNumber)) {
                    // Jahr existiert → Automatisch wechseln
                    val success = yearManager.switchToYear(nextYearNumber)
                    if (success) {
                        loadYears()
                        _uiState.value = _uiState.value.copy(
                            success = "Automatisch zu Jahr $nextYearNumber gewechselt"
                        )
                    }
                } else {
                    // Jahr existiert NICHT → Dialog anzeigen
                    val suggestion = yearManager.suggestNewYear(nextYearNumber)
                    _uiState.value = _uiState.value.copy(
                        showNewYearDialog = true,
                        newYearSuggestion = suggestion
                    )
                }
            }

        } catch (e: Exception) {
            // Fehler beim Auto-Switch ignorieren (kein kritischer Fehler)
            android.util.Log.e("YearViewModel", "Fehler beim Auto-Switch: ${e.message}")
        }
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

    /**
     * Zeigt den "Jahr bearbeiten" Dialog
     */
    fun showEditYearDialog(year: YearSettings) {
        _uiState.value = _uiState.value.copy(
            showEditYearDialog = true,
            editYear = year
        )
    }

    /**
     * Schließt den "Jahr bearbeiten" Dialog
     */
    fun dismissEditYearDialog() {
        _uiState.value = _uiState.value.copy(
            showEditYearDialog = false,
            editYear = null
        )
    }

    /**
     * Aktualisiert ein Jahr
     */
    fun updateYear(
        year: Int,
        ersterMontagImJahr: String,
        urlaubsanspruch: Int,
        vorjahresUebertrag: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val existing = yearSettingsDao.getYear(year)
                if (existing != null) {
                    yearSettingsDao.update(existing.copy(
                        ersterMontagImJahr = ersterMontagImJahr,
                        urlaubsanspruchTage = urlaubsanspruch,
                        vorjahresUebertragMinuten = vorjahresUebertrag
                    ))

                    loadYears()
                    _uiState.value = _uiState.value.copy(
                        showEditYearDialog = false,
                        editYear = null,
                        isLoading = false,
                        success = "Jahr $year erfolgreich aktualisiert!"
                    )

                    // Clear success message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    clearMessages()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Jahr $year nicht gefunden"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Aktualisieren: ${e.message}"
                )
            }
        }
    }

    /**
     * Zeigt Bestätigungs-Dialog zum Löschen eines Jahres
     */
    fun showDeleteConfirmDialog(year: YearSettings) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = true,
            deleteYear = year
        )
    }

    /**
     * Schließt den Lösch-Bestätigungs-Dialog
     */
    fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            deleteYear = null
        )
    }

    /**
     * Löscht ein Jahr (bestätigt)
     * WICHTIG: Zeiteinträge bleiben erhalten!
     */
    fun confirmDeleteYear() {
        viewModelScope.launch {
            val year = _uiState.value.deleteYear?.year ?: return@launch

            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = yearManager.deleteYear(year)

            if (success) {
                loadYears()
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmDialog = false,
                    deleteYear = null,
                    isLoading = false,
                    success = "Jahr $year gelöscht (Zeiteinträge bleiben erhalten)"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Jahr $year konnte nicht gelöscht werden (ist es aktiv?)"
                )
            }
        }
    }

    /**
     * Lädt Liste der verfügbaren Templates
     */
    private fun loadAvailableTemplates() {
        viewModelScope.launch {
            val templates = templateManager.getAvailableYears()
            _uiState.value = _uiState.value.copy(availableTemplates = templates)
        }
    }

    /**
     * Lädt ein Excel-Template für ein Jahr hoch
     */
    fun uploadTemplate(year: Int, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = templateManager.saveTemplate(year, uri)

            if (success) {
                // Aktualisiere hasExcelTemplate Flag
                yearManager.updateExcelTemplateFlag(year)

                // Reload data
                loadYears()
                loadAvailableTemplates()

                // Zeige Benachrichtigung
                com.arbeitszeit.tracker.utils.NotificationHelper.showTemplateUploadSuccess(
                    getApplication(),
                    year
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = "Excel-Vorlage für Jahr $year erfolgreich hochgeladen!"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Hochladen der Vorlage für Jahr $year"
                )
            }
        }
    }

    /**
     * Löscht das Template für ein Jahr
     */
    fun deleteTemplate(year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = templateManager.deleteTemplate(year)

            if (success) {
                // Aktualisiere hasExcelTemplate Flag auf false
                val existingYear = yearSettingsDao.getYear(year)
                if (existingYear != null) {
                    yearSettingsDao.update(existingYear.copy(hasExcelTemplate = false))
                }

                // Reload data
                loadYears()
                loadAvailableTemplates()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = "Excel-Vorlage für Jahr $year gelöscht"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Löschen der Vorlage"
                )
            }
        }
    }

    /**
     * Prüft ob ein Template für ein Jahr verfügbar ist
     */
    fun hasTemplate(year: Int): Boolean {
        return templateManager.hasTemplate(year)
    }
}
