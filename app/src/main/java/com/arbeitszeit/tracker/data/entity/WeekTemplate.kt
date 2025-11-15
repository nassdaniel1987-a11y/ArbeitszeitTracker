package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wochen-Vorlage: Speichert eine Musterwoche die auf andere Wochen angewendet werden kann
 */
@Entity(tableName = "week_templates")
data class WeekTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
