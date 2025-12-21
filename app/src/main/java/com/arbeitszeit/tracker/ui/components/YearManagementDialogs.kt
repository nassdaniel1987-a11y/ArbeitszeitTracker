package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.YearSettings

/**
 * Dialog zum Bearbeiten eines Jahres
 */
@Composable
fun EditYearDialog(
    year: YearSettings,
    onDismiss: () -> Unit,
    onSave: (ersterMontag: String, urlaubsanspruch: Int, vorjahresUebertrag: Int) -> Unit
) {
    var ersterMontag by remember { mutableStateOf("") }
    var urlaubsanspruch by remember { mutableStateOf(year.urlaubsanspruchTage.toString()) }
    var ueberstundenStunden by remember { mutableStateOf("0") }
    var ueberstundenMinuten by remember { mutableStateOf("00") }

    // Initialisiere Werte
    LaunchedEffect(year) {
        // Format: yyyy-MM-dd → dd.MM.yyyy
        val parts = year.ersterMontagImJahr.split("-")
        if (parts.size == 3) {
            ersterMontag = "${parts[2]}.${parts[1]}.${parts[0]}"
        }

        // Vorjahresübertrag in Stunden:Minuten
        val isNegative = year.vorjahresUebertragMinuten < 0
        val absMinuten = kotlin.math.abs(year.vorjahresUebertragMinuten)
        val hours = absMinuten / 60
        val mins = absMinuten % 60
        ueberstundenStunden = if (isNegative) "-$hours" else "$hours"
        ueberstundenMinuten = mins.toString().padStart(2, '0')
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Jahr ${year.year} bearbeiten") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Bearbeite die Einstellungen für das Jahr ${year.year}:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = ersterMontag,
                    onValueChange = { ersterMontag = it },
                    label = { Text("Erster Montag") },
                    placeholder = { Text("TT.MM.JJJJ") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = urlaubsanspruch,
                    onValueChange = { urlaubsanspruch = it },
                    label = { Text("Urlaubsanspruch (Tage)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.BeachAccess, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Vorjahresübertrag (Überstunden):",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ueberstundenStunden,
                        onValueChange = { ueberstundenStunden = it },
                        label = { Text("Stunden") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ueberstundenMinuten,
                        onValueChange = { ueberstundenMinuten = it },
                        label = { Text("Minuten") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Format: dd.MM.yyyy → yyyy-MM-dd
                    val ersterMontagFormatted = ersterMontag.trim().let { datum ->
                        if (datum.isNotBlank()) {
                            val parts = datum.split(".")
                            if (parts.size == 3) {
                                val tag = parts[0].padStart(2, '0')
                                val monat = parts[1].padStart(2, '0')
                                val jahr = parts[2]
                                "$jahr-$monat-$tag"
                            } else year.ersterMontagImJahr
                        } else year.ersterMontagImJahr
                    }

                    // Überstunden in Minuten umrechnen
                    val stunden = ueberstundenStunden.toIntOrNull() ?: 0
                    val minuten = ueberstundenMinuten.toIntOrNull() ?: 0
                    val vorjahresUebertragMinuten = (stunden * 60) + minuten

                    onSave(
                        ersterMontagFormatted,
                        urlaubsanspruch.toIntOrNull() ?: year.urlaubsanspruchTage,
                        vorjahresUebertragMinuten
                    )
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
 * Bestätigungs-Dialog zum Löschen eines Jahres
 */
@Composable
fun DeleteYearConfirmDialog(
    year: YearSettings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Jahr ${year.year} löschen?") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Möchtest du wirklich das Jahr ${year.year} löschen?",
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "ℹ️ Hinweis:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "• Zeiteinträge bleiben erhalten\n" +
                            "• Du kannst sie später wieder einem Jahr zuordnen\n" +
                            "• Diese Aktion kann nicht rückgängig gemacht werden",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
