package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Arbeitsort für Geofencing
 */
@Entity(tableName = "work_locations")
data class WorkLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Name des Arbeitsorts (z.B. "Hauptbüro", "Außenstelle")
    val name: String,

    // GPS-Koordinaten
    val latitude: Double,
    val longitude: Double,

    // Formatierte Adresse (z.B. "Musterstraße 123, 70173 Stuttgart")
    val address: String? = null,

    // Radius in Metern (z.B. 100)
    val radiusMeters: Float = 100f,

    // Ist dieser Ort aktiviert?
    val enabled: Boolean = true,

    // Zeitstempel
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
