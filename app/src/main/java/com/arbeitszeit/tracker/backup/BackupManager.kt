package com.arbeitszeit.tracker.backup

import android.content.Context
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        val timeEntries = timeEntryDao.getAllEntriesFlow().first()
        val settings = settingsDao.getSettings()

        // JSON-Objekt erstellen
        val backupJson = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)

            // TimeEntries
            val entriesArray = JSONArray()
            for (entry in timeEntries) {
                val entryJson = JSONObject().apply {
                    put("id", entry.id)
                    put("datum", entry.datum)
                    put("wochentag", entry.wochentag)
                    put("kalenderwoche", entry.kalenderwoche)
                    put("jahr", entry.jahr)
                    put("startZeit", entry.startZeit)
                    put("endZeit", entry.endZeit)
                    put("pauseMinuten", entry.pauseMinuten)
                    put("sollMinuten", entry.sollMinuten)
                    put("typ", entry.typ)
                    put("notiz", entry.notiz)
                    put("arbeitszeitBereitschaft", entry.arbeitszeitBereitschaft)
                    put("isManualEntry", entry.isManualEntry)
                }
                entriesArray.put(entryJson)
            }
            put("timeEntries", entriesArray)

            // UserSettings
            settings?.let { s ->
                val settingsJson = JSONObject().apply {
                    put("name", s.name)
                    put("einrichtung", s.einrichtung)
                    put("arbeitsumfangProzent", s.arbeitsumfangProzent)
                    put("wochenStundenMinuten", s.wochenStundenMinuten)
                    put("arbeitsTageProWoche", s.arbeitsTageProWoche)
                    put("ferienbetreuung", s.ferienbetreuung)
                    put("ueberstundenVorjahrMinuten", s.ueberstundenVorjahrMinuten)
                    put("letzterUebertragMinuten", s.letzterUebertragMinuten)
                    put("ersterMontagImJahr", s.ersterMontagImJahr)
                    put("montagSollMinuten", s.montagSollMinuten)
                    put("dienstagSollMinuten", s.dienstagSollMinuten)
                    put("mittwochSollMinuten", s.mittwochSollMinuten)
                    put("donnerstagSollMinuten", s.donnerstagSollMinuten)
                    put("freitagSollMinuten", s.freitagSollMinuten)
                    put("samstagSollMinuten", s.samstagSollMinuten)
                    put("sonntagSollMinuten", s.sonntagSollMinuten)
                    put("workingDays", s.workingDays)
                    put("geofencingEnabled", s.geofencingEnabled)
                    put("geofencingStartHour", s.geofencingStartHour)
                    put("geofencingEndHour", s.geofencingEndHour)
                    put("geofencingActiveDays", s.geofencingActiveDays)
                    put("darkMode", s.darkMode)
                    put("selectedTemplateYear", s.selectedTemplateYear)
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
                timeEntryDao.deleteAllEntries()
            }

            // TimeEntries wiederherstellen
            val entriesArray = backupJson.getJSONArray("timeEntries")
            var entriesRestored = 0
            for (i in 0 until entriesArray.length()) {
                val entryJson = entriesArray.getJSONObject(i)
                val datum = entryJson.getString("datum")

                // Wenn nicht replaceExisting, prüfe ob Eintrag bereits existiert
                if (!replaceExisting) {
                    val existing = timeEntryDao.getEntryByDate(datum)
                    if (existing != null) {
                        // Eintrag existiert bereits, überspringe
                        continue
                    }
                }

                val entry = TimeEntry(
                    datum = datum,
                    wochentag = entryJson.getString("wochentag"),
                    kalenderwoche = entryJson.getInt("kalenderwoche"),
                    jahr = entryJson.getInt("jahr"),
                    startZeit = if (entryJson.isNull("startZeit")) null else entryJson.getInt("startZeit"),
                    endZeit = if (entryJson.isNull("endZeit")) null else entryJson.getInt("endZeit"),
                    pauseMinuten = entryJson.getInt("pauseMinuten"),
                    sollMinuten = entryJson.getInt("sollMinuten"),
                    typ = entryJson.getString("typ"),
                    notiz = entryJson.optString("notiz", ""),
                    arbeitszeitBereitschaft = entryJson.optInt("arbeitszeitBereitschaft", 0),
                    isManualEntry = entryJson.optBoolean("isManualEntry", false)
                )
                timeEntryDao.insert(entry)
                entriesRestored++
            }

            // UserSettings wiederherstellen
            if (backupJson.has("settings")) {
                val settingsJson = backupJson.getJSONObject("settings")
                val settings = UserSettings(
                    name = settingsJson.getString("name"),
                    einrichtung = settingsJson.getString("einrichtung"),
                    arbeitsumfangProzent = settingsJson.getInt("arbeitsumfangProzent"),
                    wochenStundenMinuten = settingsJson.getInt("wochenStundenMinuten"),
                    arbeitsTageProWoche = settingsJson.optInt("arbeitsTageProWoche", 5),
                    ferienbetreuung = settingsJson.optBoolean("ferienbetreuung", true),
                    ueberstundenVorjahrMinuten = settingsJson.getInt("ueberstundenVorjahrMinuten"),
                    letzterUebertragMinuten = settingsJson.optInt("letzterUebertragMinuten", 0),
                    ersterMontagImJahr = if (settingsJson.isNull("ersterMontagImJahr")) null else settingsJson.getString("ersterMontagImJahr"),
                    montagSollMinuten = if (settingsJson.isNull("montagSollMinuten")) null else settingsJson.getInt("montagSollMinuten"),
                    dienstagSollMinuten = if (settingsJson.isNull("dienstagSollMinuten")) null else settingsJson.getInt("dienstagSollMinuten"),
                    mittwochSollMinuten = if (settingsJson.isNull("mittwochSollMinuten")) null else settingsJson.getInt("mittwochSollMinuten"),
                    donnerstagSollMinuten = if (settingsJson.isNull("donnerstagSollMinuten")) null else settingsJson.getInt("donnerstagSollMinuten"),
                    freitagSollMinuten = if (settingsJson.isNull("freitagSollMinuten")) null else settingsJson.getInt("freitagSollMinuten"),
                    samstagSollMinuten = if (settingsJson.isNull("samstagSollMinuten")) null else settingsJson.getInt("samstagSollMinuten"),
                    sonntagSollMinuten = if (settingsJson.isNull("sonntagSollMinuten")) null else settingsJson.getInt("sonntagSollMinuten"),
                    workingDays = settingsJson.optString("workingDays", "12345"),
                    geofencingEnabled = settingsJson.optBoolean("geofencingEnabled", false),
                    geofencingStartHour = settingsJson.optInt("geofencingStartHour", 6),
                    geofencingEndHour = settingsJson.optInt("geofencingEndHour", 20),
                    geofencingActiveDays = settingsJson.optString("geofencingActiveDays", "12345"),
                    darkMode = settingsJson.optString("darkMode", "system"),
                    selectedTemplateYear = if (settingsJson.isNull("selectedTemplateYear")) null else settingsJson.getInt("selectedTemplateYear"),
                    bundesland = if (settingsJson.isNull("bundesland")) null else settingsJson.getString("bundesland"),
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
