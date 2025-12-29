# 🎨 UI Improvements Plan - ArbeitszeitTracker 2.0

## Übersicht

Modernisierung der Phone App UI mit Focus auf:
- ✨ Moderne Animationen & Transitions
- 🎯 Bessere UX & Accessibility
- 📊 Neue Dashboard-Features
- 🎨 Customization & Theming

---

## 🏠 1. Home Screen - Komplett überarbeitet

### ❌ **Aktuell:**
- Liste von Widgets
- Statisch
- Kein Fokus auf "Was ist wichtig JETZT?"

### ✅ **Neu: Dashboard-Konzept**

```kotlin
@Composable
fun NewHomeScreen(viewModel: HomeViewModel) {
    var selectedTab by remember { mutableStateOf(DashboardTab.TODAY) }

    Scaffold(
        topBar = {
            // Moderne TopBar mit Gradient
            GradientTopBar(
                title = "Zeiterfassung",
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Hero Card: Heute im Fokus
            HeroTodayCard(viewModel)

            Spacer(height = 16.dp)

            // Tab Switcher
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == DashboardTab.TODAY,
                    onClick = { selectedTab = DashboardTab.TODAY },
                    text = { Text("Heute") }
                )
                Tab(
                    selected = selectedTab == DashboardTab.WEEK,
                    onClick = { selectedTab = DashboardTab.WEEK },
                    text = { Text("Woche") }
                )
                Tab(
                    selected = selectedTab == DashboardTab.INSIGHTS,
                    onClick = { selectedTab = DashboardTab.INSIGHTS },
                    text = { Text("Insights") }
                )
            }

            // Content Area mit Animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                }
            ) { tab ->
                when (tab) {
                    DashboardTab.TODAY -> TodayDashboard(viewModel)
                    DashboardTab.WEEK -> WeekDashboard(viewModel)
                    DashboardTab.INSIGHTS -> InsightsDashboard(viewModel)
                }
            }
        }
    }
}

enum class DashboardTab { TODAY, WEEK, INSIGHTS }
```

### **Hero Today Card** (Der Eye-Catcher)

```kotlin
@Composable
fun HeroTodayCard(viewModel: HomeViewModel) {
    val todayEntry by viewModel.todayEntry.collectAsState()
    val isWorking = todayEntry?.startZeit != null && todayEntry?.endZeit == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isWorking) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Background Pattern
            AnimatedBackgroundPattern(isWorking = isWorking)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWorking) {
                        // Pulsierender Punkt
                        PulsingDot(color = Color.Green)
                        Spacer(width = 8.dp)
                        Text(
                            "Arbeit läuft seit ${formatTime(todayEntry?.startZeit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(width = 8.dp)
                        Text(
                            "Bereit zum Starten",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Live Timer (wenn am Arbeiten)
                if (isWorking) {
                    LiveWorkTimer(startTime = todayEntry?.startZeit ?: 0)
                }

                // Quick Action Button
                Button(
                    onClick = {
                        if (isWorking) viewModel.stampOut()
                        else viewModel.stampIn()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWorking) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        if (isWorking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(width = 8.dp)
                    Text(if (isWorking) "Ausstempeln" else "Einstempeln")
                }
            }
        }
    }
}

@Composable
fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(scale)
            .background(color, shape = CircleShape)
    )
}

@Composable
fun LiveWorkTimer(startTime: Int) {
    var elapsedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(startTime) {
        while (true) {
            val now = TimeUtils.currentTimeInMinutes()
            elapsedTime = (now - startTime).toLong()
            delay(60_000) // Update jede Minute
        }
    }

    Text(
        text = formatDuration(elapsedTime),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}
```

---

## 📊 2. Insights Dashboard (NEU!)

### **Insights Screen - Dein Arbeitsverhalten visualisiert**

```kotlin
@Composable
fun InsightsDashboard(viewModel: HomeViewModel) {
    val insights by viewModel.insights.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Wochenübersicht mit Chart
        item {
            WeeklyChartCard(insights.weeklyData)
        }

        // 2. Arbeitsmuster
        item {
            WorkPatternsCard(insights.patterns)
        }

        // 3. Streak Counter
        item {
            StreakCard(insights.currentStreak)
        }

        // 4. Überstunden-Trend
        item {
            OvertimeTrendCard(insights.overtimeTrend)
        }

        // 5. Empfehlungen
        item {
            RecommendationsCard(insights.recommendations)
        }
    }
}

@Composable
fun WeeklyChartCard(weekData: List<DayData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Diese Woche",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Simple Bar Chart
            BarChart(
                data = weekData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Legende
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = Color.Green, label = "Ist-Zeit")
                LegendItem(color = Color.Gray, label = "Soll-Zeit")
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<DayData>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val maxHeight = data.maxOfOrNull { maxOf(it.sollMinutes, it.istMinutes) } ?: 1

        data.forEachIndexed { index, day ->
            val x = index * (barWidth * 2) + barWidth / 2

            // Soll-Balken (grau)
            val sollHeight = (day.sollMinutes.toFloat() / maxHeight) * size.height
            drawRect(
                color = Color.Gray.copy(alpha = 0.3f),
                topLeft = Offset(x, size.height - sollHeight),
                size = Size(barWidth * 0.8f, sollHeight)
            )

            // Ist-Balken (grün oder rot)
            val istHeight = (day.istMinutes.toFloat() / maxHeight) * size.height
            val color = if (day.istMinutes >= day.sollMinutes) Color.Green else Color.Red
            drawRect(
                color = color,
                topLeft = Offset(x + barWidth * 0.2f, size.height - istHeight),
                size = Size(barWidth * 0.8f, istHeight)
            )
        }
    }
}

data class DayData(
    val day: String,
    val sollMinutes: Int,
    val istMinutes: Int
)
```

### **Work Patterns Card - "Wann arbeitest du am liebsten?"**

```kotlin
@Composable
fun WorkPatternsCard(patterns: WorkPatterns) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Deine Arbeitsmuster",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Durchschnittliche Startzeit
            PatternRow(
                icon = Icons.Default.WbSunny,
                label = "Durchschnittlicher Start",
                value = formatTime(patterns.avgStartTime),
                trend = patterns.startTimeTrend
            )

            // Produktivster Tag
            PatternRow(
                icon = Icons.Default.Star,
                label = "Produktivster Tag",
                value = patterns.mostProductiveDay,
                subtext = "+${patterns.avgExtraMinutes} Min"
            )

            // Pausenverhalten
            PatternRow(
                icon = Icons.Default.Coffee,
                label = "Ø Pause",
                value = "${patterns.avgPauseMinutes} Min",
                trend = if (patterns.avgPauseMinutes < 30) TrendDirection.DOWN else TrendDirection.NEUTRAL
            )
        }
    }
}

@Composable
fun PatternRow(
    icon: ImageVector,
    label: String,
    value: String,
    trend: TrendDirection? = null,
    subtext: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtext != null) {
                    Text(
                        subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (trend != null) {
                Icon(
                    when (trend) {
                        TrendDirection.UP -> Icons.Default.TrendingUp
                        TrendDirection.DOWN -> Icons.Default.TrendingDown
                        TrendDirection.NEUTRAL -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = when (trend) {
                        TrendDirection.UP -> Color.Green
                        TrendDirection.DOWN -> Color.Red
                        TrendDirection.NEUTRAL -> Color.Gray
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

enum class TrendDirection { UP, DOWN, NEUTRAL }

data class WorkPatterns(
    val avgStartTime: Int,
    val startTimeTrend: TrendDirection,
    val mostProductiveDay: String,
    val avgExtraMinutes: Int,
    val avgPauseMinutes: Int
)
```

### **Streak Card - Gamification**

```kotlin
@Composable
fun StreakCard(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Streak 🔥",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tage ohne vergessene Einträge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }

            // Großer Streak Counter
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        streak.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Text(
                        "Tage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
```

---

## 🎨 3. Theme Customization (Neue Settings)

### **Custom Theme Builder**

```kotlin
@Composable
fun ThemeCustomizationScreen() {
    var selectedAccentColor by remember { mutableStateOf(AccentColor.BLUE) }
    var useDynamicColor by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1.0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Accent Color Picker
        item {
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Akzentfarbe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(height = 12.dp)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(AccentColor.values()) { color ->
                            ColorChip(
                                color = color,
                                isSelected = selectedAccentColor == color,
                                onClick = { selectedAccentColor = color }
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Color Toggle
        item {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Material You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Farben vom Wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = { useDynamicColor = it }
                    )
                }
            }
        }

        // Font Size Slider
        item {
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Schriftgröße",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${(fontScale * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        valueRange = 0.8f..1.5f,
                        steps = 6
                    )

                    Text(
                        "Beispieltext",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale
                        )
                    )
                }
            }
        }

        // Preview Card
        item {
            Text(
                "Vorschau",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = selectedAccentColor.color
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "So sieht's aus!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "Deine ausgewählte Farbe in Aktion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

enum class AccentColor(val color: Color) {
    BLUE(Color(0xFF2196F3)),
    GREEN(Color(0xFF4CAF50)),
    PURPLE(Color(0xFF9C27B0)),
    ORANGE(Color(0xFFFF9800)),
    RED(Color(0xFFF44336)),
    TEAL(Color(0xFF009688))
}

@Composable
fun ColorChip(
    color: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(color.color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 4.dp,
                    color = Color.White,
                    shape = CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
```

---

## ✨ 4. Animationen & Transitions

### **Shared Element Transitions** (zwischen Screens)

```kotlin
// Navigation mit Shared Elements
@Composable
fun CalendarToDetailTransition(
    dateEntry: TimeEntry,
    onBack: () -> Unit
) {
    SharedTransitionLayout {
        AnimatedContent(
            targetState = showDetail,
            transitionSpec = {
                fadeIn() + scaleIn(initialScale = 0.9f) togetherWith
                    fadeOut() + scaleOut(targetScale = 1.1f)
            }
        ) { isDetail ->
            if (isDetail) {
                EntryDetailScreen(
                    entry = dateEntry,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent
                )
            } else {
                CalendarDayCell(
                    entry = dateEntry,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent
                )
            }
        }
    }
}
```

### **Pull-to-Refresh Animation**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableCalendar(viewModel: CalendarViewModel) {
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshCurrentMonth()
            delay(1000)
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        CalendarContent()

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

### **Swipe-to-Delete mit Undo**

```kotlin
@Composable
fun SwipeableEntryCard(
    entry: TimeEntry,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Roter Hintergrund mit Trash Icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        TimeEntryCard(entry = entry)
    }
}
```

---

## 🎯 5. Accessibility Improvements

### **High Contrast Mode**

```kotlin
@Composable
fun AccessibilitySettings() {
    var highContrastMode by remember { mutableStateOf(false) }

    if (highContrastMode) {
        // Override Theme Colors
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                background = Color.Black,
                onBackground = Color.White
            )
        ) {
            Content()
        }
    }
}
```

### **Screen Reader Optimierung**

```kotlin
@Composable
fun AccessibleTimeEntry(entry: TimeEntry) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = buildString {
                append("Eintrag vom ${entry.datum}. ")
                append("Start: ${formatTime(entry.startZeit)}. ")
                append("Ende: ${formatTime(entry.endZeit)}. ")
                append("Arbeitszeit: ${formatDuration(entry.getIstMinuten())}. ")
            }
        }
    ) {
        // Card Content
    }
}
```

---

## 🚀 Implementation Priority

### Phase 1: Core Improvements (2 Wochen)
1. ✅ Hero Today Card (Home Screen)
2. ✅ Live Work Timer
3. ✅ Weekly Chart (Insights)
4. ✅ Streak Counter

### Phase 2: Nice-to-Have (1 Woche)
5. ✅ Work Patterns Card
6. ✅ Theme Customization
7. ✅ Animations (Shared Elements)

### Phase 3: Polish (1 Woche)
8. ✅ Accessibility Improvements
9. ✅ Pull-to-Refresh
10. ✅ Swipe Gestures

---

## 📊 Vorher/Nachher

| Feature | Aktuell | Neu |
|---------|---------|-----|
| Home Screen | Statische Liste | Dashboard mit Live-Timer |
| Insights | Keine | Charts, Patterns, Streaks |
| Theming | Fest | Customizable |
| Animationen | Basic | Shared Elements, Gestures |
| Accessibility | Basic | High Contrast, Screen Reader |

---

Welche UI-Features interessieren dich am meisten? 🎨
