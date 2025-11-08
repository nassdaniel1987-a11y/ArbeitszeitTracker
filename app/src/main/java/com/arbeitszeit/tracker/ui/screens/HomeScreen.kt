package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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
                Text("Diese Woche", style = MaterialTheme.typography.titleMedium)
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
