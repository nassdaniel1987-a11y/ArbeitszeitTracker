package com.arbeitszeit.tracker.ui.sections

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.backup.BackupManager
import com.arbeitszeit.tracker.drive.DriveManager
import com.arbeitszeit.tracker.drive.GoogleSignInManager
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Cloud Backup Section - Google Drive Integration
 *
 * Erweitert die BackupSection um Google Drive Cloud-Backup Funktionalität.
 */
@Composable
fun CloudBackupSection(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backupManager = remember { BackupManager(context) }
    val signInManager = remember { GoogleSignInManager(context) }

    // State
    val accountState by signInManager.accountState.collectAsState()
    var driveBackups by remember { mutableStateOf<List<DriveManager.DriveFile>>(emptyList()) }
    var isLoadingDriveBackups by remember { mutableStateOf(false) }
    var isUploadingToDrive by remember { mutableStateOf(false) }
    var showDriveRestoreDialog by remember { mutableStateOf<DriveManager.DriveFile?>(null) }

    // DriveManager nur erstellen wenn angemeldet
    val driveManager = remember(accountState) {
        if (accountState is GoogleSignInManager.AccountState.SignedIn) {
            val credential = signInManager.getCredentials()
            if (credential != null) DriveManager(context, credential) else null
        } else {
            null
        }
    }

    // Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val signInResult = signInManager.handleSignInResult(result.data)
            when (signInResult) {
                is GoogleSignInManager.SignInResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Erfolgreich bei Google angemeldet",
                            duration = SnackbarDuration.Short
                        )
                        // Lade Drive Backups
                        loadDriveBackups(driveManager, snackbarHostState) { backups ->
                            driveBackups = backups
                        }
                    }
                }
                is GoogleSignInManager.SignInResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Anmeldung fehlgeschlagen: ${signInResult.message}",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        }
    }

    // Lade Drive Backups beim Start (wenn angemeldet)
    LaunchedEffect(accountState) {
        if (accountState is GoogleSignInManager.AccountState.SignedIn && driveManager != null) {
            isLoadingDriveBackups = true
            loadDriveBackups(driveManager, snackbarHostState) { backups ->
                driveBackups = backups
                isLoadingDriveBackups = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Google Drive Cloud-Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        "Sichere deine Arbeitszeit-Daten automatisch in deiner Google Drive Cloud. " +
                                "Backups sind verschlüsselt und nur für dich zugänglich.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Google Sign-In Status
        item {
            when (val state = accountState) {
                is GoogleSignInManager.AccountState.SignedOut -> {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Nicht angemeldet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Melde dich mit deinem Google-Konto an, um Cloud-Backup zu verwenden.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val signInIntent = signInManager.getSignInIntent()
                                    signInLauncher.launch(signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Mit Google anmelden")
                            }
                        }
                    }
                }

                is GoogleSignInManager.AccountState.SignedIn -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Angemeldet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        state.account.email ?: "Unbekannt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            signInManager.signOut()
                                            driveBackups = emptyList()
                                            snackbarHostState.showSnackbar(
                                                "Abgemeldet",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                ) {
                                    Text("Abmelden")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cloud Backup Aktionen (nur wenn angemeldet)
        if (accountState is GoogleSignInManager.AccountState.SignedIn && driveManager != null) {
            item {
                Text(
                    "Cloud Backup erstellen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            isUploadingToDrive = true
                            try {
                                // 1. Erstelle lokales Backup
                                val localBackup = backupManager.createBackup()

                                // 2. Lade zu Google Drive hoch
                                val result = driveManager.uploadBackup(localBackup)

                                result.fold(
                                    onSuccess = {
                                        snackbarHostState.showSnackbar(
                                            "Backup erfolgreich zu Google Drive hochgeladen",
                                            duration = SnackbarDuration.Short
                                        )
                                        // Aktualisiere Liste
                                        loadDriveBackups(driveManager, snackbarHostState) { backups ->
                                            driveBackups = backups
                                        }
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            "Upload fehlgeschlagen: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Fehler: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isUploadingToDrive = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploadingToDrive
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isUploadingToDrive) "Wird hochgeladen..." else "Zu Google Drive hochladen")
                }
            }

            // Drive Backups Liste
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Google Drive Backups (${driveBackups.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isLoadingDriveBackups) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoadingDriveBackups = true
                                    loadDriveBackups(driveManager, snackbarHostState) { backups ->
                                        driveBackups = backups
                                        isLoadingDriveBackups = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, "Aktualisieren")
                        }
                    }
                }
            }

            if (isLoadingDriveBackups) {
                item {
                    Card {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            } else if (driveBackups.isEmpty()) {
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Keine Cloud-Backups vorhanden",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(driveBackups.size) { index ->
                    val driveBackup = driveBackups[index]
                    DriveBackupItemCard(
                        driveBackup = driveBackup,
                        onRestore = { showDriveRestoreDialog = driveBackup },
                        onDelete = {
                            scope.launch {
                                val result = driveManager.deleteBackup(driveBackup.id)
                                result.fold(
                                    onSuccess = {
                                        driveBackups = driveBackups.filter { it.id != driveBackup.id }
                                        snackbarHostState.showSnackbar(
                                            "Backup gelöscht",
                                            duration = SnackbarDuration.Short
                                        )
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            "Löschen fehlgeschlagen: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Drive Restore Dialog
    showDriveRestoreDialog?.let { driveBackup ->
        var replaceExisting by remember { mutableStateOf(false) }
        var isRestoring by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isRestoring) showDriveRestoreDialog = null },
            title = { Text("Backup von Drive wiederherstellen?") },
            text = {
                Column {
                    Text("Möchten Sie dieses Cloud-Backup wiederherstellen?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        driveBackup.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { if (!isRestoring) replaceExisting = !replaceExisting }
                    ) {
                        Checkbox(
                            checked = replaceExisting,
                            onCheckedChange = { if (!isRestoring) replaceExisting = it },
                            enabled = !isRestoring
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Vorhandene Einträge überschreiben",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (replaceExisting) "Alle Daten werden ersetzt" else "Nur neue Einträge importieren",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (replaceExisting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isRestoring) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            try {
                                // 1. Download von Drive
                                val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
                                val downloadResult = driveManager?.downloadBackup(driveBackup, tempFile)

                                downloadResult?.fold(
                                    onSuccess = { downloadedFile ->
                                        // 2. Restore aus lokalem File
                                        val restoreResult = backupManager.restoreBackup(
                                            downloadedFile,
                                            replaceExisting = replaceExisting
                                        )

                                        when (restoreResult) {
                                            is BackupManager.RestoreResult.Success -> {
                                                snackbarHostState.showSnackbar(
                                                    "${restoreResult.entriesRestored} Einträge wiederhergestellt",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                            is BackupManager.RestoreResult.Error -> {
                                                snackbarHostState.showSnackbar(
                                                    restoreResult.message,
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }

                                        // Cleanup
                                        downloadedFile.delete()
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            "Download fehlgeschlagen: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Fehler: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isRestoring = false
                                showDriveRestoreDialog = null
                            }
                        }
                    },
                    enabled = !isRestoring
                ) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDriveRestoreDialog = null },
                    enabled = !isRestoring
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun DriveBackupItemCard(
    driveBackup: DriveManager.DriveFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            driveBackup.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatBackupDate(driveBackup.modifiedTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatFileSize(driveBackup.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.CloudDownload, "Von Drive wiederherstellen")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Löschen")
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Cloud-Backup löschen?") },
            text = { Text("Möchten Sie dieses Backup wirklich aus Google Drive löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

private suspend fun loadDriveBackups(
    driveManager: DriveManager?,
    snackbarHostState: SnackbarHostState,
    onLoaded: (List<DriveManager.DriveFile>) -> Unit
) {
    if (driveManager == null) return

    val result = driveManager.listBackups()
    result.fold(
        onSuccess = { backups -> onLoaded(backups) },
        onFailure = { e ->
            snackbarHostState.showSnackbar(
                "Laden fehlgeschlagen: ${e.message}",
                duration = SnackbarDuration.Long
            )
            onLoaded(emptyList())
        }
    )
}

private fun formatBackupDate(timestamp: Long): String {
    val date = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
