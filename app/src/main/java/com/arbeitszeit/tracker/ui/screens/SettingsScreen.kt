package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToGeofencing: () -> Unit = {}
) {
    val settings by viewModel.userSettings.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Stammdaten", "Arbeitszeit", "Sollzeiten", "Erweitert")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> StammdatenTab(viewModel, settings)
            1 -> ArbeitszeitTab(viewModel, settings, onNavigateToGeofencing)
            2 -> SollzeitenTab(viewModel, settings)
            3 -> ErweitertTab(viewModel)
        }
    }
}

@Composable
private fun StammdatenTab(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?
) {
    var name by remember { mutableStateOf(settings?.name ?: "") }
    var einrichtung by remember { mutableStateOf(settings?.einrichtung ?: "") }

    LaunchedEffect(settings) {
        settings?.let {
            name = it.name
            einrichtung = it.einrichtung
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Persönliche Daten",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = einrichtung,
            onValueChange = { einrichtung = it },
            label = { Text("Einrichtung") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.updateStammdaten(
                    name = name,
                    einrichtung = einrichtung
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }
    }
}

@Composable
private fun ArbeitszeitTab(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onNavigateToGeofencing: () -> Unit
) {
    var prozent by remember { mutableStateOf(settings?.arbeitsumfangProzent?.toString() ?: "100") }
    var stunden by remember { mutableStateOf("40") }
    var minuten by remember { mutableStateOf("00") }
    var arbeitsTage by remember { mutableStateOf(settings?.arbeitsTageProWoche?.toString() ?: "5") }
    var ferienbetreuung by remember { mutableStateOf(settings?.ferienbetreuung ?: false) }
    var ersterMontag by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        settings?.let {
            prozent = it.arbeitsumfangProzent.toString()
            stunden = (it.wochenStundenMinuten / 60).toString()
            minuten = (it.wochenStundenMinuten % 60).toString().padStart(2, '0')
            arbeitsTage = it.arbeitsTageProWoche.toString()
            ferienbetreuung = it.ferienbetreuung

            it.ersterMontagImJahr?.let { datum ->
                val parts = datum.split("-")
                if (parts.size == 3) {
                    ersterMontag = "${parts[2]}.${parts[1]}.${parts[0]}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Arbeitszeiteinstellungen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = prozent,
            onValueChange = { prozent = it },
            label = { Text("Arbeitsumfang (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Wochenstunden", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stunden,
                onValueChange = { stunden = it },
                label = { Text("Stunden") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minuten,
                onValueChange = { minuten = it },
                label = { Text("Minuten") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = arbeitsTage,
            onValueChange = { arbeitsTage = it },
            label = { Text("Arbeitstage/Woche") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Ferienbetreuung")
            Spacer(Modifier.weight(1f))
            Switch(checked = ferienbetreuung, onCheckedChange = { ferienbetreuung = it })
        }

        Divider()

        Text("Kalenderwochenberechnung", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = ersterMontag,
            onValueChange = { ersterMontag = it },
            label = { Text("Erster Montag im Jahr") },
            placeholder = { Text("z.B. 06.01.2025") },
            supportingText = { Text("Format: TT.MM.JJJJ - Leer lassen für ISO 8601") },
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        // Geofencing / Automatische Zeiterfassung
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Automatische Zeiterfassung",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Starte/beende Arbeitszeit automatisch basierend auf deinem Standort",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(onClick = onNavigateToGeofencing) {
                    Text("Konfigurieren")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val wochenMinuten = (stunden.toIntOrNull() ?: 0) * 60 + (minuten.toIntOrNull() ?: 0)

                val ersterMontagFormatted = if (ersterMontag.isNotBlank()) {
                    val parts = ersterMontag.split(".")
                    if (parts.size == 3) {
                        val tag = parts[0].padStart(2, '0')
                        val monat = parts[1].padStart(2, '0')
                        val jahr = parts[2]
                        "$jahr-$monat-$tag"
                    } else null
                } else null

                viewModel.updateArbeitszeit(
                    arbeitsumfangProzent = prozent.toIntOrNull() ?: 100,
                    wochenStundenMinuten = wochenMinuten,
                    arbeitsTageProWoche = arbeitsTage.toIntOrNull() ?: 5,
                    ferienbetreuung = ferienbetreuung,
                    ersterMontagImJahr = ersterMontagFormatted
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }
    }
}

@Composable
private fun SollzeitenTab(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?
) {
    var useIndividualDays by remember { mutableStateOf(false) }
    var montagH by remember { mutableStateOf("") }
    var montagM by remember { mutableStateOf("") }
    var dienstagH by remember { mutableStateOf("") }
    var dienstagM by remember { mutableStateOf("") }
    var mittwochH by remember { mutableStateOf("") }
    var mittwochM by remember { mutableStateOf("") }
    var donnerstagH by remember { mutableStateOf("") }
    var donnerstagM by remember { mutableStateOf("") }
    var freitagH by remember { mutableStateOf("") }
    var freitagM by remember { mutableStateOf("") }
    var samstagH by remember { mutableStateOf("") }
    var samstagM by remember { mutableStateOf("") }
    var sonntagH by remember { mutableStateOf("") }
    var sonntagM by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        settings?.let {
            useIndividualDays = it.hasIndividualDailyHours()

            it.montagSollMinuten?.let { min ->
                montagH = (min / 60).toString()
                montagM = (min % 60).toString().padStart(2, '0')
            }
            it.dienstagSollMinuten?.let { min ->
                dienstagH = (min / 60).toString()
                dienstagM = (min % 60).toString().padStart(2, '0')
            }
            it.mittwochSollMinuten?.let { min ->
                mittwochH = (min / 60).toString()
                mittwochM = (min % 60).toString().padStart(2, '0')
            }
            it.donnerstagSollMinuten?.let { min ->
                donnerstagH = (min / 60).toString()
                donnerstagM = (min % 60).toString().padStart(2, '0')
            }
            it.freitagSollMinuten?.let { min ->
                freitagH = (min / 60).toString()
                freitagM = (min % 60).toString().padStart(2, '0')
            }
            it.samstagSollMinuten?.let { min ->
                samstagH = (min / 60).toString()
                samstagM = (min % 60).toString().padStart(2, '0')
            }
            it.sonntagSollMinuten?.let { min ->
                sonntagH = (min / 60).toString()
                sonntagM = (min % 60).toString().padStart(2, '0')
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Individuelle Soll-Zeiten pro Tag",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Leer lassen für Standardberechnung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useIndividualDays,
                onCheckedChange = { useIndividualDays = it }
            )
        }

        if (useIndividualDays) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayTimeInput("Montag", montagH, montagM, { montagH = it }, { montagM = it })
                    DayTimeInput("Dienstag", dienstagH, dienstagM, { dienstagH = it }, { dienstagM = it })
                    DayTimeInput("Mittwoch", mittwochH, mittwochM, { mittwochH = it }, { mittwochM = it })
                    DayTimeInput("Donnerstag", donnerstagH, donnerstagM, { donnerstagH = it }, { donnerstagM = it })
                    DayTimeInput("Freitag", freitagH, freitagM, { freitagH = it }, { freitagM = it })
                    DayTimeInput("Samstag", samstagH, samstagM, { samstagH = it }, { samstagM = it })
                    DayTimeInput("Sonntag", sonntagH, sonntagM, { sonntagH = it }, { sonntagM = it })
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val montagMin = if (useIndividualDays && montagH.isNotBlank()) {
                    (montagH.toIntOrNull() ?: 0) * 60 + (montagM.toIntOrNull() ?: 0)
                } else null

                val dienstagMin = if (useIndividualDays && dienstagH.isNotBlank()) {
                    (dienstagH.toIntOrNull() ?: 0) * 60 + (dienstagM.toIntOrNull() ?: 0)
                } else null

                val mittwochMin = if (useIndividualDays && mittwochH.isNotBlank()) {
                    (mittwochH.toIntOrNull() ?: 0) * 60 + (mittwochM.toIntOrNull() ?: 0)
                } else null

                val donnerstagMin = if (useIndividualDays && donnerstagH.isNotBlank()) {
                    (donnerstagH.toIntOrNull() ?: 0) * 60 + (donnerstagM.toIntOrNull() ?: 0)
                } else null

                val freitagMin = if (useIndividualDays && freitagH.isNotBlank()) {
                    (freitagH.toIntOrNull() ?: 0) * 60 + (freitagM.toIntOrNull() ?: 0)
                } else null

                val samstagMin = if (useIndividualDays && samstagH.isNotBlank()) {
                    (samstagH.toIntOrNull() ?: 0) * 60 + (samstagM.toIntOrNull() ?: 0)
                } else null

                val sonntagMin = if (useIndividualDays && sonntagH.isNotBlank()) {
                    (sonntagH.toIntOrNull() ?: 0) * 60 + (sonntagM.toIntOrNull() ?: 0)
                } else null

                viewModel.updateSollzeiten(
                    montagSollMinuten = montagMin,
                    dienstagSollMinuten = dienstagMin,
                    mittwochSollMinuten = mittwochMin,
                    donnerstagSollMinuten = donnerstagMin,
                    freitagSollMinuten = freitagMin,
                    samstagSollMinuten = samstagMin,
                    sonntagSollMinuten = sonntagMin
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = useIndividualDays
        ) {
            Text("Speichern")
        }
    }
}

@Composable
private fun ErweitertTab(
    viewModel: SettingsViewModel
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Erweiterte Einstellungen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.weight(1f))

        // Gefahrenbereich
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Gefahrenbereich",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Diese Aktion kann nicht rückgängig gemacht werden!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Alle Daten löschen")
                }
            }
        }
    }

    // Bestätigungsdialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Alle Daten löschen?") },
            text = { Text("Möchtest du wirklich ALLE Zeiteinträge und Einstellungen löschen? Diese Aktion kann nicht rückgängig gemacht werden!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllTimeEntries()
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

@Composable
private fun DayTimeInput(
    dayName: String,
    hours: String,
    minutes: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = dayName,
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = hours,
            onValueChange = onHoursChange,
            label = { Text("h") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        Text(":", style = MaterialTheme.typography.bodyLarge)

        OutlinedTextField(
            value = minutes,
            onValueChange = onMinutesChange,
            label = { Text("m") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}
