package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Soll-Arbeitszeit Profil
 * Einfache Vorlage mit individuellen Tageszeiten (Mo-So)
 */
@Entity(tableName = "soll_zeit_vorlagen")
data class SollZeitVorlage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                        // z.B. "Normal", "Ferienbetreuung"

    // Soll-Arbeitszeiten pro Wochentag (in Minuten)
    val montagSollMinuten: Int = 0,
    val dienstagSollMinuten: Int = 0,
    val mittwochSollMinuten: Int = 0,
    val donnerstagSollMinuten: Int = 0,
    val freitagSollMinuten: Int = 0,
    val samstagSollMinuten: Int = 0,
    val sonntagSollMinuten: Int = 0,

    // Ist dies die Standard-Vorlage?
    val isDefault: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Gibt die Soll-Minuten für einen bestimmten Wochentag zurück
     * @param dayOfWeek 1=Montag, 2=Dienstag, ..., 7=Sonntag
     */
    fun getSollMinutenForDay(dayOfWeek: Int): Int {
        return when (dayOfWeek) {
            1 -> montagSollMinuten
            2 -> dienstagSollMinuten
            3 -> mittwochSollMinuten
            4 -> donnerstagSollMinuten
            5 -> freitagSollMinuten
            6 -> samstagSollMinuten
            7 -> sonntagSollMinuten
            else -> 0
        }
    }

    /**
     * Berechnet die Gesamt-Wochenstunden
     */
    fun getWochenStundenMinuten(): Int {
        return montagSollMinuten + dienstagSollMinuten + mittwochSollMinuten +
               donnerstagSollMinuten + freitagSollMinuten + samstagSollMinuten + sonntagSollMinuten
    }
}
