package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Schließtage der Einrichtung
 *
 * Speichert Zeiträume, in denen die Einrichtung geschlossen ist
 * (z.B. Betriebsferien, Brückentage, Schließzeiten).
 *
 * Diese Tage werden automatisch als "Pflicht-Urlaub" markiert
 * und in der KI-Urlaubsplanung berücksichtigt.
 */
@Entity(
    tableName = "closing_days",
    indices = [
        Index(value = ["year"]),
        Index(value = ["startDate", "endDate"])
    ]
)
data class ClosingDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Beschreibung des Schließtags/Zeitraums
     * z.B. "Weihnachtsferien 2025", "Brückentag Pfingsten", "Betriebsurlaub"
     */
    val title: String,

    /**
     * Startdatum des Zeitraums (yyyy-MM-dd)
     * Bei einzelnen Tagen = endDate
     */
    val startDate: String,

    /**
     * Enddatum des Zeitraums (yyyy-MM-dd)
     * Bei einzelnen Tagen = startDate
     */
    val endDate: String,

    /**
     * Jahr des Schließtags (für bessere Filterung)
     * Wird aus startDate extrahiert
     */
    val year: Int,

    /**
     * Optionale Notiz
     * z.B. "Notdienst verfügbar" oder "Schulschließung"
     */
    val note: String? = null,

    /**
     * Farbe für Kalenderdarstellung (optional)
     * Hex-Code z.B. "#FF5252"
     */
    val color: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Prüft ob ein Datum innerhalb des Schließzeitraums liegt
     * @param date yyyy-MM-dd Format
     */
    fun contains(date: String): Boolean {
        return date >= startDate && date <= endDate
    }

    /**
     * Gibt die Anzahl der Schließtage zurück
     */
    fun getDurationDays(): Int {
        val start = java.time.LocalDate.parse(startDate)
        val end = java.time.LocalDate.parse(endDate)
        return java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
    }

    /**
     * Prüft ob es sich um einen einzelnen Tag handelt
     */
    fun isSingleDay(): Boolean {
        return startDate == endDate
    }

    /**
     * Formatierte Anzeige des Zeitraums
     * z.B. "24.12.2025 - 26.12.2025" oder "01.05.2025"
     */
    fun getFormattedDateRange(): String {
        val start = java.time.LocalDate.parse(startDate)
        val end = java.time.LocalDate.parse(endDate)

        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")

        return if (isSingleDay()) {
            start.format(formatter)
        } else {
            "${start.format(formatter)} - ${end.format(formatter)}"
        }
    }
}
