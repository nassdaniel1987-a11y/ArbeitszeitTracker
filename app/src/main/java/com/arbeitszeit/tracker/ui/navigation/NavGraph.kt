package com.arbeitszeit.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arbeitszeit.tracker.ui.screens.*
import com.arbeitszeit.tracker.viewmodel.*
import kotlinx.serialization.Serializable

// Type-safe Navigation mit Kotlin Serialization (Compose Navigation 2.8+)
@Serializable object HomeRoute
@Serializable object CalendarRoute
@Serializable object UeberstundenRoute
@Serializable object DataManagementRoute  // Neu: kombiniert Export + Import
@Deprecated("Use DataManagementRoute instead") @Serializable object ExportRoute
@Deprecated("Use DataManagementRoute instead") @Serializable object ImportRoute
@Serializable object SettingsRoute
@Serializable object GeofencingRoute
@Serializable object TemplateManagementRoute
@Serializable object ArbeitszeitvorlagenRoute  // Sollzeit-Vorlagen
@Deprecated("Use ArbeitszeitvorlagenRoute instead") @Serializable object WeekTemplatesRoute
@Serializable object HelpRoute
@Serializable object VacationPlannerRoute
@Serializable object ClosingDaysRoute
@Serializable object YearManagementRoute

// Backwards compatibility - wird später entfernt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Ueberstunden : Screen("ueberstunden")
    object DataManagement : Screen("data_management")  // Neu: kombiniert Export + Import
    @Deprecated("Use DataManagement instead") object Export : Screen("export")
    @Deprecated("Use DataManagement instead") object Import : Screen("import")
    object Settings : Screen("settings")
    object Geofencing : Screen("geofencing")
    object TemplateManagement : Screen("template_management")
    object Arbeitszeitvorlagen : Screen("arbeitszeitvorlagen")  // Sollzeit-Vorlagen
    @Deprecated("Use Arbeitszeitvorlagen instead") object WeekTemplates : Screen("week_templates")
    object Help : Screen("help")
    object VacationPlanner : Screen("vacation_planner")
    object ClosingDays : Screen("closing_days")
    object YearManagement : Screen("year_management")
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
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToWeekTemplates = { navController.navigate(Screen.Arbeitszeitvorlagen.route) },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) }
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

        composable(Screen.DataManagement.route) {
            val viewModel: ExportViewModel = viewModel()
            DataManagementScreen(viewModel = viewModel)
        }

        // Backward compatibility - deprecated routes
        @Suppress("DEPRECATION")
        composable(Screen.Export.route) {
            val viewModel: ExportViewModel = viewModel()
            ExportScreen(viewModel = viewModel)
        }

        @Suppress("DEPRECATION")
        composable(Screen.Import.route) {
            val viewModel: ExportViewModel = viewModel()
            ImportScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToGeofencing = { navController.navigate(Screen.Geofencing.route) },
                onNavigateToTemplateManagement = { navController.navigate(Screen.TemplateManagement.route) },
                onNavigateToYearManagement = { navController.navigate(Screen.YearManagement.route) }
            )
        }

        composable(Screen.Geofencing.route) {
            val viewModel: GeofencingViewModel = viewModel()
            GeofencingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TemplateManagement.route) {
            val viewModel: TemplateViewModel = viewModel()
            TemplateManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Arbeitszeitvorlagen.route) {
            val viewModel: SettingsViewModel = viewModel()
            ArbeitszeitvorlagenScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Backward compatibility - deprecated route
        @Suppress("DEPRECATION")
        composable(Screen.WeekTemplates.route) {
            val viewModel: WeekTemplatesViewModel = viewModel()
            WeekTemplatesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VacationPlanner.route) {
            val viewModel: VacationPlannerViewModel = viewModel()
            VacationPlannerScreen(
                viewModel = viewModel,
                onNavigateToClosingDays = { navController.navigate(Screen.ClosingDays.route) }
            )
        }

        composable(Screen.ClosingDays.route) {
            val viewModel: ClosingDayViewModel = viewModel()
            ClosingDaysScreen(viewModel = viewModel)
        }

        composable(Screen.YearManagement.route) {
            val viewModel: YearViewModel = viewModel()
            YearManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
