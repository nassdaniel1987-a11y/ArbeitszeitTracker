package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.TimeUtils

@Composable
fun WeekEntryCard(entry: TimeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Typ-Icon mit Farb-Background
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (entry.typ) {
                        TimeEntry.TYP_URLAUB -> TypeUrlaub.copy(alpha = 0.15f)
                        TimeEntry.TYP_KRANK -> TypeKrank.copy(alpha = 0.15f)
                        TimeEntry.TYP_FEIERTAG -> TypeFeiertag.copy(alpha = 0.15f)
                        TimeEntry.TYP_ABWESEND -> TypeAbwesend.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        imageVector = when (entry.typ) {
                            TimeEntry.TYP_URLAUB -> Icons.Default.BeachAccess
                            TimeEntry.TYP_KRANK -> Icons.Default.LocalHospital
                            TimeEntry.TYP_FEIERTAG -> Icons.Default.Event
                            TimeEntry.TYP_ABWESEND -> Icons.Default.EventBusy
                            else -> Icons.Default.Work
                        },
                        contentDescription = entry.typ,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = when (entry.typ) {
                            TimeEntry.TYP_URLAUB -> TypeUrlaub
                            TimeEntry.TYP_KRANK -> TypeKrank
                            TimeEntry.TYP_FEIERTAG -> TypeFeiertag
                            TimeEntry.TYP_ABWESEND -> TypeAbwesend
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${entry.wochentag} ${entry.datum.substring(8)}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Notiz-Indikator
                        if (entry.notiz.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    Icons.Filled.Comment,
                                    contentDescription = "Hat Notiz",
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (entry.typ == TimeEntry.TYP_NORMAL) {
                        Text(
                            "${TimeUtils.formatTimeForDisplay(entry.startZeit)} - ${TimeUtils.formatTimeForDisplay(entry.endZeit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            entry.typ,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Differenz-Anzeige
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (entry.getDifferenzMinuten() >= 0)
                    OvertimeColor.copy(alpha = 0.15f)
                else
                    UndertimeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    TimeUtils.formatDifferenz(entry.getDifferenzMinuten()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (entry.getDifferenzMinuten() >= 0) OvertimeColor else UndertimeColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}
