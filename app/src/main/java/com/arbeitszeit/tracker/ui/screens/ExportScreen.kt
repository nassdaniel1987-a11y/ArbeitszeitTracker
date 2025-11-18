package com.arbeitszeit.tracker.ui.screens

import android.content.Intent
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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: ExportViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    // Jahr-Auswahl: Aktuellas Jahr ± 5 Jahre
    val currentYear = LocalDate.now().year
    val availableYears = (currentYear - 5..currentYear + 5).toList()

    // Activity Result Launcher für Cloud-Export (Gesamtjahr)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportToCloud(it, isSimpleExport = false) }
    }

    // Activity Result Launcher für Cloud-Export (Einfach)
    val createDocumentSimpleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportToCloud(it, isSimpleExport = true) }
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
                "Excel Export",
                style = MaterialTheme.typography.titleLarge
            )
        }

        HorizontalDivider()

        // Jahr-Auswahl Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Jahr auswählen",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Jahr-Auswahl mit Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectYear(selectedYear - 1) },
                        enabled = selectedYear > availableYears.first()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Vorheriges Jahr")
                    }

                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.selectYear(selectedYear + 1) },
                        enabled = selectedYear < availableYears.last()
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Nächstes Jahr")
                    }
                }

                if (selectedYear != currentYear) {
                    TextButton(
                        onClick = { viewModel.selectYear(currentYear) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Zum aktuellen Jahr ($currentYear)")
                    }
                }
            }
        }

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
                    onClick = { viewModel.showFileNameDialog(isSimpleExport = false) },
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
                    onClick = { viewModel.showFileNameDialog(isSimpleExport = true) },
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Share-Buttons (OneDrive, E-Mail, etc.)
                Text(
                    "Teilen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Text(
                    "Funktioniert mit OneDrive, Google Drive, E-Mail, WhatsApp, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )

                OutlinedButton(
                    onClick = { viewModel.shareExport(isSimpleExport = false) },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("📤 Teilen: Gesamtjahr")
                }

                OutlinedButton(
                    onClick = { viewModel.shareExport(isSimpleExport = true) },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("📤 Teilen: Wochenblöcke")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Cloud-Export Buttons (nur Google Drive)
                Text(
                    "Direkt in Cloud speichern",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Text(
                    "Meist nur Google Drive verfügbar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )

                OutlinedButton(
                    onClick = {
                        createDocumentLauncher.launch("Arbeitszeit_${selectedYear}.xlsx")
                    },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cloud: Gesamtjahr")
                }

                OutlinedButton(
                    onClick = {
                        createDocumentSimpleLauncher.launch("Arbeitszeiten_${selectedYear}_Einfach.xlsx")
                    },
                    enabled = !uiState.isExporting && !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cloud: Wochenblöcke")
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

    // Export Vorschau Dialog
    uiState.previewData?.let { previewData ->
        ExportPreviewDialog(
            previewData = previewData,
            onConfirm = {
                viewModel.closePreview()
                viewModel.showFileNameDialog(isSimpleExport = false)
            },
            onDismiss = { viewModel.closePreview() }
        )
    }

    // Dateinamen-Eingabe Dialog
    if (uiState.showFileNameDialog) {
        FileNameInputDialog(
            isSimpleExport = uiState.isSimpleExport,
            onConfirm = { fileName ->
                if (uiState.isSimpleExport) {
                    viewModel.exportSimpleExcel(fileName)
                } else {
                    viewModel.exportExcel(fileName)
                }
            },
            onDismiss = { viewModel.dismissFileNameDialog() }
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
fun FileNameInputDialog(
    isSimpleExport: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember {
        mutableStateOf(
            if (isSimpleExport) {
                "Arbeitszeiten_${LocalDate.now().year}_Einfach"
            } else {
                "Arbeitszeit_${LocalDate.now().year}"
            }
        )
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Dateinamen eingeben")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Gib einen Namen für die Export-Datei ein:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        showError = it.isBlank()
                    },
                    label = { Text("Dateiname") },
                    placeholder = { Text("z.B. Arbeitszeit_2025") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                "Bitte einen Dateinamen eingeben",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(".xlsx wird automatisch hinzugefügt")
                        }
                    },
                    singleLine = true
                )

                Text(
                    "Die Datei wird im Ordner Downloads/ArbeitszeitTracker gespeichert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onConfirm(fileName.trim())
                    } else {
                        showError = true
                    }
                },
                enabled = fileName.isNotBlank()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
