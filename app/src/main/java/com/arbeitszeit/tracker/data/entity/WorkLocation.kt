package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.osmdroid.util.GeoPoint

/**
 * Arbeitsort für Geofencing
 * Unterstützt zwei Modi: Kreis (Radius) oder Polygon (freie Bereichsmarkierung)
 */
@Entity(tableName = "work_locations")
data class WorkLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Name des Arbeitsorts (z.B. "Hauptbüro", "Außenstelle", "Schulgelände")
    val name: String,

    // GPS-Koordinaten (Mittelpunkt)
    val latitude: Double,
    val longitude: Double,

    // Formatierte Adresse (z.B. "Musterstraße 123, 70173 Stuttgart")
    val address: String? = null,

    // Radius in Metern (z.B. 100) - Nur für Kreis-Modus
    val radiusMeters: Float = 100f,

    // Polygon-Punkte als JSON String: "[{lat:48.x,lng:9.x},{lat:48.y,lng:9.y},...]"
    // Wenn null = Kreis-Modus, wenn gesetzt = Polygon-Modus
    val polygonPoints: String? = null,

    // Ist dieser Ort aktiviert?
    val enabled: Boolean = true,

    // Zeitstempel
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Prüft ob dieser Ort ein Polygon ist (statt Kreis)
     */
    fun isPolygon(): Boolean = !polygonPoints.isNullOrEmpty()

    /**
     * Gibt die Polygon-Punkte als Liste zurück
     */
    fun getPolygonPointsList(): List<GeoPoint> {
        if (polygonPoints.isNullOrEmpty()) return emptyList()

        return try {
            val jsonArray = JSONArray(polygonPoints)
            (0 until jsonArray.length()).map { i ->
                val point = jsonArray.getJSONObject(i)
                GeoPoint(point.getDouble("lat"), point.getDouble("lng"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Prüft ob ein Punkt innerhalb des Arbeitsorts liegt
     * - Für Kreis: Distanz-Check
     * - Für Polygon: Point-in-Polygon Check
     */
    fun containsPoint(latitude: Double, longitude: Double): Boolean {
        return if (isPolygon()) {
            isPointInPolygon(latitude, longitude, getPolygonPointsList())
        } else {
            // Kreis-Modus: Distanz berechnen
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                this.latitude, this.longitude,
                results
            )
            results[0] <= radiusMeters
        }
    }

    /**
     * Point-in-Polygon Algorithmus (Ray Casting)
     */
    private fun isPointInPolygon(lat: Double, lng: Double, polygon: List<GeoPoint>): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]

            if ((pi.longitude > lng) != (pj.longitude > lng) &&
                lat < (pj.latitude - pi.latitude) * (lng - pi.longitude) /
                      (pj.longitude - pi.longitude) + pi.latitude
            ) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    companion object {
        /**
         * Erstellt einen JSON String aus einer Liste von GeoPoints
         */
        fun polygonPointsToJson(points: List<GeoPoint>): String {
            val jsonArray = JSONArray()
            points.forEach { point ->
                val jsonObj = org.json.JSONObject()
                jsonObj.put("lat", point.latitude)
                jsonObj.put("lng", point.longitude)
                jsonArray.put(jsonObj)
            }
            return jsonArray.toString()
        }
    }
}

