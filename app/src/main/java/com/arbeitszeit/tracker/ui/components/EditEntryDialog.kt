package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
    onSave: (startZeit: Int?, endZeit: Int?, pauseMinuten: Int, typ: String, notiz: String) -> Unit
) {
    // Zeit als Minuten speichern
    var startZeitMinuten by remember { mutableStateOf(entry?.startZeit) }
    var endZeitMinuten by remember { mutableStateOf(entry?.endZeit) }

    var pauseStr by remember {
        mutableStateOf(if (entry?.pauseMinuten ?: 0 > 0) entry!!.pauseMinuten.toString() else "")
    }
    var selectedTyp by remember { mutableStateOf(entry?.typ ?: TimeEntry.TYP_NORMAL) }
    var notiz by remember { mutableStateOf(entry?.notiz ?: "") }

    // Dialog-States für TimePicker
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

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

                    // Pause
                    OutlinedTextField(
                        value = pauseStr,
                        onValueChange = { pauseStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Pause (Minuten)") },
                        placeholder = { Text("30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
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
            Button(
                onClick = {
                    val pauseMinuten = if (selectedTyp == TimeEntry.TYP_NORMAL) {
                        pauseStr.toIntOrNull() ?: 0
                    } else 0

                    onSave(startZeitMinuten, endZeitMinuten, pauseMinuten, selectedTyp, notiz)
                }
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
