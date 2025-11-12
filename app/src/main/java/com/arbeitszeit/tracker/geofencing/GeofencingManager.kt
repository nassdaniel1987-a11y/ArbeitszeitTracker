package com.arbeitszeit.tracker.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofencingManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

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
}
