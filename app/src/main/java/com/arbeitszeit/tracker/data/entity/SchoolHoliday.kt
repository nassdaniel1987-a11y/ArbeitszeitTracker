package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Schulferien für ein Bundesland
 *
 * Wird verwendet um:
 * - Schulferien in der Urlaubsplanung zu berücksichtigen
 * - Optimale Urlaubszeiten vorzuschlagen (Schulferien + ein paar Tage = super lange frei!)
 * - Konflikte mit Ferienbetreuung zu erkennen
 */
@Entity(tableName = "school_holidays")
data class SchoolHoliday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Bundesland-Code (z.B. "BW", "BY", "BE")
     */
    val bundesland: String,

    /**
     * Jahr der Ferien
     */
    val year: Int,

    /**
     * Name der Ferien (z.B. "Sommerferien", "Weihnachtsferien", "Osterferien")
     */
    val name: String,

    /**
     * Startdatum (yyyy-MM-dd)
     */
    val startDate: String,

    /**
     * Enddatum (yyyy-MM-dd)
     */
    val endDate: String,

    /**
     * Quelle der Daten (z.B. "ferien-api.de" oder "manuell")
     */
    val source: String = "manuell",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Prüft ob ein Datum in den Ferien liegt
     */
    fun contains(date: String): Boolean {
        return date >= startDate && date <= endDate
    }

    /**
     * Gibt die Anzahl der Ferientage zurück
     */
    fun getDurationDays(): Int {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        return java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
    }

    /**
     * Formatierte Anzeige des Zeitraums
     */
    fun getFormattedDateRange(): String {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")

        return "${start.format(formatter)} - ${end.format(formatter)}"
    }
}
