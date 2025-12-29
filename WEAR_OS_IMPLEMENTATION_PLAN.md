# 🎯 Wear OS Implementation Plan - ArbeitszeitTracker 2.0

## 📋 Übersicht

Dieser Plan zeigt, wie wir eine **moderne, performante Wear OS Integration** umsetzen, die:
- ✅ Mit der Phone App kommuniziert (nicht separat!)
- ✅ Butterweich läuft (Horologist + Best Practices)
- ✅ Einfach zu warten ist (Shared Logic)

---

## 🏗️ Architektur

### Prinzip: "Phone = Brain, Watch = Remote Control"

```
┌──────────────────────────────────────────────────────┐
│                    PHONE APP                          │
│                                                       │
│  ┌────────────┐    ┌─────────────────────────┐      │
│  │  Room DB   │───→│  WearDataLayerService   │      │
│  │  (Master)  │    │  - Sendet Updates       │      │
│  └────────────┘    │  - Empfängt Actions     │      │
│                    └─────────────────────────┘      │
│                              ↕                        │
│                    [Bluetooth LE]                     │
│                              ↕                        │
└──────────────────────────────────────────────────────┘
                              ↕
┌──────────────────────────────────────────────────────┐
│                   WEAR MODULE                         │
│                                                       │
│  ┌─────────────────────────────────────────────┐    │
│  │  WearDataClient                             │    │
│  │  - Empfängt Daten                           │    │
│  │  - Cache (für Offline)                      │    │
│  │  - Sendet User-Actions zurück              │    │
│  └─────────────────────────────────────────────┘    │
│                         │                            │
│                         ↓                            │
│  ┌─────────────────────────────────────────────┐    │
│  │  Compose for Wear OS UI                     │    │
│  │  - TimeStampScreen (Haupt-Screen)           │    │
│  │  - TodayStatsScreen                         │    │
│  │  - QuickActionsScreen                       │    │
│  └─────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
```

---

## 📦 Module Setup

### 1. `settings.gradle.kts` erweitern

```kotlin
include(":app")
include(":wear")        // NEU
include(":shared")      // NEU (optional, für geteilte Models)
```

### 2. `wear/build.gradle.kts` erstellen

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.arbeitszeit.tracker.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arbeitszeit.tracker"
        minSdk = 30  // Wear OS 3.0+
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wear OS Compose
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)

    // Horologist (Google Best Practices)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)
    implementation(libs.horologist.datalayer)

    // Wearable Data Layer (Communication)
    implementation(libs.play.services.wearable)

    // Serialization (für Data Transfer)
    implementation(libs.kotlinx.serialization.json)

    // Shared Module (optional)
    implementation(project(":shared"))
}
```

### 3. `gradle/libs.versions.toml` erweitern

```toml
[versions]
wear-compose = "1.4.0"
horologist = "0.6.13"
play-services-wearable = "18.2.0"

[libraries]
# Wear OS
androidx-wear-compose-material = { module = "androidx.wear.compose:compose-material", version.ref = "wear-compose" }
androidx-wear-compose-foundation = { module = "androidx.wear.compose:compose-foundation", version.ref = "wear-compose" }
androidx-wear-compose-navigation = { module = "androidx.wear.compose:compose-navigation", version.ref = "wear-compose" }

# Horologist (Best Practices)
horologist-compose-layout = { module = "com.google.android.horologist:horologist-compose-layout", version.ref = "horologist" }
horologist-compose-material = { module = "com.google.android.horologist:horologist-compose-material", version.ref = "horologist" }
horologist-datalayer = { module = "com.google.android.horologist:horologist-datalayer", version.ref = "horologist" }

# Data Layer
play-services-wearable = { module = "com.google.android.gms:play-services-wearable", version.ref = "play-services-wearable" }
```

---

## 🔄 Data Communication (Das Herzstück)

### **Phone Side: WearDataService.kt**

```kotlin
package com.arbeitszeit.tracker.wear

import android.content.Context
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone-Side Service:
 * Sendet Daten zur Watch, empfängt Actions von der Watch
 */
@Singleton
class WearDataService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeEntryRepository: TimeEntryRepository
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    companion object {
        private const val TODAY_STATS_PATH = "/today_stats"
        private const val QUICK_STAMP_PATH = "/quick_stamp"
        private const val ADD_PAUSE_PATH = "/add_pause"
    }

    /**
     * Sendet Heute-Statistik zur Watch
     * Wird automatisch bei Änderungen aufgerufen
     */
    suspend fun syncTodayStats() {
        val today = LocalDate.now()
        val entry = timeEntryRepository.getEntryByDate(today.toString())

        val stats = WearTodayStats(
            date = today.toString(),
            startTime = entry?.startZeit,
            endTime = entry?.endZeit,
            pauseMinutes = entry?.pauseMinuten ?: 0,
            sollMinutes = entry?.sollMinuten ?: 0,
            istMinutes = entry?.getIstMinuten() ?: 0,
            differenzMinutes = entry?.getDifferenzMinuten() ?: 0,
            isStamped = entry != null && entry.startZeit != null
        )

        val json = Json.encodeToString(stats)
        val putDataReq = PutDataRequest.create(TODAY_STATS_PATH).apply {
            data = json.toByteArray()
        }

        dataClient.putDataItem(putDataReq).await()
    }

    /**
     * Listener für Actions von der Watch
     */
    fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            when (messageEvent.path) {
                QUICK_STAMP_PATH -> handleQuickStamp(messageEvent.data)
                ADD_PAUSE_PATH -> handleAddPause(messageEvent.data)
            }
        }
    }

    private suspend fun handleQuickStamp(data: ByteArray) {
        val action = Json.decodeFromString<QuickStampAction>(data.decodeToString())

        // Logik: Stempel ein/aus
        val today = LocalDate.now().toString()
        val entry = timeEntryRepository.getEntryByDate(today)
        val currentTime = TimeUtils.currentTimeInMinutes()

        when (action.type) {
            StampType.IN -> {
                timeEntryRepository.updateEntry(
                    date = today,
                    startZeit = currentTime,
                    endZeit = entry?.endZeit,
                    pauseMinuten = entry?.pauseMinuten ?: 0,
                    typ = TimeEntry.TYP_NORMAL,
                    notiz = entry?.notiz ?: "",
                    urlaubsJahr = null
                )
            }
            StampType.OUT -> {
                timeEntryRepository.updateEntry(
                    date = today,
                    startZeit = entry?.startZeit,
                    endZeit = currentTime,
                    pauseMinuten = entry?.pauseMinuten ?: 0,
                    typ = entry?.typ ?: TimeEntry.TYP_NORMAL,
                    notiz = entry?.notiz ?: "",
                    urlaubsJahr = null
                )
            }
        }

        // Sende Update zurück zur Watch
        syncTodayStats()
    }

    private suspend fun handleAddPause(data: ByteArray) {
        val action = Json.decodeFromString<AddPauseAction>(data.decodeToString())

        val today = LocalDate.now().toString()
        val entry = timeEntryRepository.getEntryByDate(today) ?: return

        timeEntryRepository.updateEntry(
            date = today,
            startZeit = entry.startZeit,
            endZeit = entry.endZeit,
            pauseMinuten = action.minutes,
            typ = entry.typ,
            notiz = entry.notiz,
            urlaubsJahr = entry.urlaubsJahr
        )

        syncTodayStats()
    }
}

// Shared Data Models (in shared/ Module oder in beiden Apps)
@Serializable
data class WearTodayStats(
    val date: String,
    val startTime: Int?,
    val endTime: Int?,
    val pauseMinutes: Int,
    val sollMinutes: Int,
    val istMinutes: Int,
    val differenzMinutes: Int,
    val isStamped: Boolean
)

@Serializable
data class QuickStampAction(
    val type: StampType
)

enum class StampType { IN, OUT }

@Serializable
data class AddPauseAction(
    val minutes: Int
)
```

---

### **Wear Side: WearDataRepository.kt**

```kotlin
package com.arbeitszeit.tracker.wear.data

import android.content.Context
import com.google.android.gms.wearable.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wear-Side Repository:
 * Empfängt Daten vom Phone, sendet Actions zurück
 */
@Singleton
class WearDataRepository @Inject constructor(
    private val context: Context
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    private val _todayStats = MutableStateFlow<WearTodayStats?>(null)
    val todayStats: StateFlow<WearTodayStats?> = _todayStats

    companion object {
        private const val TODAY_STATS_PATH = "/today_stats"
        private const val QUICK_STAMP_PATH = "/quick_stamp"
        private const val ADD_PAUSE_PATH = "/add_pause"
    }

    /**
     * Listener für Daten vom Phone
     */
    fun setupDataListener() {
        dataClient.addListener { dataEvents ->
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    when (event.dataItem.uri.path) {
                        TODAY_STATS_PATH -> {
                            val data = DataMapItem.fromDataItem(event.dataItem)
                            val json = data.dataMap.getString("json") ?: return@forEach
                            val stats = Json.decodeFromString<WearTodayStats>(json)
                            _todayStats.value = stats
                        }
                    }
                }
            }
        }
    }

    /**
     * Sende Quick Stamp Action zum Phone
     */
    suspend fun sendQuickStamp(type: StampType) {
        val action = QuickStampAction(type)
        val json = Json.encodeToString(action)

        // Finde verbundene Nodes (Phone)
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            messageClient.sendMessage(
                node.id,
                QUICK_STAMP_PATH,
                json.toByteArray()
            ).await()
        }
    }

    /**
     * Sende Add Pause Action zum Phone
     */
    suspend fun sendAddPause(minutes: Int) {
        val action = AddPauseAction(minutes)
        val json = Json.encodeToString(action)

        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            messageClient.sendMessage(
                node.id,
                ADD_PAUSE_PATH,
                json.toByteArray()
            ).await()
        }
    }

    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
}
```

---

## 🎨 Wear OS UI - Modern & Performant

### **TimeStampScreen.kt** (Haupt-Screen)

```kotlin
package com.arbeitszeit.tracker.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.material.Chip

/**
 * Haupt-Screen der Wear App
 * - Großer Stamp-Button
 * - Heute-Statistik
 * - Quick Actions
 */
@Composable
fun TimeStampScreen(
    viewModel: WearViewModel
) {
    val todayStats by viewModel.todayStats.collectAsState()
    val isStamped = todayStats?.isStamped ?: false

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = ScalingLazyColumnDefaults.belowTimeText().create(),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            // Haupt-Stamp-Button (GROSS!)
            item {
                Button(
                    onClick = {
                        if (isStamped) {
                            viewModel.stampOut()
                        } else {
                            viewModel.stampIn()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isStamped) "Ausstempeln" else "Einstempeln",
                            style = MaterialTheme.typography.title3
                        )
                        todayStats?.let { stats ->
                            Text(
                                text = if (isStamped) {
                                    "seit ${formatTime(stats.startTime)}"
                                } else {
                                    "Jetzt starten"
                                },
                                style = MaterialTheme.typography.caption1
                            )
                        }
                    }
                }
            }

            // Heute-Statistik
            item {
                TodayStatsCard(todayStats)
            }

            // Quick Actions
            item {
                Text(
                    text = "Schnellaktionen",
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(listOf(15, 30, 45, 60)) { minutes ->
                Chip(
                    label = "Pause +$minutes Min",
                    onClick = { viewModel.addPause(minutes) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TodayStatsCard(stats: WearTodayStats?) {
    Card(
        onClick = { /* Navigate to Details */ },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Heute",
                style = MaterialTheme.typography.caption1
            )

            stats?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Start", style = MaterialTheme.typography.caption2)
                        Text(
                            formatTime(it.startTime) ?: "--:--",
                            style = MaterialTheme.typography.title3
                        )
                    }
                    Column {
                        Text("Ende", style = MaterialTheme.typography.caption2)
                        Text(
                            formatTime(it.endTime) ?: "--:--",
                            style = MaterialTheme.typography.title3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Differenz: ${formatDifferenz(it.differenzMinutes)}",
                    style = MaterialTheme.typography.body2,
                    color = when {
                        it.differenzMinutes > 0 -> Color.Green
                        it.differenzMinutes < 0 -> Color.Red
                        else -> Color.White
                    }
                )
            }
        }
    }
}

private fun formatTime(minutes: Int?): String? {
    if (minutes == null) return null
    val h = minutes / 60
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
}

private fun formatDifferenz(minutes: Int): String {
    val sign = if (minutes >= 0) "+" else ""
    val h = minutes / 60
    val m = minutes % 60
    return String.format("%s%dh %02dm", sign, h, m)
}
```

---

## 🚀 Performance-Tipps (damit es NICHT ruckelt)

### 1. **Horologist verwenden** (Google Best Practices)

```kotlin
// Statt normalem LazyColumn
ScalingLazyColumn(  // Automatisch optimiert für runde Displays!
    columnState = ScalingLazyColumnDefaults.belowTimeText().create()
) {
    // Items...
}
```

### 2. **Data Layer Caching**

```kotlin
// In WearDataRepository
private val cache = mutableMapOf<String, WearTodayStats>()

fun setupDataListener() {
    dataClient.addListener { dataEvents ->
        dataEvents.forEach { event ->
            // Cache sofort updaten (kein DB-Zugriff!)
            cache[event.dataItem.uri.path] = parseData(event)
            _todayStats.value = cache[TODAY_STATS_PATH]
        }
    }
}
```

### 3. **Minimal UI Updates**

```kotlin
// NUR bei Änderungen neu rendern
val todayStats by viewModel.todayStats.collectAsState()

// Statt ständig neu zu zeichnen
LaunchedEffect(todayStats) {
    // Nur bei Änderung
}
```

### 4. **Battery-Aware**

```kotlin
// Nicht ständig synchen!
class WearDataService {
    private val syncInterval = 60.seconds  // Max. alle 60s

    fun startAutoSync() {
        lifecycleScope.launch {
            while (isActive) {
                syncTodayStats()
                delay(syncInterval)
            }
        }
    }
}
```

---

## 🎯 Minimal Viable Product (MVP)

### Phase 1: Grundfunktionen (1-2 Wochen)
1. ✅ Data Layer Setup (Phone ↔ Wear)
2. ✅ TimeStampScreen mit großem Button
3. ✅ Heute-Statistik anzeigen
4. ✅ Quick Stamp (Ein/Aus)

### Phase 2: Nice-to-Have (1 Woche)
5. ✅ Pause hinzufügen
6. ✅ Komplikationen (Watch Faces)
7. ✅ Vibrations-Feedback

### Phase 3: Advanced (Optional)
8. ⭐ Voice Commands ("Stempel mich ein")
9. ⭐ Tiles (Quick Access)
10. ⭐ Health Services Integration

---

## 🧪 Testing

### Testen OHNE echte Watch:

```bash
# Wear OS Emulator erstellen
1. Android Studio → Device Manager → Create Device
2. Wear OS → Wähle "Wear OS 4" (API 33+)
3. Start Emulator

# Wear App installieren
./gradlew :wear:installDebug

# Phone App mit Wear Emulator pairen
adb -d forward tcp:5601 tcp:5601  # Phone
adb -e forward tcp:5601 tcp:5601  # Emulator
```

### Testen MIT echter Watch:

```bash
# 1. Watch mit Phone pairen (Wear OS App)
# 2. Developer Mode aktivieren auf Watch
# 3. ADB Debugging aktivieren
# 4. Watch mit WiFi verbinden

adb connect <watch-ip>:5555
./gradlew :wear:installDebug
```

---

## 📊 Vorher/Nachher Vergleich

| Feature | Alte Lösung ❌ | Neue Lösung ✅ |
|---------|---------------|---------------|
| **Datenbank** | 2x Room DB | 1x Room (Phone), Cache (Wear) |
| **Sync** | Manuell/Gar nicht | Automatisch (Data Layer) |
| **Performance** | Ruckelig | Butterweich (Horologist) |
| **Komplexität** | 2 separate Apps | 1 Projekt, 2 Module |
| **Wartung** | Doppelter Code | Shared Logic |
| **Offline** | Broken | Cache funktioniert |

---

## 🎉 Das war's!

Dieser Ansatz ist:
- ✅ **Modern** (2025 Best Practices)
- ✅ **Performant** (Horologist + Caching)
- ✅ **Wartbar** (Shared Code)
- ✅ **Zuverlässig** (Official Data Layer API)

Willst du, dass ich mit der Implementation anfange? 🚀
