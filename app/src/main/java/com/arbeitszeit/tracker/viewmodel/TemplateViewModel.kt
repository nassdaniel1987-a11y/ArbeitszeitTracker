package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.template.TemplateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TemplateUiState(
    val isUploading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class TemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val templateManager = TemplateManager(application)

    private val _availableYears = MutableStateFlow<List<Int>>(emptyList())
    val availableYears: StateFlow<List<Int>> = _availableYears.asStateFlow()

    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        loadAvailableYears()
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            _availableYears.value = templateManager.getAvailableYears()
        }
    }

    fun uploadTemplate(year: Int, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = TemplateUiState(isUploading = true)

            val success = templateManager.saveTemplate(year, uri)

            if (success) {
                loadAvailableYears() // Refresh list
                _uiState.value = TemplateUiState(
                    successMessage = "Vorlage für $year erfolgreich hochgeladen"
                )
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = TemplateUiState()
            } else {
                _uiState.value = TemplateUiState(
                    errorMessage = "Fehler beim Hochladen der Vorlage"
                )
                // Clear error message after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.value = TemplateUiState()
            }
        }
    }

    fun deleteTemplate(year: Int) {
        viewModelScope.launch {
            val success = templateManager.deleteTemplate(year)

            if (success) {
                loadAvailableYears() // Refresh list
                _uiState.value = TemplateUiState(
                    successMessage = "Vorlage für $year gelöscht"
                )
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = TemplateUiState()
            } else {
                _uiState.value = TemplateUiState(
                    errorMessage = "Fehler beim Löschen der Vorlage"
                )
                // Clear error message after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.value = TemplateUiState()
            }
        }
    }
}
