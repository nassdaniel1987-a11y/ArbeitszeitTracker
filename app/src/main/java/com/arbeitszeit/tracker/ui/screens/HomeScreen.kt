package com.arbeitszeit.tracker.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.components.*
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.HomeViewModel
import com.arbeitszeit.tracker.viewmodel.WeekSummary
import java.time.LocalDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToWeekTemplates: () -> Unit = {}
) {
    val todayEntry by viewModel.todayEntry.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val weekEntries by viewModel.weekEntries.collectAsState()
    val selectedWeekDate by viewModel.selectedWeekDate.collectAsState()
    val locationStatus by viewModel.locationStatus.collectAsState()
    val deletedEntry by viewModel.deletedEntry.collectAsState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showQuickActionMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Animation state for staggered fade-in
    var itemsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        itemsVisible = true
    }

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
                    viewModel.undoDeleteEntry()
                }
                SnackbarResult.Dismissed -> {
                    viewModel.clearDeletedEntry()
                }
            }
        }
    }

    val view = LocalView.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLICK)
                        viewModel.quickStamp()
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLICK)
                            viewModel.quickStamp()
                        },
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            showQuickActionMenu = true
                        }
                    )
                ) {
                    Icon(Icons.Default.Add, "Schnell stempeln")
                }

                // Quick Action Dropdown Menu
                DropdownMenu(
                    expanded = showQuickActionMenu,
                    onDismissRequest = { showQuickActionMenu = false },
                    offset = DpOffset(0.dp, (-8).dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("🏖️ Urlaub eintragen") },
                        leadingIcon = { Icon(Icons.Default.BeachAccess, null) },
                        onClick = {
                            viewModel.setTyp(TimeEntry.TYP_URLAUB)
                            showQuickActionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🤒 Krank eintragen") },
                        leadingIcon = { Icon(Icons.Default.LocalHospital, null) },
                        onClick = {
                            viewModel.setTyp(TimeEntry.TYP_KRANK)
                            showQuickActionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📅 Feiertag eintragen") },
                        leadingIcon = { Icon(Icons.Default.Event, null) },
                        onClick = {
                            viewModel.setTyp(TimeEntry.TYP_FEIERTAG)
                            showQuickActionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🚫 Abwesend eintragen") },
                        leadingIcon = { Icon(Icons.Default.EventBusy, null) },
                        onClick = {
                            viewModel.setTyp(TimeEntry.TYP_ABWESEND)
                            showQuickActionMenu = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("📋 Zum Kalender") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        onClick = {
                            onNavigateToCalendar()
                            showQuickActionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📝 Wochen-Vorlagen") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = {
                            onNavigateToWeekTemplates()
                            showQuickActionMenu = false
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 4 })
                ) {
                    TimeEntryCard(
                        entry = todayEntry,
                        onStartClick = { showStartTimePicker = true },
                        onEndClick = { showEndTimePicker = true },
                        onPauseClick = { showPauseDialog = true },
                        onTypChange = { viewModel.setTyp(it) }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                            slideInVertically(animationSpec = tween(400, delayMillis = 100), initialOffsetY = { it / 4 })
                ) {
                    GeofencingStatusCard(
                        locationStatus = locationStatus,
                        onRefresh = { viewModel.checkLocationStatus() }
                    )
                }
            }

            // Wochen-Statistik Dashboard
            item {
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 150)) +
                            slideInVertically(animationSpec = tween(400, delayMillis = 150), initialOffsetY = { it / 4 })
                ) {
                    val summary = viewModel.getWeekSummary()
                    val today = LocalDate.now()
                    val daysUntilWeekend = when (today.dayOfWeek.value) {
                        in 1..5 -> 6 - today.dayOfWeek.value // Mo-Fr: Tage bis Samstag
                        else -> 0 // Wochenende
                    }

                    WeekStatsCard(
                        istMinuten = summary.istMinuten,
                        sollMinuten = summary.sollMinuten,
                        daysRemaining = daysUntilWeekend
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                            slideInVertically(animationSpec = tween(400, delayMillis = 200), initialOffsetY = { it / 4 })
                ) {
                    WeekNavigationHeader(
                        selectedWeekDate = selectedWeekDate,
                        isCurrentWeek = viewModel.isCurrentWeek(),
                        onPreviousWeek = { viewModel.previousWeek() },
                        onNextWeek = { viewModel.nextWeek() },
                        onGoToCurrentWeek = { viewModel.goToCurrentWeek() },
                        firstMonday = userSettings?.ersterMontagImJahr
                    )
                }
            }

            items(
                items = weekEntries,
                key = { it.datum } // Wichtig für stabile Liste bei Swipe
            ) { entry ->
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 4 })
                ) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteEntry(entry.datum)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            // Roter Hintergrund mit Lösch-Icon beim Wischen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        content = {
                            WeekEntryCard(entry = entry)
                        }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = itemsVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) +
                            slideInVertically(animationSpec = tween(400, delayMillis = 300), initialOffsetY = { it / 4 })
                ) {
                    WeekSummaryCard(summary = viewModel.getWeekSummary())
                }
            }
        }
    }

    // Pause Dialog mit Slider
    if (showPauseDialog && todayEntry != null) {
        PauseSliderDialog(
            currentPauseMinutes = todayEntry.pauseMinuten,
            onDismiss = { showPauseDialog = false },
            onConfirm = { minutes ->
                viewModel.setPause(minutes)
                showPauseDialog = false
            }
        )
    }
}

@Composable
private fun PauseSliderDialog(
    currentPauseMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pauseMinutes by remember { mutableFloatStateOf(currentPauseMinutes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Coffee, "Pause") },
        title = { Text("Pause einstellen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Anzeige der aktuellen Pause
                Text(
                    text = "${pauseMinutes.toInt()} Minuten",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Slider (0-120 Minuten)
                Slider(
                    value = pauseMinutes,
                    onValueChange = { pauseMinutes = it },
                    valueRange = 0f..120f,
                    steps = 23, // Alle 5 Minuten: 0, 5, 10, ..., 120
                    modifier = Modifier.fillMaxWidth()
                )

                // Schnellauswahl-Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 15, 30, 45, 60).forEach { minutes ->
                        OutlinedButton(
                            onClick = { pauseMinutes = minutes.toFloat() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (pauseMinutes.toInt() == minutes) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                        ) {
                            Text("$minutes")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pauseMinutes.toInt()) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun WeekNavigationHeader(
    selectedWeekDate: LocalDate,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGoToCurrentWeek: () -> Unit,
    firstMonday: String? = null
) {
    val weekDays = DateUtils.getDaysOfWeek(selectedWeekDate)
    val weekStart = weekDays.first()
    val weekEnd = weekDays.last()
    val weekNumber = DateUtils.getCustomWeekOfYear(selectedWeekDate, firstMonday)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentWeek) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(Icons.Default.ArrowBack, "Vorherige Woche")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KW $weekNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${weekStart.dayOfMonth}.${weekStart.monthValue}. - ${weekEnd.dayOfMonth}.${weekEnd.monthValue}.${weekEnd.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNextWeek) {
                    Icon(Icons.Default.ArrowForward, "Nächste Woche")
                }
            }

            if (!isCurrentWeek) {
                Button(
                    onClick = onGoToCurrentWeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Zur aktuellen Woche")
                }
            }
        }
    }
}
