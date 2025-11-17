package com.arbeitszeit.tracker.export

import android.content.Context
import android.os.Environment
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.template.TemplateManager
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate

class ExcelExportManager(private val context: Context) {

    private val templateManager = TemplateManager(context)
    
    /**
     * Exportiert Zeiteinträge in Excel-Format (GESAMTJAHR)
     *
     * @param userSettings Benutzereinstellungen (Name, Einrichtung, etc.)
     * @param entries Alle Zeiteinträge für den Export
     * @param year Jahr
     * @param customFileName Optionaler benutzerdefinierter Dateiname (ohne .xlsx)
     * @return Die exportierte Excel-Datei
     */
    suspend fun exportToExcel(
        userSettings: UserSettings,
        entries: List<TimeEntry>,
        year: Int,
        customFileName: String? = null
    ): File = withContext(Dispatchers.IO) {

        // 1. Lade Template (Custom für Jahr oder Standard aus Assets)
        val templateStream: InputStream = templateManager.getTemplateStream(year)
            ?: context.assets.open("ANZ_Template.xlsx")

        val workbook = WorkbookFactory.create(templateStream)

        try {
            // 2. Lese wichtige Werte aus der Vorlage BEVOR wir überschreiben
            val ueberstundenVorjahr = readUeberstundenVorjahr(workbook)
            val letzterUebertrag = readLetzterUebertrag(workbook)

            // 3. Fülle Stammangaben (überschreibt mit App-Settings)
            fillStammangaben(workbook, userSettings, ueberstundenVorjahr, letzterUebertrag)

            // 4. Fülle ALLE KW-Sheets (01-04 bis 49-52)
            fillAllSheets(workbook, entries)

            // 5. Formeln zur Neuberechnung markieren
            workbook.setForceFormulaRecalculation(true)

            // 6. Speichere Datei mit custom oder default Namen in dediziertem Ordner
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Erstelle dedizierten Unterordner "ArbeitszeitTracker"
            val arbeitszeitDir = File(downloadsDir, "ArbeitszeitTracker")
            if (!arbeitszeitDir.exists()) {
                arbeitszeitDir.mkdirs()
            }

            val fileName = if (!customFileName.isNullOrBlank()) {
                // Stelle sicher, dass .xlsx Extension vorhanden ist
                if (customFileName.endsWith(".xlsx", ignoreCase = true)) {
                    customFileName
                } else {
                    "${customFileName}.xlsx"
                }
            } else {
                "Arbeitszeit_${year}.xlsx"
            }

            val outputFile = File(arbeitszeitDir, fileName)

            FileOutputStream(outputFile).use { outputStream ->
                workbook.write(outputStream)
            }

            outputFile
        } finally {
            workbook.close()
            templateStream.close()
        }
    }
    
    /**
     * Liest Überstunden Vorjahr aus der Vorlage
     */
    private fun readUeberstundenVorjahr(workbook: Workbook): Int {
        return try {
            val sheet = (0 until workbook.numberOfSheets)
                .map { workbook.getSheetAt(it) }
                .firstOrNull { it.sheetName.equals("stammangaben", ignoreCase = true) }

            val ueberstundenDecimal = sheet?.getRow(9)?.getCell(2)?.numericCellValue ?: 0.0
            TimeUtils.excelTimeToMinutes(ueberstundenDecimal)
        } catch (e: Exception) {
            0 // Fallback wenn nicht lesbar
        }
    }

    /**
     * Liest Übertrag letztes Blatt aus der Vorlage
     */
    private fun readLetzterUebertrag(workbook: Workbook): Int {
        return try {
            val sheet = (0 until workbook.numberOfSheets)
                .map { workbook.getSheetAt(it) }
                .firstOrNull { it.sheetName.equals("stammangaben", ignoreCase = true) }

            val uebertragDecimal = sheet?.getRow(10)?.getCell(2)?.numericCellValue ?: 0.0
            TimeUtils.excelTimeToMinutes(uebertragDecimal)
        } catch (e: Exception) {
            0 // Fallback wenn nicht lesbar
        }
    }

    /**
     * Füllt das Stammangaben-Sheet
     *
     * @param workbook Die Excel-Arbeitsmappe
     * @param settings Benutzereinstellungen aus der App
     * @param ueberstundenVorjahr Überstunden aus der Vorlage (werden beibehalten!)
     * @param letzterUebertrag Übertrag aus der Vorlage (wird beibehalten!)
     */
    private fun fillStammangaben(
        workbook: Workbook,
        settings: UserSettings,
        ueberstundenVorjahr: Int,
        letzterUebertrag: Int
    ) {
        // Suche Sheet case-insensitive
        val sheet = (0 until workbook.numberOfSheets)
            .map { workbook.getSheetAt(it) }
            .firstOrNull { it.sheetName.equals("stammangaben", ignoreCase = true) }
            ?: throw IllegalStateException("Sheet 'Stammangaben' nicht gefunden")
        
        // Name in B3 (Zeile 2, Index 0-basiert)
        sheet.getRow(2)?.getCell(1)?.setCellValue(settings.name)
        
        // Einrichtung in B4
        sheet.getRow(3)?.getCell(1)?.setCellValue(settings.einrichtung)
        
        // Arbeitsumfang % in C5 (als Dezimalwert: 93% = 0.93)
        sheet.getRow(4)?.getCell(2)?.setCellValue(settings.arbeitsumfangProzent / 100.0)
        
        // Wochenstunden in C7 als Excel-Zeitwert
        // Excel: 1 Tag = 1.0, daher Minuten / 1440
        val wochenStundenDecimal = TimeUtils.minutesToExcelTime(settings.wochenStundenMinuten)
        sheet.getRow(6)?.getCell(2)?.setCellValue(wochenStundenDecimal)
        
        // Ferienbetreuung in C8
        sheet.getRow(7)?.getCell(2)?.setCellValue(if (settings.ferienbetreuung) "ja" else "nein")
        
        // Arbeitstage/Woche in C9
        sheet.getRow(8)?.getCell(2)?.setCellValue(settings.arbeitsTageProWoche.toDouble())

        // Überstunden Vorjahr in C10 als Excel-Zeitwert
        // WICHTIG: Wir verwenden die Werte aus der VORLAGE, nicht aus App-Settings!
        // Grund: Beim Jahreswechsel enthält die neue Vorlage die korrekten Überstunden
        val ueberstundenDecimal = TimeUtils.minutesToExcelTime(ueberstundenVorjahr)
        sheet.getRow(9)?.getCell(2)?.setCellValue(ueberstundenDecimal)

        // Übertrag letztes Blatt in C11
        // WICHTIG: Wir verwenden die Werte aus der VORLAGE, nicht aus App-Settings!
        val letzterUebertragDecimal = TimeUtils.minutesToExcelTime(letzterUebertrag)
        sheet.getRow(10)?.getCell(2)?.setCellValue(letzterUebertragDecimal)

        // Erster Montag im Jahr in C12 (für custom KW-Berechnung)
        if (settings.ersterMontagImJahr != null) {
            // Format: DD.MM.YYYY für Excel
            val parts = settings.ersterMontagImJahr.split("-")
            if (parts.size == 3) {
                val formatted = "${parts[2]}.${parts[1]}.${parts[0]}"
                sheet.getRow(11)?.getCell(2)?.setCellValue(formatted)
            }
        }
    }

    /**
     * Füllt alle KW-Sheets mit Zeiteinträgen
     */
    private fun fillAllSheets(workbook: Workbook, entries: List<TimeEntry>) {
        // Alle 4-Wochen-Blöcke: KW 01-04, 05-08, ..., 49-52
        val blocks = listOf(
            1 to 4,
            5 to 8,
            9 to 12,
            13 to 16,
            17 to 20,
            21 to 24,
            25 to 28,
            29 to 32,
            33 to 36,
            37 to 40,
            41 to 44,
            45 to 48,
            49 to 52
        )

        blocks.forEach { (startKW, endKW) ->
            val sheetName = "KW ${String.format("%02d", startKW)}-${String.format("%02d", endKW)}"
            val sheet = workbook.getSheet(sheetName)

            if (sheet != null) {
                // Filtere Einträge für diesen Block
                val blockEntries = entries.filter { it.kalenderwoche in startKW..endKW }
                fillTimeEntries(sheet, blockEntries, startKW, endKW)
            }
        }
    }

    /**
     * Füllt die Zeiteinträge in ein KW-Sheet
     *
     * WICHTIG: Excel-Struktur pro Woche (7 Zeilen):
     * - Zeile 0-4: Mo-Fr (Arbeitstage)
     * - Zeile 5: "Sonst" (für Samstag/Sonntagarbeit)
     * - Zeile 6: Summenzeile (mit KW-Nummer in Spalte A)
     */
    private fun fillTimeEntries(
        sheet: Sheet,
        entries: List<TimeEntry>,
        startKW: Int,
        endKW: Int
    ) {
        // Gruppiere Einträge nach Kalenderwoche
        val entriesByWeek = entries
            .filter { it.kalenderwoche in startKW..endKW }
            .groupBy { it.kalenderwoche }
            .toSortedMap()

        // Wochenstart-Zeilen in Excel (0-basiert)
        // Woche 1: Zeilen 7-13 (Mo-Fr, Sonst, Summe)
        // Woche 2: Zeilen 14-20 (Mo-Fr, Sonst, Summe)
        // Woche 3: Zeilen 21-27 (Mo-Fr, Sonst, Summe)
        // Woche 4: Zeilen 28-34 (Mo-Fr, Sonst, Summe)
        val weekStartRows = listOf(7, 14, 21, 28)

        entriesByWeek.entries.forEach weekLoop@{ (kw, weekEntries) ->
            // Berechne die Position der Woche im 4-Wochen-Block
            // Beispiel: KW 25 in Block "KW 25-28" -> Position 0 -> Zeile 7
            val weekPosition = kw - startKW
            if (weekPosition < 0 || weekPosition >= weekStartRows.size) return@weekLoop

            val startRow = weekStartRows[weekPosition]

            // WICHTIG: KW-Nummer MUSS überschrieben werden!
            // Die Excel-Vorlage enthält nur Platzhalter (1,2,3,4) oder Formeln
            // Wir müssen die echte KW-Nummer basierend auf Custom Week Calculation schreiben
            val sumRowIndex = startRow + 6
            val sumRow = sheet.getRow(sumRowIndex)
            if (sumRow != null) {
                val kwCell = sumRow.getCell(0) ?: sumRow.createCell(0)
                kwCell.setCellValue(kw.toDouble())
            }

            // Sortiere Einträge nach Datum
            weekEntries.sortedBy { it.datum }.forEach { entry ->
                // Berechne tatsächlichen Wochentag aus Datum (1=Mo, 7=So)
                val date = LocalDate.parse(entry.datum)
                val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday

                // Zuordnung zu Excel-Zeilen:
                // Mo (1) -> Zeile 0
                // Di (2) -> Zeile 1
                // Mi (3) -> Zeile 2
                // Do (4) -> Zeile 3
                // Fr (5) -> Zeile 4
                // Sa (6) -> Zeile 5 ("Sonst"-Zeile)
                // So (7) -> Zeile 5 ("Sonst"-Zeile)
                val dayOffset = when (dayOfWeek) {
                    in 1..5 -> dayOfWeek - 1  // Mo-Fr: 0-4
                    else -> 5  // Sa/So: "Sonst"-Zeile (Index 5)
                }

                val rowIndex = startRow + dayOffset
                val row = sheet.getRow(rowIndex) ?: return@forEach

                // Spalte C (Index 2): Soll-Zeit
                if (entry.sollMinuten > 0) {
                    val cell = row.getCell(2) ?: row.createCell(2)
                    cell.setCellValue(TimeUtils.minutesToExcelTime(entry.sollMinuten))
                }

                // Spalte D (Index 3): Von (Start)
                if (entry.startZeit != null) {
                    val cell = row.getCell(3) ?: row.createCell(3)
                    cell.setCellValue(TimeUtils.minutesToExcelTime(entry.startZeit))
                }

                // Spalte E (Index 4): Bis (Ende)
                if (entry.endZeit != null) {
                    val cell = row.getCell(4) ?: row.createCell(4)
                    cell.setCellValue(TimeUtils.minutesToExcelTime(entry.endZeit))
                }

                // Spalte F (Index 5): Pause
                if (entry.pauseMinuten > 0) {
                    val cell = row.getCell(5) ?: row.createCell(5)
                    cell.setCellValue(TimeUtils.minutesToExcelTime(entry.pauseMinuten))
                }

                // Spalte G (Index 6): Ist - wird von Excel-Formel berechnet!
                // NICHT überschreiben, Formel bleibt erhalten!

                // Spalte H (Index 7): Typ (U/K/F/AB)
                if (entry.typ != TimeEntry.TYP_NORMAL) {
                    val cell = row.getCell(7) ?: row.createCell(7)
                    cell.setCellValue(entry.typ)
                }

                // Spalte I (Index 8): Differenz - wird von Excel-Formel berechnet!
                // NICHT überschreiben, Formel bleibt erhalten!

                // Spalte J (Index 9): AZ aus Bereitschaft
                if (entry.arbeitszeitBereitschaft > 0) {
                    val cell = row.getCell(9) ?: row.createCell(9)
                    cell.setCellValue(TimeUtils.minutesToExcelTime(entry.arbeitszeitBereitschaft))
                }
            }
        }
    }
    
    /**
     * Prüft ob Template-Datei existiert
     */
    fun isTemplateAvailable(): Boolean {
        return try {
            context.assets.open("ANZ_Template.xlsx").use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gibt den Dateinamen für einen Export zurück
     */
    fun getExportFileName(year: Int): String {
        return "Arbeitszeit_${year}.xlsx"
    }
}
