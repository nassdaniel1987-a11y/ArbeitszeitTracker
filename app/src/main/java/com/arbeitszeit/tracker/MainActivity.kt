package com.arbeitszeit.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.geofencing.GeofencingManager
import com.arbeitszeit.tracker.ui.navigation.NavGraph
import com.arbeitszeit.tracker.ui.navigation.Screen
import com.arbeitszeit.tracker.ui.theme.ArbeitszeitTrackerTheme
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.NotificationHelper
import com.arbeitszeit.tracker.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduleReminders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification Channels erstellen
        NotificationHelper.createNotificationChannels(this)

        // Notification Permission anfragen (Android 13+)
        requestNotificationPermission()

        // Geofencing initialisieren (wenn aktiviert)
        initializeGeofencing()

        // EINMALIG: Migriere KW-Nummern für alte Einträge
        migrateKalenderwochen()

        setContent {
            // Observe settings for dark mode
            val database = AppDatabase.getDatabase(this)
            val settings by database.userSettingsDao().getSettingsFlow()
                .collectAsState(initial = null)

            val darkTheme = when (settings?.darkMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme() // "system" or null
            }

            ArbeitszeitTrackerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            // Header
                            Surface(
                                modifier = Modifier.height(120.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Arbeitszeit Tracker",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Navigation Items
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Home, null) },
                                label = { Text("Home") },
                                selected = currentRoute == Screen.Home.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Timeline, null) },
                                label = { Text("Überstunden") },
                                selected = currentRoute == Screen.Ueberstunden.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Ueberstunden.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.CalendarMonth, null) },
                                label = { Text("Kalender") },
                                selected = currentRoute == Screen.Calendar.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Calendar.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.ContentCopy, null) },
                                label = { Text("Wochen-Vorlagen") },
                                selected = currentRoute == Screen.WeekTemplates.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.WeekTemplates.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.FileDownload, null) },
                                label = { Text("Export") },
                                selected = currentRoute == Screen.Export.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Export.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.FileUpload, null) },
                                label = { Text("Import") },
                                selected = currentRoute == Screen.Import.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Import.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, null) },
                                label = { Text("Einstellungen") },
                                selected = currentRoute == Screen.Settings.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Settings.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.HelpOutline, null) },
                                label = { Text("Hilfe") },
                                selected = currentRoute == Screen.Help.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Help.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            // TopBar nur auf Hauptscreens (Home, Überstunden, Kalender)
                            val showTopBar = currentRoute in listOf(
                                Screen.Home.route,
                                Screen.Ueberstunden.route,
                                Screen.Calendar.route,
                                Screen.Export.route,
                                Screen.Import.route,
                                Screen.Settings.route
                            )

                            if (showTopBar) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            when (currentRoute) {
                                                Screen.Home.route -> "Home"
                                                Screen.Ueberstunden.route -> "Überstunden"
                                                Screen.Calendar.route -> "Kalender"
                                                Screen.Export.route -> "Export"
                                                Screen.Import.route -> "Import"
                                                Screen.Settings.route -> "Einstellungen"
                                                else -> "Arbeitszeit Tracker"
                                            }
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, "Menü öffnen")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.padding(padding)) {
                            NavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleReminders()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleReminders()
        }
    }

    private fun scheduleReminders() {
        ReminderWorker.scheduleMorningReminder(this)
        ReminderWorker.scheduleEveningReminder(this)
        ReminderWorker.scheduleMissingEntriesCheck(this)
    }

    private fun initializeGeofencing() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(this@MainActivity)
            val settings = database.userSettingsDao().getSettings()

            // Nur initialisieren wenn Geofencing aktiviert ist
            if (settings?.geofencingEnabled == true) {
                val enabledLocations = database.workLocationDao().getEnabledLocations()
                if (enabledLocations.isNotEmpty()) {
                    val geofencingManager = GeofencingManager(this@MainActivity)
                    geofencingManager.startGeofencing(enabledLocations)
                }
            }
        }
    }

    /**
     * Migriert alle Zeiteinträge auf Custom Kalenderwochen-Berechnung
     *
     * Hintergrund: Alte Einträge wurden möglicherweise mit ISO 8601 KW erstellt.
     * Diese Funktion berechnet für jeden Eintrag die korrekte Custom KW basierend
     * auf dem Datum und den aktuellen Settings (ersterMontagImJahr).
     *
     * Wird nur einmalig beim App-Start ausgeführt wenn noch nicht migriert.
     */
    private fun migrateKalenderwochen() {
        CoroutineScope(Dispatchers.IO).launch {
            val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val hasMigrated = sharedPrefs.getBoolean("kw_migration_done", false)

            if (hasMigrated) {
                return@launch // Migration wurde bereits durchgeführt
            }

            val database = AppDatabase.getDatabase(this@MainActivity)
            val settings = database.userSettingsDao().getSettings()
            val timeEntryDao = database.timeEntryDao()

            // Lade alle Einträge
            val allEntries = timeEntryDao.getEntriesByYear(LocalDate.now().year)

            var updatedCount = 0
            allEntries.forEach { entry ->
                val date = LocalDate.parse(entry.datum)

                // Berechne KW mit Custom-Methode
                val newKW = DateUtils.getCustomWeekOfYear(date, settings?.ersterMontagImJahr)
                val newJahr = DateUtils.getCustomWeekBasedYear(date, settings?.ersterMontagImJahr)

                // Aktualisiere nur wenn sich KW oder Jahr geändert haben
                if (entry.kalenderwoche != newKW || entry.jahr != newJahr) {
                    timeEntryDao.update(entry.copy(
                        kalenderwoche = newKW,
                        jahr = newJahr,
                        updatedAt = System.currentTimeMillis()
                    ))
                    updatedCount++
                }
            }

            // Markiere Migration als abgeschlossen
            sharedPrefs.edit().putBoolean("kw_migration_done", true).apply()

            android.util.Log.i("MainActivity", "KW-Migration abgeschlossen: $updatedCount von ${allEntries.size} Einträgen aktualisiert")
        }
    }
}
