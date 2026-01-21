package com.arbeitszeit.tracker.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class GeofencingManager(private val context: Context) {

    companion object {
        private const val TAG = "GeofencingManager"
        private const val LOCATION_TIMEOUT_MS = 10000L // 10 Sekunden Timeout
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val database = AppDatabase.getDatabase(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Aktiviert Geofencing für alle aktivierten Arbeitsorte
     * HINWEIS: Nur kreisförmige Bereiche werden unterstützt.
     * Polygon-Arbeitsorte werden ignoriert, da die Android Geofencing API nur Kreise unterstützt.
     */
    fun startGeofencing(locations: List<WorkLocation>) {
        if (!hasLocationPermission()) {
            return
        }

        // Filter nur kreisförmige Arbeitsorte (keine Polygone)
        // Die Android Geofencing API unterstützt nur kreisförmige Regionen
        @Suppress("DEPRECATION")
        val circularLocations = locations.filter { !it.isPolygon() }

        val geofences = circularLocations.map { location ->
            Geofence.Builder()
                .setRequestId(location.id.toString())
                .setCircularRegion(
                    location.latitude,
                    location.longitude,
                    location.radiusMeters
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        if (geofences.isEmpty()) {
            return
        }

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    // Geofencing erfolgreich aktiviert
                }
                addOnFailureListener { exception ->
                    // Fehler beim Aktivieren
                    exception.printStackTrace()
                }
            }
        } catch (securityException: SecurityException) {
            securityException.printStackTrace()
        }
    }

    /**
     * Deaktiviert alle Geofences
     */
    fun stopGeofencing() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofencing erfolgreich deaktiviert
            }
            addOnFailureListener { exception ->
                exception.printStackTrace()
            }
        }
    }

    /**
     * Aktualisiert Geofences (entfernt alte, fügt neue hinzu)
     */
    fun updateGeofencing(locations: List<WorkLocation>) {
        stopGeofencing()
        if (locations.isNotEmpty()) {
            startGeofencing(locations)
        }
    }

    /**
     * Prüft ob Standort-Berechtigung vorhanden ist
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Prüft ob Hintergrund-Standort-Berechtigung vorhanden ist (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Nicht erforderlich vor Android 10
        }
    }

    /**
     * Prüft ob sich der Nutzer gerade an einem Arbeitsort befindet
     *
     * HINWEIS: Diese Methode nutzt SharedPreferences, die vom GeofenceBroadcastReceiver
     * gesetzt werden wenn Geofence-Events eintreten (ENTER/EXIT).
     *
     * @return true wenn der User am Arbeitsort ist, false sonst
     */
    fun isCurrentlyAtWorkLocation(): Boolean {
        val prefs = context.getSharedPreferences("geofencing_state", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_at_work_location", false)
    }

    /**
     * Setzt den Status ob sich der User am Arbeitsort befindet
     * Wird vom GeofenceBroadcastReceiver aufgerufen
     */
    fun setAtWorkLocation(isAtWork: Boolean) {
        val prefs = context.getSharedPreferences("geofencing_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_at_work_location", isAtWork).apply()
    }

    /**
     * Führt eine aktive Standortprüfung durch und prüft ob sich der User an einem Arbeitsort befindet.
     *
     * Im Gegensatz zu isCurrentlyAtWorkLocation() (SharedPreferences-basiert) holt diese Methode
     * den aktuellen GPS-Standort und prüft aktiv gegen alle aktivierten Arbeitsorte.
     *
     * @return true wenn der User sich aktuell an einem aktivierten Arbeitsort befindet
     */
    suspend fun checkActiveLocationAtWork(): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Keine Standort-Berechtigung für aktive Prüfung")
            return false
        }

        try {
            // Hole aktuellen Standort mit Timeout
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getCurrentLocation()
            }

            if (location == null) {
                Log.w(TAG, "Konnte aktuellen Standort nicht ermitteln (Timeout oder nicht verfügbar)")
                // Fallback auf SharedPreferences-Wert
                return isCurrentlyAtWorkLocation()
            }

            Log.d(TAG, "Aktueller Standort: ${location.latitude}, ${location.longitude}")

            // Lade alle aktivierten Arbeitsorte
            val enabledLocations = database.workLocationDao().getEnabledLocations()

            if (enabledLocations.isEmpty()) {
                Log.d(TAG, "Keine aktivierten Arbeitsorte konfiguriert")
                return false
            }

            // Prüfe ob der aktuelle Standort in einem der Arbeitsorte liegt
            for (workLocation in enabledLocations) {
                if (workLocation.containsPoint(location.latitude, location.longitude)) {
                    Log.i(TAG, "User befindet sich am Arbeitsort: ${workLocation.name}")
                    // Aktualisiere auch den SharedPreferences-Status
                    setAtWorkLocation(true)
                    return true
                }
            }

            Log.d(TAG, "User befindet sich nicht an einem Arbeitsort")
            setAtWorkLocation(false)
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei aktiver Standortprüfung: ${e.message}", e)
            // Fallback auf SharedPreferences-Wert
            return isCurrentlyAtWorkLocation()
        }
    }

    /**
     * Holt den aktuellen Standort als suspending function
     */
    private suspend fun getCurrentLocation(): android.location.Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Fehler beim Abrufen des Standorts: ${exception.message}")
                continuation.resume(null)
            }
        } catch (securityException: SecurityException) {
            Log.e(TAG, "SecurityException beim Abrufen des Standorts", securityException)
            continuation.resume(null)
        }
    }
}
