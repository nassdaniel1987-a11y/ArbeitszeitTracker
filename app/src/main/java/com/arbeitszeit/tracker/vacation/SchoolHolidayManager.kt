package com.arbeitszeit.tracker.vacation

import android.content.Context
import android.util.Log
import com.arbeitszeit.tracker.data.dao.SchoolHolidayDao
import com.arbeitszeit.tracker.data.entity.SchoolHoliday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager für Schulferien in Deutschland
 *
 * Verwaltet Schulferien für alle Bundesländer und stellt diese
 * für die KI-gestützte Urlaubsplanung bereit.
 *
 * Datenquelle: Kultusministerkonferenz (KMK)
 */
class SchoolHolidayManager(
    private val schoolHolidayDao: SchoolHolidayDao
) {

    companion object {
        private const val TAG = "SchoolHolidayManager"
        const val SOURCE_MANUAL = "manuell"
        const val SOURCE_KMK = "KMK"
    }

    /**
     * Initialisiert Schulferien für ein Bundesland und Jahr
     * Falls noch nicht vorhanden, werden diese in die Datenbank geschrieben
     */
    suspend fun initializeHolidays(bundesland: String, year: Int) = withContext(Dispatchers.IO) {
        try {
            // Prüfe ob bereits vorhanden
            val count = schoolHolidayDao.getCountByBundeslandAndYear(bundesland, year)
            if (count > 0) {
                Log.d(TAG, "Schulferien für $bundesland $year bereits vorhanden")
                return@withContext
            }

            // Hole Schulferien-Daten
            val holidays = getHolidaysForBundeslandAndYear(bundesland, year)

            if (holidays.isEmpty()) {
                Log.w(TAG, "Keine Schulferien für $bundesland $year gefunden")
                return@withContext
            }

            // Speichere in Datenbank
            schoolHolidayDao.insertAll(holidays)
            Log.d(TAG, "Schulferien für $bundesland $year erfolgreich gespeichert (${holidays.size} Einträge)")

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Initialisieren der Schulferien", e)
        }
    }

    /**
     * Gibt Schulferien für ein Bundesland und Jahr zurück
     */
    private fun getHolidaysForBundeslandAndYear(bundesland: String, year: Int): List<SchoolHoliday> {
        return when (year) {
            2025 -> getHolidays2025(bundesland)
            2026 -> getHolidays2026(bundesland)
            else -> emptyList()
        }
    }

    /**
     * Schulferien 2025 für alle Bundesländer
     */
    private fun getHolidays2025(bundesland: String): List<SchoolHoliday> {
        val holidays = mutableListOf<SchoolHoliday>()

        when (bundesland) {
            "BW" -> {  // Baden-Württemberg
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Winterferien", startDate = "2025-02-28", endDate = "2025-03-02", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Osterferien", startDate = "2025-04-14", endDate = "2025-04-25", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Pfingstferien", startDate = "2025-06-10", endDate = "2025-06-21", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Sommerferien", startDate = "2025-07-31", endDate = "2025-09-13", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Herbstferien", startDate = "2025-10-27", endDate = "2025-10-31", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2025, name = "Weihnachtsferien", startDate = "2025-12-22", endDate = "2026-01-05", source = SOURCE_KMK)
                ))
            }
            "BY" -> {  // Bayern
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Winterferien", startDate = "2025-03-03", endDate = "2025-03-07", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Osterferien", startDate = "2025-04-14", endDate = "2025-04-26", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Pfingstferien", startDate = "2025-06-10", endDate = "2025-06-20", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Sommerferien", startDate = "2025-08-01", endDate = "2025-09-15", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Herbstferien", startDate = "2025-11-03", endDate = "2025-11-07", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2025, name = "Weihnachtsferien", startDate = "2025-12-22", endDate = "2026-01-05", source = SOURCE_KMK)
                ))
            }
            "BE" -> {  // Berlin
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "BE", year = 2025, name = "Winterferien", startDate = "2025-02-03", endDate = "2025-02-08", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BE", year = 2025, name = "Osterferien", startDate = "2025-04-14", endDate = "2025-04-25", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BE", year = 2025, name = "Sommerferien", startDate = "2025-07-24", endDate = "2025-09-06", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BE", year = 2025, name = "Herbstferien", startDate = "2025-10-20", endDate = "2025-11-01", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BE", year = 2025, name = "Weihnachtsferien", startDate = "2025-12-22", endDate = "2026-01-02", source = SOURCE_KMK)
                ))
            }
            "NW" -> {  // Nordrhein-Westfalen
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "NW", year = 2025, name = "Osterferien", startDate = "2025-04-14", endDate = "2025-04-26", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "NW", year = 2025, name = "Pfingstferien", startDate = "2025-06-10", endDate = "2025-06-10", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "NW", year = 2025, name = "Sommerferien", startDate = "2025-07-14", endDate = "2025-08-26", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "NW", year = 2025, name = "Herbstferien", startDate = "2025-10-13", endDate = "2025-10-25", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "NW", year = 2025, name = "Weihnachtsferien", startDate = "2025-12-22", endDate = "2026-01-06", source = SOURCE_KMK)
                ))
            }
            // Weitere Bundesländer können hier ergänzt werden
            else -> {
                Log.w(TAG, "Bundesland $bundesland nicht unterstützt für 2025")
            }
        }

        return holidays
    }

    /**
     * Schulferien 2026 für alle Bundesländer
     */
    private fun getHolidays2026(bundesland: String): List<SchoolHoliday> {
        val holidays = mutableListOf<SchoolHoliday>()

        when (bundesland) {
            "BW" -> {  // Baden-Württemberg
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "BW", year = 2026, name = "Osterferien", startDate = "2026-03-30", endDate = "2026-04-10", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2026, name = "Pfingstferien", startDate = "2026-05-26", endDate = "2026-06-06", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2026, name = "Sommerferien", startDate = "2026-07-30", endDate = "2026-09-12", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2026, name = "Herbstferien", startDate = "2026-10-26", endDate = "2026-10-30", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BW", year = 2026, name = "Weihnachtsferien", startDate = "2026-12-23", endDate = "2027-01-09", source = SOURCE_KMK)
                ))
            }
            "BY" -> {  // Bayern
                holidays.addAll(listOf(
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Winterferien", startDate = "2026-02-16", endDate = "2026-02-20", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Osterferien", startDate = "2026-03-30", endDate = "2026-04-10", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Pfingstferien", startDate = "2026-05-26", endDate = "2026-06-05", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Sommerferien", startDate = "2026-08-03", endDate = "2026-09-14", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Herbstferien", startDate = "2026-11-02", endDate = "2026-11-06", source = SOURCE_KMK),
                    SchoolHoliday(bundesland = "BY", year = 2026, name = "Weihnachtsferien", startDate = "2026-12-23", endDate = "2027-01-05", source = SOURCE_KMK)
                ))
            }
            // Weitere Bundesländer können hier ergänzt werden
            else -> {
                Log.w(TAG, "Bundesland $bundesland nicht unterstützt für 2026")
            }
        }

        return holidays
    }

    /**
     * Hilfsmethode um alle verfügbaren Bundesländer zu bekommen
     */
    fun getSupportedBundeslaender(): List<Pair<String, String>> {
        return listOf(
            "BW" to "Baden-Württemberg",
            "BY" to "Bayern",
            "BE" to "Berlin",
            "BB" to "Brandenburg",
            "HB" to "Bremen",
            "HH" to "Hamburg",
            "HE" to "Hessen",
            "MV" to "Mecklenburg-Vorpommern",
            "NI" to "Niedersachsen",
            "NW" to "Nordrhein-Westfalen",
            "RP" to "Rheinland-Pfalz",
            "SL" to "Saarland",
            "SN" to "Sachsen",
            "ST" to "Sachsen-Anhalt",
            "SH" to "Schleswig-Holstein",
            "TH" to "Thüringen"
        )
    }

    /**
     * Gibt den vollen Namen eines Bundeslands zurück
     */
    fun getBundeslandName(code: String): String {
        return getSupportedBundeslaender().find { it.first == code }?.second ?: code
    }
}
