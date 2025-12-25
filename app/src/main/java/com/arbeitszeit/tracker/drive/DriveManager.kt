package com.arbeitszeit.tracker.drive

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Google Drive Manager
 *
 * Verwaltet Cloud-Backup Operationen mit Google Drive API.
 * Verwendet Drive.File Scope - nur App-eigene Dateien.
 */
class DriveManager(
    private val context: Context,
    private val credential: GoogleAccountCredential
) {

    private val driveService: Drive by lazy {
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Arbeitszeit Tracker")
            .build()
    }

    /**
     * App-Folder Name in Google Drive
     */
    private val appFolderName = "ArbeitszeitTrackerBackups"

    /**
     * Uploaded ein Backup zu Google Drive
     */
    suspend fun uploadBackup(localFile: java.io.File): Result<DriveFile> = withContext(Dispatchers.IO) {
        try {
            // Finde oder erstelle App-Folder
            val folderId = getOrCreateAppFolder()

            // Metadata für Drive-Datei
            val fileMetadata = File().apply {
                name = localFile.name
                parents = listOf(folderId)
                mimeType = "application/json"
            }

            // Upload File Content
            val mediaContent = FileContent("application/json", localFile)

            val file = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, size, modifiedTime")
                .execute()

            Result.success(
                DriveFile(
                    id = file.id,
                    name = file.name,
                    size = file.getSizeSafely() ?: 0,
                    modifiedTime = file.modifiedTime?.value ?: 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lädt ein Backup von Google Drive herunter
     */
    suspend fun downloadBackup(driveFile: DriveFile, outputFile: java.io.File): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            val outputStream: OutputStream = FileOutputStream(outputFile)

            driveService.files()
                .get(driveFile.id)
                .executeMediaAndDownloadTo(outputStream)

            outputStream.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listet alle Backups im App-Folder
     */
    suspend fun listBackups(): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        try {
            // Finde App-Folder (oder erstelle ihn)
            val folderId = getOrCreateAppFolder()

            // Liste Dateien im Folder
            val result = driveService.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()

            val files = result.files.map { file ->
                DriveFile(
                    id = file.id,
                    name = file.name,
                    size = file.getSizeSafely() ?: 0,
                    modifiedTime = file.modifiedTime?.value ?: 0
                )
            }

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Löscht ein Backup aus Google Drive
     */
    suspend fun deleteBackup(driveFileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            driveService.files().delete(driveFileId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Findet oder erstellt den App-Folder in Google Drive
     */
    private fun getOrCreateAppFolder(): String {
        // Suche nach existierendem Folder
        val result = driveService.files().list()
            .setQ("name = '$appFolderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        // Wenn Folder existiert, gib ID zurück
        if (result.files.isNotEmpty()) {
            return result.files[0].id
        }

        // Sonst: Erstelle neuen Folder
        val folderMetadata = File().apply {
            name = appFolderName
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = driveService.files()
            .create(folderMetadata)
            .setFields("id")
            .execute()

        return folder.id
    }

    /**
     * Datenklasse für Drive-Dateien
     */
    data class DriveFile(
        val id: String,
        val name: String,
        val size: Long,
        val modifiedTime: Long
    )

    /**
     * Upload Progress Callback
     */
    interface UploadProgressListener {
        fun onProgress(bytesUploaded: Long, totalBytes: Long)
    }

    /**
     * Download Progress Callback
     */
    interface DownloadProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }
}

/**
 * Extension: Safe get size (null-safe)
 */
private fun File.getSizeSafely(): Long? {
    return try {
        this.size?.toLong()
    } catch (e: Exception) {
        null
    }
}
