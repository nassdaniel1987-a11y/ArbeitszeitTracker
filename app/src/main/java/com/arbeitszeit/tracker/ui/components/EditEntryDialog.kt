package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
    onSave: (startZeit: Int?, endZeit: Int?, pauseMinuten: Int, typ: String, notiz: String, urlaubsJahr: Int?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Extrahiere Jahr aus Datum (yyyy-MM-dd)
    val datumJahr = datum.split("-")[0].toInt()

    // Zeit als Minuten speichern
    var startZeitMinuten by remember { mutableStateOf(entry?.startZeit) }
    var endZeitMinuten by remember { mutableStateOf(entry?.endZeit) }

    var pauseMinuten by remember { mutableIntStateOf(entry?.pauseMinuten ?: 0) }
    var selectedTyp by remember { mutableStateOf(entry?.typ ?: TimeEntry.TYP_NORMAL) }
    var notiz by remember { mutableStateOf(entry?.notiz ?: "") }
    var urlaubsJahr by remember { mutableStateOf<Int?>(entry?.urlaubsJahr) }

    // Dialog-States
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPauseSlider by remember { mutableStateOf(false) }

    // Prüfe ob Eintrag Daten hat (zum Anzeigen des Löschen-Buttons)
    val hasData = entry?.let {
        it.startZeit != null || it.endZeit != null || it.pauseMinuten > 0 ||
        it.typ != TimeEntry.TYP_NORMAL || it.notiz.isNotEmpty()
    } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Bearbeiten",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Eintrag bearbeiten")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Datum-Anzeige als Chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Kalender",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            datum,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()

                // Typ-Auswahl
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = "Typ",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Typ:", style = MaterialTheme.typography.labelMedium)
                }
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

                // Jahr-Auswahl für Urlaub (Resturlaub-Feature)
                if (selectedTyp == TimeEntry.TYP_URLAUB) {
                    HorizontalDivider()

                    Text("Urlaubsjahr:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = urlaubsJahr == null,
                            onClick = { urlaubsJahr = null },
                            label = { Text("$datumJahr (aktuell)") }
                        )
                        FilterChip(
                            selected = urlaubsJahr == datumJahr - 1,
                            onClick = { urlaubsJahr = datumJahr - 1 },
                            label = { Text("${datumJahr - 1} (Resturlaub)") }
                        )
                    }
                    Text(
                        text = "Wähle das Jahr, für das dieser Urlaubstag zählen soll",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Zeitfelder immer anzeigen (auch bei Urlaub/Krank/Feiertag)
                HorizontalDivider()

                // Start-Zeit mit TimePicker
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Startzeit:", style = MaterialTheme.typography.labelMedium)
                }
                TimePickerButton(
                    label = "Start",
                    timeMinutes = startZeitMinuten,
                    onTimeSelected = { startZeitMinuten = it },
                    onClear = { startZeitMinuten = null }
                )

                // End-Zeit mit TimePicker
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Ende",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Endzeit:", style = MaterialTheme.typography.labelMedium)
                }
                TimePickerButton(
                    label = "Ende",
                    timeMinutes = endZeitMinuten,
                    onTimeSelected = { endZeitMinuten = it },
                    onClear = { endZeitMinuten = null }
                )

                // Pause mit Slider
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Coffee,
                        contentDescription = "Pause",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text("Pause:", style = MaterialTheme.typography.labelMedium)
                }
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

                HorizontalDivider()

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
                        // Pause wird jetzt bei allen Typen gespeichert
                        onSave(startZeitMinuten, endZeitMinuten, pauseMinuten, selectedTyp, notiz, urlaubsJahr)
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
            timePickerState = timePickerState,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val minutes = timePickerState.hour * 60 + timePickerState.minute
                onTimeSelected(minutes)
                showTimePicker = false
            },
            onQuickTimeSelect = { minutes ->
                onTimeSelected(minutes)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * Dialog für TimePicker mit Quick-Time-Buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    timePickerState: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onQuickTimeSelect: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zeit auswählen")

                // Quick-Time-Buttons
                Text(
                    "Schnellauswahl:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val now = TimeUtils.currentTimeInMinutes()
                            onQuickTimeSelect(now)
                        },
                        label = { Text("Jetzt", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Icon",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = {
                            val time = TimeUtils.currentTimeInMinutes() - 30
                            onQuickTimeSelect(time.coerceAtLeast(0))
                        },
                        label = { Text("-30min", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = {
                            val time = TimeUtils.currentTimeInMinutes() - 60
                            onQuickTimeSelect(time.coerceAtLeast(0))
                        },
                        label = { Text("-1h", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Text(
                    "Oder manuell einstellen:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
                Text(
                    text = "Schnellauswahl:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$minutes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Min",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
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
