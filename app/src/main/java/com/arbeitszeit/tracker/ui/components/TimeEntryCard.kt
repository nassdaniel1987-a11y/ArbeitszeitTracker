@file:OptIn(ExperimentalMaterial3Api::class)

package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Heute", style = MaterialTheme.typography.titleLarge)
                    Text(
                        DateUtils.formatForDisplayWithWeekday(LocalDate.now()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Aktueller Typ-Badge
                Surface(
                    color = when (entry?.typ) {
                        TimeEntry.TYP_NORMAL -> MaterialTheme.colorScheme.primary
                        TimeEntry.TYP_URLAUB -> Color(0xFF4CAF50)
                        TimeEntry.TYP_KRANK -> Color(0xFFF44336)
                        TimeEntry.TYP_FEIERTAG -> Color(0xFF2196F3)
                        TimeEntry.TYP_ABWESEND -> Color(0xFF9E9E9E)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (entry?.typ) {
                            TimeEntry.TYP_NORMAL -> "Arbeit"
                            TimeEntry.TYP_URLAUB -> "Urlaub"
                            TimeEntry.TYP_KRANK -> "Krank"
                            TimeEntry.TYP_FEIERTAG -> "Feiertag"
                            TimeEntry.TYP_ABWESEND -> "Abwesend"
                            else -> "?"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider()

            // Schnellaktionen für Typ-Auswahl
            Text(
                text = "Schnellauswahl",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTypeButton(
                    icon = Icons.Default.Work,
                    label = "Arbeit",
                    type = TimeEntry.TYP_NORMAL,
                    isSelected = entry?.typ == TimeEntry.TYP_NORMAL,
                    onClick = { onTypChange(TimeEntry.TYP_NORMAL) },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QuickTypeButton(
                    icon = Icons.Default.BeachAccess,
                    label = "Urlaub",
                    type = TimeEntry.TYP_URLAUB,
                    isSelected = entry?.typ == TimeEntry.TYP_URLAUB,
                    onClick = { onTypChange(TimeEntry.TYP_URLAUB) },
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                QuickTypeButton(
                    icon = Icons.Default.LocalHospital,
                    label = "Krank",
                    type = TimeEntry.TYP_KRANK,
                    isSelected = entry?.typ == TimeEntry.TYP_KRANK,
                    onClick = { onTypChange(TimeEntry.TYP_KRANK) },
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTypeButton(
                    icon = Icons.Default.Event,
                    label = "Feiertag",
                    type = TimeEntry.TYP_FEIERTAG,
                    isSelected = entry?.typ == TimeEntry.TYP_FEIERTAG,
                    onClick = { onTypChange(TimeEntry.TYP_FEIERTAG) },
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                QuickTypeButton(
                    icon = Icons.Default.EventBusy,
                    label = "Abwesend",
                    type = TimeEntry.TYP_ABWESEND,
                    isSelected = entry?.typ == TimeEntry.TYP_ABWESEND,
                    onClick = { onTypChange(TimeEntry.TYP_ABWESEND) },
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
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

@Composable
fun QuickTypeButton(
    icon: ImageVector,
    label: String,
    type: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = if (isSelected) 2.dp else 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) color else MaterialTheme.colorScheme.outline
            )
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
