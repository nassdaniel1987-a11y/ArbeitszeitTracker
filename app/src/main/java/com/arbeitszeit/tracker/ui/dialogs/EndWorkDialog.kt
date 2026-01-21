package com.arbeitszeit.tracker.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.autostart.RunningTimeState
import java.time.format.DateTimeFormatter

/**
 * Dialog zum Beenden der Arbeitszeiterfassung
 *
 * Features:
 * - Zeigt Start, Ende, Dauer an
 * - Pause hinzufügen (Checkbox + Eingabe)
 * - Speichern oder Verwerfen
 * - Zeigt Überstunden-Differenz
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndWorkDialog(
    runningState: RunningTimeState,
    defaultPauseMinutes: Int,
    expectedWorkMinutes: Int,
    onSave: (pauseMinutes: Int) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    var addPause by remember { mutableStateOf(true) }
    var pauseMinutes by remember { mutableStateOf(defaultPauseMinutes.toString()) }

    val endTime = runningState.calculateEndTime()
    val totalMinutes = runningState.calculateDurationMinutes()
    val pauseMin = if (addPause) pauseMinutes.toIntOrNull() ?: 0 else 0
    val actualWorkMinutes = totalMinutes - pauseMin
    val differenceMinutes = actualWorkMinutes - expectedWorkMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (runningState.isAutoStart) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = if (runningState.isAutoStart) "Auto-Start" else "Arbeitszeit beenden"
            )
        },
        title = {
            Text("Arbeitszeit beenden")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info: Auto-Start oder Manuell
                if (runningState.isAutoStart) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoMode,
                                contentDescription = "Automatischer Start",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Automatisch gestartet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Zeiten anzeigen
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRow(
                            label = "Start:",
                            value = runningState.getStartTimeFormatted(),
                            icon = Icons.Default.PlayArrow
                        )

                        TimeRow(
                            label = "Ende:",
                            value = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            icon = Icons.Default.Stop
                        )

                        HorizontalDivider()

                        TimeRow(
                            label = "Dauer:",
                            value = runningState.getDurationFormatted(),
                            icon = Icons.Default.Timer,
                            emphasized = true
                        )
                    }
                }

                // Soll-Zeit und Differenz
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (differenceMinutes >= 0)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRow(
                            label = "Soll:",
                            value = formatMinutes(expectedWorkMinutes),
                            icon = Icons.Default.AccessTime
                        )

                        if (addPause && pauseMin > 0) {
                            TimeRow(
                                label = "Pause:",
                                value = formatMinutes(pauseMin),
                                icon = Icons.Default.FreeBreakfast
                            )
                        }

                        HorizontalDivider()

                        TimeRow(
                            label = "Differenz:",
                            value = "${if (differenceMinutes >= 0) "+" else ""}${formatMinutes(differenceMinutes)}",
                            icon = if (differenceMinutes >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            emphasized = true
                        )
                    }
                }

                // Pause-Checkbox und Eingabe
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = addPause,
                            onCheckedChange = { addPause = it }
                        )
                        Text(
                            "Pause hinzufügen",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (addPause) {
                        OutlinedTextField(
                            value = pauseMinutes,
                            onValueChange = { pauseMinutes = it },
                            label = { Text("Pause (Minuten)") },
                            leadingIcon = { Icon(Icons.Default.FreeBreakfast, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(pauseMin) }
            ) {
                Icon(Icons.Default.Save, contentDescription = "Speichern")
                Spacer(Modifier.width(8.dp))
                Text("Speichern")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Verwerfen")
                    Spacer(Modifier.width(4.dp))
                    Text("Nicht eintragen")
                }

                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}

/**
 * Zeile für Zeit-Information
 */
@Composable
private fun TimeRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = if (emphasized)
                    MaterialTheme.typography.titleMedium
                else
                    MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            value,
            style = if (emphasized)
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * Formatiert Minuten zu "Xh YYmin" Format
 */
private fun formatMinutes(totalMinutes: Int): String {
    val isNegative = totalMinutes < 0
    val absMinutes = kotlin.math.abs(totalMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    val sign = if (isNegative) "-" else ""
    return "$sign${hours}h ${minutes.toString().padStart(2, '0')}min"
}
