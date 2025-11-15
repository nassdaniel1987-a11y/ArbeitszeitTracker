package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.components.EmptyStates
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.viewmodel.MonthSummary
import com.arbeitszeit.tracker.viewmodel.UeberstundenViewModel
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UeberstundenScreen(
    viewModel: UeberstundenViewModel
) {
    val summary by viewModel.ueberstundenSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Überstunden") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gesamtüberstunden Card
            item {
                GesamtUeberstundenCard(
                    gesamtUeberstunden = summary.gesamtUeberstunden,
                    viewModel = viewModel
                )
            }

            // Details Card
            item {
                DetailsCard(
                    laufendesJahr = summary.laufendesJahr,
                    vorjahrUebertrag = summary.vorjahrUebertrag,
                    letzterUebertrag = summary.letzterUebertrag,
                    viewModel = viewModel
                )
            }

            // Monatliche Aufschlüsselung Header
            item {
                Text(
                    text = "Monatliche Aufschlüsselung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Monatsliste
            items(summary.monatsSummen) { monatsSummary ->
                MonatCard(
                    monatsSummary = monatsSummary,
                    viewModel = viewModel
                )
            }

            // Hinweis wenn keine Daten
            if (summary.monatsSummen.isEmpty()) {
                item {
                    EmptyStates.NoOvertimeData()
                }
            }
        }
    }
}

@Composable
private fun GesamtUeberstundenCard(
    gesamtUeberstunden: Int,
    viewModel: UeberstundenViewModel
) {
    val isPositive = gesamtUeberstunden >= 0
    val backgroundColor = if (isPositive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val textColor = if (isPositive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gesamtüberstunden",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.minutesToHoursString(gesamtUeberstunden),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "Stunden",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DetailsCard(
    laufendesJahr: Int,
    vorjahrUebertrag: Int,
    letzterUebertrag: Int,
    viewModel: UeberstundenViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            DetailRow(
                label = "Laufendes Jahr",
                value = viewModel.minutesToHoursString(laufendesJahr),
                icon = Icons.Default.CalendarToday
            )

            DetailRow(
                label = "Vorjahresübertrag",
                value = viewModel.minutesToHoursString(vorjahrUebertrag),
                icon = Icons.Default.History
            )

            if (letzterUebertrag != 0) {
                DetailRow(
                    label = "Letzter Übertrag (4 Wochen)",
                    value = viewModel.minutesToHoursString(letzterUebertrag),
                    icon = Icons.Default.SwapHoriz
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonatCard(
    monatsSummary: MonthSummary,
    viewModel: UeberstundenViewModel
) {
    val isPositive = monatsSummary.differenzMinuten >= 0
    val monatName = monatsSummary.yearMonth.month.getDisplayName(
        TextStyle.FULL,
        Locale.GERMAN
    )
    val jahr = monatsSummary.yearMonth.year

    // Calculate progress percentage
    val progress = if (monatsSummary.sollMinutenGesamt > 0) {
        (monatsSummary.istMinutenGesamt.toFloat() / monatsSummary.sollMinutenGesamt.toFloat())
            .coerceIn(0f, 1.5f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$monatName $jahr",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${monatsSummary.anzahlTage} Tage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPositive) OvertimeColor else UndertimeColor
                    )
                    Text(
                        text = viewModel.minutesToHoursString(monatsSummary.differenzMinuten),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) OvertimeColor else UndertimeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                progress >= 1.0f -> ProgressGood
                                progress >= 0.8f -> ProgressWarning
                                else -> ProgressBad
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Soll",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.minutesToHoursString(monatsSummary.sollMinutenGesamt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Erfüllung",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Ist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.minutesToHoursString(monatsSummary.istMinutenGesamt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
