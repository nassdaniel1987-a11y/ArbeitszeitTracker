package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.clickable
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
import com.arbeitszeit.tracker.ui.components.EditEntryDialog
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.viewmodel.CalendarViewModel
import com.arbeitszeit.tracker.viewmodel.EntryStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val month by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthEntries.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

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

        // Wochentag-Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Berechne Offset für ersten Tag des Monats
            val firstDayOfMonth = month.atDay(1)
            val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Montag, 7 = Sonntag
            val offset = dayOfWeek - 1 // Anzahl leerer Zellen am Anfang (0-6)

            // Füge leere Zellen für Tage vor dem ersten des Monats hinzu
            items(offset) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            // Füge die eigentlichen Tage des Monats hinzu
            items(month.lengthOfMonth()) { day ->
                val date = month.atDay(day + 1)
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val entry = entries.find { it.datum == dateString }
                val status = viewModel.getEntryStatus(entry)

                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${day + 1}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Zeige Kurzinfo wenn Daten vorhanden
                            if (entry != null && entry.startZeit != null) {
                                Text(
                                    entry.typ,
                                    style = MaterialTheme.typography.labelSmall
                                )
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
            }
        )
    }
}
