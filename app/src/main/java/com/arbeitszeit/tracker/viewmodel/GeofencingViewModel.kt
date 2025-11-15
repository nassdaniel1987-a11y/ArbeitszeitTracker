package com.arbeitszeit.tracker.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.geofencing.GeofencingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val settings = settingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

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
            // Reverse Geocoding: Koordinaten → Adresse
            val address = getAddressFromLocation(latitude, longitude)

            val location = WorkLocation(
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                radiusMeters = radiusMeters,
                polygonPoints = null,
                enabled = true
            )
            workLocationDao.insert(location)
            updateGeofences()
        }
    }

    /**
     * Ermittelt die Adresse aus Koordinaten (Reverse Geocoding)
     */
    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(getApplication(), java.util.Locale.GERMANY)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+): Neue asynchrone API
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.let { addr ->
                            buildString {
                                // Straße + Hausnummer
                                addr.thoroughfare?.let { street ->
                                    append(street)
                                    addr.subThoroughfare?.let { number ->
                                        append(" $number")
                                    }
                                    append(", ")
                                }
                                // PLZ + Ort
                                addr.postalCode?.let { append("$it ") }
                                addr.locality?.let { append(it) }
                            }.takeIf { it.isNotBlank() }
                        }
                        continuation.resume(address)
                    }
                }
            } else {
                // Android < 13: Alte synchrone API
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    buildString {
                        // Straße + Hausnummer
                        addr.thoroughfare?.let { street ->
                            append(street)
                            addr.subThoroughfare?.let { number ->
                                append(" $number")
                            }
                            append(", ")
                        }
                        // PLZ + Ort
                        addr.postalCode?.let { append("$it ") }
                        addr.locality?.let { append(it) }
                    }.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GeofencingViewModel", "Error getting address", e)
            null
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
     * Aktualisiert einen Arbeitsort (Name, Radius)
     */
    fun updateWorkLocation(location: WorkLocation, newName: String, newRadius: Float) {
        viewModelScope.launch {
            workLocationDao.update(location.copy(
                name = newName,
                radiusMeters = newRadius,
                updatedAt = System.currentTimeMillis()
            ))
            updateGeofences()
        }
    }

    /**
     * Aktiviert/Deaktiviert Geofencing global
     */
    fun toggleGeofencing(enabled: Boolean) {
        android.util.Log.d("GeofencingViewModel", "toggleGeofencing called with enabled=$enabled")
        viewModelScope.launch {
            var currentSettings = settingsDao.getSettings()
            android.util.Log.d("GeofencingViewModel", "Current settings before update: geofencingEnabled=${currentSettings?.geofencingEnabled}")

            // Erstelle Standard-Settings, falls keine vorhanden
            if (currentSettings == null) {
                android.util.Log.d("GeofencingViewModel", "No settings found, creating default settings")
                currentSettings = UserSettings(
                    name = "",
                    einrichtung = "",
                    arbeitsumfangProzent = 100,
                    wochenStundenMinuten = 2400, // 40 Stunden
                    arbeitsTageProWoche = 5,
                    ferienbetreuung = false,
                    ueberstundenVorjahrMinuten = 0,
                    ersterMontagImJahr = null,
                    workingDays = "12345", // Mo-Fr
                    geofencingEnabled = false,
                    geofencingStartHour = 6,
                    geofencingEndHour = 20,
                    geofencingActiveDays = "12345"
                )
                settingsDao.insertOrUpdate(currentSettings)
            }

            val updatedSettings = currentSettings.copy(
                geofencingEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            settingsDao.insertOrUpdate(updatedSettings)
            android.util.Log.d("GeofencingViewModel", "Settings updated: geofencingEnabled=${updatedSettings.geofencingEnabled}")

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
            settingsDao.insertOrUpdate(currentSettings.copy(
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
            settingsDao.insertOrUpdate(currentSettings.copy(
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
