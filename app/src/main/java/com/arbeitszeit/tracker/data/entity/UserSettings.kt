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

    // Export Einstellungen
    val selectedTemplateYear: Int? = null,   // Welches Jahr der Vorlage verwendet werden soll (null = Standard aus Assets)

    // Feiertags-Einstellungen
    val bundesland: String? = null,          // Bundesland-Code (z.B. "BW", "BY") für bundeslandspezifische Feiertage

    // Urlaubs-Einstellungen
    val urlaubsanspruchTage: Int = 30,       // Jahresurlaub in Tagen (Standard: 30 Tage)

    // Jahr-Management
    val autoSwitchYear: Boolean = true,      // Automatischer Wechsel am ersten Montag des Jahres

    // Auto-Start Einstellungen
    val autoStartEnabled: Boolean = false,           // Auto-Start basierend auf Wochenvorlagen aktivieren
    val autoStartRequiresGeofencing: Boolean = true, // Auto-Start nur wenn am Arbeitsort (Geofencing)
    val autoStartReminderMinutes: Int = 5,           // Vor-Erinnerung X Minuten vor Auto-Start
    val autoStartDefaultPauseMinutes: Int = 30,      // Standard-Pausenzeit beim Beenden

    // Auto-Start Zeiten pro Wochentag (in Minuten seit Mitternacht, z.B. 480 = 08:00)
    // null = kein Auto-Start an diesem Tag
    val autoStartMontagZeit: Int? = null,
    val autoStartDienstagZeit: Int? = null,
    val autoStartMittwochZeit: Int? = null,
    val autoStartDonnerstagZeit: Int? = null,
    val autoStartFreitagZeit: Int? = null,
    val autoStartSamstagZeit: Int? = null,
    val autoStartSonntagZeit: Int? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
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

    /**
     * Gibt die Auto-Start Zeit für einen bestimmten Wochentag zurück
     * @param dayOfWeek 1=Montag, 2=Dienstag, ..., 7=Sonntag
     * @return Minuten seit Mitternacht oder null wenn kein Auto-Start an diesem Tag
     */
    fun getAutoStartZeitForDay(dayOfWeek: Int): Int? {
        return when (dayOfWeek) {
            1 -> autoStartMontagZeit
            2 -> autoStartDienstagZeit
            3 -> autoStartMittwochZeit
            4 -> autoStartDonnerstagZeit
            5 -> autoStartFreitagZeit
            6 -> autoStartSamstagZeit
            7 -> autoStartSonntagZeit
            else -> null
        }
    }
}
