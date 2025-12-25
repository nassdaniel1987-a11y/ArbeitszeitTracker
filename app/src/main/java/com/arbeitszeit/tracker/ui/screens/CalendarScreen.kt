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
 * Minimalistischer Kalender im Android/Google Kalender-Stil
 * - Clean, einfaches Design
 * - Gut lesbar in Light & Dark Mode
 * - Swipe-Gesten zwischen Monaten
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
            WeekdayHeader(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

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
                        .padding(horizontal = 16.dp)
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
 * Kompakte Statistik-Card
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
        // Soll/Ist
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

        // Differenz
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimeUtils.formatDifferenz(totalDiff),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    totalDiff > 0 -> Color(0xFF10B981) // Grün
                    totalDiff < 0 -> Color(0xFFEF4444) // Rot
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

            MinimalDayCell(
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
 * Minimalistische Tag-Zelle (Android-Kalender-Stil)
 */
@Composable
private fun MinimalDayCell(
    date: LocalDate,
    entry: TimeEntry?,
    status: EntryStatus,
    isToday: Boolean,
    userSettings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onClick: () -> Unit
) {
    val bundesland = HolidayUtils.Bundesland.fromShortCode(userSettings?.bundesland)
    val isHoliday = HolidayUtils.isHoliday(date, bundesland)

    // Bestimme Farben basierend auf Theme
    val dayNumberColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Tag-Nummer (mit Kreis für Heute)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = backgroundColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    color = dayNumberColor
                )
            }

            Spacer(Modifier.height(2.dp))

            // Event-Indikatoren (kleine Punkte unter der Zahl)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(8.dp)
            ) {
                // Feiertags-Punkt
                if (isHoliday) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = Color(0xFFFFD700), // Gold
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.width(3.dp))
                }

                // Entry-Status-Punkt
                if (entry != null) {
                    val dotColor = when (status) {
                        EntryStatus.COMPLETE -> Color(0xFF10B981) // Grün
                        EntryStatus.PARTIAL -> Color(0xFFF59E0B) // Orange
                        EntryStatus.SPECIAL -> when (entry.typ) {
                            TimeEntry.TYP_URLAUB -> Color(0xFF06B6D4) // Cyan
                            TimeEntry.TYP_KRANK -> Color(0xFFEF4444) // Rot
                            else -> Color(0xFF6366F1) // Indigo
                        }
                        else -> Color.Transparent
                    }

                    if (dotColor != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    color = dotColor,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }

        // Überstunden-Badge (nur wenn signifikant)
        if (entry != null && entry.typ == TimeEntry.TYP_NORMAL) {
            val diff = entry.getDifferenzMinuten()
            if (kotlin.math.abs(diff) >= 60) { // Nur bei >= 1h
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
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
