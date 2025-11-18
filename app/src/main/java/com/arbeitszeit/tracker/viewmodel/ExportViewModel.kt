package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.export.ExcelExportManager
import com.arbeitszeit.tracker.export.SimpleExcelExportManager
import com.arbeitszeit.tracker.import.ExcelImportManager
import com.arbeitszeit.tracker.import.ImportResult
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()
    private val exportManager = ExcelExportManager(application)
    private val simpleExportManager = SimpleExcelExportManager(application)
    private val importManager = ExcelImportManager(application)
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    // Aktuelles Jahr und Settings für KW-Berechnung
    private val currentYear = LocalDate.now().year

    private val _selectedKW = MutableStateFlow(1)
    val selectedKW: StateFlow<Int> = _selectedKW.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentYear)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    init {
        // Initialisiere mit aktueller KW basierend auf Settings
        viewModelScope.launch {
            val settings = settingsDao.getSettings()
            val currentWeek = DateUtils.getCustomWeekOfYear(
                LocalDate.now(),
                settings?.ersterMontagImJahr
            )
            _selectedKW.value = currentWeek
        }
    }
    
    /**
     * Wählt eine Kalenderwoche aus
     */
    fun selectKW(kw: Int) {
        _selectedKW.value = kw
    }

    /**
     * Wählt ein Jahr für den Export aus
     */
    fun selectYear(year: Int) {
        _selectedYear.value = year
    }
    
    /**
     * Zeigt den Dialog für die Dateinamen-Eingabe
     */
    fun showFileNameDialog(isSimpleExport: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            showFileNameDialog = true,
            isSimpleExport = isSimpleExport
        )
    }

    /**
     * Schließt den Dialog für die Dateinamen-Eingabe
     */
    fun dismissFileNameDialog() {
        _uiState.value = _uiState.value.copy(showFileNameDialog = false)
    }

    /**
     * Exportiert Excel für das gesamte Jahr
     */
    fun exportExcel(customFileName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null, showFileNameDialog = false)

            try {
                val year = _selectedYear.value

                // Lade Settings
                val settings = settingsDao.getSettings()
                if (settings == null) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "Bitte erst Einstellungen ausfüllen"
                    )
                    return@launch
                }

                // Lade ALLE Einträge des Jahres
                val entries = timeEntryDao.getEntriesByYear(year)

                // Exportiere GESAMTJAHR
                val file = exportManager.exportToExcel(
                    userSettings = settings,
                    entries = entries,
                    year = year,
                    customFileName = customFileName
                )

                // Zeige Erfolg
                NotificationHelper.showExportSuccess(
                    getApplication(),
                    file.name
                )

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    exportSuccess = true
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Exportiert Excel als einfache Tabelle (ohne Template)
     */
    fun exportSimpleExcel(customFileName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null, showFileNameDialog = false)

            try {
                val kw = _selectedKW.value
                val (startKW, endKW) = DateUtils.getWeekRangeForSheet(kw)
                val year = _selectedYear.value

                // Lade Settings
                val settings = settingsDao.getSettings()
                if (settings == null) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "Bitte erst Einstellungen ausfüllen"
                    )
                    return@launch
                }

                // Lade alle Einträge für den Zeitraum
                val entries = timeEntryDao.getEntriesByWeekRange(year, startKW, endKW)

                // Exportiere als einfache Tabelle
                val file = simpleExportManager.exportToSimpleExcel(
                    userSettings = settings,
                    entries = entries,
                    startKW = startKW,
                    endKW = endKW,
                    year = year,
                    customFileName = customFileName
                )

                // Zeige Erfolg
                NotificationHelper.showExportSuccess(
                    getApplication(),
                    file.name
                )

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    exportSuccess = true
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Prüft ob Template verfügbar ist
     */
    fun checkTemplate(): Boolean {
        return exportManager.isTemplateAvailable()
    }
    
    /**
     * Gibt den erwarteten Dateinamen zurück
     */
    fun getExpectedFileName(): String {
        val year = _selectedYear.value
        return exportManager.getExportFileName(year)
    }
    
    /**
     * Setzt Export-Erfolg zurück
     */
    fun resetExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }

    /**
     * Importiert Excel-Datei
     *
     * @param uri URI der Excel-Datei
     * @param importStammdaten Sollen Stammdaten auch importiert werden?
     */
    fun importExcel(uri: Uri, importStammdaten: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)

            try {
                val result = importManager.importFromExcel(uri, importStammdaten)

                when (result) {
                    is ImportResult.Success -> {
                        // Speichere importierte Stammdaten
                        if (importStammdaten && result.userSettings != null) {
                            // Lade existierende Settings um Geofencing-Einstellungen zu erhalten
                            val existingSettings = settingsDao.getSettings()

                            android.util.Log.d("ExportViewModel", "Import: importStammdaten=$importStammdaten")
                            android.util.Log.d("ExportViewModel", "Importierte Settings: name=${result.userSettings.name}, " +
                                    "einrichtung=${result.userSettings.einrichtung}, wochenstunden=${result.userSettings.wochenStundenMinuten}")
                            android.util.Log.d("ExportViewModel", "Existierende Settings: name=${existingSettings?.name}, " +
                                    "einrichtung=${existingSettings?.einrichtung}, wochenstunden=${existingSettings?.wochenStundenMinuten}")

                            // Merge: Stammdaten aus Import, Geofencing-Einstellungen behalten
                            val mergedSettings = result.userSettings.copy(
                                geofencingEnabled = existingSettings?.geofencingEnabled ?: false,
                                geofencingStartHour = existingSettings?.geofencingStartHour ?: 6,
                                geofencingEndHour = existingSettings?.geofencingEndHour ?: 20,
                                geofencingActiveDays = existingSettings?.geofencingActiveDays ?: "12345",
                                // Individuelle Tagessoll-Zeiten auch behalten, falls nicht im Import
                                montagSollMinuten = result.userSettings.montagSollMinuten ?: existingSettings?.montagSollMinuten,
                                dienstagSollMinuten = result.userSettings.dienstagSollMinuten ?: existingSettings?.dienstagSollMinuten,
                                mittwochSollMinuten = result.userSettings.mittwochSollMinuten ?: existingSettings?.mittwochSollMinuten,
                                donnerstagSollMinuten = result.userSettings.donnerstagSollMinuten ?: existingSettings?.donnerstagSollMinuten,
                                freitagSollMinuten = result.userSettings.freitagSollMinuten ?: existingSettings?.freitagSollMinuten,
                                samstagSollMinuten = result.userSettings.samstagSollMinuten ?: existingSettings?.samstagSollMinuten,
                                sonntagSollMinuten = result.userSettings.sonntagSollMinuten ?: existingSettings?.sonntagSollMinuten
                            )

                            android.util.Log.d("ExportViewModel", "Merged Settings: name=${mergedSettings.name}, " +
                                    "einrichtung=${mergedSettings.einrichtung}, wochenstunden=${mergedSettings.wochenStundenMinuten}")

                            settingsDao.insertOrUpdate(mergedSettings)

                            android.util.Log.d("ExportViewModel", "Settings gespeichert!")

                            // Aktualisiere sollMinuten für alle bestehenden Einträge basierend auf neuen Stammdaten
                            updateSollMinutenForAllEntries(mergedSettings)
                        }

                        // Speichere importierte Zeiteinträge
                        // Prüfe für jeden Eintrag, ob bereits einer für dieses Datum existiert
                        result.entries.forEach { importedEntry ->
                            val existingEntry = timeEntryDao.getEntryByDate(importedEntry.datum)

                            if (existingEntry != null) {
                                // Aktualisiere bestehenden Eintrag mit importierten Daten
                                timeEntryDao.update(existingEntry.copy(
                                    wochentag = importedEntry.wochentag,
                                    kalenderwoche = importedEntry.kalenderwoche,
                                    jahr = importedEntry.jahr,
                                    startZeit = importedEntry.startZeit,
                                    endZeit = importedEntry.endZeit,
                                    pauseMinuten = importedEntry.pauseMinuten,
                                    sollMinuten = importedEntry.sollMinuten,
                                    typ = importedEntry.typ,
                                    notiz = importedEntry.notiz,
                                    arbeitszeitBereitschaft = importedEntry.arbeitszeitBereitschaft,
                                    isManualEntry = true,
                                    updatedAt = System.currentTimeMillis()
                                ))
                            } else {
                                // Füge neuen Eintrag hinzu
                                timeEntryDao.insert(importedEntry)
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            importSuccess = true,
                            importedEntriesCount = result.entriesCount
                        )

                        NotificationHelper.showImportSuccess(
                            getApplication(),
                            result.entriesCount
                        )
                    }

                    is ImportResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            error = result.message
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Import fehlgeschlagen: ${e.message}"
                )
            }
        }
    }

    /**
     * Setzt Import-Erfolg zurück
     */
    fun resetImportSuccess() {
        _uiState.value = _uiState.value.copy(importSuccess = false, importedEntriesCount = 0)
    }

    /**
     * Aktualisiert sollMinuten für alle Einträge basierend auf neuen Stammdaten
     * Wird nach Import von Stammdaten aufgerufen
     */
    private suspend fun updateSollMinutenForAllEntries(settings: com.arbeitszeit.tracker.data.entity.UserSettings) {
        val year = java.time.LocalDate.now().year
        val allEntries = timeEntryDao.getEntriesByYear(year)

        allEntries.forEach { entry ->
            // Berechne neue sollMinuten basierend auf Settings
            val date = java.time.LocalDate.parse(entry.datum)
            val dayOfWeek = date.dayOfWeek.value

            val newSollMinuten = if (DateUtils.isWeekend(date)) {
                // Wochenende: Prüfe individuelle Zeiten
                settings.getSollMinutenForDay(dayOfWeek) ?: 0
            } else {
                // Werktag: Individuelle Zeit oder Standard
                settings.getSollMinutenForDay(dayOfWeek)
                    ?: (settings.wochenStundenMinuten / settings.arbeitsTageProWoche)
            }

            // Aktualisiere nur wenn sollMinuten sich geändert haben
            if (entry.sollMinuten != newSollMinuten) {
                timeEntryDao.update(entry.copy(
                    sollMinuten = newSollMinuten,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    /**
     * Lädt Vorschau-Daten für den Export
     */
    fun loadExportPreview() {
        viewModelScope.launch {
            try {
                val year = _selectedYear.value
                val settings = settingsDao.getSettings()
                val entries = timeEntryDao.getEntriesByYear(year)

                _uiState.value = _uiState.value.copy(
                    previewData = ExportPreviewData(
                        year = year,
                        totalEntries = entries.size,
                        entries = entries.take(10), // Nur erste 10 für Vorschau
                        userName = settings?.name ?: "",
                        einrichtung = settings?.einrichtung ?: ""
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Vorschau konnte nicht geladen werden: ${e.message}"
                )
            }
        }
    }

    /**
     * Schließt die Vorschau
     */
    fun closePreview() {
        _uiState.value = _uiState.value.copy(previewData = null)
    }

    /**
     * Exportiert und teilt Excel-Datei (via Share Intent)
     * Funktioniert mit OneDrive, Google Drive, E-Mail, WhatsApp, etc.
     */
    fun shareExport(isSimpleExport: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)

            try {
                val year = _selectedYear.value
                val settings = settingsDao.getSettings()
                if (settings == null) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "Bitte erst Einstellungen ausfüllen"
                    )
                    return@launch
                }

                val file = if (isSimpleExport) {
                    // Einfacher Export
                    val kw = _selectedKW.value
                    val (startKW, endKW) = com.arbeitszeit.tracker.utils.DateUtils.getWeekRangeForSheet(kw)
                    val entries = timeEntryDao.getEntriesByWeekRange(year, startKW, endKW)

                    simpleExportManager.exportToSimpleExcel(
                        userSettings = settings,
                        entries = entries,
                        startKW = startKW,
                        endKW = endKW,
                        year = year
                    )
                } else {
                    // Gesamtjahr-Export
                    val entries = timeEntryDao.getEntriesByYear(year)

                    exportManager.exportToExcel(
                        userSettings = settings,
                        entries = entries,
                        year = year
                    )
                }

                // Öffne Share-Dialog
                val context = getApplication<Application>()
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(
                    android.content.Intent.createChooser(shareIntent, "Excel teilen mit...").apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Share fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Exportiert Excel in die Cloud (via Storage Access Framework)
     * Wird aufgerufen nachdem der Nutzer einen Speicherort gewählt hat
     */
    fun exportToCloud(uri: Uri, isSimpleExport: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)

            try {
                val year = _selectedYear.value
                val settings = settingsDao.getSettings()
                if (settings == null) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "Bitte erst Einstellungen ausfüllen"
                    )
                    return@launch
                }

                val success = if (isSimpleExport) {
                    // Einfacher Export
                    val kw = _selectedKW.value
                    val (startKW, endKW) = com.arbeitszeit.tracker.utils.DateUtils.getWeekRangeForSheet(kw)
                    val entries = timeEntryDao.getEntriesByWeekRange(year, startKW, endKW)

                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        simpleExportManager.exportToStream(
                            userSettings = settings,
                            entries = entries,
                            startKW = startKW,
                            endKW = endKW,
                            year = year,
                            outputStream = outputStream
                        )
                    } ?: false
                } else {
                    // Gesamtjahr-Export
                    val entries = timeEntryDao.getEntriesByYear(year)

                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        exportManager.exportToStream(
                            userSettings = settings,
                            entries = entries,
                            year = year,
                            outputStream = outputStream
                        )
                    } ?: false
                }

                if (success) {
                    NotificationHelper.showExportSuccess(
                        getApplication(),
                        "In Cloud gespeichert"
                    )

                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "Cloud-Export fehlgeschlagen"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Cloud-Export fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastExportedFile: File? = null,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val importedEntriesCount: Int = 0,
    val error: String? = null,
    val previewData: ExportPreviewData? = null,
    val showFileNameDialog: Boolean = false,
    val isSimpleExport: Boolean = false
)

data class ExportPreviewData(
    val year: Int,
    val totalEntries: Int,
    val entries: List<com.arbeitszeit.tracker.data.entity.TimeEntry>,
    val userName: String,
    val einrichtung: String
)
