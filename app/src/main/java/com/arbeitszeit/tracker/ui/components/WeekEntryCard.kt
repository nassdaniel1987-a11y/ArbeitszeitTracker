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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Typ-Icon (nur bei Sondertypen)
                    when (entry.typ) {
                        TimeEntry.TYP_URLAUB -> Icon(
                            Icons.Default.BeachAccess,
                            contentDescription = "Urlaub",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        TimeEntry.TYP_KRANK -> Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = "Krank",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        TimeEntry.TYP_FEIERTAG -> Icon(
                            Icons.Default.Event,
                            contentDescription = "Feiertag",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        TimeEntry.TYP_ABWESEND -> Icon(
                            Icons.Default.EventBusy,
                            contentDescription = "Abwesend",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("${entry.wochentag} ${entry.datum.substring(8)}")

                    // Notiz-Icon
                    if (entry.notiz.isNotEmpty()) {
                        Icon(
                            Icons.Default.Comment,
                            contentDescription = "Hat Notiz",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (entry.typ == TimeEntry.TYP_NORMAL) {
                    Text(
                        "${TimeUtils.formatTimeForDisplay(entry.startZeit)} - ${TimeUtils.formatTimeForDisplay(entry.endZeit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        entry.typ,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                TimeUtils.formatDifferenz(entry.getDifferenzMinuten()),
                color = if (entry.getDifferenzMinuten() >= 0) OvertimeColor else UndertimeColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
