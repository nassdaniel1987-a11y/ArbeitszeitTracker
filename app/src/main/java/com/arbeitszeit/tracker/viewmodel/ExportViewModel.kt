package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.export.ExcelExportManager
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
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    private val _selectedKW = MutableStateFlow(DateUtils.getWeekOfYear(LocalDate.now()))
    val selectedKW: StateFlow<Int> = _selectedKW.asStateFlow()
    
    /**
     * Wählt eine Kalenderwoche aus
     */
    fun selectKW(kw: Int) {
        _selectedKW.value = kw
    }
    
    /**
     * Exportiert Excel für die ausgewählte KW
     */
    fun exportExcel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)
            
            try {
                val kw = _selectedKW.value
                val (startKW, endKW) = DateUtils.getWeekRangeForSheet(kw)
                val year = LocalDate.now().year
                
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
                
                // Exportiere
                val file = exportManager.exportToExcel(
                    userSettings = settings,
                    entries = entries,
                    startKW = startKW,
                    endKW = endKW,
                    year = year
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
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export fehlgeschlagen: ${e.message}"
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
        val kw = _selectedKW.value
        val (startKW, endKW) = DateUtils.getWeekRangeForSheet(kw)
        val year = LocalDate.now().year
        return exportManager.getExportFileName(year, startKW, endKW)
    }
    
    /**
     * Setzt Export-Erfolg zurück
     */
    fun resetExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val lastExportedFile: File? = null,
    val exportSuccess: Boolean = false,
    val error: String? = null
)
