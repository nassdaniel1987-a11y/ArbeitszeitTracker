package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.utils.TimeUtils

/**
 * Dialog zum Bearbeiten von Zeiteinträgen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryDialog(
    entry: TimeEntry?,
    datum: String,
    onDismiss: () -> Unit,
    onSave: (startZeit: Int?, endZeit: Int?, pauseMinuten: Int, typ: String, notiz: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Zeit als Minuten speichern
    var startZeitMinuten by remember { mutableStateOf(entry?.startZeit) }
    var endZeitMinuten by remember { mutableStateOf(entry?.endZeit) }

    var pauseMinuten by remember { mutableIntStateOf(entry?.pauseMinuten ?: 0) }
    var selectedTyp by remember { mutableStateOf(entry?.typ ?: TimeEntry.TYP_NORMAL) }
    var notiz by remember { mutableStateOf(entry?.notiz ?: "") }

    // Dialog-States für TimePicker
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPauseSlider by remember { mutableStateOf(false) }

    // Prüfe ob Eintrag Daten hat (zum Anzeigen des Löschen-Buttons)
    val hasData = entry?.let {
        it.startZeit != null || it.endZeit != null || it.pauseMinuten > 0 ||
        it.typ != TimeEntry.TYP_NORMAL || it.notiz.isNotEmpty()
    } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eintrag bearbeiten")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Datum: $datum", style = MaterialTheme.typography.bodyMedium)

                Divider()

                // Typ-Auswahl
                Text("Typ:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTyp == TimeEntry.TYP_NORMAL,
                        onClick = { selectedTyp = TimeEntry.TYP_NORMAL },
                        label = { Text("Normal") }
                    )
                    FilterChip(
                        selected = selectedTyp == TimeEntry.TYP_URLAUB,
                        onClick = { selectedTyp = TimeEntry.TYP_URLAUB },
                        label = { Text("U") }
                    )
                    FilterChip(
                        selected = selectedTyp == TimeEntry.TYP_KRANK,
                        onClick = { selectedTyp = TimeEntry.TYP_KRANK },
                        label = { Text("K") }
                    )
                    FilterChip(
                        selected = selectedTyp == TimeEntry.TYP_FEIERTAG,
                        onClick = { selectedTyp = TimeEntry.TYP_FEIERTAG },
                        label = { Text("F") }
                    )
                    FilterChip(
                        selected = selectedTyp == TimeEntry.TYP_ABWESEND,
                        onClick = { selectedTyp = TimeEntry.TYP_ABWESEND },
                        label = { Text("AB") }
                    )
                }

                // Zeitfelder nur bei NORMAL anzeigen
                if (selectedTyp == TimeEntry.TYP_NORMAL) {
                    Divider()

                    // Start-Zeit mit TimePicker
                    Text("Startzeit:", style = MaterialTheme.typography.labelMedium)
                    TimePickerButton(
                        label = "Start",
                        timeMinutes = startZeitMinuten,
                        onTimeSelected = { startZeitMinuten = it },
                        onClear = { startZeitMinuten = null }
                    )

                    // End-Zeit mit TimePicker
                    Text("Endzeit:", style = MaterialTheme.typography.labelMedium)
                    TimePickerButton(
                        label = "Ende",
                        timeMinutes = endZeitMinuten,
                        onTimeSelected = { endZeitMinuten = it },
                        onClear = { endZeitMinuten = null }
                    )

                    // Pause mit Slider
                    Text("Pause:", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        onClick = { showPauseSlider = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (pauseMinuten > 0) {
                                "$pauseMinuten Min"
                            } else {
                                "Keine Pause"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Divider()

                // Notiz
                OutlinedTextField(
                    value = notiz,
                    onValueChange = { notiz = it },
                    label = { Text("Notiz (optional)") },
                    placeholder = { Text("Bemerkungen...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Löschen-Button (nur wenn Eintrag Daten hat und onDelete übergeben wurde)
                if (hasData && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Löschen")
                    }
                }

                Button(
                    onClick = {
                        val pause = if (selectedTyp == TimeEntry.TYP_NORMAL) {
                            pauseMinuten
                        } else 0

                        onSave(startZeitMinuten, endZeitMinuten, pause, selectedTyp, notiz)
                    }
                ) {
                    Text("Speichern")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    // Pause Slider Dialog
    if (showPauseSlider) {
        PauseSliderDialog(
            currentPauseMinutes = pauseMinuten,
            onDismiss = { showPauseSlider = false },
            onConfirm = { minutes ->
                pauseMinuten = minutes
                showPauseSlider = false
            }
        )
    }

    // Bestätigungsdialog für Löschen
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eintrag löschen?") },
            text = { Text("Möchtest du diesen Eintrag wirklich löschen? Alle Zeiten und Notizen werden entfernt.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteConfirmDialog = false
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
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Button mit TimePicker für Zeitauswahl
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerButton(
    label: String,
    timeMinutes: Int?,
    onTimeSelected: (Int) -> Unit,
    onClear: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    // TimePicker State
    val timePickerState = rememberTimePickerState(
        initialHour = timeMinutes?.let { it / 60 } ?: 8,
        initialMinute = timeMinutes?.let { it % 60 } ?: 0,
        is24Hour = true
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (timeMinutes != null) {
                    TimeUtils.minutesToTimeString(timeMinutes)
                } else {
                    "--:--"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (timeMinutes != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Löschen")
            }
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val minutes = timePickerState.hour * 60 + timePickerState.minute
                onTimeSelected(minutes)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * Dialog für TimePicker
 */
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        text = { content() }
    )
}

/**
 * Pause Slider Dialog
 */
@Composable
private fun PauseSliderDialog(
    currentPauseMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pauseMinutes by remember { mutableFloatStateOf(currentPauseMinutes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Coffee, "Pause") },
        title = { Text("Pause einstellen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Anzeige der aktuellen Pause
                Text(
                    text = "${pauseMinutes.toInt()} Minuten",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Slider (0-120 Minuten)
                Slider(
                    value = pauseMinutes,
                    onValueChange = { pauseMinutes = it },
                    valueRange = 0f..120f,
                    steps = 23, // Alle 5 Minuten: 0, 5, 10, ..., 120
                    modifier = Modifier.fillMaxWidth()
                )

                // Schnellauswahl-Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 15, 30, 45, 60).forEach { minutes ->
                        OutlinedButton(
                            onClick = { pauseMinutes = minutes.toFloat() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (pauseMinutes.toInt() == minutes) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                        ) {
                            Text("$minutes")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pauseMinutes.toInt()) }) {
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
