package com.arbeitszeit.tracker.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OpenStreetMapView(
    workLocations: List<WorkLocation>,
    modifier: Modifier = Modifier,
    initialZoom: Double = 15.0,
    showCurrentLocation: Boolean = true,
    onMapReady: ((MapView) -> Unit)? = null,
    onCenterToMyLocation: ((MapView?) -> Unit)? = null
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Track current location
    val currentLocation by produceLocationUpdates(context, showCurrentLocation)

    DisposableEffect(Unit) {
        // Setup location overlay
        if (showCurrentLocation) {
            val overlay = MyLocationNewOverlay(mapView).apply {
                enableMyLocation()
                enableFollowLocation()
            }
            myLocationOverlay = overlay
            mapView.overlays.add(overlay)
        }

        onDispose {
            myLocationOverlay?.disableMyLocation()
            mapView.onDetach()
        }
    }

    LaunchedEffect(workLocations, currentLocation) {
        updateMapMarkers(mapView, workLocations, currentLocation, context)
        onMapReady?.invoke(mapView)
    }

    // Expose map view for centering to current location
    LaunchedEffect(Unit) {
        onCenterToMyLocation?.invoke(mapView)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { map ->
            // Update map when composition changes
            updateMapMarkers(map, workLocations, currentLocation, context)
        }
    )
}

// Function to center map to current location
fun centerMapToMyLocation(mapView: MapView?) {
    mapView?.let { map ->
        val myLocationOverlay = map.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()
        myLocationOverlay?.myLocation?.let { location ->
            map.controller.animateTo(location)
            map.controller.setZoom(16.0)
        }
    }
}

@Composable
@SuppressLint("MissingPermission")
private fun produceLocationUpdates(
    context: Context,
    enabled: Boolean
): State<Location?> {
    val locationState = remember { mutableStateOf<Location?>(null) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@DisposableEffect onDispose { }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // 5 seconds
            setMaxUpdateDelayMillis(15000L)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                locationState.value = result.lastLocation
            }
        }

        // Permission already checked above
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )

        // Get last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { locationState.value = it }
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    return locationState
}

private fun createMapView(context: Context): MapView {
    // OSMDroid configuration
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )
    Configuration.getInstance().userAgentValue = context.packageName

    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)

        controller.setZoom(15.0)

        // Default center (Deutschland)
        controller.setCenter(GeoPoint(51.0, 10.5))
    }
}

private fun updateMapMarkers(
    mapView: MapView,
    workLocations: List<WorkLocation>,
    currentLocation: Location?,
    context: Context
) {
    // Don't clear all overlays - preserve MyLocationOverlay
    val myLocationOverlay = mapView.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()
    mapView.overlays.clear()
    myLocationOverlay?.let { mapView.overlays.add(it) }

    if (workLocations.isEmpty()) {
        mapView.invalidate()
        return
    }

    // Add markers and circles for each work location
    workLocations.forEach { location ->
        // Check if current location is inside this specific location
        val isInsideThisLocation = currentLocation?.let { loc ->
            location.enabled && isLocationInsideGeofence(
                loc.latitude,
                loc.longitude,
                location.latitude,
                location.longitude,
                location.radiusMeters.toDouble()
            )
        } ?: false

        // Add marker
        val marker = Marker(mapView).apply {
            position = GeoPoint(location.latitude, location.longitude)
            title = location.name + if (isInsideThisLocation) " ✓ (Du bist hier)" else ""
            snippet = buildString {
                if (!location.address.isNullOrBlank()) {
                    append(location.address)
                    append("\n")
                }
                append("Radius: ${location.radiusMeters.toInt()}m")
                if (isInsideThisLocation) {
                    append("\n✓ Du befindest dich in diesem Bereich")
                }
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Set marker color based on enabled status and if user is inside
            if (isInsideThisLocation) {
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                // Marker is brighter when user is inside
                alpha = 1.0f
            } else if (location.enabled) {
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                alpha = 0.8f
            } else {
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                alpha = 0.5f
            }
        }
        mapView.overlays.add(marker)

        // Add circle for radius
        val circle = Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(
                GeoPoint(location.latitude, location.longitude),
                location.radiusMeters.toDouble()
            )

            // Change color based on whether user is inside
            fillPaint.color = when {
                isInsideThisLocation -> android.graphics.Color.argb(80, 76, 175, 80) // Bright green - Du bist DRIN
                location.enabled -> android.graphics.Color.argb(50, 33, 150, 243) // Blue - Aktiv aber DRAUSSEN
                else -> android.graphics.Color.argb(30, 128, 128, 128) // Gray - Deaktiviert
            }

            outlinePaint.color = when {
                isInsideThisLocation -> android.graphics.Color.argb(255, 76, 175, 80) // Solid green
                location.enabled -> android.graphics.Color.argb(200, 33, 150, 243) // Blue outline
                else -> android.graphics.Color.argb(150, 128, 128, 128)
            }

            outlinePaint.strokeWidth = if (isInsideThisLocation) 5f else 3f
        }
        mapView.overlays.add(circle)
    }

    // Center map on locations
    if (workLocations.size == 1) {
        val location = workLocations.first()
        mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
        mapView.controller.setZoom(15.0)
    } else if (workLocations.size > 1) {
        // Calculate bounding box to show all locations
        val latitudes = workLocations.map { it.latitude }
        val longitudes = workLocations.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0

        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))

        // Calculate appropriate zoom level (simplified)
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)

        val zoom = when {
            maxDiff > 1.0 -> 8.0
            maxDiff > 0.5 -> 10.0
            maxDiff > 0.1 -> 12.0
            maxDiff > 0.05 -> 13.0
            else -> 14.0
        }
        mapView.controller.setZoom(zoom)
    }

    mapView.invalidate()
}

// Helper function to check if a point is inside a geofence
private fun isLocationInsideGeofence(
    currentLat: Double,
    currentLng: Double,
    geofenceLat: Double,
    geofenceLng: Double,
    radiusMeters: Double
): Boolean {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        currentLat, currentLng,
        geofenceLat, geofenceLng,
        results
    )
    return results[0] <= radiusMeters
}
