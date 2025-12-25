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

    // Pager für Swipe-Gesten
    val initialPage = 1200
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2400 }
    )

    // Berechne aktuellen Monat
    val currentPageMonth = remember(pagerState.currentPage) {
        val offset = pagerState.currentPage - initialPage
        month.plusMonths(offset.toLong())
    }

    // Synchronisiere ViewModel
    LaunchedEffect(currentPageMonth) {
        if (currentPageMonth != month) {
            viewModel.setMonth(currentPageMonth)
        }
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
                                    pagerState.animateScrollToPage(initialPage)
                                    viewModel.setMonth(month)
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
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val pageMonth = month.plusMonths((page - initialPage).toLong())
                val pageEntries = remember(pageMonth) {
                    if (pageMonth == month) entries else emptyList()
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
                    items(offset) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    // Tage des Monats
                    items(pageMonth.lengthOfMonth()) { day ->
                        val date = pageMonth.atDay(day + 1)
                        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val entry = entries.find { it.datum == dateString }
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
                onSave = { startZeit, endZeit, pauseMinuten, typ, notiz ->
                    viewModel.updateEntry(
                        date = selectedDate!!,
                        startZeit = startZeit,
                        endZeit = endZeit,
                        pauseMinuten = pauseMinuten,
                        typ = typ,
                        notiz = notiz
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
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cellBackgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isToday) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                // Tag-Nummer (GROß)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 18.sp,
                        color = if (isToday)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

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
                                .fillMaxWidth(0.85f)
                                .height(4.dp),
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                    }

                    // Entry-Status-Balken (GROß)
                    if (entry != null) {
                        val (barColor, label) = when {
                            entry.typ == TimeEntry.TYP_URLAUB -> Color(0xFF06B6D4) to "Urlaub"
                            entry.typ == TimeEntry.TYP_KRANK -> Color(0xFFEF4444) to "Krank"
                            entry.typ == TimeEntry.TYP_FEIERTAG -> Color(0xFF6366F1) to "Feiertag"
                            entry.typ == TimeEntry.TYP_ABWESEND -> Color(0xFF6B7280) to "Abwesend"
                            status == EntryStatus.COMPLETE -> Color(0xFF10B981) to "✓"
                            status == EntryStatus.PARTIAL -> Color(0xFFF59E0B) to "~"
                            else -> MaterialTheme.colorScheme.outline to "○"
                        }

                        // Großer farbiger Balken
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(18.dp),
                            color = barColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
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
