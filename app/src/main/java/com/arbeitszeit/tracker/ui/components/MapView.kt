package com.arbeitszeit.tracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.arbeitszeit.tracker.data.entity.WorkLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun OpenStreetMapView(
    workLocations: List<WorkLocation>,
    modifier: Modifier = Modifier,
    initialZoom: Double = 15.0,
    onMapReady: ((MapView) -> Unit)? = null
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    LaunchedEffect(workLocations) {
        updateMapMarkers(mapView, workLocations, context)
        onMapReady?.invoke(mapView)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { map ->
            // Update map when composition changes
            updateMapMarkers(map, workLocations, context)
        }
    )
}

private fun createMapView(context: Context): MapView {
    // OSMDroid configuration
    Configuration.getInstance().load(
        context,
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
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
    context: Context
) {
    mapView.overlays.clear()

    if (workLocations.isEmpty()) {
        mapView.invalidate()
        return
    }

    // Add markers and circles for each work location
    workLocations.forEach { location ->
        // Add marker
        val marker = Marker(mapView).apply {
            position = GeoPoint(location.latitude, location.longitude)
            title = location.name
            snippet = if (!location.address.isNullOrBlank()) {
                location.address
            } else {
                "Radius: ${location.radiusMeters.toInt()}m"
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Set marker color based on enabled status
            if (location.enabled) {
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
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
            fillPaint.color = if (location.enabled) {
                android.graphics.Color.argb(50, 76, 175, 80) // Green with transparency
            } else {
                android.graphics.Color.argb(30, 128, 128, 128) // Gray with transparency
            }
            outlinePaint.color = if (location.enabled) {
                android.graphics.Color.argb(200, 76, 175, 80)
            } else {
                android.graphics.Color.argb(150, 128, 128, 128)
            }
            outlinePaint.strokeWidth = 3f
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
