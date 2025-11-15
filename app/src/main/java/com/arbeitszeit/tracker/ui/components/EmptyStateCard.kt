package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Wiederverwendbare Empty State Komponente
 * Zeigt einen freundlichen Hinweis wenn keine Daten vorhanden sind
 */
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Optional Action Button
            if (actionText != null && onActionClick != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

/**
 * Vordefinierte Empty States für verschiedene Screens
 */
object EmptyStates {
    @Composable
    fun NoWeekEntries() {
        EmptyStateCard(
            icon = Icons.Default.EventBusy,
            title = "Keine Einträge",
            description = "Für diese Woche sind noch keine Zeiteinträge vorhanden. " +
                    "Nutze den Plus-Button, um schnell zu stempeln, oder trage die Zeiten manuell im Kalender ein."
        )
    }

    @Composable
    fun NoMonthEntries() {
        EmptyStateCard(
            icon = Icons.Default.CalendarMonth,
            title = "Keine Einträge",
            description = "Für diesen Monat sind noch keine Zeiteinträge vorhanden. " +
                    "Wähle einen Tag aus, um einen Eintrag zu erstellen."
        )
    }

    @Composable
    fun NoOvertimeData() {
        EmptyStateCard(
            icon = Icons.Default.QueryStats,
            title = "Keine Daten",
            description = "Noch keine Überstunden-Daten vorhanden. " +
                    "Sobald du Zeiteinträge erfasst hast, werden hier deine Überstunden angezeigt."
        )
    }

    @Composable
    fun NoSearchResults(searchQuery: String) {
        EmptyStateCard(
            icon = Icons.Default.SearchOff,
            title = "Keine Ergebnisse",
            description = "Für \"$searchQuery\" wurden keine Einträge gefunden. " +
                    "Versuche es mit einem anderen Suchbegriff."
        )
    }

    @Composable
    fun NoLocations(onAddLocation: () -> Unit) {
        EmptyStateCard(
            icon = Icons.Default.AddLocation,
            title = "Keine Standorte",
            description = "Du hast noch keine Standorte für die automatische Zeiterfassung konfiguriert. " +
                    "Füge deinen Arbeitsplatz hinzu, um die automatische Zeiterfassung zu nutzen.",
            actionText = "Standort hinzufügen",
            onActionClick = onAddLocation
        )
    }

    @Composable
    fun ErrorState(errorMessage: String, onRetry: (() -> Unit)? = null) {
        EmptyStateCard(
            icon = Icons.Default.ErrorOutline,
            title = "Fehler",
            description = errorMessage,
            actionText = if (onRetry != null) "Erneut versuchen" else null,
            onActionClick = onRetry
        )
    }
}
