package com.arbeitszeit.tracker.export

import android.content.Context
import android.os.Environment
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class SimpleExcelExportManager(private val context: Context) {

    /**
     * Exportiert Zeiteinträge als einfache Excel-Tabelle in Wochenblöcken
     */
    suspend fun exportToSimpleExcel(
        userSettings: UserSettings,
        entries: List<TimeEntry>,
        startKW: Int,
        endKW: Int,
        year: Int
    ): File = withContext(Dispatchers.IO) {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Arbeitszeiten")

        // Gruppiere Einträge nach Kalenderwoche
        val entriesByWeek = entries
            .filter { it.kalenderwoche in startKW..endKW }
            .groupBy { it.kalenderwoche }
            .toSortedMap()

        var currentRow = 0

        // Styles erstellen
        val headerStyle = createHeaderStyle(workbook)
        val timeStyle = createTimeStyle(workbook)
        val dateHeaderStyle = createDateHeaderStyle(workbook)

        // Falls keine Einträge vorhanden sind, zeige Info-Meldung
        if (entriesByWeek.isEmpty()) {
            val infoRow = sheet.createRow(currentRow++)
            infoRow.createCell(0).setCellValue("Keine Zeiteinträge für KW $startKW bis $endKW vorhanden")
        }

        // Für jede Woche einen Block erstellen
        entriesByWeek.forEach { (kw, weekEntries) ->
            // Datumsbereich der Woche ermitteln
            val sortedEntries = weekEntries.sortedBy { it.datum }
            if (sortedEntries.isNotEmpty()) {
                val firstDate = try {
                    LocalDate.parse(sortedEntries.first().datum)
                } catch (e: Exception) {
                    LocalDate.now()
                }
                val lastDate = try {
                    LocalDate.parse(sortedEntries.last().datum)
                } catch (e: Exception) {
                    LocalDate.now()
                }

                // Datumsbereich als Überschrift (z.B. "KW 25: 23.06.2025 - 29.06.2025")
                val dateRangeRow = sheet.createRow(currentRow++)
                val dateCell = dateRangeRow.createCell(0)
                dateCell.setCellValue("KW $kw: ${DateUtils.dateToGermanString(firstDate)} - ${DateUtils.dateToGermanString(lastDate)}")
                dateCell.cellStyle = dateHeaderStyle

                // Spaltenüberschriften
                val headerRow = sheet.createRow(currentRow++)
                val headers = listOf("Wochentag", "Soll", "Von", "Bis", "Pause", "Ist", "Typ")
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerStyle
                }

                // Daten für jeden Tag der Woche
                sortedEntries.forEach { entry ->
                    val dataRow = sheet.createRow(currentRow++)

                    // Wochentag
                    dataRow.createCell(0).setCellValue(entry.wochentag)

                    // Soll
                    if (entry.sollMinuten > 0) {
                        val cell = dataRow.createCell(1)
                        cell.setCellValue(TimeUtils.minutesToTimeString(entry.sollMinuten))
                        cell.cellStyle = timeStyle
                    }

                    // Von
                    if (entry.startZeit != null) {
                        val cell = dataRow.createCell(2)
                        cell.setCellValue(TimeUtils.minutesToTimeString(entry.startZeit))
                        cell.cellStyle = timeStyle
                    }

                    // Bis
                    if (entry.endZeit != null) {
                        val cell = dataRow.createCell(3)
                        cell.setCellValue(TimeUtils.minutesToTimeString(entry.endZeit))
                        cell.cellStyle = timeStyle
                    }

                    // Pause
                    if (entry.pauseMinuten > 0) {
                        val cell = dataRow.createCell(4)
                        cell.setCellValue(TimeUtils.minutesToTimeString(entry.pauseMinuten))
                        cell.cellStyle = timeStyle
                    }

                    // Ist (berechnet)
                    if (entry.startZeit != null && entry.endZeit != null) {
                        val istMinuten = entry.endZeit - entry.startZeit - entry.pauseMinuten
                        if (istMinuten > 0) {
                            val cell = dataRow.createCell(5)
                            cell.setCellValue(TimeUtils.minutesToTimeString(istMinuten))
                            cell.cellStyle = timeStyle
                        }
                    }

                    // Typ (wenn nicht NORMAL)
                    if (entry.typ != TimeEntry.TYP_NORMAL) {
                        dataRow.createCell(6).setCellValue(entry.typ)
                    }
                }

                // Leerzeile zwischen Wochen
                currentRow++
            }
        }

        // Spaltenbreiten anpassen
        try {
            for (i in 0..6) {
                sheet.autoSizeColumn(i)
            }
        } catch (e: Exception) {
            // Ignoriere Fehler beim Auto-Sizing
        }

        // Datei speichern - verwende app-spezifisches Verzeichnis (keine Berechtigungen nötig)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir

        val fileName = "Arbeitszeiten_${year}_KW${String.format("%02d", startKW)}-${String.format("%02d", endKW)}_Einfach.xlsx"
        val outputFile = File(downloadsDir, fileName)

        try {
            FileOutputStream(outputFile).use { outputStream ->
                workbook.write(outputStream)
            }
        } finally {
            workbook.close()
        }

        outputFile
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        val font = workbook.createFont()
        font.bold = true
        setFont(font)
        fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        fillPattern = FillPatternType.SOLID_FOREGROUND
        borderBottom = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
    }

    private fun createDateHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 12
        setFont(font)
    }

    private fun createTimeStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.RIGHT
    }

    /**
     * Gibt den erwarteten Dateinamen zurück
     */
    fun getExportFileName(year: Int, startKW: Int, endKW: Int): String {
        return "Arbeitszeiten_${year}_KW${String.format("%02d", startKW)}-${String.format("%02d", endKW)}_Einfach.xlsx"
    }
}
