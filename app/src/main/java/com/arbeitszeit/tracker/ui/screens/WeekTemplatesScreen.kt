package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.WeekTemplate
import com.arbeitszeit.tracker.ui.components.EmptyStates
import com.arbeitszeit.tracker.viewmodel.DayTimeEntry
import com.arbeitszeit.tracker.viewmodel.WeekTemplatesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekTemplatesScreen(
    viewModel: WeekTemplatesViewModel,
    onNavigateBack: () -> Unit
) {
    val templates by viewModel.allTemplates.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<WeekTemplate?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Wochen-Vorlagen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Neue Vorlage")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    "Wochen-Vorlagen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Speichere wiederkehrende Wochenmuster als Vorlagen",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Template Liste
            if (templates.isEmpty()) {
                item {
                    EmptyStates.ErrorState(
                        errorMessage = "Noch keine Vorlagen vorhanden. Erstelle deine erste Vorlage aus einer bestehenden Woche!",
                        onRetry = null
                    )
                }
            } else {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onApply = {
                            selectedTemplate = template
                            showApplyDialog = true
                        },
                        onDelete = {
                            viewModel.deleteTemplate(template)
                        }
                    )
                }
            }
        }
    }

    // Create Template Dialog
    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, dayEntries ->
                viewModel.createTemplateManually(name, description, dayEntries)
                showCreateDialog = false
            }
        )
    }

    // Apply Template Dialog
    if (showApplyDialog && selectedTemplate != null) {
        ApplyTemplateDialog(
            template = selectedTemplate!!,
            onDismiss = {
                showApplyDialog = false
                selectedTemplate = null
            },
            onConfirm = { weekStartDate ->
                viewModel.applyTemplateToWeek(selectedTemplate!!.id, weekStartDate)
                showApplyDialog = false
                selectedTemplate = null
                onNavigateBack()  // Zurück zum HomeScreen
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: WeekTemplate,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (template.description.isNotEmpty()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Anwenden")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Vorlage löschen?") },
            text = { Text("Möchtest du die Vorlage \"${template.name}\" wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Löschen")
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

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, dayEntries: Map<Int, DayTimeEntry>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Zeit-Eingaben für jeden Tag (1=Mo, 2=Di, ..., 7=So)
    val dayData = remember {
        mutableStateMapOf<Int, DayData>().apply {
            (1..7).forEach { day -> put(day, DayData()) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCopy, "Vorlage erstellen") },
        title = { Text("Neue Dienstplan-Vorlage") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Erstelle eine Vorlage mit deinem Dienstplan",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vorlagen-Name") },
                    placeholder = { Text("z.B. Frühdienst") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    placeholder = { Text("z.B. 6:00-14:30 Uhr") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                HorizontalDivider()

                Text(
                    "Dienstzeiten eingeben",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Eingabefelder für jeden Tag
                val dayNames = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
                dayNames.forEachIndexed { index, dayName ->
                    val dayOfWeek = index + 1
                    DayTimeInputRow(
                        dayName = dayName,
                        dayData = dayData[dayOfWeek] ?: DayData(),
                        onDataChange = { newData -> dayData[dayOfWeek] = newData }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Konvertiere zu Map<Int, DayTimeEntry> - nur aktivierte Tage
                    val entries = dayData.mapNotNull { (day, data) ->
                        if (!data.enabled) return@mapNotNull null

                        val startMinutes = timeToMinutes(data.startHour, data.startMinute)
                        val endMinutes = timeToMinutes(data.endHour, data.endMinute)
                        val pause = data.pause.toIntOrNull() ?: 0

                        if (startMinutes != null && endMinutes != null) {
                            day to DayTimeEntry(startMinutes, endMinutes, pause)
                        } else null
                    }.toMap()

                    onConfirm(name, description, entries)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Erstellen")
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
private fun DayTimeInputRow(
    dayName: String,
    dayData: DayData,
    onDataChange: (DayData) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (dayData.enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = dayData.enabled,
                    onCheckedChange = { onDataChange(dayData.copy(enabled = it)) }
                )
                Text(
                    dayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (dayData.enabled) {
                // Start Zeit mit Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Von:", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        val timeText = if (dayData.startHour.isNotEmpty() && dayData.startMinute.isNotEmpty()) {
                            "${dayData.startHour.padStart(2, '0')}:${dayData.startMinute.padStart(2, '0')}"
                        } else {
                            "Zeit wählen"
                        }
                        Text(timeText)
                    }
                }

                // End Zeit mit Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bis:", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        val timeText = if (dayData.endHour.isNotEmpty() && dayData.endMinute.isNotEmpty()) {
                            "${dayData.endHour.padStart(2, '0')}:${dayData.endMinute.padStart(2, '0')}"
                        } else {
                            "Zeit wählen"
                        }
                        Text(timeText)
                    }
                }

                // Pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pause:", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = dayData.pause,
                        onValueChange = { if (it.length <= 3) onDataChange(dayData.copy(pause = it)) },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("30") },
                        suffix = { Text("Min", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true
                    )
                }
            }  // Ende if (dayData.enabled)
        }
    }

    // Time Picker Dialoge
    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Startzeit für $dayName",
            initialHour = dayData.startHour.toIntOrNull() ?: 8,
            initialMinute = dayData.startMinute.toIntOrNull() ?: 0,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                onDataChange(dayData.copy(
                    startHour = hour.toString(),
                    startMinute = minute.toString()
                ))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Endzeit für $dayName",
            initialHour = dayData.endHour.toIntOrNull() ?: 16,
            initialMinute = dayData.endMinute.toIntOrNull() ?: 0,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                onDataChange(dayData.copy(
                    endHour = hour.toString(),
                    endMinute = minute.toString()
                ))
                showEndTimePicker = false
            }
        )
    }
}

// Helper data class
private data class DayData(
    val enabled: Boolean = false,
    val startHour: String = "",
    val startMinute: String = "",
    val endHour: String = "",
    val endMinute: String = "",
    val pause: String = ""
)

// Helper function
private fun timeToMinutes(hour: String, minute: String): Int? {
    val h = hour.toIntOrNull() ?: return null
    val m = minute.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

@Composable
private fun ApplyTemplateDialog(
    template: WeekTemplate,
    onDismiss: () -> Unit,
    onConfirm: (weekStartDate: LocalDate) -> Unit
) {
    val currentWeekStart = remember {
        val today = LocalDate.now()
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentPaste, "Vorlage anwenden") },
        title = { Text("Vorlage anwenden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Die Vorlage \"${template.name}\" wird auf die aktuelle Woche angewendet.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Bestehende Einträge werden überschrieben!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentWeekStart) }) {
                Text("Anwenden")
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
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
