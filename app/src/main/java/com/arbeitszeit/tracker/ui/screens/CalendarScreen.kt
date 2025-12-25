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
 * Kalender im Android/Google Kalender-Stil
 * - Wie die echte Kalender-App
 * - Zeigt Event-Infos unter jedem Tag
 * - Clean Design, gut lesbar
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
            // TopBar: Monat + Heute-Button
            CalendarTopBar(
                month = currentPageMonth,
                onTodayClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(initialPage)
                        viewModel.setMonth(month)
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Kompakte Statistik
            MonthStatistics(
                entries = entries,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Wochentag-Header
            WeekdayHeader(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Kalender mit Swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageMonth = month.plusMonths((page - initialPage).toLong())
                val pageEntries = remember(pageMonth) {
                    if (pageMonth == month) entries else emptyList()
                }

                CalendarGrid(
                    month = pageMonth,
                    entries = pageEntries,
                    today = today,
                    userSettings = userSettings,
                    onDayClick = { date ->
                        selectedDate = date
                        showEditDialog = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                )
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
 * TopBar: Monat/Jahr + Heute-Button
 */
@Composable
private fun CalendarTopBar(
    month: YearMonth,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month.atDay(1).format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FilledTonalButton(
            onClick = onTodayClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Today,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Heute", fontSize = 14.sp)
        }
    }
}

/**
 * Kompakte Statistik
 */
@Composable
private fun MonthStatistics(
    entries: List<TimeEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val (totalSoll, totalIst, totalDiff) = remember(entries) {
        val soll = entries.sumOf { it.sollMinuten }
        val ist = entries.sumOf { it.getIstMinuten() }
        val diff = ist - soll
        Triple(soll, ist, diff)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Soll: ${TimeUtils.minutesToHoursMinutes(totalSoll)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ist: ${TimeUtils.minutesToHoursMinutes(totalIst)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimeUtils.formatDifferenz(totalDiff),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    totalDiff > 0 -> Color(0xFF10B981)
                    totalDiff < 0 -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "${entries.count { it.isComplete() }} / ${entries.size} Tage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Wochentag-Header
 */
@Composable
private fun WeekdayHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Kalender-Grid
 */
@Composable
private fun CalendarGrid(
    month: YearMonth,
    entries: List<TimeEntry>,
    today: LocalDate,
    userSettings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onDayClick: (String) -> Unit,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier
    ) {
        // Offset für ersten Tag
        val firstDayOfMonth = month.atDay(1)
        val offset = firstDayOfMonth.dayOfWeek.value - 1

        // Leere Zellen
        items(offset) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        // Tage des Monats
        items(month.lengthOfMonth()) { day ->
            val date = month.atDay(day + 1)
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entry = entries.find { it.datum == dateString }
            val status = viewModel.getEntryStatus(entry)
            val isToday = date == today

            AndroidCalendarDayCell(
                date = date,
                entry = entry,
                status = status,
                isToday = isToday,
                userSettings = userSettings,
                onClick = { onDayClick(dateString) }
            )
        }
    }
}

/**
 * Tag-Zelle wie in der Android Kalender-App
 * - Tag-Nummer oben
 * - Event-Info als kleiner farbiger Balken darunter
 * - Leichter Hintergrund wenn Einträge vorhanden
 */
@Composable
private fun AndroidCalendarDayCell(
    date: LocalDate,
    entry: TimeEntry?,
    status: EntryStatus,
    isToday: Boolean,
    userSettings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onClick: () -> Unit
) {
    val bundesland = HolidayUtils.Bundesland.fromShortCode(userSettings?.bundesland)
    val isHoliday = HolidayUtils.isHoliday(date, bundesland)

    // Hintergrund für Tage mit Einträgen (sehr subtil)
    val cellBackgroundColor = when {
        isToday -> Color.Transparent
        entry != null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(cellBackgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            // Tag-Nummer (mit Kreis für Heute)
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    color = if (isToday)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(2.dp))

            // Event-Info (wie Google Calendar - farbige Zeile)
            if (entry != null || isHoliday) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feiertag-Indikator
                    if (isHoliday) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp),
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                    }

                    // Entry-Status als farbiger Balken
                    if (entry != null) {
                        val (barColor, label) = when {
                            entry.typ == TimeEntry.TYP_URLAUB -> Color(0xFF06B6D4) to "U"
                            entry.typ == TimeEntry.TYP_KRANK -> Color(0xFFEF4444) to "K"
                            entry.typ == TimeEntry.TYP_FEIERTAG -> Color(0xFF6366F1) to "F"
                            entry.typ == TimeEntry.TYP_ABWESEND -> Color(0xFF6B7280) to "A"
                            status == EntryStatus.COMPLETE -> Color(0xFF10B981) to "✓"
                            status == EntryStatus.PARTIAL -> Color(0xFFF59E0B) to "~"
                            else -> Color(0xFF9CA3AF) to "○"
                        }

                        // Farbiger Balken mit Label
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp),
                            color = barColor,
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
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        // Arbeitszeit-Info (wenn normal)
                        if (entry.typ == TimeEntry.TYP_NORMAL && (entry.startZeit != null || entry.endZeit != null)) {
                            val timeText = when {
                                entry.startZeit != null && entry.endZeit != null -> {
                                    "${TimeUtils.minutesToTimeString(entry.startZeit)}-${TimeUtils.minutesToTimeString(entry.endZeit)}"
                                }
                                entry.startZeit != null -> TimeUtils.minutesToTimeString(entry.startZeit)
                                entry.endZeit != null -> TimeUtils.minutesToTimeString(entry.endZeit)
                                else -> ""
                            }

                            if (timeText.isNotEmpty()) {
                                Text(
                                    text = timeText,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Überstunden-Badge (Ecke)
        if (entry != null && entry.typ == TimeEntry.TYP_NORMAL) {
            val diff = entry.getDifferenzMinuten()
            if (kotlin.math.abs(diff) >= 30) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    color = if (diff > 0)
                        Color(0xFF10B981).copy(alpha = 0.9f)
                    else
                        Color(0xFFEF4444).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (diff > 0) "+${diff / 60}" else "${diff / 60}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
