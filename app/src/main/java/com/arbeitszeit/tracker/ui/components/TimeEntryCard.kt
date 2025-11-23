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
    // Check if time is currently running
    val isRunning = entry?.startZeit != null && entry.endZeit == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pulsing(enabled = isRunning, minScale = 0.98f, maxScale = 1.02f, duration = 2000),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            // Hero Header mit Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = when (entry?.typ) {
                            TimeEntry.TYP_URLAUB -> GradientSuccess
                            TimeEntry.TYP_KRANK -> GradientError
                            TimeEntry.TYP_FEIERTAG -> GradientCyan
                            else -> GradientPrimary
                        }
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Heute",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                DateUtils.formatForDisplayWithWeekday(LocalDate.now()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Status Badge
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.3f)
                            )
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Große Zeit-Anzeige bei Arbeit
                    if (entry?.typ == TimeEntry.TYP_NORMAL) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HeroTimeDisplay(
                                label = "Ist",
                                time = TimeUtils.minutesToHoursMinutes(entry.getIstMinuten())
                            )
                            HeroTimeDisplay(
                                label = "Soll",
                                time = TimeUtils.minutesToHoursMinutes(entry.sollMinuten)
                            )
                            HeroTimeDisplay(
                                label = "Diff",
                                time = TimeUtils.formatDifferenz(entry.getDifferenzMinuten()),
                                highlight = entry.getDifferenzMinuten() != 0
                            )
                        }
                    }
                }
            }

            // Content Area - weiß
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Zeit Buttons - nur bei Arbeit
                if (entry?.typ == TimeEntry.TYP_NORMAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernTimeButton("Von", entry.startZeit, onStartClick, Modifier.weight(1f))
                        ModernTimeButton("Bis", entry.endZeit, onEndClick, Modifier.weight(1f))
                        ModernTimeButton("Pause", entry.pauseMinuten, onPauseClick, Modifier.weight(1f), true)
                    }
                }

                // Typ-Auswahl
                Text(
                    text = "Schnellauswahl",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
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
                        color = TypeNormal,
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeButton(
                        icon = Icons.Default.BeachAccess,
                        label = "Urlaub",
                        type = TimeEntry.TYP_URLAUB,
                        isSelected = entry?.typ == TimeEntry.TYP_URLAUB,
                        onClick = { onTypChange(TimeEntry.TYP_URLAUB) },
                        color = TypeUrlaub,
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeButton(
                        icon = Icons.Default.LocalHospital,
                        label = "Krank",
                        type = TimeEntry.TYP_KRANK,
                        isSelected = entry?.typ == TimeEntry.TYP_KRANK,
                        onClick = { onTypChange(TimeEntry.TYP_KRANK) },
                        color = TypeKrank,
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
                        color = TypeFeiertag,
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeButton(
                        icon = Icons.Default.EventBusy,
                        label = "Abwesend",
                        type = TimeEntry.TYP_ABWESEND,
                        isSelected = entry?.typ == TimeEntry.TYP_ABWESEND,
                        onClick = { onTypChange(TimeEntry.TYP_ABWESEND) },
                        color = TypeAbwesend,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroTimeDisplay(
    label: String,
    time: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            time,
            style = MaterialTheme.typography.headlineMedium,
            color = if (highlight) Color.White else Color.White.copy(alpha = 0.95f),
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
fun ModernTimeButton(
    label: String,
    time: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPause: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isPause && time != null) TimeUtils.minutesToHoursMinutes(time)
                else TimeUtils.formatTimeForDisplay(time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
