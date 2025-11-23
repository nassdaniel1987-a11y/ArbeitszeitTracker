package com.arbeitszeit.tracker.ui.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.SollZeitVorlage
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Arbeitszeitvorlagen-Verwaltung Section
 * Zeigt alle Vorlagen, ermöglicht Erstellen/Editieren/Löschen
 */
@Composable
fun ArbeitszeitvorlagenSection(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val vorlageDao = remember { database.sollZeitVorlageDao() }

    val vorlagen by vorlageDao.getAllVorlagenFlow().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingVorlage by remember { mutableStateOf<SollZeitVorlage?>(null) }
    var deletingVorlage by remember { mutableStateOf<SollZeitVorlage?>(null) }

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
                            "Arbeitszeitvorlagen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        "Erstelle mehrere Vorlagen für unterschiedliche Arbeitszeitmodelle " +
                        "(z.B. Normal, Ferienbetreuung, Sommer). Du kannst pro Woche oder " +
                        "pro Tag wählen, welche Vorlage gilt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Neue Vorlage Button
        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Neue Vorlage erstellen")
            }
        }

        // Vorlagen Liste
        item {
            if (vorlagen.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Noch keine Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Erstelle dein erstes Soll-Arbeitszeit Profil",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(vorlagen) { vorlage ->
            VorlageCard(
                vorlage = vorlage,
                onEdit = { editingVorlage = vorlage },
                onDelete = { deletingVorlage = vorlage },
                onSetDefault = {
                    scope.launch {
                        vorlageDao.setAsDefault(vorlage.id)
                        snackbarHostState.showSnackbar("${vorlage.name} als Standard gesetzt")
                    }
                }
            )
        }
    }

    // Dialoge
    if (showCreateDialog) {
        VorlageEditDialog(
            vorlage = null,
            onDismiss = { showCreateDialog = false },
            onSave = { newVorlage ->
                scope.launch {
                    vorlageDao.insert(newVorlage)
                    showCreateDialog = false
                    snackbarHostState.showSnackbar("Vorlage '${newVorlage.name}' erstellt")
                }
            }
        )
    }

    editingVorlage?.let { vorlage ->
        VorlageEditDialog(
            vorlage = vorlage,
            onDismiss = { editingVorlage = null },
            onSave = { updatedVorlage ->
                scope.launch {
                    vorlageDao.update(updatedVorlage)
                    editingVorlage = null
                    snackbarHostState.showSnackbar("Vorlage '${updatedVorlage.name}' aktualisiert")
                }
            }
        )
    }

    deletingVorlage?.let { vorlage ->
        AlertDialog(
            onDismissRequest = { deletingVorlage = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Vorlage löschen?") },
            text = {
                Text(
                    "Möchtest du die Vorlage '${vorlage.name}' wirklich löschen? " +
                    "Bestehende Zeiteinträge behalten ihre Soll-Zeiten."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            vorlageDao.delete(vorlage)
                            deletingVorlage = null
                            snackbarHostState.showSnackbar("Vorlage '${vorlage.name}' gelöscht")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVorlage = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun VorlageCard(
    vorlage: SollZeitVorlage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            vorlage.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (vorlage.isDefault) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Standard") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val wochenStunden = vorlage.getWochenStundenMinuten()
                    Text(
                        "${wochenStunden / 60}:${String.format("%02d", wochenStunden % 60)} Std/Woche gesamt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Weniger" else "Mehr"
                        )
                    }
                }
            }

            // Erweiterte Details
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Wochentage Details
                Text(
                    "Soll-Zeiten pro Tag:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                days.forEachIndexed { index, day ->
                    val sollMinuten = vorlage.getSollMinutenForDay(index + 1)
                    if (sollMinuten > 0) {
                        Text(
                            "$day: ${sollMinuten / 60}:${String.format("%02d", sollMinuten % 60)} Std",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Aktionen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!vorlage.isDefault) {
                        OutlinedButton(
                            onClick = onSetDefault,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Als Standard")
                        }
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bearbeiten")
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Löschen")
                    }
                }
            }
        }
    }
}

@Composable
private fun VorlageEditDialog(
    vorlage: SollZeitVorlage?,
    onDismiss: () -> Unit,
    onSave: (SollZeitVorlage) -> Unit
) {
    var name by remember { mutableStateOf(vorlage?.name ?: "") }

    // Tageszeiten (in Minuten)
    var montagStd by remember { mutableStateOf((vorlage?.montagSollMinuten ?: 0) / 60) }
    var montagMin by remember { mutableStateOf((vorlage?.montagSollMinuten ?: 0) % 60) }

    var dienstagStd by remember { mutableStateOf((vorlage?.dienstagSollMinuten ?: 0) / 60) }
    var dienstagMin by remember { mutableStateOf((vorlage?.dienstagSollMinuten ?: 0) % 60) }

    var mittwochStd by remember { mutableStateOf((vorlage?.mittwochSollMinuten ?: 0) / 60) }
    var mittwochMin by remember { mutableStateOf((vorlage?.mittwochSollMinuten ?: 0) % 60) }

    var donnerstagStd by remember { mutableStateOf((vorlage?.donnerstagSollMinuten ?: 0) / 60) }
    var donnerstagMin by remember { mutableStateOf((vorlage?.donnerstagSollMinuten ?: 0) % 60) }

    var freitagStd by remember { mutableStateOf((vorlage?.freitagSollMinuten ?: 0) / 60) }
    var freitagMin by remember { mutableStateOf((vorlage?.freitagSollMinuten ?: 0) % 60) }

    var samstagStd by remember { mutableStateOf((vorlage?.samstagSollMinuten ?: 0) / 60) }
    var samstagMin by remember { mutableStateOf((vorlage?.samstagSollMinuten ?: 0) % 60) }

    var sonntagStd by remember { mutableStateOf((vorlage?.sonntagSollMinuten ?: 0) / 60) }
    var sonntagMin by remember { mutableStateOf((vorlage?.sonntagSollMinuten ?: 0) % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vorlage == null) "Neues Profil" else "Profil bearbeiten") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profil-Name") },
                    placeholder = { Text("z.B. Ferienbetreuung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                // Soll-Zeiten pro Tag
                Text(
                    "Soll-Arbeitszeiten pro Tag",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Montag
                DayTimeInput2("Montag", montagStd, montagMin,
                    onStdChange = { montagStd = it },
                    onMinChange = { montagMin = it }
                )

                // Dienstag
                DayTimeInput2("Dienstag", dienstagStd, dienstagMin,
                    onStdChange = { dienstagStd = it },
                    onMinChange = { dienstagMin = it }
                )

                // Mittwoch
                DayTimeInput2("Mittwoch", mittwochStd, mittwochMin,
                    onStdChange = { mittwochStd = it },
                    onMinChange = { mittwochMin = it }
                )

                // Donnerstag
                DayTimeInput2("Donnerstag", donnerstagStd, donnerstagMin,
                    onStdChange = { donnerstagStd = it },
                    onMinChange = { donnerstagMin = it }
                )

                // Freitag
                DayTimeInput2("Freitag", freitagStd, freitagMin,
                    onStdChange = { freitagStd = it },
                    onMinChange = { freitagMin = it }
                )

                // Samstag
                DayTimeInput2("Samstag", samstagStd, samstagMin,
                    onStdChange = { samstagStd = it },
                    onMinChange = { samstagMin = it }
                )

                // Sonntag
                DayTimeInput2("Sonntag", sonntagStd, sonntagMin,
                    onStdChange = { sonntagStd = it },
                    onMinChange = { sonntagMin = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newVorlage = SollZeitVorlage(
                        id = vorlage?.id ?: 0,
                        name = name,
                        montagSollMinuten = montagStd * 60 + montagMin,
                        dienstagSollMinuten = dienstagStd * 60 + dienstagMin,
                        mittwochSollMinuten = mittwochStd * 60 + mittwochMin,
                        donnerstagSollMinuten = donnerstagStd * 60 + donnerstagMin,
                        freitagSollMinuten = freitagStd * 60 + freitagMin,
                        samstagSollMinuten = samstagStd * 60 + samstagMin,
                        sonntagSollMinuten = sonntagStd * 60 + sonntagMin,
                        isDefault = vorlage?.isDefault ?: false
                    )
                    onSave(newVorlage)
                },
                enabled = name.isNotBlank()
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

// Vereinfachte Tageseingabe ohne Auto-Checkbox
@Composable
private fun DayTimeInput2(
    dayName: String,
    std: Int,
    min: Int,
    onStdChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            dayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )

        OutlinedTextField(
            value = std.toString(),
            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) onStdChange(it.toIntOrNull() ?: 0) },
            label = { Text("Std") },
            modifier = Modifier.width(80.dp),
            singleLine = true
        )
        Text(":", modifier = Modifier.padding(horizontal = 4.dp))
        OutlinedTextField(
            value = String.format("%02d", min),
            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) onMinChange(it.toIntOrNull() ?: 0) },
            label = { Text("Min") },
            modifier = Modifier.width(80.dp),
            singleLine = true
        )
    }
}