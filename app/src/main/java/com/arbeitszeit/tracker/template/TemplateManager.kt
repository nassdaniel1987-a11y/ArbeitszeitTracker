package com.arbeitszeit.tracker.template

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Verwaltet Excel-Vorlagen für verschiedene Jahre
 */
class TemplateManager(private val context: Context) {

    private val templatesDir: File
        get() = File(context.filesDir, "templates").also { it.mkdirs() }

    /**
     * Speichert eine Excel-Vorlage für ein bestimmtes Jahr
     *
     * @param year Das Jahr für diese Vorlage
     * @param uri URI der hochzuladenden Excel-Datei
     * @return true wenn erfolgreich gespeichert
     */
    suspend fun saveTemplate(year: Int, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val templateFile = File(templatesDir, "template_$year.xlsx")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(templateFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("TemplateManager", "Fehler beim Speichern der Vorlage", e)
            false
        }
    }

    /**
     * Lädt die Vorlage für ein bestimmtes Jahr
     *
     * @param year Das Jahr
     * @return FileInputStream der Vorlage, oder null wenn nicht vorhanden
     */
    fun getTemplateStream(year: Int): FileInputStream? {
        val templateFile = File(templatesDir, "template_$year.xlsx")
        return if (templateFile.exists()) {
            FileInputStream(templateFile)
        } else {
            null
        }
    }

    /**
     * Prüft ob eine Vorlage für ein bestimmtes Jahr existiert
     *
     * @param year Das Jahr
     * @return true wenn Vorlage vorhanden
     */
    fun hasTemplate(year: Int): Boolean {
        val templateFile = File(templatesDir, "template_$year.xlsx")
        return templateFile.exists()
    }

    /**
     * Löscht die Vorlage für ein bestimmtes Jahr
     *
     * @param year Das Jahr
     * @return true wenn erfolgreich gelöscht
     */
    fun deleteTemplate(year: Int): Boolean {
        val templateFile = File(templatesDir, "template_$year.xlsx")
        return templateFile.delete()
    }

    /**
     * Gibt eine Liste aller Jahre zurück, für die Vorlagen existieren
     *
     * @return Liste von Jahren (z.B. [2025, 2026, 2027])
     */
    fun getAvailableYears(): List<Int> {
        return templatesDir.listFiles()
            ?.mapNotNull { file ->
                // Dateinamen wie "template_2026.xlsx" -> 2026 extrahieren
                val match = Regex("template_(\\d{4})\\.xlsx").find(file.name)
                match?.groupValues?.get(1)?.toIntOrNull()
            }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Gibt den Dateinamen der Vorlage zurück
     *
     * @param year Das Jahr
     * @return Dateiname (z.B. "template_2026.xlsx")
     */
    fun getTemplateFileName(year: Int): String {
        return "template_$year.xlsx"
    }
}
