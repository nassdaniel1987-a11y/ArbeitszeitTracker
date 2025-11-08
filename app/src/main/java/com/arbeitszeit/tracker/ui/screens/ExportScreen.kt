package com.arbeitszeit.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
        Text("Excel Export & Import", style = MaterialTheme.typography.titleLarge)

        Divider()

        // EXPORT SECTION
        Text("Export", style = MaterialTheme.typography.titleMedium)

        Text("Kalenderwoche: $selectedKW")

        Slider(
            value = selectedKW.toFloat(),
            onValueChange = { viewModel.selectKW(it.toInt()) },
            valueRange = 1f..52f,
            steps = 51
        )

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
                Text("Excel exportieren (Template)")
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Export erfolgreich: ${viewModel.getExpectedFileName()}",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Divider()

        // IMPORT SECTION
        Text("Import", style = MaterialTheme.typography.titleMedium)

        Text(
            "Importiere eine vorhandene Excel-Datei mit Zeiteinträgen",
            style = MaterialTheme.typography.bodySmall
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Import erfolgreich: ${uiState.importedEntriesCount} Einträge importiert",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ERROR MESSAGE
        uiState.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Fehler: $it",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
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
