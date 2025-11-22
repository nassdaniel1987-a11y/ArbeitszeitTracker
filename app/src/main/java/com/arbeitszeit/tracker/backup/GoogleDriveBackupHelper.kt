package com.arbeitszeit.tracker.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.google.api.client.http.FileContent as GoogleFileContent

/**
 * GoogleDriveBackupHelper - Verwaltet Google Drive Integration für Backups
 *
 * WICHTIG: Benötigt Google Cloud Console Setup:
 * 1. OAuth 2.0 Client ID erstellen
 * 2. SHA-1 Fingerprint der App hinzufügen
 * 3. google-services.json hinzufügen
 */
class GoogleDriveBackupHelper(private val context: Context) {

    companion object {
        private const val BACKUP_FOLDER_NAME = "ArbeitszeitTracker Backups"
        const val REQUEST_CODE_SIGN_IN = 1001
    }

    /**
     * Erstellt einen GoogleSignIn Intent für OAuth
     */
    fun getSignInIntent(): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(context, signInOptions)
        return client.signInIntent
    }

    /**
     * Prüft ob der Nutzer bereits angemeldet ist
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_FILE)
        )
    }

    /**
     * Gibt das aktuell angemeldete Google-Konto zurück
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Meldet den Nutzer ab
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(context, signInOptions)
        client.signOut()
    }

    /**
     * Lädt ein Backup zu Google Drive hoch
     */
    suspend fun uploadBackup(backupFile: File, account: GoogleSignInAccount): DriveUploadResult = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)

            // Backup-Ordner finden oder erstellen
            val folderId = getOrCreateBackupFolder(driveService)

            // Datei-Metadaten erstellen
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupFile.name
                parents = listOf(folderId)
                description = "Arbeitszeit Tracker Backup vom ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}"
            }

            val mediaContent = GoogleFileContent("application/json", backupFile)

            // Datei hochladen
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, modifiedTime")
                .execute()

            DriveUploadResult.Success(file.id, file.name, file.getSize())
        } catch (e: Exception) {
            DriveUploadResult.Error("Upload fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Listet alle Backups auf Google Drive auf
     */
    suspend fun listBackups(account: GoogleSignInAccount): List<DriveBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            val folderId = getOrCreateBackupFolder(driveService)

            val query = "'$folderId' in parents and trashed = false"
            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime, description)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files.map { file ->
                DriveBackupInfo(
                    id = file.id,
                    name = file.name,
                    size = file.getSize(),
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    description = file.description ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Lädt ein Backup von Google Drive herunter
     */
    suspend fun downloadBackup(fileId: String, account: GoogleSignInAccount): File? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)

            // Datei-Metadaten holen
            val file = driveService.files().get(fileId)
                .setFields("id, name")
                .execute()

            // Datei-Inhalt herunterladen
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)

            // Lokale Datei erstellen
            val localFile = File(context.cacheDir, file.name)
            localFile.writeBytes(outputStream.toByteArray())

            localFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Löscht ein Backup von Google Drive
     */
    suspend fun deleteBackup(fileId: String, account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            driveService.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Erstellt einen Drive Service für API-Zugriff
     */
    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Arbeitszeit Tracker")
            .build()
    }

    /**
     * Findet oder erstellt den Backup-Ordner auf Google Drive
     */
    private fun getOrCreateBackupFolder(driveService: Drive): String {
        // Suche nach existierendem Ordner
        val query = "name = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            // Ordner erstellen
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }

            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()

            folder.id
        }
    }

    /**
     * Informationen über ein Drive-Backup
     */
    data class DriveBackupInfo(
        val id: String,
        val name: String,
        val size: Long,
        val modifiedTime: Long,
        val description: String
    )

    /**
     * Ergebnis eines Drive-Uploads
     */
    sealed class DriveUploadResult {
        data class Success(val fileId: String, val fileName: String, val size: Long) : DriveUploadResult()
        data class Error(val message: String) : DriveUploadResult()
    }
}
