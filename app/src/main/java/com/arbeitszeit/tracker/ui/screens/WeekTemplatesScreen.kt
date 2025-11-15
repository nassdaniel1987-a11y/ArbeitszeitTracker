package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.WeekTemplate
import com.arbeitszeit.tracker.ui.components.EmptyStates
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
            TopAppBar(
                title = { Text("Wochen-Vorlagen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            onConfirm = { name, description, weekStartDate ->
                viewModel.createTemplateFromWeek(name, description, weekStartDate)
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
    onConfirm: (name: String, description: String, weekStartDate: LocalDate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val currentWeekStart = remember {
        val today = LocalDate.now()
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCopy, "Vorlage erstellen") },
        title = { Text("Neue Vorlage aus aktueller Woche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Erstelle eine Vorlage aus der aktuellen Woche",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Standard-Woche") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    placeholder = { Text("z.B. Montag-Freitag 8-16 Uhr") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, currentWeekStart) },
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
