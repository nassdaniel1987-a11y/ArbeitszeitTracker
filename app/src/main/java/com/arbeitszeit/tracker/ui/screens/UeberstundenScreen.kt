package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.ui.components.EmptyStates
import com.arbeitszeit.tracker.ui.theme.*
import com.arbeitszeit.tracker.viewmodel.MonthSummary
import com.arbeitszeit.tracker.viewmodel.UeberstundenViewModel
import java.time.YearMonth
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
                title = { Text("Zeitkonto") },
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
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Haupt-Card: Gesamtüberstunden
            item {
                GesamtUeberstundenCard(
                    gesamtUeberstunden = summary.gesamtUeberstunden,
                    laufendesJahr = summary.laufendesJahr,
                    vorjahrUebertrag = summary.vorjahrUebertrag,
                    viewModel = viewModel
                )
            }

            // Monatliche Übersicht Header
            if (summary.monatsSummen.isNotEmpty()) {
                item {
                    Text(
                        text = "Monatliche Übersicht",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Monatsliste - kompakt
                items(summary.monatsSummen) { monatsSummary ->
                    MonatRow(
                        monatsSummary = monatsSummary,
                        viewModel = viewModel
                    )
                }
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
    laufendesJahr: Int,
    vorjahrUebertrag: Int,
    viewModel: UeberstundenViewModel
) {
    val isPositive = gesamtUeberstunden >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hauptanzeige: Gesamtüberstunden
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isPositive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    Column {
                        Text(
                            text = "Gesamtüberstunden",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isPositive) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            }
                        )

                        Text(
                            text = viewModel.minutesToHoursString(gesamtUeberstunden),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (isPositive) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                }
            )

            // Aufschlüsselung
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dieses Jahr
                Column {
                    Text(
                        text = "Dieses Jahr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPositive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (laufendesJahr >= 0) Icons.Default.Add else Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (laufendesJahr >= 0) OvertimeColor else UndertimeColor
                        )
                        Text(
                            text = viewModel.minutesToHoursString(laufendesJahr),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (laufendesJahr >= 0) OvertimeColor else UndertimeColor
                        )
                    }
                }

                // Vorjahresübertrag
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vorjahresübertrag",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPositive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (vorjahrUebertrag >= 0) Icons.Default.Add else Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (vorjahrUebertrag >= 0) OvertimeColor else UndertimeColor
                        )
                        Text(
                            text = viewModel.minutesToHoursString(vorjahrUebertrag),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (vorjahrUebertrag >= 0) OvertimeColor else UndertimeColor
                        )
                    }
                }
            }

            // Hinweis wenn Vorjahresübertrag 0 ist
            if (vorjahrUebertrag == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPositive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                        }
                    )
                    Text(
                        text = "Vorjahresübertrag in Einstellungen → Persönliche Daten hinterlegen",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPositive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Kompakte Monatszeile - zeigt nur Monat und +/- Stunden
 */
@Composable
private fun MonatRow(
    monatsSummary: MonthSummary,
    viewModel: UeberstundenViewModel
) {
    val isPositive = monatsSummary.differenzMinuten >= 0
    val monatName = monatsSummary.yearMonth.month.getDisplayName(
        TextStyle.FULL,
        Locale.GERMAN
    )
    val jahr = monatsSummary.yearMonth.year
    val isCurrentMonth = monatsSummary.yearMonth == YearMonth.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMonth) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Linke Seite: Monat + Tage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Farbindikator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPositive) OvertimeColor else UndertimeColor)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$monatName $jahr",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCurrentMonth) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Aktuell",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${monatsSummary.anzahlTage} Arbeitstage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Rechte Seite: Überstunden
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
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
    }
}
