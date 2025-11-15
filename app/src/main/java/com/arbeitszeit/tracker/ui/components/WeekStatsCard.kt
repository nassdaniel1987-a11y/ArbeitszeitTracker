package com.arbeitszeit.tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.theme.OvertimeColor
import com.arbeitszeit.tracker.ui.theme.UndertimeColor
import com.arbeitszeit.tracker.utils.TimeUtils

@Composable
fun WeekStatsCard(
    istMinuten: Int,
    sollMinuten: Int,
    daysRemaining: Int
) {
    val differenz = istMinuten - sollMinuten
    val progress = if (sollMinuten > 0) (istMinuten.toFloat() / sollMinuten.toFloat()).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Diese Woche",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (daysRemaining > 0) {
                        Text(
                            text = "Noch $daysRemaining ${if (daysRemaining == 1) "Tag" else "Tage"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                    imageVector = when {
                        differenz > 0 -> Icons.Default.TrendingUp
                        differenz < 0 -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when {
                        differenz > 0 -> OvertimeColor
                        differenz < 0 -> UndertimeColor
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            // Fortschrittsbalken
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = when {
                        differenz >= 0 -> OvertimeColor
                        else -> UndertimeColor
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${TimeUtils.minutesToHoursMinutes(istMinuten)} / ${TimeUtils.minutesToHoursMinutes(sollMinuten)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Differenz-Anzeige
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        differenz > 0 -> "Überstunden"
                        differenz < 0 -> "Fehlstunden"
                        else -> "Ausgeglichen"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = TimeUtils.formatDifferenz(differenz),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        differenz > 0 -> OvertimeColor
                        differenz < 0 -> UndertimeColor
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
    }
}
