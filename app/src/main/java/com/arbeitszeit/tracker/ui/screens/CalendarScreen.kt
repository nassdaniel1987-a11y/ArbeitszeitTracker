package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.viewmodel.CalendarViewModel
import com.arbeitszeit.tracker.viewmodel.EntryStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val month by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthEntries.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.Default.ChevronLeft, "Vorheriger Monat")
            }
            Text("${month.month} ${month.year}", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.Default.ChevronRight, "Nächster Monat")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(month.lengthOfMonth()) { day ->
                val date = month.atDay(day + 1)
                val entry = entries.find { it.datum == date.toString() }
                val status = viewModel.getEntryStatus(entry)
                
                Card(
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
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("${day + 1}")
                    }
                }
            }
        }
    }
}
