package com.arbeitszeit.tracker.import

import android.content.Context
import android.net.Uri
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Importiert Zeiteinträge und Stammdaten aus Excel-Dateien
 */
class ExcelImportManager(private val context: Context) {

    /**
     * Importiert eine Excel-Datei (ANZ_Template Format)
     *
     * @param uri URI der zu importierenden Excel-Datei
     * @param importStammdaten Sollen Stammdaten auch importiert werden?
     * @return ImportResult mit importierten Daten
     */
    suspend fun importFromExcel(
        uri: Uri,
        importStammdaten: Boolean = true
    ): ImportResult = withContext(Dispatchers.IO) {

        val entries = mutableListOf<TimeEntry>()
        var userSettings: UserSettings? = null

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)

                // Debug: Liste alle Sheets auf
                android.util.Log.d("ExcelImport", "Excel hat ${workbook.numberOfSheets} Sheets:")
                for (i in 0 until workbook.numberOfSheets) {
                    android.util.Log.d("ExcelImport", "  Sheet $i: ${workbook.getSheetAt(i).sheetName}")
                }

                // 1. Lese Stammdaten wenn gewünscht (ZUERST, um ersterMontagImJahr zu haben)
                if (importStammdaten) {
                    android.util.Log.d("ExcelImport", "Versuche Stammangaben-Sheet zu lesen...")

                    // Suche Sheet case-insensitive (kann "Stammangaben" oder "stammangaben" sein)
                    val stammdatenSheet = (0 until workbook.numberOfSheets)
                        .map { workbook.getSheetAt(it) }
                        .firstOrNull { it.sheetName.equals("stammangaben", ignoreCase = true) }

                    userSettings = readStammdaten(stammdatenSheet)
                    if (userSettings == null) {
                        android.util.Log.w("ExcelImport", "Stammangaben konnten nicht gelesen werden!")
                    }
                } else {
                    android.util.Log.d("ExcelImport", "importStammdaten=false, überspringe Stammangaben")
                }

                // 2. Lese alle KW-Sheets mit der custom week calculation
                // Sheets: "KW 01-04", "KW 05-08", "KW 09-12", etc.
                val ersterMontag = userSettings?.ersterMontagImJahr
                for (i in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(i)
                    val sheetName = sheet.sheetName

                    // Nur KW-Sheets verarbeiten
                    if (sheetName.startsWith("KW ")) {
                        entries.addAll(readTimeEntriesFromSheet(sheet, ersterMontag))
                    }
                }

                workbook.close()
            }

            android.util.Log.d("ExcelImport", "Import abgeschlossen: ${entries.size} Einträge, userSettings=${if (userSettings != null) "vorhanden" else "null"}")

            ImportResult.Success(
                entries = entries,
                userSettings = userSettings,
                entriesCount = entries.size
            )

        } catch (e: Exception) {
            android.util.Log.e("ExcelImport", "Import-Fehler: ${e.message}", e)
            ImportResult.Error("Import fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Liest Stammdaten aus dem Stammangaben-Sheet
     */
    private fun readStammdaten(sheet: Sheet?): UserSettings? {
        if (sheet == null) {
            android.util.Log.w("ExcelImport", "Stammangaben sheet nicht gefunden!")
            return null
        }

        try {
            // B3: Name
            val name = sheet.getRow(2)?.getCell(1)?.stringCellValue ?: ""

            // B4: Einrichtung
            val einrichtung = sheet.getRow(3)?.getCell(1)?.stringCellValue ?: ""

            // C5: Arbeitsumfang % (als Dezimal: 0.93 = 93%)
            val arbeitsumfangDecimal = sheet.getRow(4)?.getCell(2)?.numericCellValue ?: 1.0
            val arbeitsumfangProzent = (arbeitsumfangDecimal * 100).toInt()

            // C7: Wochenstunden (Excel-Zeitwert)
            val wochenStundenDecimal = sheet.getRow(6)?.getCell(2)?.numericCellValue ?: 0.0
            val wochenStundenMinuten = TimeUtils.excelTimeToMinutes(wochenStundenDecimal)

            // C8: Ferienbetreuung
            val ferienbetreuungStr = sheet.getRow(7)?.getCell(2)?.stringCellValue ?: "nein"
            val ferienbetreuung = ferienbetreuungStr.lowercase() == "ja"

            // C9: Arbeitstage/Woche
            val arbeitsTageProWoche = sheet.getRow(8)?.getCell(2)?.numericCellValue?.toInt() ?: 5

            // C10: Überstunden Vorjahr (Excel-Zeitwert)
            val ueberstundenDecimal = sheet.getRow(9)?.getCell(2)?.numericCellValue ?: 0.0
            val ueberstundenVorjahrMinuten = TimeUtils.excelTimeToMinutes(ueberstundenDecimal)

            // C11: Übertrag letztes Blatt (Excel-Zeitwert)
            val letzterUebertragDecimal = sheet.getRow(10)?.getCell(2)?.numericCellValue ?: 0.0
            val letzterUebertragMinuten = TimeUtils.excelTimeToMinutes(letzterUebertragDecimal)

            // C12: Erster Montag im Jahr (DD.MM.YYYY -> yyyy-MM-dd)
            var ersterMontagImJahr: String? = null
            val ersterMontagStr = sheet.getRow(11)?.getCell(2)?.stringCellValue
            if (ersterMontagStr != null && ersterMontagStr.isNotBlank()) {
                val parts = ersterMontagStr.split(".")
                if (parts.size == 3) {
                    val tag = parts[0].padStart(2, '0')
                    val monat = parts[1].padStart(2, '0')
                    val jahr = parts[2]
                    ersterMontagImJahr = "$jahr-$monat-$tag"
                }
            }

            val settings = UserSettings(
                id = 1,
                name = name,
                einrichtung = einrichtung,
                arbeitsumfangProzent = arbeitsumfangProzent,
                wochenStundenMinuten = wochenStundenMinuten,
                arbeitsTageProWoche = arbeitsTageProWoche,
                ferienbetreuung = ferienbetreuung,
                ueberstundenVorjahrMinuten = ueberstundenVorjahrMinuten,
                letzterUebertragMinuten = letzterUebertragMinuten,
                ersterMontagImJahr = ersterMontagImJahr,
                updatedAt = System.currentTimeMillis()
            )

            android.util.Log.d("ExcelImport", "Stammdaten gelesen: Name=$name, Einrichtung=$einrichtung, " +
                    "Arbeitsumfang=$arbeitsumfangProzent%, Wochenstunden=${wochenStundenMinuten}min, " +
                    "Arbeitstage=$arbeitsTageProWoche, ErsterMontag=$ersterMontagImJahr")

            return settings

        } catch (e: Exception) {
            android.util.Log.e("ExcelImport", "Fehler beim Lesen der Stammdaten: ${e.message}", e)
            return null
        }
    }

    /**
     * Liest Zeiteinträge aus einem KW-Sheet
     */
    private fun readTimeEntriesFromSheet(sheet: Sheet, ersterMondagImJahr: String?): List<TimeEntry> {
        val entries = mutableListOf<TimeEntry>()

        // Wochenstart-Zeilen in Excel (0-basiert)
        // Woche 1: Zeile 8-14 (Index 7-13)
        // Woche 2: Zeile 15-21 (Index 14-20)
        // Woche 3: Zeile 22-28 (Index 21-27)
        // Woche 4: Zeile 29-35 (Index 28-34)
        val weekStartRows = listOf(7, 14, 21, 28)

        for ((weekIndex, startRow) in weekStartRows.withIndex()) {
            // Lese KW-Nummer aus Summenzeile (startRow + 6, Spalte A)
            val sumRowIndex = startRow + 6
            val kwRow = sheet.getRow(sumRowIndex)
            val kw = kwRow?.getCell(0)?.numericCellValue?.toInt() ?: continue

            // Lese 6 Tage (Mo-Sa oder Mo-So)
            for (dayIndex in 0 until 6) {
                val rowIndex = startRow + dayIndex
                val row = sheet.getRow(rowIndex) ?: continue

                // Berechne Datum aus KW und Wochentag
                val entry = readTimeEntryFromRow(row, kw, dayIndex, ersterMondagImJahr)
                if (entry != null) {
                    entries.add(entry)
                }
            }
        }

        return entries
    }

    /**
     * Liest einen einzelnen Zeiteintrag aus einer Excel-Zeile
     */
    private fun readTimeEntryFromRow(row: Row, kw: Int, dayIndex: Int, ersterMondagImJahr: String?): TimeEntry? {
        try {
            // Berechne Datum aus KW und Tag
            val year = java.time.Year.now().value // TODO: Jahr aus Dateiname oder Sheet extrahieren
            val date = getDateFromWeekAndDay(year, kw, dayIndex, ersterMondagImJahr)
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val weekday = getWeekdayShort(date.dayOfWeek.value)

            // Spalte C (Index 2): Soll-Zeit
            val sollDecimal = row.getCell(2)?.numericCellValue ?: 0.0
            val sollMinuten = TimeUtils.excelTimeToMinutes(sollDecimal)

            // Spalte D (Index 3): Von (Start)
            val startDecimal = row.getCell(3)?.numericCellValue
            val startZeit = if (startDecimal != null && startDecimal > 0) {
                TimeUtils.excelTimeToMinutes(startDecimal)
            } else null

            // Spalte E (Index 4): Bis (Ende)
            val endDecimal = row.getCell(4)?.numericCellValue
            val endZeit = if (endDecimal != null && endDecimal > 0) {
                TimeUtils.excelTimeToMinutes(endDecimal)
            } else null

            // Spalte F (Index 5): Pause
            val pauseDecimal = row.getCell(5)?.numericCellValue ?: 0.0
            val pauseMinuten = TimeUtils.excelTimeToMinutes(pauseDecimal)

            // Spalte H (Index 7): Typ (U/K/F/AB)
            val typCell = row.getCell(7)
            val typ = if (typCell?.cellType == CellType.STRING) {
                typCell.stringCellValue.trim()
            } else {
                TimeEntry.TYP_NORMAL
            }

            // Spalte J (Index 9): AZ aus Bereitschaft
            val bereitschaftDecimal = row.getCell(9)?.numericCellValue ?: 0.0
            val arbeitszeitBereitschaft = TimeUtils.excelTimeToMinutes(bereitschaftDecimal)

            // Nur Eintrag erstellen wenn Daten vorhanden sind
            if (sollMinuten > 0 || startZeit != null || endZeit != null) {
                return TimeEntry(
                    datum = dateString,
                    wochentag = weekday,
                    kalenderwoche = kw,
                    jahr = year,
                    startZeit = startZeit,
                    endZeit = endZeit,
                    pauseMinuten = pauseMinuten,
                    sollMinuten = sollMinuten,
                    typ = typ,
                    arbeitszeitBereitschaft = arbeitszeitBereitschaft,
                    isManualEntry = true, // Import ist immer manuell
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

        } catch (e: Exception) {
            // Zeile überspringen bei Fehler
        }

        return null
    }

    /**
     * Berechnet Datum aus Kalenderwoche und Wochentag
     * Verwendet custom week calculation wenn ersterMondagImJahr gesetzt ist
     */
    private fun getDateFromWeekAndDay(year: Int, kw: Int, dayIndex: Int, ersterMondagImJahr: String?): LocalDate {
        if (ersterMondagImJahr != null) {
            // Custom week calculation: KW1 beginnt am ersten Montag
            val firstMonday = LocalDate.parse(ersterMondagImJahr)
            // Berechne Datum: erster Montag + (kw - 1) Wochen + dayIndex Tage
            return firstMonday.plusWeeks((kw - 1).toLong()).plusDays(dayIndex.toLong())
        } else {
            // ISO 8601: Woche beginnt am Montag
            val firstDayOfYear = LocalDate.of(year, 1, 1)
            val firstMonday = firstDayOfYear.with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1)

            // Wenn 1. Januar nach Donnerstag ist, gehört er zur letzten KW des Vorjahres
            val adjustedFirstMonday = if (firstDayOfYear.dayOfWeek.value > 4) {
                firstMonday.plusWeeks(1)
            } else {
                firstMonday
            }

            return adjustedFirstMonday.plusWeeks((kw - 1).toLong()).plusDays(dayIndex.toLong())
        }
    }

    /**
     * Gibt Wochentag-Kürzel zurück
     */
    private fun getWeekdayShort(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Mo"
            2 -> "Di"
            3 -> "Mi"
            4 -> "Do"
            5 -> "Fr"
            6 -> "Sa"
            7 -> "So"
            else -> "Mo"
        }
    }
}

/**
 * Ergebnis eines Excel-Imports
 */
sealed class ImportResult {
    data class Success(
        val entries: List<TimeEntry>,
        val userSettings: UserSettings?,
        val entriesCount: Int
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}
