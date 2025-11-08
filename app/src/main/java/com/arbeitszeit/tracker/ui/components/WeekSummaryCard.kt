package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.WeekSummary

@Composable
fun WeekSummaryCard(summary: WeekSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Wochen-Zusammenfassung", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Soll: ${TimeUtils.minutesToHoursMinutes(summary.sollMinuten)}")
                    Text("Ist: ${TimeUtils.minutesToHoursMinutes(summary.istMinuten)}")
                }
                Text(
                    TimeUtils.formatDifferenz(summary.differenzMinuten),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (summary.differenzMinuten >= 0) Green500 else Red500
                )
            }
        }
    }
}
