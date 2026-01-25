package com.arbeitszeit.tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbeitszeit.tracker.ui.components.DarkModeCard
import com.arbeitszeit.tracker.ui.sections.ArbeitszeitvorlagenSection
import com.arbeitszeit.tracker.ui.sections.CloudBackupSection
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToGeofencing: () -> Unit = {},
    onNavigateToTemplateManagement: () -> Unit = {},
    onNavigateToYearManagement: () -> Unit = {}
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
            onNavigateToTemplateManagement = onNavigateToTemplateManagement,
            onNavigateToYearManagement = onNavigateToYearManagement
        )
        return
    }

    // Hauptmenü (Modern Style)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Persönliche Daten
            item {
                SettingsMenuSection(title = "Persönliche Daten")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Person,
                    title = "Name & Einrichtung",
                    subtitle = "${settings?.name ?: "Nicht gesetzt"}",
                    onClick = { selectedSection = SettingsSection.PERSONAL_DATA }
                )
            }

            // Darstellung
            item {
                SettingsMenuSection(title = "Darstellung")
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
                    title = "Wochenstunden & Prozent",
                    subtitle = "${settings?.arbeitsumfangProzent ?: 100}% • ${settings?.arbeitsTageProWoche ?: 5} Tage/Woche",
                    onClick = { selectedSection = SettingsSection.WORK_TIME }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Layers,
                    title = "Arbeitszeitvorlagen",
                    subtitle = "Normal, Ferienbetreuung, etc.",
                    onClick = { selectedSection = SettingsSection.ARBEITSZEITVORLAGEN }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Auto-Start",
                    subtitle = if (settings?.autoStartEnabled == true) "Aktiviert" else "Deaktiviert",
                    onClick = { selectedSection = SettingsSection.AUTO_START }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Celebration,
                    title = "Feiertage",
                    subtitle = settings?.bundesland?.let { code ->
                        com.arbeitszeit.tracker.utils.HolidayUtils.Bundesland.fromShortCode(code)?.displayName
                    } ?: "Nicht gesetzt",
                    onClick = { selectedSection = SettingsSection.HOLIDAYS }
                )
            }

            // Benachrichtigungen
            item {
                SettingsMenuSection(title = "Benachrichtigungen")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Benachrichtigungen & Ruhezeit",
                    subtitle = if (settings?.notificationQuietTimeEnabled == true) {
                        val startHour = (settings?.notificationQuietTimeStart ?: 1260) / 60
                        val endHour = (settings?.notificationQuietTimeEnd ?: 420) / 60
                        "Ruhezeit: ${startHour}:00 - ${endHour}:00 Uhr"
                    } else {
                        "Immer aktiv"
                    },
                    onClick = { selectedSection = SettingsSection.NOTIFICATIONS }
                )
            }

            // Automatisierung & Orte
            item {
                SettingsMenuSection(title = "Automatisierung & Orte")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Geofencing & Arbeitsorte",
                    subtitle = if (settings?.geofencingEnabled == true) "Aktiviert" else "Deaktiviert",
                    onClick = { selectedSection = SettingsSection.GEOFENCING }
                )
            }

            // Jahres-Management
            item {
                SettingsMenuSection(title = "Jahres-Management")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.CalendarMonth,
                    title = "Automatischer Jahreswechsel",
                    subtitle = if (settings?.autoSwitchYear == true) "Aktiviert" else "Deaktiviert",
                    onClick = { selectedSection = SettingsSection.YEAR_MANAGEMENT }
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.DateRange,
                    title = "Jahre verwalten",
                    subtitle = "Jahre erstellen, bearbeiten & löschen",
                    onClick = onNavigateToYearManagement
                )
            }

            // Excel & Vorlagen
            item {
                SettingsMenuSection(title = "Excel & Vorlagen")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Description,
                    title = "Excel-Vorlagen",
                    subtitle = "Vorlagen für verschiedene Jahre verwalten",
                    onClick = { selectedSection = SettingsSection.EXCEL_TEMPLATES }
                )
            }

            // Daten & Sicherheit
            item {
                SettingsMenuSection(title = "Daten & Sicherheit")
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Cloud-Backup",
                    subtitle = "Datenbank sichern und wiederherstellen",
                    onClick = { selectedSection = SettingsSection.BACKUP }
                )
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
    ARBEITSZEITVORLAGEN,
    AUTO_START,
    HOLIDAYS,
    NOTIFICATIONS,
    GEOFENCING,
    YEAR_MANAGEMENT,
    BACKUP,
    EXCEL_TEMPLATES,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 1.dp,
            focusedElevation = 2.dp,
            hoveredElevation = 3.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon mit farbigem Background
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (dangerous) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = if (dangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Öffnen",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Modern Section Header
 */
@Composable
private fun SettingsMenuSection(title: String) {
    Column(
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(0.1f),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
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
    onNavigateToTemplateManagement: () -> Unit,
    onNavigateToYearManagement: () -> Unit
) {
    // System-Back-Geste abfangen (vom Rand wischen)
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (section) {
                            SettingsSection.PERSONAL_DATA -> "Persönliche Daten"
                            SettingsSection.DARK_MODE -> "Dark Mode"
                            SettingsSection.WORK_TIME -> "Arbeitszeit"
                            SettingsSection.ARBEITSZEITVORLAGEN -> "Arbeitszeitvorlagen"
                            SettingsSection.AUTO_START -> "Auto-Start"
                            SettingsSection.HOLIDAYS -> "Feiertage"
                            SettingsSection.NOTIFICATIONS -> "Benachrichtigungen"
                            SettingsSection.GEOFENCING -> "Geofencing & Orte"
                            SettingsSection.YEAR_MANAGEMENT -> "Jahres-Management"
                            SettingsSection.BACKUP -> "Cloud-Backup"
                            SettingsSection.EXCEL_TEMPLATES -> "Excel-Vorlagen"
                            SettingsSection.DELETE_DATA -> "Daten löschen"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
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
                SettingsSection.ARBEITSZEITVORLAGEN -> ArbeitszeitvorlagenSection(viewModel, snackbarHostState)
                SettingsSection.AUTO_START -> AutoStartSection(viewModel, settings, snackbarHostState)
                SettingsSection.HOLIDAYS -> HolidaysSection(viewModel, settings, snackbarHostState)
                SettingsSection.NOTIFICATIONS -> NotificationsSection(viewModel, settings, snackbarHostState)
                SettingsSection.GEOFENCING -> GeofencingSection(onNavigateToGeofencing)
                SettingsSection.YEAR_MANAGEMENT -> YearManagementSection(viewModel, settings, snackbarHostState)
                SettingsSection.BACKUP -> CloudBackupSection(viewModel, snackbarHostState)
                SettingsSection.EXCEL_TEMPLATES -> ExcelTemplatesSection(onNavigateToTemplateManagement)
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
    var ueberstundenVorjahr by remember {
        mutableStateOf(com.arbeitszeit.tracker.utils.TimeUtils.minutesToHoursMinutes(settings?.ueberstundenVorjahrMinuten ?: 0))
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        settings?.let {
            name = it.name
            einrichtung = it.einrichtung
            ueberstundenVorjahr = com.arbeitszeit.tracker.utils.TimeUtils.minutesToHoursMinutes(it.ueberstundenVorjahrMinuten)
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

        OutlinedTextField(
            value = ueberstundenVorjahr,
            onValueChange = { ueberstundenVorjahr = it },
            label = { Text("Überstunden vom Vorjahr") },
            supportingText = { Text("Format: Stunden:Minuten (z.B. 5:30 oder -2:15)") },
            placeholder = { Text("0:00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val ueberstundenMinuten = com.arbeitszeit.tracker.utils.TimeUtils.hoursMinutesToMinutes(ueberstundenVorjahr)
                viewModel.updateStammdaten(
                    name = name,
                    einrichtung = einrichtung,
                    ueberstundenVorjahrMinuten = ueberstundenMinuten
                )
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
 * Holidays Section - Bundesland-Auswahl für Feiertage
 */
@Composable
private fun HolidaysSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val bundeslaender = com.arbeitszeit.tracker.utils.HolidayUtils.Bundesland.values()
    val selectedBundesland = settings?.bundesland?.let { code ->
        com.arbeitszeit.tracker.utils.HolidayUtils.Bundesland.fromShortCode(code)
    }

    // Urlaubsanspruch State
    var urlaubsanspruch by remember { mutableStateOf(settings?.urlaubsanspruchTage?.toString() ?: "30") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Feiertage automatisch erkennen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Wähle dein Bundesland, damit bundeslandspezifische Feiertage (wie Heilige Drei Könige oder Reformationstag) korrekt erkannt werden.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Urlaubsanspruch
        Text(
            "Urlaubsanspruch",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = urlaubsanspruch,
                    onValueChange = { urlaubsanspruch = it },
                    label = { Text("Jahresurlaub in Tagen") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = urlaubsanspruch.toIntOrNull() == null || (urlaubsanspruch.toIntOrNull() ?: 0) < 0,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (urlaubsanspruch.toIntOrNull() == null) {
                            Text("Bitte gültige Zahl eingeben")
                        } else {
                            Text("Standard: 30 Tage (gesetzliches Minimum: 20 Tage)")
                        }
                    }
                )

                Button(
                    onClick = {
                        val tage = urlaubsanspruch.toIntOrNull()
                        if (tage != null && tage >= 0) {
                            viewModel.updateUrlaubsanspruch(tage)
                            scope.launch {
                                snackbarHostState.showSnackbar("Urlaubsanspruch gespeichert: $tage Tage")
                            }
                        }
                    },
                    enabled = urlaubsanspruch.toIntOrNull() != null && (urlaubsanspruch.toIntOrNull() ?: 0) >= 0,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Speichern")
                    Spacer(Modifier.width(8.dp))
                    Text("Speichern")
                }
            }
        }

        HorizontalDivider()

        // Bundesland-Auswahl
        Text(
            "Bundesland auswählen",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option: Kein Bundesland
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (selectedBundesland == null) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                    onClick = {
                        viewModel.updateBundesland(null)
                        scope.launch {
                            snackbarHostState.showSnackbar("Bundesland zurückgesetzt")
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedBundesland == null) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Ausgewählt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            "Kein Bundesland (nur bundesweite Feiertage)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Alle Bundesländer
            items(bundeslaender.size) { index ->
                val bundesland = bundeslaender[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (selectedBundesland == bundesland) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                    onClick = {
                        viewModel.updateBundesland(bundesland.shortCode)
                        scope.launch {
                            snackbarHostState.showSnackbar("Bundesland: ${bundesland.displayName}")
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedBundesland == bundesland) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Ausgewählt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                bundesland.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedBundesland == bundesland) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                bundesland.shortCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Vorschau der Feiertage für aktuelles Jahr
        if (selectedBundesland != null) {
            HorizontalDivider()

            Text(
                "Feiertage ${java.time.LocalDate.now().year}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val currentYear = java.time.LocalDate.now().year
            val holidays = com.arbeitszeit.tracker.utils.HolidayUtils.getHolidaysForYear(currentYear, selectedBundesland)
                .sortedBy { it.date }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    holidays.forEach { holiday ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                holiday.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                com.arbeitszeit.tracker.utils.DateUtils.formatForDisplay(holiday.date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Gesamt: ${holidays.size} Feiertage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
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
    val activeYear by viewModel.activeYear.collectAsState()
    ArbeitszeitTab(viewModel, settings, activeYear, snackbarHostState)
}

/**
 * Geofencing Section
 */
@Composable
private fun GeofencingSection(
    onNavigateToGeofencing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Standort",
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

                Text(
                    "Konfiguriere die automatische Zeiterfassung basierend auf deinem Standort. " +
                    "Die App kann automatisch die Arbeitszeit starten und beenden, wenn du einen bestimmten Ort betrittst oder verlässt.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onNavigateToGeofencing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    Spacer(Modifier.width(8.dp))
                    Text("Orte konfigurieren")
                }
            }
        }

        // Hinweise Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Information",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Hinweise",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

/**
 * Jahres-Management Section
 */
@Composable
private fun YearManagementSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    var autoSwitchYear by remember { mutableStateOf(settings?.autoSwitchYear ?: true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        settings?.let {
            autoSwitchYear = it.autoSwitchYear
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Kalender",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Automatischer Jahreswechsel",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Wechselt automatisch zum neuen Jahr am ersten Montag",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    "Wenn aktiviert, wechselt die App automatisch zum nächsten Jahr, sobald der erste Montag des neuen Jahres erreicht ist. " +
                    "Falls das neue Jahr noch nicht angelegt wurde, wird automatisch ein Dialog angezeigt.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Toggle für Auto-Switch
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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
                        "Automatischer Wechsel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (autoSwitchYear) "Aktiviert" else "Deaktiviert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = autoSwitchYear,
                    onCheckedChange = { enabled ->
                        autoSwitchYear = enabled
                        scope.launch {
                            viewModel.updateAutoSwitchYear(enabled)
                            snackbarHostState.showSnackbar(
                                if (enabled) "Automatischer Jahreswechsel aktiviert"
                                else "Automatischer Jahreswechsel deaktiviert"
                            )
                        }
                    }
                )
            }
        }

        // Hinweise Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Information",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Hinweise",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "• Der Wechsel erfolgt am ersten Montag des neuen Jahres\n" +
                    "• Falls das neue Jahr noch nicht existiert, wird ein Dialog angezeigt\n" +
                    "• Du kannst Jahre jederzeit manuell wechseln (Jahr-Auswahl oben)\n" +
                    "• Überstunden werden automatisch übertragen",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Excel Templates Section
 */
@Composable
private fun ExcelTemplatesSection(
    onNavigateToTemplateManagement: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Dokument",
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

                Text(
                    "Beim Export wird automatisch die passende Vorlage für das gewählte Jahr verwendet. " +
                    "Wenn keine Vorlage vorhanden ist, wird die Standard-Vorlage aus der App verwendet.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "So funktioniert's:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "1. Im Export-Screen kannst du das Jahr auswählen\n" +
                    "2. Die App verwendet automatisch die Vorlage für dieses Jahr\n" +
                    "3. Falls keine Vorlage vorhanden: Standard-Vorlage wird verwendet",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = onNavigateToTemplateManagement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    Spacer(Modifier.width(8.dp))
                    Text("Vorlagen hochladen & verwalten")
                }
            }
        }

        // Hinweise Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Information",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Hinweise",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "• Pro Jahr kann eine eigene Vorlage hinterlegt werden\n" +
                    "• Die Vorlage muss die korrekte Struktur haben (wie ANZ_Template.xlsx)\n" +
                    "• Standard-Vorlage wird immer als Fallback verwendet\n" +
                    "• Jahr-Auswahl erfolgt direkt im Export-Screen",
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                    contentDescription = "Warnung",
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
            Icon(Icons.Default.Delete, contentDescription = "Löschen")
            Spacer(Modifier.width(8.dp))
            Text("Alle Daten löschen")
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warnung") },
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
                        contentDescription = "Dokument",
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
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
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
    activeYear: com.arbeitszeit.tracker.data.entity.YearSettings?,
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

    LaunchedEffect(settings, activeYear) {
        settings?.let {
            prozent = it.arbeitsumfangProzent.toString()
            stunden = (it.wochenStundenMinuten / 60).toString()
            minuten = (it.wochenStundenMinuten % 60).toString().padStart(2, '0')
            arbeitsTage = it.arbeitsTageProWoche.toString()
            ferienbetreuung = it.ferienbetreuung

            // Lade Arbeitstage aus Settings
            selectedWorkingDays = it.workingDays.map { char -> char.toString().toInt() }.toSet()
        }

        // Lade ersterMontag aus YearSettings (jahr-spezifisch!)
        activeYear?.let { year ->
            val datum = year.ersterMontagImJahr
            val parts = datum.split("-")
            if (parts.size == 3) {
                ersterMontag = "${parts[2]}.${parts[1]}.${parts[0]}"
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

        HorizontalDivider()

        Text("Kalenderwochenberechnung", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = ersterMontag,
            onValueChange = { ersterMontag = it },
            label = { Text("Erster Montag im Jahr") },
            placeholder = { Text("z.B. 06.01.2025") },
            supportingText = { Text("Format: TT.MM.JJJJ - Leer lassen für ISO 8601") },
            modifier = Modifier.fillMaxWidth()
        )

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

                viewModel.updateArbeitszeit(
                    arbeitsumfangProzent = prozent.toIntOrNull() ?: 100,
                    wochenStundenMinuten = wochenMinuten,
                    arbeitsTageProWoche = selectedWorkingDays.size,
                    ferienbetreuung = ferienbetreuung,
                    workingDays = workingDaysString
                )

                // Speichere ersterMontag separat in YearSettings (jahr-spezifisch!)
                ersterMontagFormatted?.let { viewModel.updateErsterMontag(it) }

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
                        contentDescription = "Standort",
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
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
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

/**
 * Auto-Start Section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoStartSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    var autoStartEnabled by remember { mutableStateOf(settings?.autoStartEnabled ?: false) }
    var autoStartRequiresGeofencing by remember { mutableStateOf(settings?.autoStartRequiresGeofencing ?: true) }
    var reminderMinutes by remember { mutableStateOf((settings?.autoStartReminderMinutes ?: 5).toString()) }
    var defaultPauseMinutes by remember { mutableStateOf((settings?.autoStartDefaultPauseMinutes ?: 30).toString()) }

    // Start-Zeiten pro Wochentag (in Minuten seit Mitternacht)
    var montagZeit by remember { mutableStateOf(settings?.autoStartMontagZeit) }
    var dienstagZeit by remember { mutableStateOf(settings?.autoStartDienstagZeit) }
    var mittwochZeit by remember { mutableStateOf(settings?.autoStartMittwochZeit) }
    var donnerstagZeit by remember { mutableStateOf(settings?.autoStartDonnerstagZeit) }
    var freitagZeit by remember { mutableStateOf(settings?.autoStartFreitagZeit) }
    var samstagZeit by remember { mutableStateOf(settings?.autoStartSamstagZeit) }
    var sonntagZeit by remember { mutableStateOf(settings?.autoStartSonntagZeit) }

    // TimePicker Dialog State
    var showTimePicker by remember { mutableStateOf<Int?>(null) }  // Tag 1-7 oder null

    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        settings?.let {
            autoStartEnabled = it.autoStartEnabled
            autoStartRequiresGeofencing = it.autoStartRequiresGeofencing
            reminderMinutes = it.autoStartReminderMinutes.toString()
            defaultPauseMinutes = it.autoStartDefaultPauseMinutes.toString()
            montagZeit = it.autoStartMontagZeit
            dienstagZeit = it.autoStartDienstagZeit
            mittwochZeit = it.autoStartMittwochZeit
            donnerstagZeit = it.autoStartDonnerstagZeit
            freitagZeit = it.autoStartFreitagZeit
            samstagZeit = it.autoStartSamstagZeit
            sonntagZeit = it.autoStartSonntagZeit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        "Automatischer Start",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Die Arbeitszeit startet automatisch zur konfigurierten Zeit. Konfiguriere die Start-Zeiten pro Wochentag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Auto-Start aktivieren
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Start aktivieren", style = MaterialTheme.typography.titleMedium)
                    Text("Startet Zeiterfassung automatisch", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = autoStartEnabled, onCheckedChange = { autoStartEnabled = it })
            }
        }

        if (autoStartEnabled) {
            // Start-Zeiten pro Wochentag
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Start-Zeiten pro Wochentag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tippe auf einen Tag um die Start-Zeit festzulegen. Ohne Zeit kein Auto-Start an diesem Tag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    // Wochentage
                    AutoStartDayRow("Montag", montagZeit, { showTimePicker = 1 }, { montagZeit = null })
                    AutoStartDayRow("Dienstag", dienstagZeit, { showTimePicker = 2 }, { dienstagZeit = null })
                    AutoStartDayRow("Mittwoch", mittwochZeit, { showTimePicker = 3 }, { mittwochZeit = null })
                    AutoStartDayRow("Donnerstag", donnerstagZeit, { showTimePicker = 4 }, { donnerstagZeit = null })
                    AutoStartDayRow("Freitag", freitagZeit, { showTimePicker = 5 }, { freitagZeit = null })
                    AutoStartDayRow("Samstag", samstagZeit, { showTimePicker = 6 }, { samstagZeit = null })
                    AutoStartDayRow("Sonntag", sonntagZeit, { showTimePicker = 7 }, { sonntagZeit = null })
                }
            }

            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nur mit Geofencing", style = MaterialTheme.typography.titleMedium)
                        Text("Auto-Start nur am Arbeitsort", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = autoStartRequiresGeofencing, onCheckedChange = { autoStartRequiresGeofencing = it })
                }
            }

            OutlinedTextField(
                value = reminderMinutes,
                onValueChange = { reminderMinutes = it },
                label = { Text("Vor-Erinnerung (Minuten)") },
                leadingIcon = { Icon(Icons.Default.Notifications, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = defaultPauseMinutes,
                onValueChange = { defaultPauseMinutes = it },
                label = { Text("Standard-Pause (Minuten)") },
                leadingIcon = { Icon(Icons.Default.FreeBreakfast, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val autoStartZeiten = mutableMapOf<Int, Int?>()
                autoStartZeiten[1] = montagZeit
                autoStartZeiten[2] = dienstagZeit
                autoStartZeiten[3] = mittwochZeit
                autoStartZeiten[4] = donnerstagZeit
                autoStartZeiten[5] = freitagZeit
                autoStartZeiten[6] = samstagZeit
                autoStartZeiten[7] = sonntagZeit

                viewModel.updateAutoStartSettings(
                    autoStartEnabled,
                    autoStartRequiresGeofencing,
                    reminderMinutes.toIntOrNull() ?: 5,
                    defaultPauseMinutes.toIntOrNull() ?: 30,
                    autoStartZeiten
                )
                scope.launch { snackbarHostState.showSnackbar("Gespeichert") }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Speichern")
        }
    }

    // TimePicker Dialog
    showTimePicker?.let { dayOfWeek ->
        val currentTime = when (dayOfWeek) {
            1 -> montagZeit
            2 -> dienstagZeit
            3 -> mittwochZeit
            4 -> donnerstagZeit
            5 -> freitagZeit
            6 -> samstagZeit
            7 -> sonntagZeit
            else -> null
        }
        val initialHour = currentTime?.let { it / 60 } ?: 8
        val initialMinute = currentTime?.let { it % 60 } ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        val dayName = when (dayOfWeek) {
            1 -> "Montag"
            2 -> "Dienstag"
            3 -> "Mittwoch"
            4 -> "Donnerstag"
            5 -> "Freitag"
            6 -> "Samstag"
            7 -> "Sonntag"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { showTimePicker = null },
            title = { Text("Start-Zeit $dayName") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = timePickerState.hour * 60 + timePickerState.minute
                        when (dayOfWeek) {
                            1 -> montagZeit = minutes
                            2 -> dienstagZeit = minutes
                            3 -> mittwochZeit = minutes
                            4 -> donnerstagZeit = minutes
                            5 -> freitagZeit = minutes
                            6 -> samstagZeit = minutes
                            7 -> sonntagZeit = minutes
                        }
                        showTimePicker = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Benachrichtigungs-Einstellungen Section (Ruhezeit)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSection(
    viewModel: SettingsViewModel,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    snackbarHostState: SnackbarHostState
) {
    var quietTimeEnabled by remember { mutableStateOf(settings?.notificationQuietTimeEnabled ?: false) }
    var quietTimeStart by remember { mutableStateOf(settings?.notificationQuietTimeStart ?: 1260) }  // 21:00
    var quietTimeEnd by remember { mutableStateOf(settings?.notificationQuietTimeEnd ?: 420) }      // 07:00
    var selectedActiveDays by remember {
        mutableStateOf((settings?.notificationActiveDays ?: "12345").map { it.toString().toInt() }.toSet())
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        settings?.let {
            quietTimeEnabled = it.notificationQuietTimeEnabled
            quietTimeStart = it.notificationQuietTimeStart
            quietTimeEnd = it.notificationQuietTimeEnd
            selectedActiveDays = it.notificationActiveDays.map { c -> c.toString().toInt() }.toSet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        "Benachrichtigungs-Ruhezeit",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Konfiguriere, wann du Erinnerungen zum Einstempeln oder zur Zeiterfassung erhalten m\u00f6chtest. An freien Tagen oder sp\u00e4t abends kannst du die Benachrichtigungen deaktivieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Aktive Tage
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "An welchen Tagen Benachrichtigungen?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "W\u00e4hle die Tage aus, an denen du Erinnerungen erhalten m\u00f6chtest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val daysOfWeek = listOf(
                    1 to "Montag",
                    2 to "Dienstag",
                    3 to "Mittwoch",
                    4 to "Donnerstag",
                    5 to "Freitag",
                    6 to "Samstag",
                    7 to "Sonntag"
                )

                daysOfWeek.forEach { (dayNum, dayName) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = selectedActiveDays.contains(dayNum),
                            onCheckedChange = { isChecked ->
                                selectedActiveDays = if (isChecked) {
                                    selectedActiveDays + dayNum
                                } else {
                                    selectedActiveDays - dayNum
                                }
                            }
                        )
                        Text(
                            text = dayName,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Ruhezeit aktivieren
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ruhezeit aktivieren", style = MaterialTheme.typography.titleMedium)
                    Text("Keine Benachrichtigungen in bestimmten Zeiten", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = quietTimeEnabled, onCheckedChange = { quietTimeEnabled = it })
            }
        }

        if (quietTimeEnabled) {
            // Ruhezeit Zeitraum
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Ruhezeit-Zeitraum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "In diesem Zeitraum werden keine Erinnerungen gesendet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Start-Zeit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ruhezeit Start", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${String.format("%02d", quietTimeStart / 60)}:${String.format("%02d", quietTimeStart % 60)} Uhr",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider()

                    // End-Zeit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ruhezeit Ende", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${String.format("%02d", quietTimeEnd / 60)}:${String.format("%02d", quietTimeEnd % 60)} Uhr",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Hinweis
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Hinweis",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Die Ruhezeit kann \u00fcber Mitternacht gehen (z.B. 21:00 - 07:00)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val activeDaysString = selectedActiveDays.sorted().joinToString("")
                viewModel.updateNotificationSettings(
                    quietTimeEnabled = quietTimeEnabled,
                    quietTimeStart = quietTimeStart,
                    quietTimeEnd = quietTimeEnd,
                    activeDays = activeDaysString
                )
                scope.launch { snackbarHostState.showSnackbar("Gespeichert") }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Speichern")
        }
    }

    // Start-Zeit Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = quietTimeStart / 60,
            initialMinute = quietTimeStart % 60,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Ruhezeit Start") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(
                    onClick = {
                        quietTimeStart = timePickerState.hour * 60 + timePickerState.minute
                        showStartTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // End-Zeit Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = quietTimeEnd / 60,
            initialMinute = quietTimeEnd % 60,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Ruhezeit Ende") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(
                    onClick = {
                        quietTimeEnd = timePickerState.hour * 60 + timePickerState.minute
                        showEndTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Zeile für einen Wochentag mit Auto-Start Zeit
 */
@Composable
private fun AutoStartDayRow(
    dayName: String,
    timeMinutes: Int?,
    onSetTime: () -> Unit,
    onClearTime: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetTime() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timeMinutes != null) {
                val hours = timeMinutes / 60
                val minutes = timeMinutes % 60
                Text(
                    text = "${String.format("%02d", hours)}:${String.format("%02d", minutes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClearTime) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Zeit entfernen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    text = "Nicht aktiv",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Bearbeiten",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
