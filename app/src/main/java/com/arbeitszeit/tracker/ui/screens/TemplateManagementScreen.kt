package com.arbeitszeit.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.TemplateViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(viewModel: TemplateViewModel) {
    val availableYears by viewModel.availableYears.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showYearPicker by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            showYearPicker = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Excel-Vorlagen", style = MaterialTheme.typography.titleLarge)

        Divider()

        // Info
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Verwalte Excel-Vorlagen für verschiedene Jahre",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Beim Export wird automatisch die Vorlage für das aktuelle Jahr verwendet. Überstunden aus dem Vorjahr bleiben erhalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Upload Button
        Button(
            onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isUploading
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Neue Vorlage hochladen")
        }

        Divider()

        // Verfügbare Vorlagen
        Text("Verfügbare Vorlagen", style = MaterialTheme.typography.titleMedium)

        if (availableYears.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "Keine benutzerdefinierten Vorlagen vorhanden.\nStandard-Vorlage wird verwendet.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableYears) { year ->
                    TemplateYearCard(
                        year = year,
                        isCurrent = year == LocalDate.now().year,
                        onDelete = { viewModel.deleteTemplate(year) }
                    )
                }
            }
        }

        // Success/Error Messages
        uiState.successMessage?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    message,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    message,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    // Year Picker Dialog
    if (showYearPicker && selectedFileUri != null) {
        YearPickerDialog(
            onYearSelected = { year ->
                viewModel.uploadTemplate(year, selectedFileUri!!)
                showYearPicker = false
                selectedFileUri = null
            },
            onDismiss = {
                showYearPicker = false
                selectedFileUri = null
            }
        )
    }
}

@Composable
fun TemplateYearCard(
    year: Int,
    isCurrent: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        colors = if (isCurrent) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Vorlage für $year",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Aktuell",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (isCurrent) {
                    Text(
                        "Wird für Exports verwendet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Löschen")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Vorlage löschen?") },
            text = { Text("Möchtest du die Vorlage für $year wirklich löschen?\nDie Standard-Vorlage wird dann verwendet.") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
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

@Composable
fun YearPickerDialog(
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = (currentYear - 1..currentYear + 5).toList()
    var selectedYear by remember { mutableStateOf(currentYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Für welches Jahr?") },
        text = {
            Column {
                Text("Wähle das Jahr für diese Vorlage:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                years.forEach { year ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedYear == year,
                            onClick = { selectedYear = year }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("$year")
                        if (year == currentYear) {
                            Spacer(Modifier.width(8.dp))
                            Text("(Aktuell)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onYearSelected(selectedYear) }) {
                Text("Hochladen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
