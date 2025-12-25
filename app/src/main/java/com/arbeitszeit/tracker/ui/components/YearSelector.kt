package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arbeitszeit.tracker.data.entity.YearSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Jahr-Auswahl Dropdown
 *
 * Zeigt das aktuelle Jahr an und ermöglicht Wechsel zu anderen Jahren
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearSelector(
    activeYear: YearSettings?,
    allYears: List<YearSettings>,
    onYearSelected: (Int) -> Unit,
    onNewYearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Aktuelles Jahr anzeigen
        Card(
            onClick = { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Jahr",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = activeYear?.year?.toString() ?: "Jahr wählen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Dropdown-Menü
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .widthIn(min = 200.dp)
        ) {
            // Überschrift
            Text(
                "Jahr auswählen",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider()

            // Jahre
            allYears.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    year.year.toString(),
                                    fontWeight = if (year.isActive) FontWeight.Bold else FontWeight.Normal
                                )
                                if (year.isCurrentYear()) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = "Aktuelles Jahr",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (year.isActive) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Aktiv",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onYearSelected(year.year)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (year.isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                )
            }

            HorizontalDivider()

            // "Neues Jahr anlegen"
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Neues Jahr anlegen",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = {
                    onNewYearClick()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Neues Jahr",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

/**
 * Dialog zum Anlegen eines neuen Jahres
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewYearDialog(
    year: Int,
    ersterMontagVorschlag: String,
    urlaubsanspruchVorschlag: Int,
    restUrlaubVorjahr: Int,
    ueberstundenVorjahr: Int,
    ueberstundenFormatted: String,
    onDismiss: () -> Unit,
    onCreate: (
        ersterMontag: String,
        urlaubsanspruch: Int,
        uebertragUeberstunden: Boolean,
        uebertragResturlaub: Boolean
    ) -> Unit
) {
    var ersterMontag by remember { mutableStateOf(ersterMontagVorschlag) }
    var urlaubsanspruch by remember { mutableStateOf(urlaubsanspruchVorschlag.toString()) }
    var uebertragUeberstunden by remember { mutableStateOf(true) }
    var uebertragResturlaub by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Überschrift
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Jahr $year anlegen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Erster Montag
                OutlinedTextField(
                    value = ersterMontag,
                    onValueChange = { ersterMontag = it },
                    label = { Text("Erster Montag im Jahr") },
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = null)
                    },
                    supportingText = {
                        val date = try {
                            LocalDate.parse(ersterMontag)
                        } catch (e: Exception) {
                            null
                        }
                        Text(
                            date?.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy"))
                                ?: "Format: yyyy-MM-dd"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Urlaubsanspruch
                OutlinedTextField(
                    value = urlaubsanspruch,
                    onValueChange = { urlaubsanspruch = it.filter { char -> char.isDigit() } },
                    label = { Text("Urlaubsanspruch (Tage)") },
                    leadingIcon = {
                        Icon(Icons.Default.BeachAccess, contentDescription = null)
                    },
                    supportingText = {
                        if (uebertragResturlaub && restUrlaubVorjahr > 0) {
                            Text("Enthält $restUrlaubVorjahr Tage Resturlaub aus ${year - 1}")
                        } else {
                            Text("Standard: 30 Tage")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                // Überstunden übertragen
                if (ueberstundenVorjahr != 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uebertragUeberstunden) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier.clickable {
                            uebertragUeberstunden = !uebertragUeberstunden
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Überstunden übertragen",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Aus ${year - 1}: $ueberstundenFormatted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (ueberstundenVorjahr > 0) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            Checkbox(
                                checked = uebertragUeberstunden,
                                onCheckedChange = { uebertragUeberstunden = it }
                            )
                        }
                    }
                }

                // Resturlaub übertragen
                if (restUrlaubVorjahr > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uebertragResturlaub) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier.clickable {
                            uebertragResturlaub = !uebertragResturlaub
                            // Urlaubsanspruch anpassen
                            val base = urlaubsanspruchVorschlag
                            urlaubsanspruch = if (uebertragResturlaub) {
                                (base + restUrlaubVorjahr).toString()
                            } else {
                                base.toString()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Resturlaub übertragen",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Aus ${year - 1}: $restUrlaubVorjahr Tage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Checkbox(
                                checked = uebertragResturlaub,
                                onCheckedChange = {
                                    uebertragResturlaub = it
                                    val base = urlaubsanspruchVorschlag
                                    urlaubsanspruch = if (it) {
                                        (base + restUrlaubVorjahr).toString()
                                    } else {
                                        base.toString()
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            val finalUrlaubsanspruch = urlaubsanspruch.toIntOrNull() ?: urlaubsanspruchVorschlag
                            onCreate(
                                ersterMontag,
                                finalUrlaubsanspruch,
                                uebertragUeberstunden,
                                uebertragResturlaub
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Jahr anlegen")
                    }
                }
            }
        }
    }
}
