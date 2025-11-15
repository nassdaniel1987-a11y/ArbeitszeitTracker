package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Einzelner Tag innerhalb einer Wochen-Vorlage
 */
@Entity(
    tableName = "week_template_entries",
    foreignKeys = [
        ForeignKey(
            entity = WeekTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class WeekTemplateEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val dayOfWeek: Int,             // 1=Montag, 2=Dienstag, ..., 7=Sonntag
    val startZeit: Int?,            // Minuten seit Mitternacht (null = nicht eingetragen)
    val endZeit: Int?,              // Minuten seit Mitternacht
    val pauseMinuten: Int = 0,
    val typ: String = TimeEntry.TYP_NORMAL,  // NORMAL, URLAUB, KRANK, FEIERTAG, ABWESEND
    val notiz: String = ""
)
