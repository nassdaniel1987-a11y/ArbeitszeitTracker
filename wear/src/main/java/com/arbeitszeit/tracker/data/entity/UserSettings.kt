package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val einrichtung: String,
    val arbeitsumfangProzent: Int,
    val wochenStundenMinuten: Int,
    val arbeitsTageProWoche: Int = 5,
    val ferienbetreuung: Boolean = true,
    val ueberstundenVorjahrMinuten: Int,
    val letzterUebertragMinuten: Int = 0,
    val ersterMontagImJahr: String? = null,
    val workingDays: String = "12345",
    val geofencingEnabled: Boolean = false,
    val geofencingStartHour: Int = 6,
    val geofencingEndHour: Int = 20,
    val geofencingActiveDays: String = "12345",
    val darkMode: String = "system",
    val selectedTemplateYear: Int? = null,
    val bundesland: String? = null,
    val urlaubsanspruchTage: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isWorkingDay(dayOfWeek: Int): Boolean {
        return workingDays.contains(dayOfWeek.toString())
    }

    fun getWorkingDaysCount(): Int {
        return workingDays.length
    }
}
