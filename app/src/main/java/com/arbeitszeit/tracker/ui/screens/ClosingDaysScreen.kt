package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbeitszeit.tracker.data.entity.ClosingDay
import com.arbeitszeit.tracker.viewmodel.ClosingDayViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosingDaysScreen(
    viewModel: ClosingDayViewModel = viewModel()
) {
    val currentYear = remember { LocalDate.now().year }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClosingDay by remember { mutableStateOf<ClosingDay?>(null) }

    val closingDays by viewModel.getClosingDaysForYear(selectedYear).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schließtage") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Schließtag hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Jahr-Auswahl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorheriges Jahr")
                }

                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächstes Jahr")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info-Karte
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Schließtage sind Tage, an denen deine Einrichtung geschlossen ist. Diese werden automatisch bei der Urlaubsplanung berücksichtigt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Liste der Schließtage
            if (closingDays.isEmpty()) {
                // Leere Ansicht
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keine Schließtage für $selectedYear",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tippe auf +, um Schließtage hinzuzufügen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(closingDays) { closingDay ->
                        ClosingDayCard(
                            closingDay = closingDay,
                            onEdit = { editingClosingDay = it },
                            onDelete = { viewModel.deleteClosingDay(it) }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        AddClosingDayDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, startDate, endDate, note ->
                viewModel.addClosingDay(
                    title = title,
                    startDate = startDate,
                    endDate = endDate,
                    note = note
                )
                showAddDialog = false
            }
        )
    }

    editingClosingDay?.let { closingDay ->
        EditClosingDayDialog(
            closingDay = closingDay,
            onDismiss = { editingClosingDay = null },
            onSave = { updated ->
                viewModel.updateClosingDay(updated)
                editingClosingDay = null
            }
        )
    }
}

@Composable
fun ClosingDayCard(
    closingDay: ClosingDay,
    onEdit: (ClosingDay) -> Unit,
    onDelete: (ClosingDay) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = closingDay.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = closingDay.getFormattedDateRange(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!closingDay.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = closingDay.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${closingDay.getDurationDays()} Tag${if (closingDay.getDurationDays() > 1) "e" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Actions
            IconButton(onClick = { onEdit(closingDay) }) {
                Icon(Icons.Default.Edit, "Bearbeiten")
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Schließtag löschen?") },
            text = { Text("Möchtest du '${closingDay.title}' wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(closingDay)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClosingDayDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, startDate: String, endDate: String, note: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schließtag hinzufügen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung") },
                    placeholder = { Text("z.B. Weihnachtsferien 2025") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Startdatum (yyyy-MM-dd)") },
                    placeholder = { Text("2025-12-24") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Enddatum (yyyy-MM-dd)") },
                    placeholder = { Text("2025-12-26") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    placeholder = { Text("z.B. Notdienst verfügbar") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                        onSave(title, startDate, endDate, note.ifEmpty { null })
                    }
                },
                enabled = title.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClosingDayDialog(
    closingDay: ClosingDay,
    onDismiss: () -> Unit,
    onSave: (ClosingDay) -> Unit
) {
    var title by remember { mutableStateOf(closingDay.title) }
    var startDate by remember { mutableStateOf(closingDay.startDate) }
    var endDate by remember { mutableStateOf(closingDay.endDate) }
    var note by remember { mutableStateOf(closingDay.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schließtag bearbeiten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Startdatum (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Enddatum (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                        onSave(
                            closingDay.copy(
                                title = title,
                                startDate = startDate,
                                endDate = endDate,
                                note = note.ifEmpty { null }
                            )
                        )
                    }
                },
                enabled = title.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
