package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var startZeitStr by remember {
        mutableStateOf(
            if (entry?.startZeit != null) TimeUtils.minutesToTimeString(entry.startZeit) else ""
        )
    }
    var endZeitStr by remember {
        mutableStateOf(
            if (entry?.endZeit != null) TimeUtils.minutesToTimeString(entry.endZeit) else ""
        )
    }
    var pauseStr by remember {
        mutableStateOf(if (entry?.pauseMinuten ?: 0 > 0) entry!!.pauseMinuten.toString() else "")
    }
    var selectedTyp by remember { mutableStateOf(entry?.typ ?: TimeEntry.TYP_NORMAL) }
    var notiz by remember { mutableStateOf(entry?.notiz ?: "") }

    var showStartError by remember { mutableStateOf(false) }
    var showEndError by remember { mutableStateOf(false) }

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

                    // Start-Zeit
                    OutlinedTextField(
                        value = startZeitStr,
                        onValueChange = {
                            startZeitStr = it
                            showStartError = it.isNotEmpty() && !TimeUtils.isValidTimeString(it)
                        },
                        label = { Text("Start (HH:MM)") },
                        placeholder = { Text("08:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = showStartError,
                        supportingText = if (showStartError) {
                            { Text("Ungültiges Format (HH:MM)") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // End-Zeit
                    OutlinedTextField(
                        value = endZeitStr,
                        onValueChange = {
                            endZeitStr = it
                            showEndError = it.isNotEmpty() && !TimeUtils.isValidTimeString(it)
                        },
                        label = { Text("Ende (HH:MM)") },
                        placeholder = { Text("16:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = showEndError,
                        supportingText = if (showEndError) {
                            { Text("Ungültiges Format (HH:MM)") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
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
                    // Validierung
                    if (selectedTyp == TimeEntry.TYP_NORMAL) {
                        if (startZeitStr.isNotEmpty() && !TimeUtils.isValidTimeString(startZeitStr)) {
                            showStartError = true
                            return@Button
                        }
                        if (endZeitStr.isNotEmpty() && !TimeUtils.isValidTimeString(endZeitStr)) {
                            showEndError = true
                            return@Button
                        }
                    }

                    // Konvertiere Werte
                    val startZeit = if (selectedTyp == TimeEntry.TYP_NORMAL && startZeitStr.isNotEmpty()) {
                        TimeUtils.timeStringToMinutes(startZeitStr)
                    } else null

                    val endZeit = if (selectedTyp == TimeEntry.TYP_NORMAL && endZeitStr.isNotEmpty()) {
                        TimeUtils.timeStringToMinutes(endZeitStr)
                    } else null

                    val pauseMinuten = if (selectedTyp == TimeEntry.TYP_NORMAL) {
                        pauseStr.toIntOrNull() ?: 0
                    } else 0

                    onSave(startZeit, endZeit, pauseMinuten, selectedTyp, notiz)
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
