package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.import.ExcelImportManager
import com.arbeitszeit.tracker.import.ImportResult
import com.arbeitszeit.tracker.year.YearManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Setup Wizard Steps
 */
enum class SetupStep {
    WELCOME,           // Willkommen
    TEMPLATE_UPLOAD,   // Excel-Vorlage hochladen (optional)
    USER_DATA,         // Benutzerdaten (auto-gefüllt oder manuell)
    COMPLETION         // Abschluss
}

/**
 * UI State für Setup Wizard
 */
data class SetupUiState(
    val currentStep: SetupStep = SetupStep.WELCOME,
    val isLoading: Boolean = false,

    // Template Upload
    val templateUri: Uri? = null,
    val hasTemplate: Boolean = false,

    // User Data (von Template oder manuell)
    val name: String = "",
    val einrichtung: String = "",
    val arbeitsumfangProzent: Int = 100,
    val wochenStundenMinuten: Int = 2400,  // 40:00 Stunden
    val arbeitsTageProWoche: Int = 5,
    val ferienbetreuung: Boolean = true,
    val ersterMontagImJahr: String = "",
    val urlaubsanspruchTage: Int = 30,

    // Validation
    val nameError: String? = null,
    val einrichtungError: String? = null,
    val ersterMontagError: String? = null,

    // State
    val error: String? = null,
    val success: String? = null,
    val setupComplete: Boolean = false
)

/**
 * ViewModel für den Setup Wizard
 *
 * Verwaltet den ersten Einrichtungsprozess für neue Benutzer:
 * 1. Willkommen
 * 2. Excel-Vorlage hochladen (optional)
 * 3. Benutzerdaten eingeben/bestätigen
 * 4. Abschluss
 */
class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userSettingsDao = database.userSettingsDao()
    private val yearManager = YearManager(application)
    private val excelImportManager = ExcelImportManager(application)

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        // Initialisiere mit aktuellem Jahr
        val currentYear = LocalDate.now().year
        val firstMonday = findFirstMonday(currentYear)
        _uiState.value = _uiState.value.copy(
            ersterMontagImJahr = firstMonday
        )
    }

    /**
     * Findet den ersten Montag im Jahr
     */
    private fun findFirstMonday(year: Int): String {
        var date = LocalDate.of(year, 1, 1)
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            date = date.plusDays(1)
        }
        return date.toString()  // yyyy-MM-dd
    }

    /**
     * Nächster Schritt
     */
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = when (currentStep) {
            SetupStep.WELCOME -> SetupStep.TEMPLATE_UPLOAD
            SetupStep.TEMPLATE_UPLOAD -> SetupStep.USER_DATA
            SetupStep.USER_DATA -> SetupStep.COMPLETION
            SetupStep.COMPLETION -> return  // Bereits am Ende
        }

        _uiState.value = _uiState.value.copy(currentStep = nextStep)
    }

    /**
     * Vorheriger Schritt
     */
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val previousStep = when (currentStep) {
            SetupStep.WELCOME -> return  // Bereits am Anfang
            SetupStep.TEMPLATE_UPLOAD -> SetupStep.WELCOME
            SetupStep.USER_DATA -> SetupStep.TEMPLATE_UPLOAD
            SetupStep.COMPLETION -> SetupStep.USER_DATA
        }

        _uiState.value = _uiState.value.copy(currentStep = previousStep)
    }

    /**
     * Überspringt Template-Upload und geht direkt zu manueller Eingabe
     */
    fun skipTemplateUpload() {
        _uiState.value = _uiState.value.copy(
            currentStep = SetupStep.USER_DATA,
            hasTemplate = false,
            templateUri = null
        )
    }

    /**
     * Lädt Excel-Vorlage hoch und liest Benutzerdaten aus
     */
    fun uploadTemplate(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Excel importieren (nur Stammdaten)
                val result = excelImportManager.importFromExcel(
                    uri = uri,
                    importStammdaten = true
                )

                when (result) {
                    is ImportResult.Success -> {
                        val userSettings = result.userSettings

                        if (userSettings != null) {
                            // Daten aus Template übernehmen
                            _uiState.value = _uiState.value.copy(
                                templateUri = uri,
                                hasTemplate = true,
                                name = userSettings.name,
                                einrichtung = userSettings.einrichtung,
                                arbeitsumfangProzent = userSettings.arbeitsumfangProzent,
                                wochenStundenMinuten = userSettings.wochenStundenMinuten,
                                arbeitsTageProWoche = userSettings.arbeitsTageProWoche,
                                ferienbetreuung = userSettings.ferienbetreuung,
                                ersterMontagImJahr = userSettings.ersterMontagImJahr
                                    ?: findFirstMonday(LocalDate.now().year),
                                isLoading = false,
                                success = "Excel-Vorlage erfolgreich geladen!"
                            )

                            // Automatisch zum nächsten Schritt
                            kotlinx.coroutines.delay(500)
                            nextStep()
                            clearMessages()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Keine Benutzerdaten in der Vorlage gefunden"
                            )
                        }
                    }
                    is ImportResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden der Vorlage: ${e.message}"
                )
            }
        }
    }

    /**
     * Aktualisiert Benutzername
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = null
        )
    }

    /**
     * Aktualisiert Einrichtung
     */
    fun updateEinrichtung(einrichtung: String) {
        _uiState.value = _uiState.value.copy(
            einrichtung = einrichtung,
            einrichtungError = null
        )
    }

    /**
     * Aktualisiert Arbeitsumfang
     */
    fun updateArbeitsumfang(prozent: Int) {
        _uiState.value = _uiState.value.copy(
            arbeitsumfangProzent = prozent
        )
    }

    /**
     * Aktualisiert Wochenstunden
     */
    fun updateWochenStunden(minuten: Int) {
        _uiState.value = _uiState.value.copy(
            wochenStundenMinuten = minuten
        )
    }

    /**
     * Aktualisiert Arbeitstage/Woche
     */
    fun updateArbeitsTage(tage: Int) {
        _uiState.value = _uiState.value.copy(
            arbeitsTageProWoche = tage
        )
    }

    /**
     * Aktualisiert Ferienbetreuung
     */
    fun updateFerienbetreuung(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            ferienbetreuung = enabled
        )
    }

    /**
     * Aktualisiert Erster Montag
     */
    fun updateErsterMontag(datum: String) {
        _uiState.value = _uiState.value.copy(
            ersterMontagImJahr = datum,
            ersterMontagError = null
        )
    }

    /**
     * Aktualisiert Urlaubsanspruch
     */
    fun updateUrlaubsanspruch(tage: Int) {
        _uiState.value = _uiState.value.copy(
            urlaubsanspruchTage = tage
        )
    }

    /**
     * Validiert Benutzerdaten
     */
    private fun validateUserData(): Boolean {
        var isValid = true
        val errors = mutableListOf<String>()

        android.util.Log.d("SetupViewModel", "Validierung gestartet")
        android.util.Log.d("SetupViewModel", "Name: '${_uiState.value.name}'")
        android.util.Log.d("SetupViewModel", "Einrichtung: '${_uiState.value.einrichtung}'")
        android.util.Log.d("SetupViewModel", "Erster Montag: '${_uiState.value.ersterMontagImJahr}'")

        // Name prüfen
        if (_uiState.value.name.isBlank()) {
            android.util.Log.e("SetupViewModel", "Validierung fehlgeschlagen: Name ist leer")
            errors.add("Name fehlt")
            _uiState.value = _uiState.value.copy(
                nameError = "Name darf nicht leer sein"
            )
            isValid = false
        }

        // Einrichtung prüfen
        if (_uiState.value.einrichtung.isBlank()) {
            android.util.Log.e("SetupViewModel", "Validierung fehlgeschlagen: Einrichtung ist leer")
            errors.add("Einrichtung fehlt")
            _uiState.value = _uiState.value.copy(
                einrichtungError = "Einrichtung darf nicht leer sein"
            )
            isValid = false
        }

        // Erster Montag prüfen (yyyy-MM-dd)
        try {
            LocalDate.parse(_uiState.value.ersterMontagImJahr)
            android.util.Log.d("SetupViewModel", "Erster Montag ist gültig")
        } catch (e: Exception) {
            android.util.Log.e("SetupViewModel", "Validierung fehlgeschlagen: Ungültiges Datum - ${e.message}")
            errors.add("Datum ungültig")
            _uiState.value = _uiState.value.copy(
                ersterMontagError = "Ungültiges Datum (Format: yyyy-MM-dd)"
            )
            isValid = false
        }

        // Toast mit Fehlern anzeigen
        if (!isValid) {
            Toast.makeText(
                getApplication(),
                "Validierung fehlgeschlagen: ${errors.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
        }

        android.util.Log.d("SetupViewModel", "Validierung abgeschlossen: isValid=$isValid")
        return isValid
    }

    /**
     * Schließt Setup ab: Speichert Benutzerdaten und erstellt aktuelles Jahr
     * VEREINFACHTE VERSION - ohne komplexe Dispatcher-Wechsel
     */
    fun completeSetup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Validierung auf Main Thread
                val isValid = withContext(Dispatchers.Main) {
                    validateUserData()
                }

                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        currentStep = SetupStep.USER_DATA,
                        error = "Bitte alle Felder korrekt ausfüllen"
                    )
                    return@launch
                }

                // Loading aktivieren
                _uiState.value = _uiState.value.copy(isLoading = true)

                val state = _uiState.value

                // Jahr aus Datum extrahieren
                val currentYear = try {
                    LocalDate.parse(state.ersterMontagImJahr).year
                } catch (e: Exception) {
                    LocalDate.now().year
                }

                // 1. UserSettings speichern
                val userSettings = UserSettings(
                    id = 1,
                    name = state.name,
                    einrichtung = state.einrichtung,
                    arbeitsumfangProzent = state.arbeitsumfangProzent,
                    wochenStundenMinuten = state.wochenStundenMinuten,
                    arbeitsTageProWoche = state.arbeitsTageProWoche,
                    ferienbetreuung = state.ferienbetreuung,
                    ueberstundenVorjahrMinuten = 0,
                    letzterUebertragMinuten = 0,
                    ersterMontagImJahr = state.ersterMontagImJahr,
                    urlaubsanspruchTage = state.urlaubsanspruchTage,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userSettingsDao.insertOrUpdate(userSettings)

                // 2. Jahr erstellen - DIREKT ohne Result-Wrapper
                try {
                    val yearSettings = createYearDirectly(
                        currentYear,
                        state.ersterMontagImJahr,
                        state.urlaubsanspruchTage
                    )

                    // 3. Template speichern falls vorhanden
                    if (state.hasTemplate && state.templateUri != null) {
                        val templateManager = com.arbeitszeit.tracker.template.TemplateManager(getApplication())
                        val saved = templateManager.saveTemplate(currentYear, state.templateUri)
                        if (saved) {
                            yearManager.updateExcelTemplateFlag(currentYear)
                        }
                    }

                    // Erfolg!
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        setupComplete = true,
                        currentStep = SetupStep.COMPLETION,
                        success = "Einrichtung erfolgreich abgeschlossen!"
                    )

                } catch (e: Exception) {
                    android.util.Log.e("SetupViewModel", "Fehler beim Jahr erstellen: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Fehler beim Erstellen des Jahres: ${e.message}"
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("SetupViewModel", "Setup-Fehler: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler: ${e.message}"
                )
            }
        }
    }

    /**
     * Erstellt ein Jahr direkt - ohne Result-Wrapper
     * Vereinfachte Version nur für Setup
     */
    private suspend fun createYearDirectly(
        year: Int,
        ersterMontagImJahr: String,
        urlaubsanspruch: Int
    ): YearSettings {
        val yearSettingsDao = database.yearSettingsDao()

        // Prüfe ob Jahr bereits existiert
        if (yearSettingsDao.yearExists(year)) {
            throw Exception("Jahr $year existiert bereits")
        }

        // YearSettings erstellen
        val newYearSettings = YearSettings(
            year = year,
            ersterMontagImJahr = ersterMontagImJahr,
            urlaubsanspruchTage = urlaubsanspruch,
            vorjahresUebertragMinuten = 0,
            hasExcelTemplate = false,
            isActive = true,  // Als aktives Jahr setzen
            createdAt = System.currentTimeMillis()
        )

        // Speichern
        yearSettingsDao.insert(newYearSettings)

        return newYearSettings
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
     * Löscht Fehlermeldung
     */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
