package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.LocationStatus

@Composable
fun GeofencingStatusCard(
    locationStatus: LocationStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (locationStatus) {
                is LocationStatus.AtWork -> MaterialTheme.colorScheme.primaryContainer
                is LocationStatus.NotAtWork -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator circle
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (locationStatus) {
                                is LocationStatus.AtWork -> Color(0xFF4CAF50) // Green
                                is LocationStatus.NotAtWork -> Color(0xFFFF9800) // Orange
                                is LocationStatus.GeofencingDisabled -> Color.Gray
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )

                Column {
                    Text(
                        text = when (locationStatus) {
                            is LocationStatus.AtWork -> "Am Arbeitsort"
                            is LocationStatus.NotAtWork -> "Nicht am Arbeitsort"
                            is LocationStatus.GeofencingDisabled -> "Automatik deaktiviert"
                            is LocationStatus.NoLocations -> "Keine Orte konfiguriert"
                            is LocationStatus.NoPermission -> "Keine Berechtigung"
                            is LocationStatus.LocationUnavailable -> "Standort nicht verfügbar"
                            is LocationStatus.Unknown -> "Status unbekannt"
                            is LocationStatus.Error -> "Fehler"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (locationStatus is LocationStatus.AtWork) {
                        Text(
                            text = locationStatus.location.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (locationStatus is LocationStatus.Error) {
                        Text(
                            text = locationStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Refresh button (only show when geofencing is active)
            if (locationStatus !is LocationStatus.GeofencingDisabled &&
                locationStatus !is LocationStatus.NoLocations
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Status aktualisieren",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
