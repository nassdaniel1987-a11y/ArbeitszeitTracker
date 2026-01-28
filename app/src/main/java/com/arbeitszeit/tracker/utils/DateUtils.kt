package com.arbeitszeit.tracker.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val weekField = WeekFields.of(Locale.GERMANY)
    
    /**
     * Konvertiert LocalDate zu String (yyyy-MM-dd)
     */
    fun dateToString(date: LocalDate): String {
        return date.format(dateFormatter)
    }
    
    /**
     * Konvertiert String zu LocalDate
     */
    fun stringToDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString, dateFormatter)
    }
    
    /**
     * Heutiges Datum als String
     */
    fun today(): String {
        return dateToString(LocalDate.now())
    }
    
    /**
     * Gestriges Datum als String
     */
    fun yesterday(): String {
        return dateToString(LocalDate.now().minusDays(1))
    }
    
    /**
     * Kalenderwoche für ein Datum (ISO 8601)
     */
    fun getWeekOfYear(date: LocalDate): Int {
        return date.get(weekField.weekOfWeekBasedYear())
    }

    /**
     * Kalenderwoche für ein Datum-String
     */
    fun getWeekOfYear(dateString: String): Int {
        return getWeekOfYear(stringToDate(dateString))
    }

    /**
     * Jahr (Wochenjahr nach ISO 8601)
     */
    fun getWeekBasedYear(date: LocalDate): Int {
        return date.get(weekField.weekBasedYear())
    }

    /**
     * Jahr für Datum-String
     */
    fun getWeekBasedYear(dateString: String): Int {
        return getWeekBasedYear(stringToDate(dateString))
    }

    /**
     * Findet den ersten Montag eines Jahres
     */
    fun findFirstMonday(year: Int): LocalDate {
        var date = LocalDate.of(year, 1, 1)
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            date = date.plusDays(1)
        }
        return date
    }

    /**
     * Berechnet Kalenderwoche basierend auf einem benutzerdefinierten ersten Montag
     * @param date Das zu prüfende Datum
     * @param firstMondayString Der erste Montag des Jahres (yyyy-MM-dd), null = ISO 8601
     * @return Kalenderwoche (1-52/53)
     *
     * WICHTIG: Wenn der erste Montag im Dezember liegt (ab Tag 25), wird er als
     * erster Montag für das NÄCHSTE Kalenderjahr interpretiert.
     * Beispiel: "2025-12-29" als erster Montag bedeutet KW 1 von Jahr 2026.
     */
    fun getCustomWeekOfYear(date: LocalDate, firstMondayString: String?): Int {
        if (firstMondayString == null) {
            return getWeekOfYear(date)
        }

        val firstMonday = stringToDate(firstMondayString)

        // Bestimme das "intendierte Jahr" für diesen ersten Montag
        // Wenn der Montag im Dezember liegt (ab Tag 25), ist er für das nächste Jahr gedacht
        val intendedYear = if (firstMonday.monthValue == 12 && firstMonday.dayOfMonth >= 25) {
            firstMonday.year + 1
        } else {
            firstMonday.year
        }

        // Wenn das Datum vor dem ersten Montag liegt, gehört es zum vorherigen Jahr
        if (date.isBefore(firstMonday)) {
            // Rekursiv das vorherige Jahr prüfen
            // WICHTIG: Wir müssen den ersten Montag des VORHERIGEN intendierten Jahres finden!
            val previousYearFirstMonday = findFirstMonday(intendedYear - 1)
            return getCustomWeekOfYear(date, dateToString(previousYearFirstMonday))
        }

        // Berechne Wochendifferenz zwischen erstem Montag und dem Datum
        val daysSinceFirstMonday = java.time.temporal.ChronoUnit.DAYS.between(firstMonday, date)
        val weeksSinceFirstMonday = daysSinceFirstMonday / 7

        return (weeksSinceFirstMonday + 1).toInt()
    }

    /**
     * Berechnet das Jahr basierend auf einem benutzerdefinierten ersten Montag
     *
     * WICHTIG: Wenn der erste Montag im Dezember liegt (ab Tag 25), wird er als
     * erster Montag für das NÄCHSTE Kalenderjahr interpretiert.
     * Beispiel: "2025-12-29" als erster Montag bedeutet Jahr 2026.
     */
    fun getCustomWeekBasedYear(date: LocalDate, firstMondayString: String?): Int {
        if (firstMondayString == null) {
            return getWeekBasedYear(date)
        }

        val firstMonday = stringToDate(firstMondayString)

        // Bestimme das "intendierte Jahr" für diesen ersten Montag
        // Wenn der Montag im Dezember liegt (ab Tag 25), ist er für das nächste Jahr gedacht
        // z.B. "2025-12-29" -> Jahr 2026
        val intendedYear = if (firstMonday.monthValue == 12 && firstMonday.dayOfMonth >= 25) {
            firstMonday.year + 1
        } else {
            firstMonday.year
        }

        // Wenn das Datum vor dem ersten Montag liegt, gehört es zum vorherigen Jahr
        if (date.isBefore(firstMonday)) {
            // Prüfe rekursiv gegen Vorjahr
            // WICHTIG: Wir müssen den ersten Montag des VORHERIGEN intendierten Jahres finden!
            val previousYearFirstMonday = findFirstMonday(intendedYear - 1)
            return getCustomWeekBasedYear(date, dateToString(previousYearFirstMonday))
        } else {
            // Das Datum liegt nach dem ersten Montag -> gehört zum intendierten Jahr
            return intendedYear
        }
    }
    
    /**
     * Wochentag als deutscher Kurzname (Mo, Di, ...)
     */
    fun getWeekdayShort(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "Mo"
            2 -> "Di"
            3 -> "Mi"
            4 -> "Do"
            5 -> "Fr"
            6 -> "Sa"
            7 -> "So"
            else -> ""
        }
    }
    
    /**
     * Montag der aktuellen Woche
     */
    fun getMondayOfWeek(date: LocalDate): LocalDate {
        return date.with(weekField.dayOfWeek(), 1)
    }
    
    /**
     * Sonntag der aktuellen Woche
     */
    fun getSundayOfWeek(date: LocalDate): LocalDate {
        return date.with(weekField.dayOfWeek(), 7)
    }
    
    /**
     * Alle Tage einer Woche (Mo-So) als Liste
     */
    fun getDaysOfWeek(date: LocalDate): List<LocalDate> {
        val monday = getMondayOfWeek(date)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
    
    /**
     * Datumsformat für UI (z.B. "08.11.2025")
     */
    fun formatForDisplay(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date)
    }
    
    /**
     * Datumsformat für UI mit Wochentag (z.B. "Mo, 08.11.2025")
     */
    fun formatForDisplayWithWeekday(date: LocalDate): String {
        val weekday = getWeekdayShort(date)
        return "$weekday, ${formatForDisplay(date)}"
    }
    
    /**
     * Prüft ob ein Datum ein Wochenende ist
     */
    fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek.value in 6..7
    }
    
    /**
     * Prüft ob ein Datum heute ist
     */
    fun isToday(dateString: String): Boolean {
        return dateString == today()
    }
    
    /**
     * Berechnet welches KW-Sheet ein bestimmtes Datum hat
     * Gibt Sheet-Namen zurück (z.B. "KW 01-04")
     */
    fun getSheetNameForWeek(kw: Int): String {
        val startKW = ((kw - 1) / 4) * 4 + 1
        val endKW = startKW + 3
        return "KW ${String.format("%02d", startKW)}-${String.format("%02d", endKW)}"
    }
    
    /**
     * Gibt die Start- und End-KW für ein Sheet zurück
     */
    fun getWeekRangeForSheet(kw: Int): Pair<Int, Int> {
        val startKW = ((kw - 1) / 4) * 4 + 1
        val endKW = startKW + 3
        return Pair(startKW, endKW)
    }

    /**
     * Konvertiert LocalDate zu deutschem Format (dd.MM.yyyy)
     */
    fun dateToGermanString(date: LocalDate): String {
        val germanFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return date.format(germanFormatter)
    }
}
