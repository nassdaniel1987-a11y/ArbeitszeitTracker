package com.arbeitszeit.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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
            ArbeitszeitTrackerTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            // Navigation Items
                            val items = listOf(
                                Triple(Screen.Home, Icons.Default.Home, "Home"),
                                Triple(Screen.Calendar, Icons.Default.CalendarMonth, "Kalender"),
                                Triple(Screen.Ueberstunden, Icons.Default.Timeline, "Überstunden"),
                                Triple(Screen.Export, Icons.Default.FileDownload, "Export"),
                                Triple(Screen.Settings, Icons.Default.Settings, "Einstellungen")
                            )

                            items.forEach { (screen, icon, label) ->
                                NavigationBarItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, label) },
                                    label = { Text(label) }
                                )
                            }
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