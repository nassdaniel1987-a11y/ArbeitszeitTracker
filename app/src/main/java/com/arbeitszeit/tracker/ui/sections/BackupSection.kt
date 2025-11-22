package com.arbeitszeit.tracker.ui.sections

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.backup.BackupManager
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * BackupSection - Cloud-Backup und Wiederherstellung
 */
@Composable
fun BackupSection(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }

    var isCreatingBackup by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf(listOf<BackupManager.BackupInfo>()) }
    var showRestoreDialog by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }

    // Lade verfügbare Backups beim Start
    LaunchedEffect(Unit) {
        availableBackups = backupManager.getAvailableBackups()
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
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Cloud-Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        "Sichere deine Arbeitszeit-Daten lokal oder in der Cloud. " +
                        "Backups enthalten alle Zeiteinträge und Einstellungen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Lokales Backup erstellen
        item {
            Text(
                "Lokales Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        isCreatingBackup = true
                        try {
                            val backupFile = backupManager.createBackup()
                            availableBackups = backupManager.getAvailableBackups()
                            snackbarHostState.showSnackbar(
                                "Backup erfolgreich erstellt: ${backupFile.name}",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                "Fehler beim Erstellen: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        } finally {
                            isCreatingBackup = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingBackup
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isCreatingBackup) "Erstelle Backup..." else "Neues Backup erstellen")
            }
        }

        // Verfügbare Backups
        item {
            Text(
                "Verfügbare Backups (${availableBackups.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (availableBackups.isEmpty()) {
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
                            "Keine Backups vorhanden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(availableBackups.size) { index ->
                val backup = availableBackups[index]
                BackupItemCard(
                    backup = backup,
                    onRestore = { showRestoreDialog = backup },
                    onDelete = {
                        scope.launch {
                            backup.file.delete()
                            availableBackups = backupManager.getAvailableBackups()
                            snackbarHostState.showSnackbar(
                                "Backup gelöscht",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }

        // Google Drive Hinweis
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.padding(top = 16.dp)
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
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Google Drive Integration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        "Google Drive Sync wird in einer zukünftigen Version verfügbar sein. " +
                        "Aktuell können Sie Backups manuell exportieren und in Ihrer Cloud speichern.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    // Restore Dialog
    showRestoreDialog?.let { backup ->
        var replaceExisting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("Backup wiederherstellen?") },
            text = {
                Column {
                    Text("Möchten Sie dieses Backup wiederherstellen?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        backup.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    // Checkbox: Vorhandene überschreiben
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { replaceExisting = !replaceExisting }
                    ) {
                        Checkbox(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = backupManager.restoreBackup(backup.file, replaceExisting = replaceExisting)
                            when (result) {
                                is BackupManager.RestoreResult.Success -> {
                                    snackbarHostState.showSnackbar(
                                        "${result.entriesRestored} Einträge wiederhergestellt",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                is BackupManager.RestoreResult.Error -> {
                                    snackbarHostState.showSnackbar(
                                        result.message,
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                            showRestoreDialog = null
                        }
                    }
                ) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun BackupItemCard(
    backup: BackupManager.BackupInfo,
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
                    Text(
                        backup.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatBackupDate(backup.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatFileSize(backup.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, "Wiederherstellen")
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
            title = { Text("Backup löschen?") },
            text = { Text("Möchten Sie dieses Backup wirklich löschen?") },
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
