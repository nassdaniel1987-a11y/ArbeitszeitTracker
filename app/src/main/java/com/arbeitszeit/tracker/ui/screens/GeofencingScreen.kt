package com.arbeitszeit.tracker.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.ui.components.OpenStreetMapView
import com.arbeitszeit.tracker.ui.components.centerMapToMyLocation
import com.arbeitszeit.tracker.viewmodel.GeofencingViewModel
import com.arbeitszeit.tracker.viewmodel.PermissionStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencingScreen(
    viewModel: GeofencingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val workLocations by viewModel.workLocations.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val permissionStatus = remember { viewModel.checkPermissions() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<WorkLocation?>(null) }
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler { onNavigateBack() }

    // Berechtigungsanfrage
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Berechtigungen wurden bearbeitet */ }

    val isEnabled = settings?.geofencingEnabled ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arbeitsorte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Add Button
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Arbeitsort hinzufügen")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Berechtigungswarnung
            if (permissionStatus != PermissionStatus.GRANTED) {
                PermissionBanner(
                    status = permissionStatus,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            // Große Karte
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
            ) {
                OpenStreetMapView(
                    workLocations = workLocations,
                    modifier = Modifier.fillMaxSize(),
                    onCenterToMyLocation = { mapView -> mapViewRef = mapView }
                )

                // Karten-Buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mein Standort Button
                    FloatingActionButton(
                        onClick = { centerMapToMyLocation(mapViewRef) },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Mein Standort",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Legende unten links
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendItem(color = Color(0xFF4CAF50), text = "Hier")
                        LegendItem(color = Color(0xFF2196F3), text = "Aktiv")
                        LegendItem(color = Color(0xFF808080), text = "Aus")
                    }
                }
            }

            // Status-Leiste
            StatusBar(
                isEnabled = isEnabled,
                settings = settings,
                onToggle = { enabled ->
                    if (permissionStatus == PermissionStatus.GRANTED || !enabled) {
                        viewModel.toggleGeofencing(enabled)
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Standortberechtigung erforderlich")
                        }
                    }
                },
                onSettingsClick = { showSettingsDialog = true }
            )

            // Arbeitsorte Liste
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (workLocations.isEmpty()) {
                    item {
                        EmptyLocationsCard(onAddClick = { showAddDialog = true })
                    }
                }

                items(workLocations) { location ->
                    WorkLocationItem(
                        location = location,
                        onToggle = { viewModel.toggleWorkLocation(location) },
                        onEdit = {
                            locationToEdit = location
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteWorkLocation(location)
                            scope.launch {
                                snackbarHostState.showSnackbar("${location.name} gelöscht")
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialoge
    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lng, radius ->
                viewModel.addWorkLocation(name, lat, lng, radius)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("$name hinzugefügt")
                }
            }
        )
    }

    if (showEditDialog && locationToEdit != null) {
        EditLocationDialog(
            location = locationToEdit!!,
            onDismiss = {
                showEditDialog = false
                locationToEdit = null
            },
            onSave = { newName, newRadius ->
                viewModel.updateWorkLocation(locationToEdit!!, newName, newRadius)
                showEditDialog = false
                locationToEdit = null
            }
        )
    }

    if (showSettingsDialog) {
        GeofencingSettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { startHour, endHour, activeDays ->
                viewModel.updateGeofencingSettings(startHour, endHour, activeDays)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PermissionBanner(
    status: PermissionStatus,
    onRequestPermission: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onRequestPermission() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Standortberechtigung fehlt",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    if (status == PermissionStatus.LOCATION_ONLY)
                        "Hintergrund-Standort wird benötigt"
                    else
                        "Tippen um Berechtigung zu erteilen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatusBar(
    isEnabled: Boolean,
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = if (isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isEnabled) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        if (isEnabled) "Automatisch aktiv" else "Deaktiviert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isEnabled && settings != null) {
                        val days = formatActiveDays(settings.geofencingActiveDays)
                        Text(
                            "$days • ${settings.geofencingStartHour}:00–${settings.geofencingEndHour}:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEnabled) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Einstellungen",
                            tint = Color.White
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

private fun formatActiveDays(days: String): String {
    return when (days) {
        "12345" -> "Mo–Fr"
        "123456" -> "Mo–Sa"
        "1234567" -> "Täglich"
        else -> {
            val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            days.map { dayNames.getOrNull(it.digitToInt() - 1) ?: "" }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
        }
    }
}

@Composable
private fun EmptyLocationsCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AddLocation,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Kein Arbeitsort",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Füge deinen Arbeitsort hinzu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hinzufügen")
            }
        }
    }
}

@Composable
private fun WorkLocationItem(
    location: WorkLocation,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (location.enabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status-Indikator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (location.enabled) Color(0xFF4CAF50) else Color(0xFF808080)
                        )
                )

                Column {
                    Text(
                        location.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (!location.address.isNullOrBlank()) {
                        Text(
                            location.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Radius: ${location.radiusMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Bearbeiten",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = location.enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Löschen?") },
            text = { Text("\"${location.name}\" wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var locationFetched by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AddLocation, contentDescription = null) },
        title = { Text("Arbeitsort hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Büro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Aktuellen Standort Button
                Button(
                    onClick = {
                        try {
                            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                            if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            ) {
                                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

                                location?.let {
                                    latitude = String.format(java.util.Locale.US, "%.6f", it.latitude)
                                    longitude = String.format(java.util.Locale.US, "%.6f", it.longitude)
                                    locationFetched = true
                                }
                            }
                        } catch (e: SecurityException) { /* Berechtigung fehlt */ }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aktuellen Standort verwenden")
                }

                if (locationFetched) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                "$latitude, $longitude",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "Oder Koordinaten eingeben:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = {
                            latitude = it
                            locationFetched = false
                        },
                        label = { Text("Breite") },
                        placeholder = { Text("48.123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = {
                            longitude = it
                            locationFetched = false
                        },
                        label = { Text("Länge") },
                        placeholder = { Text("9.123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (Meter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
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

@Composable
private fun EditLocationDialog(
    location: WorkLocation,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var radius by remember { mutableStateOf(location.radiusMeters.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (Meter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Standort",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!location.address.isNullOrBlank()) {
                            Text(
                                location.address,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rad = radius.replace(',', '.').toFloatOrNull()
                    if (name.isNotBlank() && rad != null && rad > 0) {
                        onSave(name, rad)
                    }
                },
                enabled = name.isNotBlank() && (radius.replace(',', '.').toFloatOrNull() ?: 0f) > 0
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeofencingSettingsDialog(
    settings: com.arbeitszeit.tracker.data.entity.UserSettings?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit
) {
    var startHour by remember { mutableStateOf(settings?.geofencingStartHour ?: 6) }
    var endHour by remember { mutableStateOf(settings?.geofencingEndHour ?: 20) }
    var activeDays by remember { mutableStateOf(settings?.geofencingActiveDays ?: "12345") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        title = { Text("Zeiteinstellungen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Zeitfenster
                Text(
                    "Aktive Zeiten",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startHour.toString(),
                        onValueChange = { startHour = it.toIntOrNull() ?: startHour },
                        label = { Text("Von") },
                        suffix = { Text("Uhr") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text("–")
                    OutlinedTextField(
                        value = endHour.toString(),
                        onValueChange = { endHour = it.toIntOrNull() ?: endHour },
                        label = { Text("Bis") },
                        suffix = { Text("Uhr") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // Aktive Tage
                Text(
                    "Aktive Tage",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Mo" to "1", "Di" to "2", "Mi" to "3", "Do" to "4", "Fr" to "5", "Sa" to "6", "So" to "7")
                        .forEach { (label, dayNum) ->
                            FilterChip(
                                selected = activeDays.contains(dayNum),
                                onClick = {
                                    activeDays = if (activeDays.contains(dayNum)) {
                                        activeDays.replace(dayNum, "")
                                    } else {
                                        (activeDays + dayNum).toCharArray().sorted().joinToString("")
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(startHour, endHour, activeDays) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
