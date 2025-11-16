package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.components.EditEntryDialog
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.CalendarViewModel
import com.arbeitszeit.tracker.viewmodel.EntryStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val month by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthEntries.collectAsState()
    val deletedEntry by viewModel.deletedEntry.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = LocalDate.now()

    // Zeige Snackbar wenn Eintrag gelöscht wurde
    LaunchedEffect(deletedEntry) {
        if (deletedEntry != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Eintrag gelöscht",
                actionLabel = "Rückgängig",
                duration = SnackbarDuration.Short
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // User hat auf "Rückgängig" geklickt
                    viewModel.undoDeleteEntry()
                }
                SnackbarResult.Dismissed -> {
                    // Snackbar wurde geschlossen ohne Undo
                    viewModel.clearDeletedEntry()
                }
            }
        }
    }

    // Berechne Monats-Statistik
    val (totalSoll, totalIst, diffAndCompleted) = remember(entries) {
        val totalSoll = entries.sumOf { it.sollMinuten }
        val totalIst = entries.sumOf { it.getIstMinuten() }
        val totalDiff = totalIst - totalSoll
        val completedDays = entries.count { it.isComplete() }
        Triple(totalSoll, totalIst, totalDiff to completedDays)
    }
    val (totalDiff, completedDays) = diffAndCompleted

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Monatsnavigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, "Vorheriger Monat")
                }
                Text(
                    text = "${month.month} ${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, "Nächster Monat")
                }
            }
        }

        // Monats-Statistik mit Fade-In Animation
        if (entries.isNotEmpty()) {
            item {
                var visible by remember { mutableStateOf(false) }

                LaunchedEffect(month) {
                    visible = false
                    delay(100)
                    visible = true
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) +
                            expandVertically(animationSpec = tween(400))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                totalDiff > 0 -> MaterialTheme.colorScheme.primaryContainer
                                totalDiff < 0 -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monats-Übersicht",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Soll: ${TimeUtils.minutesToHoursMinutes(totalSoll)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Ist: ${TimeUtils.minutesToHoursMinutes(totalIst)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = TimeUtils.formatDifferenz(totalDiff),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    totalDiff > 0 -> OvertimeColor
                                    totalDiff < 0 -> UndertimeColor
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "$completedDays von ${entries.size} Tagen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    }
                }
            }
        }

        // Farb-Legende
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(StatusComplete, "Vollständig")
                    LegendItem(StatusPartial, "Teilweise")
                    LegendItem(StatusEmpty, "Leer")
                    LegendItem(StatusSpecial, "Urlaub/Krank")
                }
            }
        }

        // Wochentag-Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Kalender-Grid
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(80.dp * ((month.lengthOfMonth() + (month.atDay(1).dayOfWeek.value - 1)) / 7 + 1))
            ) {
                // Berechne Offset für ersten Tag des Monats
                val firstDayOfMonth = month.atDay(1)
                val dayOfWeek = firstDayOfMonth.dayOfWeek.value
                val offset = dayOfWeek - 1

                // Füge leere Zellen für Tage vor dem ersten des Monats hinzu
                items(offset) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                // Füge die eigentlichen Tage des Monats hinzu mit staggered Animation
                items(month.lengthOfMonth()) { day ->
                    val date = month.atDay(day + 1)
                    val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val entry = entries.find { it.datum == dateString }
                    val status = viewModel.getEntryStatus(entry)
                    val isToday = date == today

                    // Staggered Fade-in Animation
                    var visible by remember(month) { mutableStateOf(false) }
                    LaunchedEffect(month) {
                        delay((day * 15L).coerceAtMost(300))
                        visible = true
                    }

                    // Scale Animation beim Klicken
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "day_click_scale"
                    )

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300)) +
                                scaleIn(initialScale = 0.8f, animationSpec = tween(300))
                    ) {
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .scale(scale)
                                .then(
                                    if (isToday) Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    isPressed = true
                                    selectedDate = dateString
                                    showEditDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when (status) {
                                    EntryStatus.COMPLETE -> StatusComplete
                                    EntryStatus.PARTIAL -> StatusPartial
                                    EntryStatus.EMPTY -> StatusEmpty
                                    EntryStatus.SPECIAL -> StatusSpecial
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${day + 1}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    // Zeige Icon für Typ
                                    if (entry != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Icon(
                                            imageVector = when (entry.typ) {
                                                TimeEntry.TYP_URLAUB -> Icons.Default.BeachAccess
                                                TimeEntry.TYP_KRANK -> Icons.Default.LocalHospital
                                                TimeEntry.TYP_FEIERTAG -> Icons.Default.Event
                                                TimeEntry.TYP_ABWESEND -> Icons.Default.EventBusy
                                                else -> Icons.Default.Work
                                            },
                                            contentDescription = entry.typ,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && selectedDate != null) {
        val entry = entries.find { it.datum == selectedDate }

        EditEntryDialog(
            entry = entry,
            datum = selectedDate!!,
            onDismiss = {
                showEditDialog = false
                selectedDate = null
            },
            onSave = { startZeit, endZeit, pauseMinuten, typ, notiz ->
                viewModel.updateEntry(
                    date = selectedDate!!,
                    startZeit = startZeit,
                    endZeit = endZeit,
                    pauseMinuten = pauseMinuten,
                    typ = typ,
                    notiz = notiz
                )
                showEditDialog = false
                selectedDate = null
            },
            onDelete = {
                viewModel.deleteEntry(selectedDate!!)
                showEditDialog = false
                selectedDate = null
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
