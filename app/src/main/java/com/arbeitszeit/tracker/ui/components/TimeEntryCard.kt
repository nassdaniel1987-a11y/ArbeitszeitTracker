@file:OptIn(ExperimentalMaterial3Api::class)

package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import java.time.LocalDate

@Composable
fun TimeEntryCard(
    entry: TimeEntry?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onPauseClick: () -> Unit,
    onTypChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Heute", style = MaterialTheme.typography.titleLarge)
            Text(DateUtils.formatForDisplayWithWeekday(LocalDate.now()))

            Divider()

            // Typ Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Normal" to TimeEntry.TYP_NORMAL,
                    "U" to TimeEntry.TYP_URLAUB,
                    "K" to TimeEntry.TYP_KRANK,
                    "F" to TimeEntry.TYP_FEIERTAG,
                    "AB" to TimeEntry.TYP_ABWESEND
                ).forEach { (label, typ) ->
                    FilterChip(
                        selected = entry?.typ == typ,
                        onClick = { onTypChange(typ) },
                        label = { Text(label) }
                    )
                }
            }

            // Zeit Buttons
            if (entry?.typ == TimeEntry.TYP_NORMAL) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeButton("Von", entry.startZeit, onStartClick, Modifier.weight(1f))
                    TimeButton("Bis", entry.endZeit, onEndClick, Modifier.weight(1f))
                    TimeButton("Pause", entry.pauseMinuten, onPauseClick, Modifier.weight(1f), true)
                }

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusText("Soll", TimeUtils.minutesToHoursMinutes(entry.sollMinuten))
                    StatusText("Ist", TimeUtils.minutesToHoursMinutes(entry.getIstMinuten()))
                    StatusText(
                        "Diff",
                        TimeUtils.formatDifferenz(entry.getDifferenzMinuten()),
                        if (entry.getDifferenzMinuten() >= 0) Green500 else Red500
                    )
                }
            }
        }
    }
}

@Composable
fun TimeButton(
    label: String,
    time: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPause: Boolean = false
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                if (isPause && time != null) TimeUtils.minutesToHoursMinutes(time)
                else TimeUtils.formatTimeForDisplay(time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusText(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
