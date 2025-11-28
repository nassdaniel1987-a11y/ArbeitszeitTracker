package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbeitszeit.tracker.viewmodel.VacationPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationPlannerScreen(
    viewModel: VacationPlannerViewModel = viewModel(),
    onNavigateToClosingDays: () -> Unit = {}
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val vacationStats by viewModel.vacationStats.collectAsState()
    val closingDays by viewModel.closingDays.collectAsState()
    val schoolHolidays by viewModel.schoolHolidays.collectAsState()

    var showOptimizationDialog by remember { mutableStateOf(false) }
    var selectedPreference by remember { mutableStateOf("Lange am Stück") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Urlaubsplaner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToClosingDays) {
                        Icon(Icons.Default.Settings, "Schließtage verwalten")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isLoading && aiSuggestion == null) {
                ExtendedFloatingActionButton(
                    onClick = { showOptimizationDialog = true },
                    icon = { Icon(Icons.Default.AutoAwesome, "KI-Optimierung") },
                    text = { Text("KI-Optimierung") },
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Jahr-Auswahl
            item {
                YearSelector(
                    selectedYear = selectedYear,
                    onYearChange = { viewModel.setYear(it) }
                )
            }

            // Statistik-Karten
            item {
                VacationStatsCards(stats = vacationStats)
            }

            // Info wenn Bundesland fehlt
            if (userSettings?.bundesland.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bitte wähle dein Bundesland in den Einstellungen aus, um Schulferien anzuzeigen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // KI-Vorschlag
            aiSuggestion?.let { suggestion ->
                item {
                    AiSuggestionCard(
                        suggestion = suggestion,
                        onDismiss = { viewModel.clearAiSuggestion() }
                    )
                }
            }

            // Fehlermeldung
            errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(Icons.Default.Close, "Schließen")
                            }
                        }
                    }
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "KI analysiert deine Urlaubsmöglichkeiten...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Schulferien
            if (schoolHolidays.isNotEmpty()) {
                item {
                    Text(
                        text = "Schulferien ${userSettings?.bundesland ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(schoolHolidays) { holiday ->
                    HolidayCard(
                        name = holiday.name,
                        dateRange = holiday.getFormattedDateRange(),
                        days = holiday.getDurationDays(),
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Schließtage
            if (closingDays.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schließtage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToClosingDays) {
                            Text("Verwalten")
                        }
                    }
                }
                items(closingDays) { closingDay ->
                    HolidayCard(
                        name = closingDay.title,
                        dateRange = closingDay.getFormattedDateRange(),
                        days = closingDay.getDurationDays(),
                        note = closingDay.note,
                        icon = Icons.Default.Lock,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Optimierungs-Dialog
    if (showOptimizationDialog) {
        OptimizationDialog(
            selectedPreference = selectedPreference,
            onPreferenceChange = { selectedPreference = it },
            onDismiss = { showOptimizationDialog = false },
            onConfirm = {
                viewModel.optimizeVacation(selectedPreference)
                showOptimizationDialog = false
            }
        )
    }
}

@Composable
fun YearSelector(
    selectedYear: Int,
    onYearChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = { onYearChange(selectedYear - 1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, "Vorheriges Jahr")
            }

            Text(
                text = selectedYear.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { onYearChange(selectedYear + 1) }) {
                Icon(Icons.Default.KeyboardArrowRight, "Nächstes Jahr")
            }
        }
    }
}

@Composable
fun VacationStatsCards(stats: VacationPlannerViewModel.VacationStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsCard(
            modifier = Modifier.weight(1f),
            value = stats.verfuegbar.toString(),
            label = "Verfügbar",
            icon = Icons.Default.Event,
            color = MaterialTheme.colorScheme.primary
        )
        StatsCard(
            modifier = Modifier.weight(1f),
            value = stats.genommen.toString(),
            label = "Genommen",
            icon = Icons.Default.CheckCircle,
            color = MaterialTheme.colorScheme.tertiary
        )
        StatsCard(
            modifier = Modifier.weight(1f),
            value = stats.anspruch.toString(),
            label = "Anspruch",
            icon = Icons.Default.CalendarMonth,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun HolidayCard(
    name: String,
    dateRange: String,
    days: Int,
    note: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$days Tag${if (days > 1) "e" else ""}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiSuggestionCard(
    suggestion: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KI-Vorschlag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Schließen")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationDialog(
    selectedPreference: String,
    onPreferenceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val preferences = listOf(
        "Lange am Stück" to "Maximale Erholung durch lange Zeiträume",
        "Brückentage nutzen" to "Viele freie Tage mit wenigen Urlaubstagen",
        "Viele kurze Auszeiten" to "Regelmäßige Pausen übers Jahr verteilt",
        "Nur Schulferien" to "Urlaub in Schulferien für Familie"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null) },
        title = { Text("KI-Urlaubsoptimierung") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Wie möchtest du deinen Urlaub planen?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                preferences.forEach { (pref, description) ->
                    Card(
                        onClick = { onPreferenceChange(pref) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPreference == pref)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = pref,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Optimieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
