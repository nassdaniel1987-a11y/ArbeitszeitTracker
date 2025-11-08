package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.components.*
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.HomeViewModel
import com.arbeitszeit.tracker.viewmodel.WeekSummary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCalendar: () -> Unit
) {
    val todayEntry by viewModel.todayEntry.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val weekEntries by viewModel.weekEntries.collectAsState()
    val selectedWeekDate by viewModel.selectedWeekDate.collectAsState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.quickStamp() }
            ) {
                Icon(Icons.Default.AccessTime, "Quick Stempel")
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
                TimeEntryCard(
                    entry = todayEntry,
                    onStartClick = { showStartTimePicker = true },
                    onEndClick = { showEndTimePicker = true },
                    onPauseClick = { showPauseDialog = true },
                    onTypChange = { viewModel.setTyp(it) }
                )
            }
            
            item {
                WeekNavigationHeader(
                    selectedWeekDate = selectedWeekDate,
                    isCurrentWeek = viewModel.isCurrentWeek(),
                    onPreviousWeek = { viewModel.previousWeek() },
                    onNextWeek = { viewModel.nextWeek() },
                    onGoToCurrentWeek = { viewModel.goToCurrentWeek() }
                )
            }

            items(weekEntries) { entry ->
                WeekEntryCard(entry = entry)
            }
            
            item {
                WeekSummaryCard(summary = viewModel.getWeekSummary())
            }
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    selectedWeekDate: LocalDate,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGoToCurrentWeek: () -> Unit
) {
    val weekDays = DateUtils.getDaysOfWeek(selectedWeekDate)
    val weekStart = weekDays.first()
    val weekEnd = weekDays.last()
    val weekNumber = DateUtils.getWeekOfYear(selectedWeekDate)

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
