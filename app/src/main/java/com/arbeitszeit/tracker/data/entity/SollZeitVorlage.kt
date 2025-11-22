package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Vorlage für Soll-Arbeitszeiten
 * Ermöglicht mehrere verschiedene Arbeitszeitmodelle (z.B. Normal, Ferienbetreuung, Sommer, etc.)
 */
@Entity(tableName = "soll_zeit_vorlagen")
data class SollZeitVorlage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                        // z.B. "Normal", "Ferienbetreuung", "Sommer"
    val arbeitsumfangProzent: Int,           // z.B. 93
    val wochenStundenMinuten: Int,           // z.B. 37:16 = 2236 Minuten
    val arbeitsTageProWoche: Int = 5,

    // Individuelle Soll-Arbeitszeiten pro Wochentag (in Minuten, null = automatisch berechnen)
    val montagSollMinuten: Int? = null,
    val dienstagSollMinuten: Int? = null,
    val mittwochSollMinuten: Int? = null,
    val donnerstagSollMinuten: Int? = null,
    val freitagSollMinuten: Int? = null,
    val samstagSollMinuten: Int? = null,
    val sonntagSollMinuten: Int? = null,

    // Arbeitstage Definition (Mo=1, Di=2, Mi=3, Do=4, Fr=5, Sa=6, So=7)
    // z.B. "12345" = Montag bis Freitag
    val workingDays: String = "12345",

    // Farbcode für UI (optional, z.B. für visuelle Unterscheidung)
    val colorHex: String? = null,

    // Ist dies die Standard-Vorlage?
    val isDefault: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Gibt die Soll-Minuten für einen bestimmten Wochentag zurück
     * @param dayOfWeek 1=Montag, 2=Dienstag, ..., 7=Sonntag
     */
    fun getSollMinutenForDay(dayOfWeek: Int): Int? {
        return when (dayOfWeek) {
            1 -> montagSollMinuten
            2 -> dienstagSollMinuten
            3 -> mittwochSollMinuten
            4 -> donnerstagSollMinuten
            5 -> freitagSollMinuten
            6 -> samstagSollMinuten
            7 -> sonntagSollMinuten
            else -> null
        }
    }

    /**
     * Berechnet automatische Soll-Minuten für einen Arbeitstag
     * Wenn keine individuellen Zeiten gesetzt sind
     */
    fun getAutomaticSollMinutenForDay(dayOfWeek: Int): Int {
        val individualSoll = getSollMinutenForDay(dayOfWeek)
        if (individualSoll != null) return individualSoll

        // Automatisch: Wochenstunden / Arbeitstage
        return if (isWorkingDay(dayOfWeek)) {
            wochenStundenMinuten / arbeitsTageProWoche
        } else {
            0
        }
    }

    /**
     * Prüft ob individuelle Tages-Soll-Zeiten definiert sind
     */
    fun hasIndividualDailyHours(): Boolean {
        return montagSollMinuten != null ||
               dienstagSollMinuten != null ||
               mittwochSollMinuten != null ||
               donnerstagSollMinuten != null ||
               freitagSollMinuten != null ||
               samstagSollMinuten != null ||
               sonntagSollMinuten != null
    }

    /**
     * Prüft ob ein Tag ein Arbeitstag ist
     * @param dayOfWeek 1=Montag, 2=Dienstag, ..., 7=Sonntag
     */
    fun isWorkingDay(dayOfWeek: Int): Boolean {
        return workingDays.contains(dayOfWeek.toString())
    }

    /**
     * Gibt die Anzahl der definierten Arbeitstage zurück
     */
    fun getWorkingDaysCount(): Int {
        return workingDays.length
    }
}
