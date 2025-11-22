package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.components.EmptyStates
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.ui.theme.Green50
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
    val urlaubsSummary by viewModel.urlaubsSummary.collectAsState()
    val weeklyData by viewModel.weeklyData.collectAsState()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Überstunden") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
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

            // Urlaubs Card
            item {
                UrlaubsCard(
                    urlaubsSummary = urlaubsSummary
                )
            }

            // Überstunden-Trend Chart
            if (weeklyData.isNotEmpty()) {
                item {
                    UeberstundenTrendChart(
                        weeklyData = weeklyData,
                        viewModel = viewModel
                    )
                }
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

/**
 * Urlaubs Card - Zeigt Urlaubsanspruch und Resturlaub
 */
@Composable
private fun UrlaubsCard(
    urlaubsSummary: com.arbeitszeit.tracker.viewmodel.UrlaubsSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Green50
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Titel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BeachAccess,
                    contentDescription = null,
                    tint = Green700
                )
                Text(
                    "Urlaubsübersicht ${java.time.LocalDate.now().year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            // Hauptanzeige
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Anspruch
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Anspruch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${urlaubsSummary.urlaubsanspruch}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green700
                    )
                    Text(
                        "Tage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Verbraucht
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Verbraucht",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${urlaubsSummary.verbraucht}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Tage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Resturlaub
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Resturlaub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${urlaubsSummary.resturlaub}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (urlaubsSummary.resturlaub < 5) Orange700 else Green700
                    )
                    Text(
                        "Tage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fortschrittsbalken
            val progress = if (urlaubsSummary.urlaubsanspruch > 0) {
                urlaubsSummary.verbraucht.toFloat() / urlaubsSummary.urlaubsanspruch.toFloat()
            } else 0f

            Column {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress > 0.9f) Orange700 else Green700,
                )

                Text(
                    "${(progress * 100).toInt()}% verbraucht",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Info-Text für Krankheitstage
            if (urlaubsSummary.krankheitstage > 0) {
                Divider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = Red700,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Krankheitstage: ${urlaubsSummary.krankheitstage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Überstunden-Trend Chart - Zeigt wöchentliche Überstunden als Balkendiagramm
 */
@Composable
private fun UeberstundenTrendChart(
    weeklyData: List<com.arbeitszeit.tracker.viewmodel.WeekData>,
    viewModel: UeberstundenViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Überstunden-Trend (12 Wochen)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            if (weeklyData.isNotEmpty()) {
                val maxAbsValue = weeklyData.maxOfOrNull { kotlin.math.abs(it.differenzMinuten) } ?: 1
                val chartHeight = 200.dp

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(vertical = 8.dp)
                ) {
                    val barWidth = size.width / (weeklyData.size * 1.5f)
                    val chartCenterY = size.height / 2f
                    val maxBarHeight = size.height / 2f - 20f

                    // Draw zero line
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, chartCenterY),
                        end = Offset(size.width, chartCenterY),
                        strokeWidth = 2f
                    )

                    // Draw bars
                    weeklyData.forEachIndexed { index, week ->
                        val barHeight = (week.differenzMinuten.toFloat() / maxAbsValue) * maxBarHeight
                        val x = (index * size.width / weeklyData.size) + barWidth / 2
                        val barColor = if (week.differenzMinuten >= 0) {
                            androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
                        } else {
                            androidx.compose.ui.graphics.Color(0xFFF44336) // Red
                        }

                        val barTop = if (week.differenzMinuten >= 0) {
                            chartCenterY - barHeight
                        } else {
                            chartCenterY
                        }

                        val barBottom = if (week.differenzMinuten >= 0) {
                            chartCenterY
                        } else {
                            chartCenterY + kotlin.math.abs(barHeight)
                        }

                        // Draw bar
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x - barWidth / 2, barTop),
                            size = androidx.compose.ui.geometry.Size(
                                barWidth,
                                kotlin.math.abs(barBottom - barTop)
                            )
                        )
                    }
                }

                // Week labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (weeklyData.size >= 2) {
                        val firstWeek = weeklyData.first()
                        val lastWeek = weeklyData.last()

                        Text(
                            text = "KW ${firstWeek.weekNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "KW ${lastWeek.weekNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Green500, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = " Überstunden",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Red500, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = " Fehlstunden",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(
                    text = "Keine Daten verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
