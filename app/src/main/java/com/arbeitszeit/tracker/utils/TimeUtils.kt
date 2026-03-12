package com.arbeitszeit.tracker.utils

import java.time.LocalTime
import kotlin.math.roundToInt

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
     * Konvertiert Stunden:Minuten String zu Minuten (mit Support für negative Werte)
     * Beispiele: "5:30" -> 330, "-2:15" -> -135, "0:00" -> 0
     */
    fun hoursMinutesToMinutes(timeString: String): Int {
        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return 0

        val isNegative = trimmed.startsWith("-")
        val cleanString = if (isNegative) trimmed.substring(1) else trimmed

        val parts = cleanString.split(":")
        if (parts.size != 2) return 0

        return try {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val totalMinutes = hours * 60 + minutes
            if (isNegative) -totalMinutes else totalMinutes
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Konvertiert Minuten zu Excel-Dezimalwert (Bruchteil eines Tages)
     * Für Excel-Export: 1440 Minuten = 1 Tag
     */
    fun minutesToExcelTime(minutes: Int): Double {
        return minutes / 1440.0
    }

    /**
     * Konvertiert Excel-Dezimalwert zu Minuten
     * Für Excel-Import: 1.0 = 1440 Minuten (24 Stunden)
     * Verwendet Math.round für konsistente Rundung (immer bei .5 aufrunden)
     */
    fun excelTimeToMinutes(excelTime: Double): Int {
        return kotlin.math.round(excelTime * 1440.0).toInt()
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

    /**
     * Berechnet die gesetzliche Mindestpause basierend auf Arbeitszeit
     *
     * Gesetzliche Regelung in Deutschland (ArbZG §4):
     * - Bis 6h Arbeit: keine Pflichtpause
     * - 6-9h Arbeit: mindestens 30 Min Pause
     * - Über 9h Arbeit: mindestens 45 Min Pause
     *
     * @param workMinutes Arbeitszeit in Minuten (ohne Pause)
     * @return Mindestpause in Minuten
     */
    fun calculateMinimumBreak(workMinutes: Int): Int {
        return when {
            workMinutes > 9 * 60 -> 45  // Über 9h: 45 Min
            workMinutes > 6 * 60 -> 30  // Über 6h: 30 Min
            else -> 0                    // Unter 6h: keine Pflicht
        }
    }

    /**
     * Berechnet empfohlene Pause basierend auf Arbeitszeit und User-Default
     * Nimmt das Maximum aus gesetzlicher Mindestpause und User-Einstellung
     *
     * @param workMinutes Arbeitszeit in Minuten
     * @param userDefault User-Einstellung für Standard-Pause
     * @return Empfohlene Pause in Minuten
     */
    fun calculateRecommendedBreak(workMinutes: Int, userDefault: Int): Int {
        val minimum = calculateMinimumBreak(workMinutes)
        return maxOf(minimum, userDefault)
    }

    /**
     * Gibt einen Erklärungstext zurück, warum eine bestimmte Pause vorgeschlagen wird.
     * Basierend auf dem deutschen Arbeitszeitgesetz (ArbZG §4).
     *
     * @param totalDurationMinutes Gesamtdauer (Anwesenheit) in Minuten
     * @return Erklärungstext zur gesetzlichen Pausenregelung
     */
    fun getBreakLegalInfo(totalDurationMinutes: Int): String {
        return when {
            totalDurationMinutes > 9 * 60 ->
                "Gesetzliche Mindestpause: 45 Min\n" +
                "Bei mehr als 9 Stunden Arbeitszeit ist eine Pause von mindestens 45 Minuten vorgeschrieben (ArbZG §4)."
            totalDurationMinutes > 6 * 60 ->
                "Gesetzliche Mindestpause: 30 Min\n" +
                "Bei mehr als 6 Stunden Arbeitszeit ist eine Pause von mindestens 30 Minuten vorgeschrieben (ArbZG §4)."
            else ->
                "Keine gesetzliche Pausenpflicht\n" +
                "Bei bis zu 6 Stunden Arbeitszeit besteht keine gesetzliche Pausenpflicht (ArbZG §4)."
        }
    }

    /**
     * Prüft ob die eingestellte Pause unter der gesetzlichen Mindestpause liegt.
     *
     * @param totalDurationMinutes Gesamtdauer (Anwesenheit) in Minuten
     * @param pauseMinutes Eingestellte Pause in Minuten
     * @return Warntext oder null wenn Pause ausreichend
     */
    fun getBreakWarning(totalDurationMinutes: Int, pauseMinutes: Int): String? {
        val minimum = calculateMinimumBreak(totalDurationMinutes)
        if (minimum > 0 && pauseMinutes < minimum) {
            return "Die eingestellte Pause ($pauseMinutes Min) liegt unter der gesetzlichen Mindestpause von $minimum Minuten (ArbZG §4)."
        }
        return null
    }
}
