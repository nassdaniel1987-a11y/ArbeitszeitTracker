package com.arbeitszeit.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: String,                  // Format: "yyyy-MM-dd"
    val wochentag: String,              // Mo, Di, Mi, Do, Fr, Sa, So
    val kalenderwoche: Int,
    val jahr: Int,
    val startZeit: Int?,                // Minuten seit Mitternacht (null = nicht eingetragen)
    val endZeit: Int?,                  // Minuten seit Mitternacht
    val pauseMinuten: Int = 0,
    val sollMinuten: Int,               // Soll-Arbeitszeit für diesen Tag
    val typ: String = "NORMAL",         // NORMAL, URLAUB, KRANK, FEIERTAG, ABWESEND
    val notiz: String = "",
    val arbeitszeitBereitschaft: Int = 0, // Spalte J: "AZ aus Bereitschaft"
    val isManualEntry: Boolean = false,   // War ein Nachtrag?
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYP_NORMAL = "NORMAL"
        const val TYP_URLAUB = "U"
        const val TYP_KRANK = "K"
        const val TYP_FEIERTAG = "F"
        const val TYP_ABWESEND = "AB"
    }
    
    /**
     * Berechnet die Ist-Arbeitszeit in Minuten
     */
    fun getIstMinuten(): Int {
        if (startZeit == null || endZeit == null) return 0
        return endZeit - startZeit - pauseMinuten
    }
    
    /**
     * Berechnet die Differenz (Ist - Soll) in Minuten
     */
    fun getDifferenzMinuten(): Int {
        return getIstMinuten() - sollMinuten + arbeitszeitBereitschaft
    }
    
    /**
     * Prüft ob der Eintrag vollständig ist
     */
    fun isComplete(): Boolean {
        return when (typ) {
            TYP_NORMAL -> startZeit != null && endZeit != null
            else -> true // U/K/F/AB brauchen keine Zeiten
        }
    }
}
