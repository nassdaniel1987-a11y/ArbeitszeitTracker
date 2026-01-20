package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.components.EditEntryDialog
import com.arbeitszeit.tracker.utils.HolidayUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.viewmodel.CalendarViewModel
import com.arbeitszeit.tracker.viewmodel.EntryStatus
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * GROßER Kalender im Android-Stil
 * - Nutzt den ganzen Bildschirm
 * - Große, gut lesbare Zellen
 * - Deutlich sichtbare Event-Infos
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val month by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthEntries.collectAsState()
    val deletedEntry by viewModel.deletedEntry.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = LocalDate.now()
    val scope = rememberCoroutineScope()

    // Fester Referenzmonat für Pager-Berechnung (verhindert Sprünge)
    val referenceMonth = remember { YearMonth.of(2020, 1) }
    val initialPage = 1200

    // Berechne initialPage basierend auf aktuellem Monat
    val startPage = remember(month) {
        val monthsSinceReference = java.time.temporal.ChronoUnit.MONTHS.between(referenceMonth, month)
        initialPage + monthsSinceReference.toInt()
    }

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { 2400 }
    )

    // Berechne aktuellen Monat basierend auf Pager-Position
    val currentPageMonth = remember(pagerState.currentPage) {
        val offset = pagerState.currentPage - initialPage
        referenceMonth.plusMonths(offset.toLong())
    }

    // Synchronisiere ViewModel nur wenn wirklich nötig
    LaunchedEffect(currentPageMonth) {
        viewModel.setMonth(currentPageMonth)
    }

    // Snackbar für Undo
    LaunchedEffect(deletedEntry) {
        if (deletedEntry != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Eintrag gelöscht",
                actionLabel = "Rückgängig",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDeleteEntry()
                SnackbarResult.Dismissed -> viewModel.clearDeletedEntry()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TopBar: Monat + Heute-Button + Statistik in einer Zeile
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monat/Jahr
                        Text(
                            text = currentPageMonth.atDay(1).format(
                                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Heute-Button
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val currentMonth = YearMonth.now()
                                    val monthsSinceReference = java.time.temporal.ChronoUnit.MONTHS.between(referenceMonth, currentMonth)
                                    val targetPage = initialPage + monthsSinceReference.toInt()
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Heute", fontSize = 13.sp)
                        }
                    }

                    // Kompakte Statistik
                    if (entries.isNotEmpty()) {
                        val (totalSoll, totalIst, totalDiff) = remember(entries) {
                            val soll = entries.sumOf { it.sollMinuten }
                            val ist = entries.sumOf { it.getIstMinuten() }
                            Triple(soll, ist, ist - soll)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Soll: ${TimeUtils.minutesToHoursMinutes(totalSoll)} • Ist: ${TimeUtils.minutesToHoursMinutes(totalIst)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeUtils.formatDifferenz(totalDiff),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    totalDiff > 0 -> Color(0xFF10B981)
                                    totalDiff < 0 -> Color(0xFFEF4444)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Wochentag-Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // GROßER Kalender mit Swipe
            HorizontalPager(
                state = pagerState,
                key = { page ->
                    val pageMonth = referenceMonth.plusMonths((page - initialPage).toLong())
                    "${pageMonth.year}-${pageMonth.monthValue}"
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val pageMonth = referenceMonth.plusMonths((page - initialPage).toLong())
                val pageEntries by produceState(initialValue = emptyList(), pageMonth) {
                    value = viewModel.getEntriesForMonth(pageMonth)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Offset für ersten Tag
                    val firstDayOfMonth = pageMonth.atDay(1)
                    val offset = firstDayOfMonth.dayOfWeek.value - 1

                    // Leere Zellen
                    items(
                        count = offset,
                        key = { index -> "page_${page}_offset_$index" }
                    ) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    // Tage des Monats
                    items(
                        count = pageMonth.lengthOfMonth(),
                        key = { day ->
                            "page_${page}_day_${day + 1}"
                        }
                    ) { day ->
                        val date = pageMonth.atDay(day + 1)
                        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val entry = pageEntries.find { it.datum == dateString }
                        val status = viewModel.getEntryStatus(entry)
                        val isToday = date == today

                        LargeDayCell(
                            date = date,
                            entry = entry,
                            status = status,
                            isToday = isToday,
                            userSettings = userSettings,
                            onClick = {
                                selectedDate = dateString
                                showEditDialog = true
                            }
                        )
                    }
                }
            }

            // Legende
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Legende",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Erste Spalte
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LegendItem(Color(0xFF10B981), "✓", "Vollständig")
                            LegendItem(Color(0xFFF59E0B), "~", "Teilweise")
                            LegendItem(Color(0xFF06B6D4), "U", "Urlaub")
                            LegendItem(Color(0xFF6B7280), "A", "Abwesend")
                        }

                        // Zweite Spalte
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LegendItem(Color(0xFFEF4444), "K", "Krank")
                            LegendItem(Color(0xFF6366F1), "F", "Feiertag (Typ)")
                            LegendItem(Color(0xFFFFD700), "—", "Feiertag (Datum)")
                        }
                    }
                }
            }
        }

        // Edit Dialog
        if (showEditDialog && selectedDate != null) {
            val entry = entries.find { it.datum == selectedDate }

            EditEntryDialog(
                entry = entry,
                datum = selectedDate!!,
                onDismiss = {
                    showEditDialog = false
                    selectedDate = null
                },
                onSave = { startZeit, endZeit, pauseMinuten, typ, notiz, urlaubsJahr ->
                    viewModel.updateEntry(
                        date = selectedDate!!,
                        startZeit = startZeit,
                        endZeit = endZeit,
                        pauseMinuten = pauseMinuten,
                        typ = typ,
                        notiz = notiz,
                        urlaubsJahr = urlaubsJahr
                    )
                    showEditDialog = false
                    selectedDate = null
                },
                onDelete = {
                    viewModel.deleteEntry(selectedDate!!)
                    showEditDialog = false
                    selectedDate = null
                }
            )
        }
    }
}

/**
 * Legende-Item: Farbiger Balken + Label + Beschreibung
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Farbiger Balken (wie im Kalender)
        Surface(
            modifier = Modifier
                .width(32.dp)
                .height(14.dp),
            color = color,
            shape = RoundedCornerShape(3.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Beschreibung
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * GROßE Tag-Zelle mit deutlich sichtbaren Infos
 */
@Composable
private fun LargeDayCell(
    date: LocalDate,
    entry: TimeEntry?,
    status: EntryStatus,
    isToday: Boolean,
    userSettings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onClick: () -> Unit
) {
    val bundesland = HolidayUtils.Bundesland.fromShortCode(userSettings?.bundesland)
    val isHoliday = HolidayUtils.isHoliday(date, bundesland)

    // Hintergrund für Tage mit Einträgen
    val cellBackgroundColor = when {
        entry != null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .then(
                if (isToday) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = cellBackgroundColor
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (entry != null) 1.dp else 0.dp,
            pressedElevation = 2.dp,
            hoveredElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                // Tag-Nummer
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isToday)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Event-Infos (GROß und deutlich)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feiertag
                    if (isHoliday) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(3.dp),
                            color = Color(0xFFFFD700),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {}
                    }

                    // Entry-Status-Balken
                    if (entry != null) {
                        val (barColor, label) = when {
                            entry.typ == TimeEntry.TYP_URLAUB -> Color(0xFF06B6D4) to "U"
                            entry.typ == TimeEntry.TYP_KRANK -> Color(0xFFEF4444) to "K"
                            entry.typ == TimeEntry.TYP_FEIERTAG -> Color(0xFF6366F1) to "F"
                            entry.typ == TimeEntry.TYP_ABWESEND -> Color(0xFF6B7280) to "A"
                            status == EntryStatus.COMPLETE -> Color(0xFF10B981) to "✓"
                            status == EntryStatus.PARTIAL -> Color(0xFFF59E0B) to "~"
                            else -> MaterialTheme.colorScheme.outline to "○"
                        }

                        // Farbiger Status-Balken
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(16.dp),
                            color = barColor,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Arbeitszeit (GROß und lesbar)
                        if (entry.typ == TimeEntry.TYP_NORMAL) {
                            val timeText = when {
                                entry.startZeit != null && entry.endZeit != null -> {
                                    "${TimeUtils.minutesToTimeString(entry.startZeit)}-${TimeUtils.minutesToTimeString(entry.endZeit)}"
                                }
                                entry.startZeit != null -> TimeUtils.minutesToTimeString(entry.startZeit)
                                entry.endZeit != null -> TimeUtils.minutesToTimeString(entry.endZeit)
                                else -> null
                            }

                            timeText?.let {
                                Text(
                                    text = it,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Differenz
                            val diff = entry.getDifferenzMinuten()
                            if (diff != 0) {
                                Text(
                                    text = TimeUtils.formatDifferenz(diff),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
