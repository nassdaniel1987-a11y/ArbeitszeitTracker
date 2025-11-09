package com.arbeitszeit.tracker.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.viewmodel.GeofencingViewModel
import com.arbeitszeit.tracker.viewmodel.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencingScreen(viewModel: GeofencingViewModel) {
    val workLocations by viewModel.workLocations.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val permissionStatus = remember { viewModel.checkPermissions() }
    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showPermissionInfo by remember { mutableStateOf(false) }

    // Berechtigungsanfrage
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Berechtigungen wurden gewährt oder abgelehnt
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Automatische Zeiterfassung",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Text(
                "Starte und beende die Arbeitszeit automatisch basierend auf deinem Standort.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Berechtigungsstatus
        if (permissionStatus != PermissionStatus.GRANTED) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Standortberechtigung erforderlich",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (permissionStatus) {
                                PermissionStatus.DENIED -> "Die App benötigt Zugriff auf deinen Standort, um die automatische Zeiterfassung zu nutzen."
                                PermissionStatus.LOCATION_ONLY -> "Für die Hintergrundüberwachung wird die Berechtigung 'Immer zulassen' benötigt."
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text("Berechtigung erteilen")
                        }
                    }
                }
            }
        }

        // Geofencing An/Aus
        item {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Automatische Zeiterfassung",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Aktiviert",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings?.geofencingEnabled ?: false,
                        onCheckedChange = { enabled ->
                            if (permissionStatus == PermissionStatus.GRANTED || !enabled) {
                                viewModel.toggleGeofencing(enabled)
                            } else {
                                showPermissionInfo = true
                            }
                        }
                    )
                }
            }
        }

        // Zeitfenster
        if (settings?.geofencingEnabled == true) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Aktive Zeiten",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${settings?.geofencingStartHour ?: 6}:00 - ${settings?.geofencingEndHour ?: 20}:00 Uhr",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Außerhalb dieser Zeiten erhältst du keine Benachrichtigungen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Aktive Tage
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Aktive Tage",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val activeDays = settings?.geofencingActiveDays ?: "12345"
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { index, day ->
                                val dayNumber = (index + 1).toString()
                                FilterChip(
                                    selected = activeDays.contains(dayNumber),
                                    onClick = { /* TODO: Implement */ },
                                    label = { Text(day) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Arbeitsorte
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Arbeitsorte",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Arbeitsort hinzufügen")
                }
            }
        }

        if (workLocations.isEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keine Arbeitsorte",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Füge deinen ersten Arbeitsort hinzu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(workLocations) { location ->
            WorkLocationCard(
                location = location,
                onToggle = { viewModel.toggleWorkLocation(location) },
                onDelete = { viewModel.deleteWorkLocation(location) },
                onTest = {
                    // Test-Benachrichtigung für diesen Arbeitsort
                    testGeofenceNotification(context, location)
                }
            )
        }

        // Debug-Hinweis
        if (workLocations.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Test-Funktion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Tippe auf das Test-Symbol beim Arbeitsort, um die Benachrichtigung zu testen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }

    // Dialog zum Hinzufügen eines Arbeitsorts
    if (showAddDialog) {
        AddWorkLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lng, radius ->
                viewModel.addWorkLocation(name, lat, lng, radius)
                showAddDialog = false
            }
        )
    }

    // Berechtigungsinfo-Dialog
    if (showPermissionInfo) {
        AlertDialog(
            onDismissRequest = { showPermissionInfo = false },
            title = { Text("Berechtigung erforderlich") },
            text = { Text("Für die automatische Zeiterfassung wird die Standortberechtigung benötigt.") },
            confirmButton = {
                TextButton(onClick = { showPermissionInfo = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun WorkLocationCard(
    location: WorkLocation,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    location.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Radius: ${location.radiusMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onTest) {
                    Icon(Icons.Default.BugReport, "Test", tint = MaterialTheme.colorScheme.tertiary)
                }
                Switch(
                    checked = location.enabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen")
                }
            }
        }
    }
}

@Composable
private fun AddWorkLocationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var useCurrentLocation by remember { mutableStateOf(false) }

    // TODO: Location Manager für aktuellen Standort
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arbeitsort hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Hauptbüro") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        // Versuche aktuellen Standort zu ermitteln
                        try {
                            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                            if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            ) {
                                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

                                location?.let {
                                    // Verwende Locale.US um immer Punkt als Dezimaltrennzeichen zu erzeugen
                                    latitude = String.format(java.util.Locale.US, "%.6f", it.latitude)
                                    longitude = String.format(java.util.Locale.US, "%.6f", it.longitude)
                                    useCurrentLocation = true
                                }
                            }
                        } catch (e: SecurityException) {
                            // Berechtigung fehlt
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.MyLocation,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Aktuellen Standort verwenden")
                }

                if (useCurrentLocation) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Standort ermittelt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "$latitude, $longitude",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text("oder", style = MaterialTheme.typography.labelSmall)

                OutlinedTextField(
                    value = latitude,
                    onValueChange = {
                        latitude = it
                        useCurrentLocation = false
                    },
                    label = { Text("Breitengrad") },
                    placeholder = { Text("z.B. 48.775556") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = {
                        longitude = it
                        useCurrentLocation = false
                    },
                    label = { Text("Längengrad") },
                    placeholder = { Text("z.B. 9.182778") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (Meter)") },
                    placeholder = { Text("100") },
                    singleLine = true
                )
                Text(
                    "Tipp: Oder öffne Google Maps, tippe lange auf deinen Arbeitsort und kopiere die Koordinaten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Kommas durch Punkte ersetzen für deutsche Zahleneingaben
                    val lat = latitude.replace(',', '.').toDoubleOrNull()
                    val lng = longitude.replace(',', '.').toDoubleOrNull()
                    val rad = radius.replace(',', '.').toFloatOrNull()
                    if (name.isNotBlank() && lat != null && lng != null && rad != null) {
                        onAdd(name, lat, lng, rad)
                    }
                },
                enabled = name.isNotBlank() &&
                        latitude.replace(',', '.').toDoubleOrNull() != null &&
                        longitude.replace(',', '.').toDoubleOrNull() != null &&
                        radius.replace(',', '.').toFloatOrNull() != null
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

// Hilfsfunktion zum Testen der Geofence-Benachrichtigung
fun testGeofenceNotification(context: android.content.Context, location: WorkLocation) {
    val intent = android.content.Intent(context, com.arbeitszeit.tracker.geofencing.GeofenceBroadcastReceiver::class.java)
    intent.action = com.arbeitszeit.tracker.geofencing.GeofenceBroadcastReceiver.ACTION_TEST_GEOFENCE_ENTER
    intent.putExtra("location_name", location.name)
    context.sendBroadcast(intent)
}
