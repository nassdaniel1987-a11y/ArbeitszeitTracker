package com.arbeitszeit.tracker.export

import android.content.Context
import android.os.Environment
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class ExcelExportManager(private val context: Context) {
    
    /**
     * Exportiert Zeiteinträge in Excel-Format
     * 
     * @param userSettings Benutzereinstellungen (Name, Einrichtung, etc.)
     * @param entries Alle Zeiteinträge für den Export
     * @param startKW Start-Kalenderwoche
     * @param endKW End-Kalenderwoche
     * @param year Jahr
     * @return Die exportierte Excel-Datei
     */
    suspend fun exportToExcel(
        userSettings: UserSettings,
        entries: List<TimeEntry>,
        startKW: Int,
        endKW: Int,
        year: Int
    ): File = withContext(Dispatchers.IO) {
        
        // 1. Lade Template aus Assets
        val templateStream = context.assets.open("ANZ_Template.xlsx")
        val workbook = WorkbookFactory.create(templateStream)
        
        try {
            // 2. Fülle Stammangaben
            fillStammangaben(workbook, userSettings)
            
            // 3. Bestimme welches KW-Sheet (z.B. KW 21-24)
            val sheetName = DateUtils.getSheetNameForWeek(startKW)
            val sheet = workbook.getSheet(sheetName)
                ?: throw IllegalStateException("Sheet '$sheetName' nicht gefunden")
            
            // 4. Fülle Zeiteinträge
            fillTimeEntries(sheet, entries, startKW, endKW)

            // 4.5. Formeln zur Neuberechnung markieren
            workbook.setForceFormulaRecalculation(true)

            // 5. Speichere Datei
            val outputFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Arbeitszeit_${year}_KW${String.format("%02d", startKW)}-${String.format("%02d", endKW)}.xlsx"
            )
            
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
     * Füllt das Stammangaben-Sheet
     */
    private fun fillStammangaben(workbook: Workbook, settings: UserSettings) {
        val sheet = workbook.getSheet("Stammangaben")
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
        val ueberstundenDecimal = TimeUtils.minutesToExcelTime(settings.ueberstundenVorjahrMinuten)
        sheet.getRow(9)?.getCell(2)?.setCellValue(ueberstundenDecimal)
        
        // Übertrag letztes Blatt würde hier in C11 stehen, aber das wird von der App
        // automatisch aus dem vorherigen Export übernommen
        val letzterUebertragDecimal = TimeUtils.minutesToExcelTime(settings.letzterUebertragMinuten)
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
     * Füllt die Zeiteinträge in ein KW-Sheet
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
        // Woche 1: Zeile 8-14 (Index 7-13)
        // Woche 2: Zeile 15-21 (Index 14-20)
        // Woche 3: Zeile 22-28 (Index 21-27)
        // Woche 4: Zeile 29-35 (Index 28-34)
        val weekStartRows = listOf(7, 14, 21, 28)
        
        entriesByWeek.entries.forEach weekLoop@{ (kw, weekEntries) ->
            // Berechne die Position der Woche im 4-Wochen-Block
            // KW 25 in Block "KW 25-28" -> Position 0 -> Zeile 7
            // KW 26 in Block "KW 25-28" -> Position 1 -> Zeile 14
            // KW 27 in Block "KW 25-28" -> Position 2 -> Zeile 21
            // KW 28 in Block "KW 25-28" -> Position 3 -> Zeile 28
            val weekPosition = kw - startKW
            if (weekPosition < 0 || weekPosition >= weekStartRows.size) return@weekLoop

            val startRow = weekStartRows[weekPosition]

            // KW-Nummer in Spalte A der Summenzeile (Zeile startRow + 6)
            val sumRowIndex = startRow + 6
            sheet.getRow(sumRowIndex)?.getCell(0)?.setCellValue(kw.toDouble())

            // Sortiere Einträge nach Datum
            weekEntries.sortedBy { it.datum }.forEach { entry ->
                // Berechne tatsächlichen Wochentag aus Datum (1=Mo, 7=So)
                val date = LocalDate.parse(entry.datum)
                val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday
                val dayOffset = when (dayOfWeek) {
                    7 -> 6  // Sonntag -> Index 6 (7. Zeile)
                    else -> dayOfWeek - 1  // Mo=0, Di=1, Mi=2, Do=3, Fr=4, Sa=5
                }

                val rowIndex = startRow + dayOffset
                val row = sheet.getRow(rowIndex)

                if (row != null) {
                    // Spalte C (Index 2): Soll-Zeit
                    if (entry.sollMinuten > 0) {
                        row.getCell(2)?.setCellValue(TimeUtils.minutesToExcelTime(entry.sollMinuten))
                    }

                    // Spalte D (Index 3): Von (Start)
                    if (entry.startZeit != null) {
                        row.getCell(3)?.setCellValue(TimeUtils.minutesToExcelTime(entry.startZeit))
                    }

                    // Spalte E (Index 4): Bis (Ende)
                    if (entry.endZeit != null) {
                        row.getCell(4)?.setCellValue(TimeUtils.minutesToExcelTime(entry.endZeit))
                    }

                    // Spalte F (Index 5): Pause
                    if (entry.pauseMinuten > 0) {
                        row.getCell(5)?.setCellValue(TimeUtils.minutesToExcelTime(entry.pauseMinuten))
                    }

                    // Spalte G (Index 6): Ist - wird von Excel-Formel berechnet!
                    // NICHT überschreiben!

                    // Spalte H (Index 7): Typ (U/K/F/AB)
                    if (entry.typ != TimeEntry.TYP_NORMAL) {
                        row.getCell(7)?.setCellValue(entry.typ)
                    }
                }
                
                // Spalte I (Index 8): Differenz - wird von Excel-Formel berechnet!
                // NICHT überschreiben!
                
                // Spalte J (Index 9): AZ aus Bereitschaft
                if (entry.arbeitszeitBereitschaft > 0) {
                    row.getCell(9)?.setCellValue(TimeUtils.minutesToExcelTime(entry.arbeitszeitBereitschaft))
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
    fun getExportFileName(year: Int, startKW: Int, endKW: Int): String {
        return "Arbeitszeit_${year}_KW${String.format("%02d", startKW)}-${String.format("%02d", endKW)}.xlsx"
    }
}
