package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.TimeUtils

@Composable
fun WeekEntryCard(entry: TimeEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${entry.wochentag} ${entry.datum.substring(8)}")
                Text(
                    "${TimeUtils.formatTimeForDisplay(entry.startZeit)} - ${TimeUtils.formatTimeForDisplay(entry.endZeit)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                TimeUtils.formatDifferenz(entry.getDifferenzMinuten()),
                color = if (entry.getDifferenzMinuten() >= 0) Green500 else Red500
            )
        }
    }
}
