package com.arbeitszeit.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arbeitszeit.tracker.ui.screens.*
import com.arbeitszeit.tracker.viewmodel.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Ueberstunden : Screen("ueberstunden")
    object Export : Screen("export")
    object Settings : Screen("settings")
    object Geofencing : Screen("geofencing")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) }
            )
        }
        
        composable(Screen.Calendar.route) {
            val viewModel: CalendarViewModel = viewModel()
            CalendarScreen(viewModel = viewModel)
        }

        composable(Screen.Ueberstunden.route) {
            val viewModel: UeberstundenViewModel = viewModel()
            UeberstundenScreen(viewModel = viewModel)
        }

        composable(Screen.Export.route) {
            val viewModel: ExportViewModel = viewModel()
            ExportScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToGeofencing = { navController.navigate(Screen.Geofencing.route) }
            )
        }

        composable(Screen.Geofencing.route) {
            val viewModel: GeofencingViewModel = viewModel()
            GeofencingScreen(viewModel = viewModel)
        }
    }
}
