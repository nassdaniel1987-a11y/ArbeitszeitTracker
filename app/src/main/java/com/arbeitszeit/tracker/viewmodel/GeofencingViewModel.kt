package com.arbeitszeit.tracker.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.geofencing.GeofencingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GeofencingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val workLocationDao = database.workLocationDao()
    private val settingsDao = database.userSettingsDao()
    private val geofencingManager = GeofencingManager(application)

    // UI State
    private val _uiState = MutableStateFlow(GeofencingUiState())
    val uiState: StateFlow<GeofencingUiState> = _uiState.asStateFlow()

    // Alle Arbeitsorte
    val workLocations: StateFlow<List<WorkLocation>> = workLocationDao.getAllLocationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val settings = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Fügt einen neuen Arbeitsort hinzu
     */
    fun addWorkLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ) {
        viewModelScope.launch {
            val location = WorkLocation(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                enabled = true
            )
            workLocationDao.insert(location)
            updateGeofences()
        }
    }

    /**
     * Löscht einen Arbeitsort
     */
    fun deleteWorkLocation(location: WorkLocation) {
        viewModelScope.launch {
            workLocationDao.delete(location)
            updateGeofences()
        }
    }

    /**
     * Aktiviert/Deaktiviert einen Arbeitsort
     */
    fun toggleWorkLocation(location: WorkLocation) {
        viewModelScope.launch {
            workLocationDao.update(location.copy(
                enabled = !location.enabled,
                updatedAt = System.currentTimeMillis()
            ))
            updateGeofences()
        }
    }

    /**
     * Aktiviert/Deaktiviert Geofencing global
     */
    fun toggleGeofencing(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettings() ?: return@launch
            settingsDao.update(currentSettings.copy(
                geofencingEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            ))

            if (enabled) {
                updateGeofences()
            } else {
                geofencingManager.stopGeofencing()
            }
        }
    }

    /**
     * Aktualisiert Zeitfenster für Geofencing
     */
    fun updateTimeWindow(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettings() ?: return@launch
            settingsDao.update(currentSettings.copy(
                geofencingStartHour = startHour,
                geofencingEndHour = endHour,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert aktive Tage für Geofencing
     */
    fun updateActiveDays(days: String) {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettings() ?: return@launch
            settingsDao.update(currentSettings.copy(
                geofencingActiveDays = days,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Aktualisiert alle Geofences basierend auf aktuellen Einstellungen
     */
    private suspend fun updateGeofences() {
        val currentSettings = settingsDao.getSettings()
        if (currentSettings?.geofencingEnabled == true) {
            val enabledLocations = workLocationDao.getEnabledLocations()
            geofencingManager.updateGeofencing(enabledLocations)
        }
    }

    /**
     * Prüft Berechtigungen
     */
    fun checkPermissions(): PermissionStatus {
        val hasLocation = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackground = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return when {
            hasLocation && hasBackground -> PermissionStatus.GRANTED
            hasLocation -> PermissionStatus.LOCATION_ONLY
            else -> PermissionStatus.DENIED
        }
    }
}

data class GeofencingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class PermissionStatus {
    GRANTED,
    LOCATION_ONLY,
    DENIED
}
