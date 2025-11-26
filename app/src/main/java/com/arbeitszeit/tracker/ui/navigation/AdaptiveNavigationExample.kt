package com.arbeitszeit.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Adaptive Navigation Example mit Material 3 NavigationSuiteScaffold
 *
 * Passt sich automatisch an die Bildschirmgröße an:
 * - Phones (Compact): Bottom Navigation Bar
 * - Tablets Portrait (Medium): Navigation Rail (Sidebar)
 * - Tablets Landscape (Expanded): Navigation Drawer (persistent)
 *
 * Um diese Adaptive Navigation zu aktivieren:
 * 1. Ersetze in MainActivity den ModalNavigationDrawer durch AdaptiveNavigationLayout
 * 2. Passe die Navigation-Items nach Bedarf an
 */

data class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun AdaptiveNavigationLayout(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Haupt-Navigation Items
    val navigationItems = listOf(
        NavigationItem(Screen.Home.route, Icons.Default.Home, "Home"),
        NavigationItem(Screen.Ueberstunden.route, Icons.Default.Timeline, "Überstunden"),
        NavigationItem(Screen.Calendar.route, Icons.Default.CalendarMonth, "Kalender"),
        NavigationItem(Screen.Export.route, Icons.Default.FileDownload, "Export"),
        NavigationItem(Screen.Settings.route, Icons.Default.Settings, "Einstellungen")
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            navigationItems.forEach { item ->
                item(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            // Verhindere mehrfache Kopien auf dem Back Stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        // Content Area - Die NavGraph wird hier angezeigt
        NavGraph(navController = navController)
    }
}

/**
 * Alternative: Manuell das Layout-Typ basierend auf Window Size Class wählen
 *
 * Beispiel:
 * ```
 * val windowSizeClass = calculateWindowSizeClass(this)
 * val layoutType = when (windowSizeClass.widthSizeClass) {
 *     WindowWidthSizeClass.Compact -> NavigationSuiteType.NavigationBar
 *     WindowWidthSizeClass.Medium -> NavigationSuiteType.NavigationRail
 *     else -> NavigationSuiteType.NavigationDrawer
 * }
 * ```
 */

/**
 * Migration Guide:
 *
 * In MainActivity.kt ersetzen:
 *
 * Alt:
 * ```
 * ModalNavigationDrawer(
 *     drawerState = drawerState,
 *     drawerContent = { ... }
 * ) {
 *     Scaffold(topBar = { ... }) { padding ->
 *         NavGraph(navController = navController)
 *     }
 * }
 * ```
 *
 * Neu:
 * ```
 * AdaptiveNavigationLayout(navController = navController)
 * ```
 *
 * Das war's! Die Navigation passt sich jetzt automatisch an:
 * - Phone: Bottom Navigation
 * - Tablet: Side Navigation Rail oder Drawer
 */
