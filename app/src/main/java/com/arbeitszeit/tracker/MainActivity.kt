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
import com.arbeitszeit.tracker.ui.navigation.NavGraph
import com.arbeitszeit.tracker.ui.navigation.Screen
import com.arbeitszeit.tracker.ui.theme.ArbeitszeitTrackerTheme
import com.arbeitszeit.tracker.utils.NotificationHelper
import com.arbeitszeit.tracker.worker.ReminderWorker

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
}