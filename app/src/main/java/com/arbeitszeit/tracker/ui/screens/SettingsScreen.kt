package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToGeofencing: () -> Unit = {},
    onNavigateToTemplateManagement: () -> Unit = {}
) {
    val settings by viewModel.userSettings.collectAsState()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Wenn eine Unterseite ausgewählt ist, zeige diese an
    selectedSection?.let { section ->
        SettingsDetailScreen(
            section = section,
            viewModel = viewModel,
            settings = settings,
            snackbarHostState = snackbarHostState,
            onNavigateBack = { selectedSection = null },
            onNavigateToGeofencing = onNavigateToGeofencing,
            onNavigateToTemplateManagement = onNavigateToTemplateManagement
        )
        return
    }

    // Hauptmenü (Android Settings-Style)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Persönliche Daten
            item {
                SettingsMenuSection(title = "Allgemein")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Person,
                    title = "Persönliche Daten",
                    subtitle = "${settings?.name ?: "Nicht gesetzt"}",
                    onClick = { selectedSection = SettingsSection.PERSONAL_DATA }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = when (settings?.darkMode) {
                        "dark" -> "Dunkel"
                        "light" -> "Hell"
                        else -> "System"
                    },
                    onClick = { selectedSection = SettingsSection.DARK_MODE }
                )
            }

            // Arbeitszeit
            item {
                SettingsMenuSection(title = "Arbeitszeit")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Schedule,
                    title = "Arbeitszeit-Einstellungen",
                    subtitle = "${settings?.arbeitsumfangProzent ?: 100}% • ${settings?.arbeitsTageProWoche ?: 5} Tage/Woche",
                    onClick = { selectedSection = SettingsSection.WORK_TIME }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.CalendarToday,
                    title = "Individuelle Sollzeiten",
                    subtitle = if (settings?.hasIndividualDailyHours() == true) "Aktiv" else "Standard",
                    onClick = { selectedSection = SettingsSection.DAILY_HOURS }
                )
            }

            // Automatisierung
            item {
                SettingsMenuSection(title = "Automatisierung")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Geofencing",
                    subtitle = if (settings?.geofencingEnabled == true) "Aktiviert" else "Deaktiviert",
                    onClick = onNavigateToGeofencing
                )
            }

            // Daten & Vorlagen
            item {
                SettingsMenuSection(title = "Daten & Vorlagen")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Description,
                    title = "Excel-Vorlagen",
                    subtitle = "Vorlagen verwalten",
                    onClick = onNavigateToTemplateManagement
                )
            }

            // Gefährliche Aktionen
            item {
                SettingsMenuSection(title = "Gefahrenbereich")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Delete,
                    title = "Alle Daten löschen",
                    subtitle = "Nicht rückgängig machbar",
                    onClick = { selectedSection = SettingsSection.DELETE_DATA },
                    dangerous = true
                )
            }

            // Abstand am Ende
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// Settings Sections Enum
enum class SettingsSection {
    PERSONAL_DATA,
    DARK_MODE,
    WORK_TIME,
    DAILY_HOURS,
    DELETE_DATA
}

/**
 * Android-Style Settings Menu Item
 */
@Composable
private fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dangerous: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (dangerous) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (dangerous) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

/**
 * Section Header
 */
@Composable
private fun SettingsMenuSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        fontWeight = FontWeight.Bold
    )
}

/**
 * Detail Screen für einzelne Settings-Bereiche
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailScreen(
    section: SettingsSection,
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNavigateToGeofencing: () -> Unit,
    onNavigateToTemplateManagement: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (section) {
                            SettingsSection.PERSONAL_DATA -> "Persönliche Daten"
                            SettingsSection.DARK_MODE -> "Dark Mode"
                            SettingsSection.WORK_TIME -> "Arbeitszeit"
                            SettingsSection.DAILY_HOURS -> "Sollzeiten"
                            SettingsSection.DELETE_DATA -> "Daten löschen"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (section) {
                SettingsSection.PERSONAL_DATA -> PersonalDataSection(viewModel, settings, snackbarHostState)
                SettingsSection.DARK_MODE -> DarkModeSection(viewModel, settings, snackbarHostState)
                SettingsSection.WORK_TIME -> WorkTimeSection(viewModel, settings, snackbarHostState)
                SettingsSection.DAILY_HOURS -> DailyHoursSection(viewModel, settings, snackbarHostState)
                SettingsSection.DELETE_DATA -> DeleteDataSection(viewModel, snackbarHostState, onNavigateBack)
            }
        }
    }
}

/**
 * Persönliche Daten Section
 */
@Composable
private fun PersonalDataSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    var name by remember { mutableStateOf(settings?.name ?: "") }
    var einrichtung by remember { mutableStateOf(settings?.einrichtung ?: "") }
    val scope = rememberCoroutineScope()

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
                viewModel.updateStammdaten(name = name, einrichtung = einrichtung)
                scope.launch {
                    snackbarHostState.showSnackbar("Gespeichert")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }
    }
}

/**
 * Dark Mode Section
 */
@Composable
private fun DarkModeSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.arbeitszeit.tracker.ui.components.DarkModeCard(
            settings = settings,
            viewModel = viewModel
        )
    }
}

/**
 * Work Time Section
 */
@Composable
private fun WorkTimeSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    ArbeitszeitTab(viewModel, settings, snackbarHostState)
}

/**
 * Daily Hours Section
 */
@Composable
private fun DailyHoursSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
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
    val scope = rememberCoroutineScope()

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Individuelle Soll-Zeiten pro Tag",
                    style = MaterialTheme.typography.titleSmall,
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
                scope.launch {
                    snackbarHostState.showSnackbar("Sollzeiten gespeichert")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = useIndividualDays
        ) {
            Text("Speichern")
        }
    }
}

/**
 * Delete Data Section
 */
@Composable
private fun DeleteDataSection(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    "Achtung!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    "Diese Aktion löscht ALLE deine Zeiteinträge und kann nicht rückgängig gemacht werden!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Alle Daten löschen")
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Wirklich löschen?") },
            text = { Text("Alle Zeiteinträge werden unwiderruflich gelöscht!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllTimeEntries()
                        showConfirmDialog = false
                        onNavigateBack()
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
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun AllgemeinTab(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState,
    onNavigateToTemplateManagement: () -> Unit
) {
    var name by remember { mutableStateOf(settings?.name ?: "") }
    var einrichtung by remember { mutableStateOf(settings?.einrichtung ?: "") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        // Persönliche Daten
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

        Button(
            onClick = {
                viewModel.updateStammdaten(
                    name = name,
                    einrichtung = einrichtung
                )
                scope.launch {
                    snackbarHostState.showSnackbar("Stammdaten erfolgreich gespeichert")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // UI Einstellungen
        Text(
            "Benutzeroberfläche",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Dark Mode
        DarkModeCard(settings = settings, viewModel = viewModel)

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Daten & Vorlagen
        Text(
            "Daten & Vorlagen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Excel-Vorlagen Verwaltung
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Excel-Vorlagen",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Verwalte Excel-Vorlagen für verschiedene Jahre",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = onNavigateToTemplateManagement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vorlagen verwalten")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

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
private fun ArbeitszeitTab(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    var prozent by remember { mutableStateOf(settings?.arbeitsumfangProzent?.toString() ?: "100") }
    var stunden by remember { mutableStateOf("40") }
    var minuten by remember { mutableStateOf("00") }
    var arbeitsTage by remember { mutableStateOf(settings?.arbeitsTageProWoche?.toString() ?: "5") }
    var ferienbetreuung by remember { mutableStateOf(settings?.ferienbetreuung ?: false) }
    var ersterMontag by remember { mutableStateOf("") }

    // Arbeitstage Auswahl (Mo=1, Di=2, ..., So=7)
    var selectedWorkingDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // Default: Mo-Fr

    val scope = rememberCoroutineScope()

    // Validation states
    val prozentValue = prozent.toIntOrNull()
    val prozentError = when {
        prozent.isBlank() -> "Arbeitsumfang darf nicht leer sein"
        prozentValue == null -> "Bitte eine Zahl eingeben"
        prozentValue < 0 -> "Arbeitsumfang darf nicht negativ sein"
        prozentValue > 100 -> "Arbeitsumfang darf maximal 100% sein"
        else -> null
    }

    val stundenValue = stunden.toIntOrNull()
    val stundenError = when {
        stunden.isBlank() -> "Stunden dürfen nicht leer sein"
        stundenValue == null -> "Bitte eine Zahl eingeben"
        stundenValue < 0 -> "Stunden dürfen nicht negativ sein"
        else -> null
    }

    val minutenValue = minuten.toIntOrNull()
    val minutenError = when {
        minuten.isBlank() -> "Minuten dürfen nicht leer sein"
        minutenValue == null -> "Bitte eine Zahl eingeben"
        minutenValue < 0 -> "Minuten dürfen nicht negativ sein"
        minutenValue > 59 -> "Minuten dürfen maximal 59 sein"
        else -> null
    }

    val arbeitsTageValue = arbeitsTage.toIntOrNull()
    val arbeitsTageError = when {
        arbeitsTage.isBlank() -> "Arbeitstage dürfen nicht leer sein"
        arbeitsTageValue == null -> "Bitte eine Zahl eingeben"
        arbeitsTageValue < 1 -> "Mindestens 1 Arbeitstag erforderlich"
        arbeitsTageValue > 7 -> "Maximal 7 Arbeitstage möglich"
        else -> null
    }

    val hasErrors = prozentError != null || stundenError != null || minutenError != null || arbeitsTageError != null

    LaunchedEffect(settings) {
        settings?.let {
            prozent = it.arbeitsumfangProzent.toString()
            stunden = (it.wochenStundenMinuten / 60).toString()
            minuten = (it.wochenStundenMinuten % 60).toString().padStart(2, '0')
            arbeitsTage = it.arbeitsTageProWoche.toString()
            ferienbetreuung = it.ferienbetreuung

            // Lade Arbeitstage aus Settings
            selectedWorkingDays = it.workingDays.map { char -> char.toString().toInt() }.toSet()

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
            isError = prozentError != null,
            supportingText = if (prozentError != null) {
                { Text(prozentError, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Wochenstunden", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stunden,
                onValueChange = { stunden = it },
                label = { Text("Stunden") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = stundenError != null,
                supportingText = if (stundenError != null) {
                    { Text(stundenError, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minuten,
                onValueChange = { minuten = it },
                label = { Text("Minuten") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = minutenError != null,
                supportingText = if (minutenError != null) {
                    { Text(minutenError, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = arbeitsTage,
            onValueChange = { arbeitsTage = it },
            label = { Text("Arbeitstage/Woche") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = arbeitsTageError != null,
            supportingText = if (arbeitsTageError != null) {
                { Text(arbeitsTageError, color = MaterialTheme.colorScheme.error) }
            } else {
                { Text("Wird automatisch aus den ausgewählten Tagen berechnet") }
            },
            enabled = false, // Deaktiviert, da automatisch berechnet
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Arbeitstage Auswahl
        Text(
            "Welche Tage arbeitest du?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Wähle deine Arbeitstage aus. Nur diese Tage werden in der Wochenansicht angezeigt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val daysOfWeek = listOf(
            1 to "Mo",
            2 to "Di",
            3 to "Mi",
            4 to "Do",
            5 to "Fr",
            6 to "Sa",
            7 to "So"
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            daysOfWeek.forEach { (dayNum, dayName) ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = selectedWorkingDays.contains(dayNum),
                        onCheckedChange = { isChecked ->
                            selectedWorkingDays = if (isChecked) {
                                selectedWorkingDays + dayNum
                            } else {
                                selectedWorkingDays - dayNum
                            }
                            // Update arbeitsTage count
                            arbeitsTage = selectedWorkingDays.size.toString()
                        }
                    )
                    Text(
                        text = when (dayNum) {
                            1 -> "Montag"
                            2 -> "Dienstag"
                            3 -> "Mittwoch"
                            4 -> "Donnerstag"
                            5 -> "Freitag"
                            6 -> "Samstag"
                            7 -> "Sonntag"
                            else -> ""
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider()

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

        HorizontalDivider()

        // Individuelle Sollzeiten pro Tag (expandable)
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

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Individuelle Soll-Zeiten pro Tag",
                    style = MaterialTheme.typography.titleSmall,
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

                // Convert selectedWorkingDays to String (e.g., "12345" for Mo-Fr)
                val workingDaysString = selectedWorkingDays.sorted().joinToString("")

                // Sollzeiten berechnen
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

                viewModel.updateArbeitszeit(
                    arbeitsumfangProzent = prozent.toIntOrNull() ?: 100,
                    wochenStundenMinuten = wochenMinuten,
                    arbeitsTageProWoche = selectedWorkingDays.size,
                    ferienbetreuung = ferienbetreuung,
                    ersterMontagImJahr = ersterMontagFormatted,
                    workingDays = workingDaysString
                )

                // Sollzeiten speichern
                viewModel.updateSollzeiten(
                    montagSollMinuten = montagMin,
                    dienstagSollMinuten = dienstagMin,
                    mittwochSollMinuten = mittwochMin,
                    donnerstagSollMinuten = donnerstagMin,
                    freitagSollMinuten = freitagMin,
                    samstagSollMinuten = samstagMin,
                    sonntagSollMinuten = sonntagMin
                )

                scope.launch {
                    snackbarHostState.showSnackbar("Einstellungen erfolgreich gespeichert")
                }
            },
            enabled = !hasErrors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }
    }
}

@Composable
private fun AutomatisierungTab(
    onNavigateToGeofencing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Automatische Zeiterfassung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "Konfiguriere die automatische Zeiterfassung basierend auf deinem Standort. " +
            "Die App kann automatisch die Arbeitszeit starten und beenden, wenn du einen bestimmten Ort betrittst oder verlässt.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        // Geofencing Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Standortbasierte Zeiterfassung",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Automatisches Starten und Stoppen basierend auf GPS-Standort",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = onNavigateToGeofencing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Orte konfigurieren")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Hinweise
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Hinweise",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "• Die App benötigt Standortberechtigungen\n" +
                    "• Die Zeiterfassung erfolgt im Hintergrund\n" +
                    "• Du kannst mehrere Standorte definieren\n" +
                    "• Die Genauigkeit hängt vom GPS-Signal ab",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
    // Validation
    val hoursValue = hours.toIntOrNull()
    val hoursError = when {
        hours.isNotBlank() && hoursValue == null -> "Ungültig"
        hours.isNotBlank() && hoursValue != null && hoursValue < 0 -> "Negativ"
        else -> null
    }

    val minutesValue = minutes.toIntOrNull()
    val minutesError = when {
        minutes.isNotBlank() && minutesValue == null -> "Ungültig"
        minutes.isNotBlank() && minutesValue != null && minutesValue < 0 -> "Negativ"
        minutes.isNotBlank() && minutesValue != null && minutesValue > 59 -> "Max 59"
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                isError = hoursError != null,
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Text(":", style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChange,
                label = { Text("m") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = minutesError != null,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Show error messages if any
        if (hoursError != null || minutesError != null) {
            Row(
                modifier = Modifier.padding(start = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = hoursError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = minutesError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
