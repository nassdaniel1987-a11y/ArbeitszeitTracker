package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val name: String,                        // z.B. "Nass, Daniel"
    val einrichtung: String,                 // z.B. "Österfeldschule Vaihingen"
    val arbeitsumfangProzent: Int,           // z.B. 93
    val wochenStundenMinuten: Int,           // z.B. 37:16 = 2236 Minuten
    val arbeitsTageProWoche: Int = 5,
    val ferienbetreuung: Boolean = true,
    val ueberstundenVorjahrMinuten: Int,     // Übertrag, kann negativ sein
    val letzterUebertragMinuten: Int = 0,    // Übertrag aus letzter 4-Wochen-Periode
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
