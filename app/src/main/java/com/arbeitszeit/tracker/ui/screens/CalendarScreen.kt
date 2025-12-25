package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.ui.components.EditEntryDialog
import com.arbeitszeit.tracker.ui.theme.*
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
 * Moderner Kalender-Screen mit Swipe-Gesten und Material 3 Design
 * Orientiert am Android-Kalender
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

    // Pager für Swipe-Gesten zwischen Monaten
    val initialPage = 1200 // Mittlere Position für Vor-/Zurück-Navigation
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2400 } // Genug Pages für viele Jahre
    )

    // Berechne aktuellen Monat basierend auf Pager-Position
    val currentPageMonth = remember(pagerState.currentPage) {
        val offset = pagerState.currentPage - initialPage
        month.plusMonths(offset.toLong())
    }

    // Synchronisiere ViewModel wenn User swipet
    LaunchedEffect(currentPageMonth) {
        if (currentPageMonth != month) {
            viewModel.setMonth(currentPageMonth)
        }
    }

    // Zeige Snackbar wenn Eintrag gelöscht wurde
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Kompakte TopBar mit Heute-Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Monat/Jahr Anzeige
                    Text(
                        text = currentPageMonth.atDay(1).format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    // Heute-Button
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(initialPage)
                                viewModel.setMonth(month)
                            }
                        },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Monats-Statistik (kompakter)
            MonthStatistics(
                entries = entries,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Wochentag-Header
            WeekdayHeader(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

            // Swipe-able Kalender
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageMonth = month.plusMonths((page - initialPage).toLong())
                val pageEntries = remember(pageMonth) {
                    if (pageMonth == month) entries else emptyList()
                }

                CalendarMonthGrid(
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
 * Kompakte Monats-Statistik
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

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                totalDiff > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                totalDiff < 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Soll/Ist
            Column {
                Text(
                    text = "Soll: ${TimeUtils.minutesToHoursMinutes(totalSoll)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Ist: ${TimeUtils.minutesToHoursMinutes(totalIst)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Differenz
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TimeUtils.formatDifferenz(totalDiff),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        totalDiff > 0 -> OvertimeColor
                        totalDiff < 0 -> UndertimeColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "${entries.count { it.isComplete() }} / ${entries.size} Tage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Kalender-Grid für einen Monat
 */
@Composable
private fun CalendarMonthGrid(
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier
    ) {
        // Offset für ersten Tag
        val firstDayOfMonth = month.atDay(1)
        val offset = firstDayOfMonth.dayOfWeek.value - 1

        // Leere Zellen vor Monatsbeginn
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

            CalendarDayCell(
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
 * Einzelne Tag-Zelle (größer und schöner)
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    entry: TimeEntry?,
    status: EntryStatus,
    isToday: Boolean,
    userSettings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onClick: () -> Unit
) {
    // Feiertag-Info
    val bundesland = HolidayUtils.Bundesland.fromShortCode(userSettings?.bundesland)
    val isHoliday = HolidayUtils.isHoliday(date, bundesland)

    // Click Animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "day_click_scale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .then(
                if (isToday) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                EntryStatus.COMPLETE -> StatusComplete
                EntryStatus.PARTIAL -> StatusPartial
                EntryStatus.EMPTY -> StatusEmpty
                EntryStatus.SPECIAL -> StatusSpecial
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isToday) 4.dp else 1.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Haupt-Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tag-Nummer
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 18.sp,
                    color = if (status == EntryStatus.EMPTY)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        Color.White
                )

                // Typ-Icon
                if (entry != null && entry.typ != TimeEntry.TYP_NORMAL) {
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        imageVector = when (entry.typ) {
                            TimeEntry.TYP_URLAUB -> Icons.Default.BeachAccess
                            TimeEntry.TYP_KRANK -> Icons.Default.LocalHospital
                            TimeEntry.TYP_FEIERTAG -> Icons.Default.Event
                            TimeEntry.TYP_ABWESEND -> Icons.Default.EventBusy
                            else -> Icons.Default.Work
                        },
                        contentDescription = entry.typ,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Feiertags-Badge
            if (isHoliday) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp),
                    color = Color(0xFFFFD700),
                    shape = CircleShape,
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Feiertag",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(2.dp),
                        tint = Color.White
                    )
                }
            }

            // Überstunden-Badge
            if (entry != null && entry.typ == TimeEntry.TYP_NORMAL) {
                val diff = entry.getDifferenzMinuten()
                if (diff != 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp),
                        color = if (diff > 0)
                            Color(0xFF4CAF50).copy(alpha = 0.95f)
                        else
                            Color(0xFFFF5252).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = if (diff > 0) "+${diff / 60}h" else "${diff / 60}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}
