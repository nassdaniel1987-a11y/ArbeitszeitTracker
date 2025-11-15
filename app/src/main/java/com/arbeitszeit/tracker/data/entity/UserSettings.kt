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
    val ersterMontagImJahr: String? = null,  // Erster Montag für KW-Berechnung (yyyy-MM-dd)
    // Individuelle Soll-Arbeitszeiten pro Wochentag (in Minuten, null = Standard verwenden)
    val montagSollMinuten: Int? = null,
    val dienstagSollMinuten: Int? = null,
    val mittwochSollMinuten: Int? = null,
    val donnerstagSollMinuten: Int? = null,
    val freitagSollMinuten: Int? = null,
    val samstagSollMinuten: Int? = null,
    val sonntagSollMinuten: Int? = null,

    // Arbeitstage Definition (Mo=1, Di=2, Mi=3, Do=4, Fr=5, Sa=6, So=7)
    // z.B. "12345" = Montag bis Freitag, "123456" = Montag bis Samstag
    val workingDays: String = "12345",  // Default: Mo-Fr

    // Geofencing Einstellungen
    val geofencingEnabled: Boolean = false,
    val geofencingStartHour: Int = 6,        // Aktiv ab 6 Uhr
    val geofencingEndHour: Int = 20,         // Aktiv bis 20 Uhr
    val geofencingActiveDays: String = "12345", // Mo=1, Di=2, Mi=3, Do=4, Fr=5, Sa=6, So=7

    // UI Einstellungen
    val darkMode: String = "system",         // "system", "light", oder "dark"

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

    /**
     * Prüft ob Geofencing zu dieser Zeit aktiv sein soll
     * @param dayOfWeek 1=Montag, 2=Dienstag, ..., 7=Sonntag
     * @param hourOfDay 0-23
     */
    fun isGeofencingActiveNow(dayOfWeek: Int, hourOfDay: Int): Boolean {
        if (!geofencingEnabled) return false
        if (hourOfDay < geofencingStartHour || hourOfDay >= geofencingEndHour) return false
        return geofencingActiveDays.contains(dayOfWeek.toString())
    }
}
