package com.arbeitszeit.tracker.backup

import android.content.Context
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.model.TimeEntry
import com.arbeitszeit.tracker.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * BackupManager - Verwaltet Datenbank-Backups
 * Erstellt JSON-Backups der kompletten Datenbank (TimeEntries + Settings)
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_FILE_PREFIX = "arbeitszeit_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
        private const val BACKUP_VERSION = 1
    }

    /**
     * Erstellt ein vollständiges Backup der Datenbank
     * @return File-Objekt des erstellten Backups
     */
    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val timeEntryDao = database.timeEntryDao()
        val settingsDao = database.userSettingsDao()

        // Daten aus Datenbank holen
        val timeEntries = timeEntryDao.getAllEntries()
        val settings = settingsDao.getSettings()

        // JSON-Objekt erstellen
        val backupJson = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)

            // TimeEntries
            val entriesArray = JSONArray()
            timeEntries.forEach { entry ->
                val entryJson = JSONObject().apply {
                    put("datum", entry.datum)
                    put("startzeit", entry.startzeit)
                    put("endezeit", entry.endezeit)
                    put("pauseMinuten", entry.pauseMinuten)
                    put("sollMinuten", entry.sollMinuten)
                    put("notiz", entry.notiz)
                    put("typ", entry.typ)
                    put("kalenderwoche", entry.kalenderwoche)
                    put("jahr", entry.jahr)
                }
                entriesArray.put(entryJson)
            }
            put("timeEntries", entriesArray)

            // UserSettings
            settings?.let { s ->
                val settingsJson = JSONObject().apply {
                    put("standardSollStunden", s.standardSollStunden)
                    put("standardPauseMinuten", s.standardPauseMinuten)
                    put("ueberstundenVorjahrMinuten", s.ueberstundenVorjahrMinuten)
                    put("benachrichtigungenAktiv", s.benachrichtigungenAktiv)
                    put("benachrichtigungStartZeit", s.benachrichtigungStartZeit)
                    put("benachrichtigungEndeZeit", s.benachrichtigungEndeZeit)
                    put("geofencingAktiv", s.geofencingAktiv)
                    put("arbeitsortLatitude", s.arbeitsortLatitude)
                    put("arbeitsortLongitude", s.arbeitsortLongitude)
                    put("arbeitsortRadius", s.arbeitsortRadius)
                    put("bundesland", s.bundesland)
                    put("urlaubsanspruchTage", s.urlaubsanspruchTage)
                }
                put("settings", settingsJson)
            }
        }

        // Backup-Datei erstellen
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
        val backupFile = File(context.cacheDir, fileName)

        backupFile.writeText(backupJson.toString(2)) // Pretty-print mit Indent 2

        backupFile
    }

    /**
     * Stellt ein Backup wieder her
     * @param backupFile Die Backup-Datei die wiederhergestellt werden soll
     * @param replaceExisting Wenn true, werden vorhandene Daten ersetzt. Wenn false, werden Daten zusammengeführt.
     */
    suspend fun restoreBackup(backupFile: File, replaceExisting: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val backupJson = JSONObject(backupFile.readText())

            // Version prüfen
            val version = backupJson.getInt("version")
            if (version > BACKUP_VERSION) {
                return@withContext RestoreResult.Error("Backup-Version zu neu. Bitte App aktualisieren.")
            }

            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()
            val settingsDao = database.userSettingsDao()

            // Wenn replaceExisting, lösche vorhandene Daten
            if (replaceExisting) {
                timeEntryDao.deleteAll()
            }

            // TimeEntries wiederherstellen
            val entriesArray = backupJson.getJSONArray("timeEntries")
            var entriesRestored = 0
            for (i in 0 until entriesArray.length()) {
                val entryJson = entriesArray.getJSONObject(i)
                val entry = TimeEntry(
                    datum = entryJson.getString("datum"),
                    startzeit = entryJson.optString("startzeit", null),
                    endezeit = entryJson.optString("endezeit", null),
                    pauseMinuten = entryJson.getInt("pauseMinuten"),
                    sollMinuten = entryJson.getInt("sollMinuten"),
                    notiz = entryJson.optString("notiz", ""),
                    typ = entryJson.optString("typ", TimeEntry.TYP_NORMAL),
                    kalenderwoche = entryJson.getInt("kalenderwoche"),
                    jahr = entryJson.getInt("jahr")
                )
                timeEntryDao.insertOrUpdate(entry)
                entriesRestored++
            }

            // UserSettings wiederherstellen
            if (backupJson.has("settings")) {
                val settingsJson = backupJson.getJSONObject("settings")
                val settings = UserSettings(
                    standardSollStunden = settingsJson.getDouble("standardSollStunden"),
                    standardPauseMinuten = settingsJson.getInt("standardPauseMinuten"),
                    ueberstundenVorjahrMinuten = settingsJson.getInt("ueberstundenVorjahrMinuten"),
                    benachrichtigungenAktiv = settingsJson.getBoolean("benachrichtigungenAktiv"),
                    benachrichtigungStartZeit = settingsJson.optString("benachrichtigungStartZeit", null),
                    benachrichtigungEndeZeit = settingsJson.optString("benachrichtigungEndeZeit", null),
                    geofencingAktiv = settingsJson.getBoolean("geofencingAktiv"),
                    arbeitsortLatitude = settingsJson.optDouble("arbeitsortLatitude", 0.0),
                    arbeitsortLongitude = settingsJson.optDouble("arbeitsortLongitude", 0.0),
                    arbeitsortRadius = settingsJson.optInt("arbeitsortRadius", 200),
                    bundesland = settingsJson.optString("bundesland", null),
                    urlaubsanspruchTage = settingsJson.optInt("urlaubsanspruchTage", 30)
                )
                settingsDao.insertOrUpdate(settings)
            }

            RestoreResult.Success(entriesRestored)
        } catch (e: Exception) {
            RestoreResult.Error("Fehler beim Wiederherstellen: ${e.message}")
        }
    }

    /**
     * Gibt eine Liste aller verfügbaren Backups zurück
     */
    fun getAvailableBackups(): List<BackupInfo> {
        val backupDir = context.cacheDir
        return backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
        }?.map { file ->
            BackupInfo(
                file = file,
                name = file.nameWithoutExtension.removePrefix(BACKUP_FILE_PREFIX),
                size = file.length(),
                timestamp = file.lastModified()
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    /**
     * Löscht alte Backups und behält nur die neuesten [keepCount] Backups
     */
    fun cleanupOldBackups(keepCount: Int = 5) {
        val backups = getAvailableBackups()
        backups.drop(keepCount).forEach { it.file.delete() }
    }

    /**
     * Informationen über ein Backup
     */
    data class BackupInfo(
        val file: File,
        val name: String,
        val size: Long,
        val timestamp: Long
    )

    /**
     * Ergebnis einer Wiederherstellung
     */
    sealed class RestoreResult {
        data class Success(val entriesRestored: Int) : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }
}
