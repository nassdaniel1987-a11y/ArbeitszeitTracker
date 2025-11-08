package com.arbeitszeit.tracker.utils

import java.time.LocalTime

object TimeUtils {
    
    /**
     * Konvertiert Minuten seit Mitternacht zu HH:MM String
     */
    fun minutesToTimeString(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
    
    /**
     * Konvertiert HH:MM String zu Minuten seit Mitternacht
     */
    fun timeStringToMinutes(timeString: String): Int {
        val parts = timeString.split(":")
        if (parts.size != 2) return 0
        
        return try {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60 + minutes
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Aktuelle Zeit in Minuten seit Mitternacht
     */
    fun currentTimeInMinutes(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }
    
    /**
     * Aktuelle Zeit als HH:MM String
     */
    fun currentTimeString(): String {
        return minutesToTimeString(currentTimeInMinutes())
    }
    
    /**
     * Konvertiert Minuten zu Stunden:Minuten Format (z.B. "8:30" oder "40:15")
     */
    fun minutesToHoursMinutes(minutes: Int): String {
        val absoluteMinutes = kotlin.math.abs(minutes)
        val hours = absoluteMinutes / 60
        val mins = absoluteMinutes % 60
        val sign = if (minutes < 0) "-" else ""
        return String.format("%s%d:%02d", sign, hours, mins)
    }
    
    /**
     * Konvertiert Minuten zu Excel-Dezimalwert (Bruchteil eines Tages)
     * Für Excel-Export: 1440 Minuten = 1 Tag
     */
    fun minutesToExcelTime(minutes: Int): Double {
        return minutes / 1440.0
    }
    
    /**
     * Formatiert Minuten als Stunden mit 2 Dezimalstellen (z.B. "8.50h")
     */
    fun minutesToDecimalHours(minutes: Int): String {
        val hours = minutes / 60.0
        return String.format("%.2fh", hours)
    }
    
    /**
     * Formatiert Differenz mit Vorzeichen (z.B. "+0:48" oder "-1:15")
     */
    fun formatDifferenz(minutes: Int): String {
        val sign = if (minutes >= 0) "+" else ""
        return sign + minutesToHoursMinutes(minutes)
    }
    
    /**
     * Validiert Zeit-String (HH:MM Format)
     */
    fun isValidTimeString(timeString: String): Boolean {
        val parts = timeString.split(":")
        if (parts.size != 2) return false
        
        return try {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours in 0..23 && minutes in 0..59
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Rundet Minuten auf 5er-Schritte (z.B. 447 -> 445, 448 -> 450)
     */
    fun roundToFiveMinutes(minutes: Int): Int {
        return (minutes / 5) * 5 + if (minutes % 5 >= 3) 5 else 0
    }
    
    /**
     * Berechnet Arbeitszeit: Ende - Start - Pause
     */
    fun calculateWorkTime(startMinutes: Int, endMinutes: Int, pauseMinutes: Int): Int {
        return endMinutes - startMinutes - pauseMinutes
    }
    
    /**
     * Formatiert Zeit für UI-Anzeige
     */
    fun formatTimeForDisplay(minutes: Int?): String {
        return if (minutes == null) "--:--" else minutesToTimeString(minutes)
    }
}
