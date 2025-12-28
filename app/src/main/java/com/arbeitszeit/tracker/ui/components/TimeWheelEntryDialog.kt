package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arbeitszeit.tracker.data.entity.TimeEntry

/**
 * Moderner Entry-Dialog mit Time Wheel Picker
 *
 * Verwendet das elegante Time Wheel für intuitive Zeitauswahl
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeWheelEntryDialog(
    entry: TimeEntry?,
    datum: String,
    onDismiss: () -> Unit,
    onSave: (startZeit: Int?, endZeit: Int?, pauseMinuten: Int, typ: String, notiz: String, urlaubsJahr: Int?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Extrahiere Jahr aus Datum (yyyy-MM-dd)
    val datumJahr = datum.split("-")[0].toInt()

    // States
    var startZeitMinuten by remember { mutableStateOf(entry?.startZeit) }
    var endZeitMinuten by remember { mutableStateOf(entry?.endZeit) }
    var pauseMinuten by remember { mutableIntStateOf(entry?.pauseMinuten ?: 30) }
    var selectedTyp by remember { mutableStateOf(entry?.typ ?: TimeEntry.TYP_NORMAL) }
    var notiz by remember { mutableStateOf(entry?.notiz ?: "") }
    var urlaubsJahr by remember { mutableStateOf<Int?>(entry?.urlaubsJahr) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Prüfe ob Eintrag Daten hat
    val hasData = entry?.let {
        it.startZeit != null || it.endZeit != null || it.pauseMinuten > 0 ||
        it.typ != TimeEntry.TYP_NORMAL || it.notiz.isNotEmpty()
    } ?: false

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Eintrag bearbeiten",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = datum,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Schließen",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Typ-Auswahl (Modern mit größeren Cards)
                    Text(
                        text = "Typ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeCard(
                            label = "Normal",
                            abbreviation = "N",
                            isSelected = selectedTyp == TimeEntry.TYP_NORMAL,
                            onClick = { selectedTyp = TimeEntry.TYP_NORMAL },
                            modifier = Modifier.weight(1f)
                        )
                        TypeCard(
                            label = "Urlaub",
                            abbreviation = "U",
                            isSelected = selectedTyp == TimeEntry.TYP_URLAUB,
                            onClick = { selectedTyp = TimeEntry.TYP_URLAUB },
                            modifier = Modifier.weight(1f)
                        )
                        TypeCard(
                            label = "Krank",
                            abbreviation = "K",
                            isSelected = selectedTyp == TimeEntry.TYP_KRANK,
                            onClick = { selectedTyp = TimeEntry.TYP_KRANK },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeCard(
                            label = "Feiertag",
                            abbreviation = "F",
                            isSelected = selectedTyp == TimeEntry.TYP_FEIERTAG,
                            onClick = { selectedTyp = TimeEntry.TYP_FEIERTAG },
                            modifier = Modifier.weight(1f)
                        )
                        TypeCard(
                            label = "Abwesend",
                            abbreviation = "AB",
                            isSelected = selectedTyp == TimeEntry.TYP_ABWESEND,
                            onClick = { selectedTyp = TimeEntry.TYP_ABWESEND },
                            modifier = Modifier.weight(1f)
                        )
                        // Placeholder für symmetrie
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Urlaubsjahr-Auswahl
                    if (selectedTyp == TimeEntry.TYP_URLAUB) {
                        HorizontalDivider()

                        Text(
                            text = "Urlaubsjahr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = urlaubsJahr == null,
                                onClick = { urlaubsJahr = null },
                                label = { Text("$datumJahr (aktuell)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = urlaubsJahr == datumJahr - 1,
                                onClick = { urlaubsJahr = datumJahr - 1 },
                                label = { Text("${datumJahr - 1} (Resturlaub)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Wähle das Jahr, für das dieser Urlaubstag zählen soll",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Zeit-Eingabe nur bei Normal-Typ
                    if (selectedTyp == TimeEntry.TYP_NORMAL) {
                        HorizontalDivider()

                        Text(
                            text = "Arbeitszeit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TimeWheelPicker(
                            startTime = startZeitMinuten,
                            endTime = endZeitMinuten,
                            pauseMinuten = pauseMinuten,
                            onStartTimeChange = { startZeitMinuten = it },
                            onEndTimeChange = { endZeitMinuten = it },
                            onPauseChange = { pauseMinuten = it }
                        )
                    }

                    HorizontalDivider()

                    // Notiz
                    Text(
                        text = "Notiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = notiz,
                        onValueChange = { notiz = it },
                        label = { Text("Bemerkungen (optional)") },
                        placeholder = { Text("z.B. Projektname, Termin, etc.") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )
                }

                // Footer Buttons
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Löschen-Button
                    if (hasData && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Löschen")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Speichern-Button
                    Button(
                        onClick = {
                            onSave(
                                startZeitMinuten,
                                endZeitMinuten,
                                pauseMinuten,
                                selectedTyp,
                                notiz,
                                urlaubsJahr
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
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
 * Typ-Karte für moderne Typ-Auswahl
 */
@Composable
private fun TypeCard(
    label: String,
    abbreviation: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
        }
    }
}
