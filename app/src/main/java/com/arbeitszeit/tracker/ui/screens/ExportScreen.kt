package com.arbeitszeit.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: ExportViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedKW by viewModel.selectedKW.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // File Picker für Excel-Import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            showImportDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header mit Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Excel Export & Import",
                style = MaterialTheme.typography.titleLarge
            )
        }

        HorizontalDivider()

        // EXPORT SECTION als Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Export",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    "Exportiert alle Zeiteinträge des Jahres in eine Excel-Datei mit der Vorlage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                OutlinedButton(
                    onClick = { viewModel.loadExportPreview() },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vorschau anzeigen")
                }

                Button(
                    onClick = { viewModel.exportExcel() },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exportiere...")
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Excel exportieren (Gesamtjahr)")
                    }
                }

                Button(
                    onClick = { viewModel.exportSimpleExcel() },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Einfacher Export (Wochenblöcke)")
                }

                if (uiState.exportSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Erfolgreich: ${viewModel.getExpectedFileName()}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // IMPORT SECTION als Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Import",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    "Importiere eine vorhandene Excel-Datei mit Zeiteinträgen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )

                Button(
                    onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Importiere...")
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Excel importieren")
                    }
                }

                if (uiState.importSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Erfolgreich: ${uiState.importedEntriesCount} Einträge importiert",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // ERROR MESSAGE
        uiState.error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Fehler: $it",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Import Dialog mit Option für Stammdaten
    if (showImportDialog && selectedFileUri != null) {
        ImportConfirmationDialog(
            onConfirm = { importStammdaten ->
                viewModel.importExcel(selectedFileUri!!, importStammdaten)
                showImportDialog = false
                selectedFileUri = null
            },
            onDismiss = {
                showImportDialog = false
                selectedFileUri = null
            }
        )
    }

    // Export Vorschau Dialog
    uiState.previewData?.let { previewData ->
        ExportPreviewDialog(
            previewData = previewData,
            onConfirm = {
                viewModel.closePreview()
                viewModel.exportExcel()
            },
            onDismiss = { viewModel.closePreview() }
        )
    }
}

@Composable
fun ExportPreviewDialog(
    previewData: com.arbeitszeit.tracker.viewmodel.ExportPreviewData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Text("Export Vorschau ${previewData.year}")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Übersicht
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Übersicht",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text("Jahr: ${previewData.year}")
                        Text("Name: ${previewData.userName}")
                        Text("Einrichtung: ${previewData.einrichtung}")
                        Text(
                            "Einträge gesamt: ${previewData.totalEntries}",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                // Beispiel-Daten
                Text(
                    "Beispiel-Einträge (erste 10):",
                    style = MaterialTheme.typography.titleSmall
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    previewData.entries.forEach { entry ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    entry.datum,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${entry.startZeit ?: "--"}:${entry.endZeit ?: "--"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Jetzt exportieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ImportConfirmationDialog(
    onConfirm: (importStammdaten: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var importStammdaten by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excel importieren") },
        text = {
            Column {
                Text("Möchtest du auch die Stammdaten (Name, Einrichtung, etc.) importieren?")
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = importStammdaten,
                        onCheckedChange = { importStammdaten = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Stammdaten importieren")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(importStammdaten) }) {
                Text("Importieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
