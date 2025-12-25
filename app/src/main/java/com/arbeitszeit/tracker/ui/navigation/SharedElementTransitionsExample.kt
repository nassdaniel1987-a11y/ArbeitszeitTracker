package com.arbeitszeit.tracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Shared Element Transitions Example
 *
 * Demonstriert smooth Übergänge zwischen Screens mit gemeinsamen Elementen.
 *
 * Beispiel-Use-Cases:
 * - Zeiteinträge-Liste → Detail-Ansicht (Datum/Zeit-Element teilen)
 * - Kalender-Tag → Detail-Ansicht (Tag-Nummer teilen)
 * - Settings-Item → Settings-Detail (Icon teilen)
 *
 * Verfügbar ab Compose 1.7.0+ (Teil der Compose BOM 2025.11.01)
 */

// Beispiel Datenmodell
data class WorkEntry(
    val id: Int,
    val date: String,
    val hours: String,
    val description: String
)

/**
 * Haupt-Composable mit SharedTransitionLayout
 *
 * Das SharedTransitionLayout muss die gesamte Navigation umschließen,
 * damit Elemente zwischen Screens geteilt werden können.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementExample(
    navController: NavHostController = rememberNavController()
) {
    // SharedTransitionLayout umschließt die Navigation
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = "list"
        ) {
            composable("list") {
                WorkEntryListScreen(
                    animatedVisibilityScope = this,
                    onEntryClick = { entryId ->
                        navController.navigate("detail/$entryId")
                    }
                )
            }

            composable("detail/{entryId}") { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId")?.toIntOrNull() ?: 0
                WorkEntryDetailScreen(
                    entryId = entryId,
                    animatedVisibilityScope = this,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Listen-Screen mit shared elements
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.WorkEntryListScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEntryClick: (Int) -> Unit
) {
    val sampleEntries = listOf(
        WorkEntry(1, "2025-01-15", "8.5h", "Entwicklung Feature A"),
        WorkEntry(2, "2025-01-16", "7.0h", "Code Review"),
        WorkEntry(3, "2025-01-17", "9.0h", "Bug Fixes")
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Zeiteinträge") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleEntries) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick(entry.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Shared Element: Datum
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "date-${entry.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                        )

                        // Shared Element: Stunden
                        Text(
                            text = entry.hours,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "hours-${entry.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                        )

                        // Nicht-shared Element: Beschreibung (fade in/out)
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detail-Screen mit shared elements
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.WorkEntryDetailScreen(
    entryId: Int,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateBack: () -> Unit
) {
    val entry = WorkEntry(entryId, "2025-01-15", "8.5h", "Entwicklung Feature A")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shared Element: Datum (wird von Liste hierher animiert)
            Text(
                text = entry.date,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "date-${entry.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )

            // Shared Element: Stunden
            Text(
                text = entry.hours,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "hours-${entry.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )

            HorizontalDivider()

            // Zusätzliche Details (fade in)
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Weitere Details könnten hier angezeigt werden...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Migration Guide für bestehende Screens:
 *
 * 1. Umschließe die NavHost in MainActivity mit SharedTransitionLayout:
 * ```
 * SharedTransitionLayout {
 *     NavGraph(navController = navController)
 * }
 * ```
 *
 * 2. Füge in NavGraph.kt den animatedVisibilityScope Parameter hinzu:
 * ```
 * composable(Screen.Home.route) {
 *     HomeScreen(
 *         viewModel = viewModel(),
 *         animatedVisibilityScope = this  // AnimatedContentScope
 *     )
 * }
 * ```
 *
 * 3. In den Screens selbst (z.B. CalendarScreen):
 * ```
 * @Composable
 * fun SharedTransitionScope.CalendarScreen(
 *     animatedVisibilityScope: AnimatedVisibilityScope,
 *     ...
 * ) {
 *     // Verwende .sharedElement() auf Elementen, die geteilt werden sollen
 *     Text(
 *         text = dayNumber,
 *         modifier = Modifier.sharedElement(
 *             state = rememberSharedContentState(key = "day-$dayNumber"),
 *             animatedVisibilityScope = animatedVisibilityScope
 *         )
 *     )
 * }
 * ```
 *
 * Best Practices:
 * - Verwende eindeutige Keys für jedes shared element (z.B. "day-15", "entry-42")
 * - Nur Elemente teilen, die visuell zusammenhängen (z.B. gleiche Daten)
 * - Nicht zu viele shared elements auf einmal (max 2-3 pro Transition)
 * - Teste auf verschiedenen Geräten/Screen-Größen
 */
