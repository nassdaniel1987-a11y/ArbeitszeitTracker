package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.HolidayUtils
import com.arbeitszeit.tracker.viewmodel.VacationPlannerViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

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
    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
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
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Kompakter Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BeachAccess,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Urlaubsplaner",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onNavigateToClosingDays) {
                        Icon(Icons.Default.Settings, "Schließtage verwalten")
                    }
                }
            }

            // Jahr-Auswahl
            item {
                YearSelector(
                    selectedYear = selectedYear,
                    onYearChange = { viewModel.setYear(it) }
                )
            }

            // Tabs
            item {
                androidx.compose.material3.TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Übersicht") },
                        icon = { Icon(Icons.Default.Analytics, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Planen") },
                        icon = { Icon(Icons.Default.CalendarMonth, null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Brückentage") },
                        icon = { Icon(Icons.Default.TrendingUp, null) }
                    )
                }
            }

            // ============================================
            // TAB 0: ÜBERSICHT
            // ============================================
            if (selectedTab == 0) {
                // Statistik-Karten
                item {
                    VacationStatsCards(stats = vacationStats)
                }

                // Resturlaub-Warning (wenn Urlaubstage bald verfallen)
                if (vacationStats.verfuegbar > 0 && shouldShowResturlaubWarning(selectedYear)) {
                    item {
                        ResturlaubWarningCard(
                            verfuegbareTage = vacationStats.verfuegbar,
                            verfallsDatum = getResturlaubVerfallsDatum(selectedYear)
                        )
                    }
                }

                // Countdown zum nächsten Urlaub
                item {
                    NextVacationCountdownCard(viewModel = viewModel)
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
            }

            // ============================================
            // TAB 1: PLANEN (Urlaubskalender)
            // ============================================
            if (selectedTab == 1) {
                // Urlaubs-Kalender (separater Kalender für Urlaubsplanung)
                item {
                    VacationCalendarCard(
                        year = selectedYear,
                        viewModel = viewModel
                    )
                }
            }

            // ============================================
            // TAB 2: BRÜCKENTAGE
            // ============================================
            if (selectedTab == 2) {
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
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Bitte wähle dein Bundesland in den Einstellungen aus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Brückentage-Finder
                item {
                    BridgeDaysCard(year = selectedYear)
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
            onConfirm = { startDate, endDate ->
                customStartDate = startDate
                customEndDate = endDate
                viewModel.optimizeVacation(
                    preferences = selectedPreference,
                    customStartDate = startDate,
                    customEndDate = endDate
                )
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
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
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
    onConfirm: (startDate: String?, endDate: String?) -> Unit
) {
    val preferences = listOf(
        "Lange am Stück" to "Maximale Erholung durch lange Zeiträume",
        "Brückentage nutzen" to "Viele freie Tage mit wenigen Urlaubstagen",
        "Viele kurze Auszeiten" to "Regelmäßige Pausen übers Jahr verteilt",
        "Nur Schulferien" to "Urlaub in Schulferien für Familie"
    )

    var useCustomRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Zeitraum-Eingrenzung
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Zeitraum eingrenzen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = useCustomRange,
                                onCheckedChange = { useCustomRange = it }
                            )
                        }

                        if (useCustomRange) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "z.B. für Resturlaub bis 31.03.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Von (TT.MM.JJJJ)") },
                                placeholder = { Text("01.10.2024") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("Bis (TT.MM.JJJJ)") },
                                placeholder = { Text("31.03.2025") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    if (useCustomRange && startDate.isNotBlank()) startDate else null,
                    if (useCustomRange && endDate.isNotBlank()) endDate else null
                )
            }) {
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

// ============================================
// NEUE KOMPONENTEN: Urlaubsplaner-Erweiterungen
// ============================================

/**
 * Resturlaub-Warning Card
 * Zeigt Warnung wenn Urlaubstage bald verfallen (z.B. bis 31.03.)
 */
@Composable
fun ResturlaubWarningCard(
    verfuegbareTage: Int,
    verfallsDatum: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚠️ Resturlaub verfällt!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$verfuegbareTage Tag${if (verfuegbareTage > 1) "e" else ""} bis $verfallsDatum",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Countdown zum nächsten Urlaub
 */
@Composable
fun NextVacationCountdownCard(viewModel: VacationPlannerViewModel) {
    val vacationDays by viewModel.vacationDays.collectAsState()
    val today = LocalDate.now()

    // Finde den nächsten Urlaub
    val nextVacation = vacationDays
        .filter { entry ->
            val date = try {
                LocalDate.parse(entry.datum)
            } catch (e: Exception) {
                null
            }
            date != null && date.isAfter(today)
        }
        .minByOrNull {
            try {
                LocalDate.parse(it.datum)
            } catch (e: Exception) {
                LocalDate.MAX
            }
        }

    if (nextVacation != null) {
        val nextDate = LocalDate.parse(nextVacation.datum)
        val daysUntil = ChronoUnit.DAYS.between(today, nextDate).toInt()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nächster Urlaub",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = nextVacation.notiz.ifEmpty { "Urlaub" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = DateUtils.formatForDisplay(nextDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Countdown Badge
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = daysUntil.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Text(
                            text = if (daysUntil == 1) "Tag" else "Tage",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Brückentage-Finder
 * Zeigt die besten Brückentage für maximale freie Tage
 */
@Composable
fun BridgeDaysCard(year: Int) {
    val bridgeDays = remember(year) { HolidayUtils.calculateBestBridgeDays(year, null) }

    if (bridgeDays.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🌉 Top Brückentage $year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Maximale Erholung mit minimalen Urlaubstagen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                bridgeDays.take(3).forEach { bridge ->
                    BridgeDayItem(bridge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun BridgeDayItem(bridge: HolidayUtils.BridgeDay) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bridge.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = bridge.zeitraum,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${bridge.urlaubsTage}→${bridge.freieTage}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Urlaubs-Kalender Card
 * Separater Kalender zum Eintragen von geplantem Urlaub
 */
@Composable
fun VacationCalendarCard(
    year: Int,
    viewModel: VacationPlannerViewModel
) {
    val vacationDays by viewModel.vacationDays.collectAsState()
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Urlaubskalender",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Plane deinen Urlaub direkt im Kalender",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monatsnavigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
                }) {
                    Icon(Icons.Default.ChevronLeft, "Vorheriger Monat")
                }

                Text(
                    text = java.time.Month.of(selectedMonth).getDisplayName(
                        TextStyle.FULL,
                        Locale.GERMAN
                    ) + " $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    selectedMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
                }) {
                    Icon(Icons.Default.ChevronRight, "Nächster Monat")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wochentag-Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kalender-Grid
            VacationCalendarGrid(
                year = year,
                month = selectedMonth,
                vacationDays = vacationDays,
                onDayClick = { date ->
                    selectedDate = date
                    showAddDialog = true
                }
            )
        }
    }

    // Dialog zum Hinzufügen von Urlaub
    if (showAddDialog && selectedDate != null) {
        VacationAddDialog(
            date = selectedDate!!,
            onDismiss = { showAddDialog = false },
            onConfirm = { note ->
                viewModel.toggleVacationDay(selectedDate!!, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VacationCalendarGrid(
    year: Int,
    month: Int,
    vacationDays: List<TimeEntry>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = LocalDate.of(year, month, 1)
    val daysInMonth = firstDayOfMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday

    // Erstelle Liste mit allen Tagen des Monats
    val days = (1..daysInMonth).map { day ->
        LocalDate.of(year, month, day)
    }

    // Leere Tage am Anfang für Ausrichtung
    val emptyDaysAtStart = firstDayOfWeek - 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var dayIndex = 0

        // Zeilen (Wochen)
        while (dayIndex < daysInMonth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 7 Tage pro Woche
                for (i in 1..7) {
                    if (dayIndex == 0 && i <= emptyDaysAtStart) {
                        // Leerer Tag am Anfang
                        Box(modifier = Modifier.weight(1f))
                    } else if (dayIndex < daysInMonth) {
                        val date = days[dayIndex]
                        val isVacation = vacationDays.any { it.datum == date.toString() }
                        val isToday = date == LocalDate.now()
                        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
                                       date.dayOfWeek == DayOfWeek.SUNDAY

                        VacationCalendarDay(
                            day = date.dayOfMonth,
                            isVacation = isVacation,
                            isToday = isToday,
                            isWeekend = isWeekend,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                        dayIndex++
                    } else {
                        // Leerer Tag am Ende
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun VacationCalendarDay(
    day: Int,
    isVacation: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isVacation -> MaterialTheme.colorScheme.tertiary
            isToday -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "background"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVacation) 1.1f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isToday && !isVacation) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isVacation || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isVacation -> Color.White
                isWeekend -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VacationAddDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.BeachAccess, null) },
        title = { Text("Urlaub eintragen") },
        text = {
            Column {
                Text(
                    text = "Datum: ${DateUtils.formatForDisplay(date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    placeholder = { Text("z.B. Sommerurlaub") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(note) }) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Prüft ob Resturlaub-Warning angezeigt werden soll
 * (z.B. wenn wir zwischen Oktober und März sind und noch Urlaubstage verfügbar)
 */
fun shouldShowResturlaubWarning(year: Int): Boolean {
    val today = LocalDate.now()
    val currentYear = today.year

    // Zeige Warning zwischen Oktober (Vorjahr) und März (aktuelles Jahr)
    return when {
        currentYear == year && today.monthValue in 1..3 -> true
        currentYear == year - 1 && today.monthValue in 10..12 -> true
        else -> false
    }
}

/**
 * Gibt das Verfallsdatum für Resturlaub zurück (31.03. des Folgejahres)
 */
fun getResturlaubVerfallsDatum(year: Int): String {
    return "31.03.${year + 1}"
}
