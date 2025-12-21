package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Einstellungen pro Jahr
 *
 * Jedes Jahr hat seine eigenen Einstellungen für:
 * - Erster Montag (Custom KW-Berechnung)
 * - Urlaubsanspruch
 * - Vorjahresübertrag (Überstunden aus Vorjahr)
 * - Excel-Vorlage
 *
 * Vorteile:
 * - Klare Trennung der Jahre
 * - Automatischer Übertrag von Überstunden
 * - Jahr-spezifische Konfiguration
 * - Konsistente KW/Überstunden/Urlaub-Berechnung
 */
@Entity(
    tableName = "year_settings",
    indices = [Index(value = ["isActive"])]
)
data class YearSettings(
    @PrimaryKey
    val year: Int,                          // Jahr (z.B. 2025, 2026)

    val ersterMontagImJahr: String,         // Erster Montag für Custom-KW (yyyy-MM-dd)

    val urlaubsanspruchTage: Int,           // Urlaubsanspruch für dieses Jahr (z.B. 30)

    val vorjahresUebertragMinuten: Int,     // Überstunden-Übertrag aus Vorjahr (in Minuten)

    val hasExcelTemplate: Boolean,          // Ob Excel-Vorlage für dieses Jahr existiert

    val isActive: Boolean,                  // Ist dies das aktuell ausgewählte Jahr?

    val createdAt: Long,                    // Wann wurde dieses Jahr angelegt

    val isArchived: Boolean = false         // Ist das Jahr archiviert? (Optional für Zukunft)
) {
    /**
     * Gibt Vorjahresübertrag als formatierter String zurück
     * z.B. "+37:16" oder "-2:30"
     */
    fun getVorjahresUebertragFormatted(): String {
        val isNegative = vorjahresUebertragMinuten < 0
        val absMinutes = kotlin.math.abs(vorjahresUebertragMinuten)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val sign = if (isNegative) "-" else "+"
        return String.format("%s%d:%02d", sign, hours, mins)
    }

    /**
     * Prüft ob dies das aktuelle Kalenderjahr ist
     */
    fun isCurrentYear(): Boolean {
        return year == java.time.LocalDate.now().year
    }

    companion object {
        /**
         * Erstellt Default-Einstellungen für ein Jahr
         */
        fun createDefault(
            year: Int,
            ersterMontagImJahr: String,
            urlaubsanspruch: Int = 30,
            vorjahresUebertrag: Int = 0,
            isActive: Boolean = false
        ): YearSettings {
            return YearSettings(
                year = year,
                ersterMontagImJahr = ersterMontagImJahr,
                urlaubsanspruchTage = urlaubsanspruch,
                vorjahresUebertragMinuten = vorjahresUebertrag,
                hasExcelTemplate = false,
                isActive = isActive,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}
