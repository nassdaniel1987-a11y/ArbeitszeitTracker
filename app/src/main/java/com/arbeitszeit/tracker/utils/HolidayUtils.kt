package com.arbeitszeit.tracker.utils

import java.time.LocalDate
import java.time.Month

/**
 * Utility-Klasse für deutsche Feiertage
 * Unterstützt bundeslandspezifische Feiertage für 2024-2030
 */
object HolidayUtils {

    /**
     * Deutsche Bundesländer
     */
    enum class Bundesland(val displayName: String, val shortCode: String) {
        BADEN_WUERTTEMBERG("Baden-Württemberg", "BW"),
        BAYERN("Bayern", "BY"),
        BERLIN("Berlin", "BE"),
        BRANDENBURG("Brandenburg", "BB"),
        BREMEN("Bremen", "HB"),
        HAMBURG("Hamburg", "HH"),
        HESSEN("Hessen", "HE"),
        MECKLENBURG_VORPOMMERN("Mecklenburg-Vorpommern", "MV"),
        NIEDERSACHSEN("Niedersachsen", "NI"),
        NORDRHEIN_WESTFALEN("Nordrhein-Westfalen", "NW"),
        RHEINLAND_PFALZ("Rheinland-Pfalz", "RP"),
        SAARLAND("Saarland", "SL"),
        SACHSEN("Sachsen", "SN"),
        SACHSEN_ANHALT("Sachsen-Anhalt", "ST"),
        SCHLESWIG_HOLSTEIN("Schleswig-Holstein", "SH"),
        THUERINGEN("Thüringen", "TH");

        companion object {
            fun fromShortCode(code: String?): Bundesland? {
                return values().find { it.shortCode == code }
            }
        }
    }

    /**
     * Datenklasse für einen Feiertag
     */
    data class Holiday(
        val date: LocalDate,
        val name: String,
        val bundeslaender: Set<Bundesland> = Bundesland.values().toSet() // Alle BL wenn nicht spezifiziert
    )

    /**
     * Gibt alle Feiertage für ein Jahr und Bundesland zurück
     */
    fun getHolidaysForYear(year: Int, bundesland: Bundesland?): List<Holiday> {
        val holidays = mutableListOf<Holiday>()

        // Ostersonntag berechnen (Gauss-Algorithmus)
        val easterSunday = calculateEasterSunday(year)
        val easterMonday = easterSunday.plusDays(1)
        val goodFriday = easterSunday.minusDays(2)
        val ascensionDay = easterSunday.plusDays(39) // Christi Himmelfahrt
        val whitMonday = easterSunday.plusDays(50) // Pfingstmontag
        val corpusChristi = easterSunday.plusDays(60) // Fronleichnam

        // === BUNDESWEITE FEIERTAGE ===
        holidays.add(Holiday(LocalDate.of(year, Month.JANUARY, 1), "Neujahr"))
        holidays.add(Holiday(goodFriday, "Karfreitag"))
        holidays.add(Holiday(easterMonday, "Ostermontag"))
        holidays.add(Holiday(LocalDate.of(year, Month.MAY, 1), "Tag der Arbeit"))
        holidays.add(Holiday(ascensionDay, "Christi Himmelfahrt"))
        holidays.add(Holiday(whitMonday, "Pfingstmontag"))
        holidays.add(Holiday(LocalDate.of(year, Month.OCTOBER, 3), "Tag der Deutschen Einheit"))
        holidays.add(Holiday(LocalDate.of(year, Month.DECEMBER, 25), "1. Weihnachtsfeiertag"))
        holidays.add(Holiday(LocalDate.of(year, Month.DECEMBER, 26), "2. Weihnachtsfeiertag"))

        // === BUNDESLANDSPEZIFISCHE FEIERTAGE ===

        // Heilige Drei Könige (6. Januar) - BW, BY, ST
        holidays.add(Holiday(
            LocalDate.of(year, Month.JANUARY, 6),
            "Heilige Drei Könige",
            setOf(Bundesland.BADEN_WUERTTEMBERG, Bundesland.BAYERN, Bundesland.SACHSEN_ANHALT)
        ))

        // Internationaler Frauentag (8. März) - BE, MV (ab 2023)
        if (year >= 2023) {
            holidays.add(Holiday(
                LocalDate.of(year, Month.MARCH, 8),
                "Internationaler Frauentag",
                setOf(Bundesland.BERLIN, Bundesland.MECKLENBURG_VORPOMMERN)
            ))
        }

        // Fronleichnam - BW, BY, HE, NW, RP, SL, (Teile von SN, TH)
        holidays.add(Holiday(
            corpusChristi,
            "Fronleichnam",
            setOf(
                Bundesland.BADEN_WUERTTEMBERG,
                Bundesland.BAYERN,
                Bundesland.HESSEN,
                Bundesland.NORDRHEIN_WESTFALEN,
                Bundesland.RHEINLAND_PFALZ,
                Bundesland.SAARLAND
            )
        ))

        // Mariä Himmelfahrt (15. August) - BY (nur überwiegend katholische Gemeinden), SL
        holidays.add(Holiday(
            LocalDate.of(year, Month.AUGUST, 15),
            "Mariä Himmelfahrt",
            setOf(Bundesland.BAYERN, Bundesland.SAARLAND)
        ))

        // Weltkindertag (20. September) - TH (ab 2019)
        if (year >= 2019) {
            holidays.add(Holiday(
                LocalDate.of(year, Month.SEPTEMBER, 20),
                "Weltkindertag",
                setOf(Bundesland.THUERINGEN)
            ))
        }

        // Reformationstag (31. Oktober) - BB, MV, SN, ST, TH, (ab 2018 auch: HB, HH, NI, SH)
        val reformationstagStates = mutableSetOf(
            Bundesland.BRANDENBURG,
            Bundesland.MECKLENBURG_VORPOMMERN,
            Bundesland.SACHSEN,
            Bundesland.SACHSEN_ANHALT,
            Bundesland.THUERINGEN
        )
        if (year >= 2018) {
            reformationstagStates.addAll(setOf(
                Bundesland.BREMEN,
                Bundesland.HAMBURG,
                Bundesland.NIEDERSACHSEN,
                Bundesland.SCHLESWIG_HOLSTEIN
            ))
        }
        holidays.add(Holiday(
            LocalDate.of(year, Month.OCTOBER, 31),
            "Reformationstag",
            reformationstagStates
        ))

        // Allerheiligen (1. November) - BW, BY, NW, RP, SL
        holidays.add(Holiday(
            LocalDate.of(year, Month.NOVEMBER, 1),
            "Allerheiligen",
            setOf(
                Bundesland.BADEN_WUERTTEMBERG,
                Bundesland.BAYERN,
                Bundesland.NORDRHEIN_WESTFALEN,
                Bundesland.RHEINLAND_PFALZ,
                Bundesland.SAARLAND
            )
        ))

        // Buß- und Bettag (Mittwoch vor dem 23. November) - SN
        val bussUndBettag = calculateBussUndBettag(year)
        holidays.add(Holiday(
            bussUndBettag,
            "Buß- und Bettag",
            setOf(Bundesland.SACHSEN)
        ))

        // Filtern nach Bundesland (wenn angegeben)
        return if (bundesland != null) {
            holidays.filter { it.bundeslaender.contains(bundesland) }
        } else {
            holidays
        }
    }

    /**
     * Berechnet Ostersonntag für ein Jahr (Gauss-Algorithmus)
     */
    private fun calculateEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.of(year, month, day)
    }

    /**
     * Berechnet Buß- und Bettag (Mittwoch vor dem 23. November)
     */
    private fun calculateBussUndBettag(year: Int): LocalDate {
        var date = LocalDate.of(year, Month.NOVEMBER, 22)
        // Gehe zurück bis zum letzten Mittwoch vor dem 23.11.
        while (date.dayOfWeek.value != 3) { // 3 = Mittwoch
            date = date.minusDays(1)
        }
        return date
    }

    /**
     * Prüft ob ein Datum ein Feiertag ist
     */
    fun isHoliday(date: LocalDate, bundesland: Bundesland?): Boolean {
        val holidays = getHolidaysForYear(date.year, bundesland)
        return holidays.any { it.date == date }
    }

    /**
     * Gibt den Namen des Feiertags zurück (oder null)
     */
    fun getHolidayName(date: LocalDate, bundesland: Bundesland?): String? {
        val holidays = getHolidaysForYear(date.year, bundesland)
        return holidays.find { it.date == date }?.name
    }

    /**
     * Gibt alle Feiertage für einen Zeitraum zurück
     */
    fun getHolidaysInRange(startDate: LocalDate, endDate: LocalDate, bundesland: Bundesland?): List<Holiday> {
        val years = (startDate.year..endDate.year).toSet()
        val allHolidays = years.flatMap { getHolidaysForYear(it, bundesland) }
        return allHolidays.filter { it.date in startDate..endDate }
    }
}
