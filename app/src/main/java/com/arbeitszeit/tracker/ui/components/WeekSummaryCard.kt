package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.WeekSummary

@Composable
fun WeekSummaryCard(summary: WeekSummary) {
    val progress = if (summary.sollMinuten > 0) {
        ((summary.istMinuten.toFloat() / summary.sollMinuten.toFloat()) * 100).toInt().coerceIn(0, 150)
    } else 0

    val isOvertime = summary.differenzMinuten > 0
    val isUndertime = summary.differenzMinuten < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOvertime -> MaterialTheme.colorScheme.primaryContainer
                isUndertime -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header mit Icon und Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wochen-Zusammenfassung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Status Icon
                    if (isOvertime) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Überstunden",
                            tint = OvertimeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isUndertime) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Fehlstunden",
                            tint = UndertimeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Differenz prominent
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TimeUtils.formatDifferenz(summary.differenzMinuten),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOvertime -> OvertimeColor
                            isUndertime -> UndertimeColor
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = when {
                            isOvertime -> "Überstunden"
                            isUndertime -> "Fehlstunden"
                            else -> "Ausgeglichen"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fortschrittsbalken
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Soll: ${TimeUtils.minutesToHoursMinutes(summary.sollMinuten)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ist: ${TimeUtils.minutesToHoursMinutes(summary.istMinuten)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Custom Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (progress.toFloat() / 100f).coerceIn(0f, 1f))
                            .height(12.dp)
                            .background(
                                color = when {
                                    progress >= 100 -> ProgressGood
                                    progress >= 80 -> ProgressWarning
                                    else -> ProgressBad
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "$progress% der Soll-Zeit erfüllt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Zusätzliche Info: Vollständigkeit
            if (summary.totalDays > 0) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vollständig",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${summary.completedDays} von ${summary.totalDays} Tagen",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
