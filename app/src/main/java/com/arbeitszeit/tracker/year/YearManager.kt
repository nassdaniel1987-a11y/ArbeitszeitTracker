package com.arbeitszeit.tracker.year

import android.content.Context
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.template.TemplateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Verwaltet Jahre und Jahr-Einstellungen
 *
 * Zentrale Klasse für:
 * - Jahr-Wechsel
 * - Neues Jahr anlegen
 * - Überstunden-Übertrag berechnen
 * - Excel-Vorlagen-Zuordnung
 */
class YearManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val yearSettingsDao = database.yearSettingsDao()
    private val timeEntryDao = database.timeEntryDao()
    private val userSettingsDao = database.userSettingsDao()
    private val templateManager = TemplateManager(context)

    /**
     * Holt das aktuell ausgewählte Jahr
     */
    suspend fun getActiveYear(): YearSettings? = withContext(Dispatchers.IO) {
        yearSettingsDao.getActiveYear()
    }

    /**
     * Holt alle Jahre
     */
    suspend fun getAllYears(): List<YearSettings> = withContext(Dispatchers.IO) {
        yearSettingsDao.getAllYears()
    }

    /**
     * Wechselt zu einem anderen Jahr
     */
    suspend fun switchToYear(year: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            yearSettingsDao.setActiveYear(year)
            true
        } catch (e: Exception) {
            android.util.Log.e("YearManager", "Fehler beim Jahr-Wechsel", e)
            false
        }
    }

    /**
     * Prüft ob ein Jahr existiert
     */
    suspend fun yearExists(year: Int): Boolean = withContext(Dispatchers.IO) {
        yearSettingsDao.yearExists(year)
    }

    /**
     * Erstellt ein neues Jahr
     *
     * @param year Jahr (z.B. 2026)
     * @param ersterMontagImJahr Erster Montag (yyyy-MM-dd)
     * @param urlaubsanspruch Urlaubstage (z.B. 30)
     * @param uebertragUeberstunden Sollen Überstunden aus Vorjahr übertragen werden?
     * @param uebertragResturlaub Soll Resturlaub aus Vorjahr übertragen werden?
     * @return true wenn erfolgreich
     */
    suspend fun createNewYear(
        year: Int,
        ersterMontagImJahr: String? = null,
        urlaubsanspruch: Int? = null,
        uebertragUeberstunden: Boolean = true,
        uebertragResturlaub: Boolean = false
    ): Result<YearSettings> = withContext(Dispatchers.IO) {
        try {
            // 1. Prüfe ob Jahr bereits existiert
            if (yearExists(year)) {
                return@withContext Result.failure(Exception("Jahr $year existiert bereits"))
            }

            // 2. Finde oder berechne erster Montag
            val firstMonday = ersterMontagImJahr ?: findFirstMonday(year)

            // 3. Lade Vorjahr-Einstellungen oder Defaults
            val previousYear = yearSettingsDao.getYear(year - 1)
            val userSettings = userSettingsDao.getSettings()

            // 4. Urlaubsanspruch bestimmen
            val urlaubsTage = urlaubsanspruch
                ?: previousYear?.urlaubsanspruchTage
                ?: userSettings?.urlaubsanspruchTage
                ?: 30

            // 5. Resturlaub hinzufügen?
            val finalUrlaubsanspruch = if (uebertragResturlaub && previousYear != null) {
                val resturlaub = calculateRestUrlaub(year - 1)
                urlaubsTage + resturlaub
            } else {
                urlaubsTage
            }

            // 6. Überstunden aus Vorjahr berechnen
            val vorjahresUebertrag = if (uebertragUeberstunden) {
                calculateYearOvertime(year - 1)
            } else {
                0
            }

            // 7. Speichere Excel-Vorlage (Standard-Vorlage falls keine eigene existiert)
            if (!templateManager.hasTemplate(year)) {
                val saved = templateManager.saveDefaultTemplateForYear(year)
                android.util.Log.d("YearManager", "Standard-Vorlage für Jahr $year gespeichert: $saved")
            }

            // 8. Erstelle YearSettings
            val newYearSettings = YearSettings(
                year = year,
                ersterMontagImJahr = firstMonday,
                urlaubsanspruchTage = finalUrlaubsanspruch,
                vorjahresUebertragMinuten = vorjahresUebertrag,
                hasExcelTemplate = true,  // Template existiert jetzt garantiert
                isActive = false,  // Nicht sofort aktivieren
                createdAt = System.currentTimeMillis()
            )

            // 9. Speichern
            yearSettingsDao.insert(newYearSettings)

            android.util.Log.d("YearManager", "Neues Jahr $year erfolgreich angelegt: $newYearSettings")

            // 10. Neuberechnung aller Einträge dieses Kalenderjahres
            recalculateTimeEntries(year)

            Result.success(newYearSettings)

        } catch (e: Exception) {
            android.util.Log.e("YearManager", "Fehler beim Erstellen von Jahr $year", e)
            Result.failure(e)
        }
    }

    /**
     * Berechnet Überstunden eines Jahres
     *
     * @param year Jahr
     * @return Überstunden in Minuten (positiv = Plus, negativ = Minus)
     */
    suspend fun calculateYearOvertime(year: Int): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val entries = timeEntryDao.getEntriesByYear(year)
        // Nur abgeschlossene Einträge bis heute (keine zukünftigen Tage)
        entries.filter { entry ->
            val date = LocalDate.parse(entry.datum)
            !date.isAfter(today) && (date.isBefore(today) || entry.isComplete())
        }.sumOf { it.getDifferenzMinuten() }
    }

    /**
     * Berechnet Resturlaub eines Jahres
     *
     * @param year Jahr
     * @return Verbleibende Urlaubstage
     */
    suspend fun calculateRestUrlaub(year: Int): Int = withContext(Dispatchers.IO) {
        val yearSettings = yearSettingsDao.getYear(year) ?: return@withContext 0
        val entries = timeEntryDao.getEntriesByYear(year)

        val urlaubsTage = entries.count { it.typ == "U" }  // TYP_URLAUB
        val anspruch = yearSettings.urlaubsanspruchTage

        kotlin.math.max(0, anspruch - urlaubsTage)  // Mindestens 0
    }

    /**
     * Findet den ersten Montag eines Jahres
     */
    private fun findFirstMonday(year: Int): String {
        var date = LocalDate.of(year, 1, 1)
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            date = date.plusDays(1)
        }
        return date.toString()  // yyyy-MM-dd
    }

    /**
     * Aktualisiert das Excel-Template-Flag für ein Jahr
     */
    suspend fun updateExcelTemplateFlag(year: Int) = withContext(Dispatchers.IO) {
        val hasTemplate = templateManager.hasTemplate(year)
        yearSettingsDao.updateExcelTemplateFlag(year, hasTemplate)
    }

    /**
     * Aktualisiert Vorjahresübertrag manuell (falls nötig)
     */
    suspend fun updateVorjahresUebertrag(year: Int, minutes: Int) = withContext(Dispatchers.IO) {
        yearSettingsDao.updateVorjahresUebertrag(year, minutes)
    }

    /**
     * Löscht ein Jahr (nur wenn nicht aktiv)
     */
    suspend fun deleteYear(year: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val yearSettings = yearSettingsDao.getYear(year)
            if (yearSettings?.isActive == true) {
                // Aktives Jahr kann nicht gelöscht werden
                return@withContext false
            }

            yearSettings?.let {
                yearSettingsDao.delete(it)
                true
            } ?: false

        } catch (e: Exception) {
            android.util.Log.e("YearManager", "Fehler beim Löschen von Jahr $year", e)
            false
        }
    }

    /**
     * Archiviert ein Jahr
     */
    suspend fun archiveYear(year: Int) = withContext(Dispatchers.IO) {
        yearSettingsDao.archiveYear(year)
    }

    /**
     * Berechnet alle Zeiteinträge eines Kalenderjahres neu
     *
     * WICHTIG: Wird automatisch aufgerufen wenn:
     * - Ein neues Jahr angelegt wird
     * - Der "Erster Montag" eines Jahres geändert wird
     *
     * Diese Funktion:
     * - Lädt alle Einträge des Kalenderjahres (z.B. 2026-01-01 bis 2026-12-31)
     * - Berechnet für jeden Eintrag die KW und das custom year NEU
     * - Updated die Einträge in der Datenbank
     *
     * @param calendarYear Das Kalenderjahr (z.B. 2026)
     */
    suspend fun recalculateTimeEntries(calendarYear: Int) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("YearManager", "Neuberechnung aller Einträge für Kalenderjahr $calendarYear")

            // 1. Lade YearSettings für dieses Jahr
            val yearSettings = yearSettingsDao.getYear(calendarYear)
            if (yearSettings == null) {
                android.util.Log.w("YearManager", "Kein YearSettings für $calendarYear gefunden - breche ab")
                return@withContext
            }

            val ersterMontagImJahr = yearSettings.ersterMontagImJahr

            // 2. Lade ALLE Einträge des Kalenderjahres
            val startDate = "$calendarYear-01-01"
            val endDate = "$calendarYear-12-31"
            val entries = timeEntryDao.getEntriesByDateRange(startDate, endDate)

            android.util.Log.d("YearManager", "Gefunden: ${entries.size} Einträge für Kalenderjahr $calendarYear")

            // 3. Berechne für jeden Eintrag die KW und Jahr neu
            entries.forEach { entry ->
                val date = LocalDate.parse(entry.datum)

                // Berechne custom KW
                val newKW = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekOfYear(
                    date,
                    ersterMontagImJahr
                )

                // Berechne custom year
                val newYear = com.arbeitszeit.tracker.utils.DateUtils.getCustomWeekBasedYear(
                    date,
                    ersterMontagImJahr
                )

                // Update nur wenn sich was geändert hat
                if (entry.kalenderwoche != newKW || entry.jahr != newYear) {
                    android.util.Log.d("YearManager",
                        "Update ${entry.datum}: KW ${entry.kalenderwoche}→$newKW, Jahr ${entry.jahr}→$newYear"
                    )

                    timeEntryDao.update(entry.copy(
                        kalenderwoche = newKW,
                        jahr = newYear,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }

            android.util.Log.d("YearManager", "Neuberechnung für Kalenderjahr $calendarYear abgeschlossen")

        } catch (e: Exception) {
            android.util.Log.e("YearManager", "Fehler bei Neuberechnung für Jahr $calendarYear", e)
        }
    }

    /**
     * Erstellt Vorschlag für neues Jahr basierend auf Vorjahr
     *
     * @param year Jahr das erstellt werden soll
     * @return Vorschlag mit allen Werten
     */
    suspend fun suggestNewYear(year: Int): NewYearSuggestion = withContext(Dispatchers.IO) {
        val previousYear = yearSettingsDao.getYear(year - 1)
        val userSettings = userSettingsDao.getSettings()

        val firstMonday = findFirstMonday(year)
        val urlaubsanspruch = previousYear?.urlaubsanspruchTage
            ?: userSettings?.urlaubsanspruchTage
            ?: 30
        val resturlaub = calculateRestUrlaub(year - 1)
        val ueberstunden = calculateYearOvertime(year - 1)

        NewYearSuggestion(
            year = year,
            ersterMontagImJahr = firstMonday,
            urlaubsanspruch = urlaubsanspruch,
            restUrlaubVorjahr = resturlaub,
            ueberstundenVorjahr = ueberstunden
        )
    }
}

/**
 * Vorschlag für ein neues Jahr
 */
data class NewYearSuggestion(
    val year: Int,
    val ersterMontagImJahr: String,
    val urlaubsanspruch: Int,
    val restUrlaubVorjahr: Int,
    val ueberstundenVorjahr: Int
) {
    /**
     * Formatiert Überstunden als String
     */
    fun getUeberstundenFormatted(): String {
        val isNegative = ueberstundenVorjahr < 0
        val absMinutes = kotlin.math.abs(ueberstundenVorjahr)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val sign = if (isNegative) "-" else "+"
        return String.format("%s%d:%02d", sign, hours, mins)
    }
}
