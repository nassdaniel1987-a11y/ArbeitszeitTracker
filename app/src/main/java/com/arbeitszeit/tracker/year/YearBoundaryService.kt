package com.arbeitszeit.tracker.year

import android.content.Context
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

/**
 * Zentraler Service für alle Jahres-Grenzen und Berechnungen
 *
 * Verantwortlichkeiten:
 * - Jahr-Grenzen berechnen (erster Montag, letzter Freitag)
 * - Navigations-Grenzen prüfen (Kalender, Wochen)
 * - Custom Jahr/KW berechnen (delegiert an DateUtils)
 * - Einträge filtern (Custom-Year vs Calendar-Year)
 *
 * Vorteile:
 * - Single Source of Truth für alle Jahr-Berechnungen
 * - Keine doppelte Logik in ViewModels
 * - Einfach testbar
 * - Klare API
 */
class YearBoundaryService(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val yearSettingsDao = database.yearSettingsDao()
    private val timeEntryDao = database.timeEntryDao()

    // ============================================================================
    // JAHR-GRENZEN BERECHNEN
    // ============================================================================

    /**
     * Findet den ersten Montag eines Jahres
     * Delegiert an DateUtils um Logik zentral zu halten
     *
     * @param year Kalenderjahr (z.B. 2026)
     * @return Erster Montag des Jahres
     */
    fun findFirstMonday(year: Int): LocalDate {
        return DateUtils.findFirstMonday(year)
    }

    /**
     * Findet den letzten Freitag eines Arbeitsjahres
     *
     * Der letzte Freitag ist der Freitag VOR dem ersten Montag des Folgejahres.
     * Beispiel: Wenn 2027 am 4. Januar (Montag) beginnt,
     * dann endet 2026 am 1. Januar (Freitag).
     *
     * @param year Custom-Jahr (z.B. 2026)
     * @return Letzter Freitag des Arbeitsjahres
     */
    fun findLastFriday(year: Int): LocalDate {
        val firstMondayNextYear = findFirstMonday(year + 1)
        // Montag - 3 Tage = Freitag davor
        return firstMondayNextYear.minusDays(3)
    }

    /**
     * Holt die Jahres-Grenzen für ein Custom-Jahr
     *
     * @param year Custom-Jahr
     * @return Pair(ersterMontag, letzterFreitag) oder null wenn Jahr nicht existiert
     */
    suspend fun getYearBoundaries(year: Int): Pair<LocalDate, LocalDate>? = withContext(Dispatchers.IO) {
        val yearSettings = yearSettingsDao.getYear(year) ?: return@withContext null
        val firstMonday = LocalDate.parse(yearSettings.ersterMontagImJahr)
        val lastFriday = findLastFriday(year)
        Pair(firstMonday, lastFriday)
    }

    /**
     * Holt die Jahres-Grenzen für das aktuell aktive Jahr
     *
     * @return Pair(ersterMontag, letzterFreitag) oder null wenn kein aktives Jahr
     */
    suspend fun getActiveYearBoundaries(): Pair<LocalDate, LocalDate>? = withContext(Dispatchers.IO) {
        val yearSettings = yearSettingsDao.getActiveYear() ?: return@withContext null
        val firstMonday = LocalDate.parse(yearSettings.ersterMontagImJahr)
        val lastFriday = findLastFriday(yearSettings.year)
        Pair(firstMonday, lastFriday)
    }

    // ============================================================================
    // NAVIGATIONS-GRENZEN PRÜFEN
    // ============================================================================

    /**
     * Prüft ob zu einem bestimmten Monat navigiert werden kann
     *
     * Regel: Navigation innerhalb des aktiven Arbeitsjahres + 6 Monate in die Zukunft
     * - Frühestens: Monat des ersten Montags
     * - Spätestens: Monat des letzten Freitags + 6 Monate (für Planung)
     *
     * @param targetMonth Ziel-Monat
     * @return true wenn Navigation erlaubt, false sonst
     */
    suspend fun canNavigateToMonth(targetMonth: YearMonth): Boolean = withContext(Dispatchers.IO) {
        val boundaries = getActiveYearBoundaries() ?: return@withContext true // Kein Limit wenn kein Jahr aktiv
        val (firstMonday, lastFriday) = boundaries

        val earliestMonth = YearMonth.from(firstMonday)
        val latestMonth = YearMonth.from(lastFriday).plusMonths(6)  // +6 Monate für Planung

        targetMonth in earliestMonth..latestMonth
    }

    /**
     * Prüft ob zur vorherigen Woche navigiert werden kann
     *
     * @param currentWeekDate Aktuelles Wochen-Datum (beliebiger Tag der Woche)
     * @return true wenn Navigation erlaubt
     */
    suspend fun canNavigateToPreviousWeek(currentWeekDate: LocalDate): Boolean = withContext(Dispatchers.IO) {
        // Navigation ist immer erlaubt, auch in vergangene Jahre
        // DateUtils behandelt die KW-Berechnung rekursiv korrekt
        return@withContext true
    }

    /**
     * Prüft ob zur nächsten Woche navigiert werden kann
     *
     * @param currentWeekDate Aktuelles Wochen-Datum (beliebiger Tag der Woche)
     * @return true wenn Navigation erlaubt
     */
    suspend fun canNavigateToNextWeek(currentWeekDate: LocalDate): Boolean = withContext(Dispatchers.IO) {
        val boundaries = getActiveYearBoundaries() ?: return@withContext true
        val (_, lastFriday) = boundaries

        val nextWeekDate = currentWeekDate.plusWeeks(1)
        val futureLimit = lastFriday.plusMonths(6)  // +6 Monate für Planung
        nextWeekDate <= futureLimit
    }

    // ============================================================================
    // CUSTOM JAHR/KW BERECHNEN (Delegiert an DateUtils)
    // ============================================================================

    /**
     * Berechnet die Custom-Kalenderwoche für ein Datum
     *
     * @param date Datum
     * @param yearSettings Optional: YearSettings (wird geladen wenn null)
     * @return Custom-KW (1-52/53)
     */
    suspend fun getCustomWeek(date: LocalDate, yearSettings: YearSettings? = null): Int = withContext(Dispatchers.IO) {
        val settings = yearSettings ?: yearSettingsDao.getActiveYear()
        val firstMonday = settings?.ersterMontagImJahr
        DateUtils.getCustomWeekOfYear(date, firstMonday)
    }

    /**
     * Berechnet das Custom-Jahr für ein Datum
     *
     * @param date Datum
     * @param yearSettings Optional: YearSettings (wird geladen wenn null)
     * @return Custom-Jahr
     */
    suspend fun getCustomYear(date: LocalDate, yearSettings: YearSettings? = null): Int = withContext(Dispatchers.IO) {
        val settings = yearSettings ?: yearSettingsDao.getActiveYear()
        val firstMonday = settings?.ersterMontagImJahr
        DateUtils.getCustomWeekBasedYear(date, firstMonday)
    }

    /**
     * Prüft ob ein Datum im aktiven Arbeitsjahr liegt
     *
     * @param date Datum
     * @return true wenn im aktiven Jahr, false sonst
     */
    suspend fun isDateInActiveYear(date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        val boundaries = getActiveYearBoundaries() ?: return@withContext true
        val (firstMonday, lastFriday) = boundaries

        date in firstMonday..lastFriday
    }

    // ============================================================================
    // EINTRÄGE FILTERN
    // ============================================================================

    /**
     * Holt alle Einträge eines Custom-Jahres
     *
     * Verwendet das `jahr` Feld in TimeEntry (custom year field).
     * Geeignet für: Überstunden-Berechnung, Jahres-Statistiken
     *
     * @param customYear Custom-Jahr (z.B. 2026)
     * @return Liste aller Einträge mit jahr=customYear
     */
    suspend fun getEntriesForCustomYear(customYear: Int): List<TimeEntry> = withContext(Dispatchers.IO) {
        timeEntryDao.getEntriesByYear(customYear)
    }

    /**
     * Holt alle Einträge eines Kalenderjahres
     *
     * Verwendet das Datums-Feld (datum) für Filterung.
     * Geeignet für: Excel-Export, Kalender-Ansichten
     *
     * @param calendarYear Kalenderjahr (z.B. 2026)
     * @return Liste aller Einträge von 01.01.YYYY bis 31.12.YYYY
     */
    suspend fun getEntriesForCalendarYear(calendarYear: Int): List<TimeEntry> = withContext(Dispatchers.IO) {
        val startDate = "$calendarYear-01-01"
        val endDate = "$calendarYear-12-31"
        timeEntryDao.getEntriesByDateRange(startDate, endDate)
    }

    /**
     * Holt alle Einträge zwischen ersten Montag und letzten Freitag eines Custom-Jahres
     *
     * Dies ist die "echte" Arbeitsjahr-Filterung.
     * Geeignet für: Genaue Jahres-Auswertungen, Reports
     *
     * @param customYear Custom-Jahr (z.B. 2026)
     * @return Liste aller Einträge im Arbeitsjahr-Zeitraum
     */
    suspend fun getEntriesForWorkYear(customYear: Int): List<TimeEntry> = withContext(Dispatchers.IO) {
        val boundaries = getYearBoundaries(customYear) ?: return@withContext emptyList()
        val (firstMonday, lastFriday) = boundaries

        val startDate = DateUtils.dateToString(firstMonday)
        val endDate = DateUtils.dateToString(lastFriday)

        timeEntryDao.getEntriesByDateRange(startDate, endDate)
    }

    // ============================================================================
    // JAHRES-WECHSEL PRÜFEN
    // ============================================================================

    /**
     * Prüft ob heute ein Jahreswechsel stattfindet
     *
     * Ein Jahreswechsel findet statt wenn:
     * - Heute ist der erste Montag eines neuen Jahres
     * - Es gibt ein YearSettings für das neue Jahr
     * - Aber dieses Jahr ist noch nicht aktiv
     *
     * @return Pair(neuesJahr, alteresJahr) wenn Wechsel nötig, null sonst
     */
    suspend fun checkYearSwitch(): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val activeYear = yearSettingsDao.getActiveYear() ?: return@withContext null

        // Ist heute der erste Montag eines neuen Jahres?
        val calendarYear = today.year
        val firstMondayThisYear = findFirstMonday(calendarYear)

        if (today != firstMondayThisYear) {
            // Heute ist nicht der erste Montag
            return@withContext null
        }

        // Gibt es ein YearSettings für dieses Jahr?
        val newYearSettings = yearSettingsDao.getYear(calendarYear)
        if (newYearSettings == null) {
            // Neues Jahr wurde noch nicht angelegt
            return@withContext null
        }

        // Ist das neue Jahr bereits aktiv?
        if (newYearSettings.isActive) {
            // Bereits gewechselt
            return@withContext null
        }

        // Jahreswechsel nötig!
        Pair(calendarYear, activeYear.year)
    }

    /**
     * Führt einen automatischen Jahreswechsel durch
     *
     * @param newYear Neues Jahr
     * @return true wenn erfolgreich
     */
    suspend fun performYearSwitch(newYear: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            yearSettingsDao.setActiveYear(newYear)
            android.util.Log.i("YearBoundaryService", "Automatischer Jahreswechsel zu $newYear erfolgreich")
            true
        } catch (e: Exception) {
            android.util.Log.e("YearBoundaryService", "Fehler beim Jahreswechsel zu $newYear", e)
            false
        }
    }

    // ============================================================================
    // UTILITY FUNKTIONEN
    // ============================================================================

    /**
     * Formatiert Jahres-Grenzen als String (für UI)
     *
     * @param year Custom-Jahr
     * @return String wie "06.01.2026 - 01.01.2027" oder null
     */
    suspend fun formatYearBoundaries(year: Int): String? = withContext(Dispatchers.IO) {
        val boundaries = getYearBoundaries(year) ?: return@withContext null
        val (firstMonday, lastFriday) = boundaries

        "${DateUtils.formatForDisplay(firstMonday)} - ${DateUtils.formatForDisplay(lastFriday)}"
    }

    /**
     * Gibt an wie viele Tage das Arbeitsjahr hat
     *
     * @param year Custom-Jahr
     * @return Anzahl Tage oder null
     */
    suspend fun getWorkYearDayCount(year: Int): Int? = withContext(Dispatchers.IO) {
        val boundaries = getYearBoundaries(year) ?: return@withContext null
        val (firstMonday, lastFriday) = boundaries

        java.time.temporal.ChronoUnit.DAYS.between(firstMonday, lastFriday).toInt() + 1
    }

    /**
     * Gibt an wie viele KWs das Arbeitsjahr hat
     *
     * @param year Custom-Jahr
     * @return Anzahl Wochen (normalerweise 52, manchmal 53)
     */
    suspend fun getWorkYearWeekCount(year: Int): Int? = withContext(Dispatchers.IO) {
        val boundaries = getYearBoundaries(year) ?: return@withContext null
        val (firstMonday, lastFriday) = boundaries

        val weeks = java.time.temporal.ChronoUnit.WEEKS.between(firstMonday, lastFriday).toInt()
        weeks + 1 // +1 weil die letzte Woche auch zählt
    }

    /**
     * Gibt alle Montage eines Arbeitsjahres zurück
     *
     * Nützlich für: Wochen-Übersichten, KW-Listen
     *
     * @param year Custom-Jahr
     * @return Liste aller Montage im Arbeitsjahr
     */
    suspend fun getAllMondaysInWorkYear(year: Int): List<LocalDate> = withContext(Dispatchers.IO) {
        val boundaries = getYearBoundaries(year) ?: return@withContext emptyList()
        val (firstMonday, lastFriday) = boundaries

        val mondays = mutableListOf<LocalDate>()
        var current = firstMonday

        while (current <= lastFriday) {
            mondays.add(current)
            current = current.plusWeeks(1)
        }

        mondays
    }
}
