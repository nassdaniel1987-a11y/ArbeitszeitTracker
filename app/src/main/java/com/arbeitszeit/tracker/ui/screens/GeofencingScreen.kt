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
import com.arbeitszeit.tracker.ui.components.OpenStreetMapView
import com.arbeitszeit.tracker.viewmodel.GeofencingViewModel
import com.arbeitszeit.tracker.viewmodel.PermissionStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencingScreen(viewModel: GeofencingViewModel) {
    val workLocations by viewModel.workLocations.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val permissionStatus = remember { viewModel.checkPermissions() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<WorkLocation?>(null) }
    var showPermissionInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Berechtigungsanfrage
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Berechtigungen wurden gewährt oder abgelehnt
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            val isEnabled = settings?.geofencingEnabled ?: false

            // Debug logging
            android.util.Log.d("GeofencingScreen", "Rendering card - isEnabled: $isEnabled, settings: $settings")

            Card(
                colors = if (isEnabled) {
                    CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50) // Grün
                    )
                } else {
                    CardDefaults.cardColors()
                }
            ) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isEnabled) androidx.compose.ui.graphics.Color.White
                                   else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isEnabled) "Aktiviert" else "Deaktiviert",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
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

        // Status-Anzeige
        if (settings?.geofencingEnabled == true) {
            item {
                val activeLocationsCount = workLocations.count { it.enabled }
                val hasPermissions = permissionStatus == PermissionStatus.GRANTED
                val isFullyOperational = hasPermissions && activeLocationsCount > 0

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFullyOperational) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFullyOperational) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isFullyOperational) {
                                androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFullyOperational) "Geofencing aktiv" else "Geofencing eingeschränkt",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isFullyOperational) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    append("$activeLocationsCount aktive${if (activeLocationsCount == 1) "r" else ""} Arbeitsort${if (activeLocationsCount != 1) "e" else ""}")
                                    if (!hasPermissions) {
                                        append(" • Berechtigungen fehlen")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFullyOperational) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isFullyOperational) {
                                Text(
                                    text = "Die Zeiterfassung startet/stoppt automatisch an deinen Arbeitsorten",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
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

        // Karte mit Arbeitsorten
        if (workLocations.isNotEmpty()) {
            item {
                var showMap by remember { mutableStateOf(true) }

                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Karte",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showMap = !showMap }) {
                                Icon(
                                    if (showMap) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showMap) "Karte ausblenden" else "Karte anzeigen"
                                )
                            }
                        }

                        if (showMap) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                OpenStreetMapView(
                                    workLocations = workLocations,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Text(
                                "Grüne Bereiche = Aktive Orte • Graue Bereiche = Deaktivierte Orte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
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
                onEdit = {
                    locationToEdit = location
                    showEditDialog = true
                },
                onDelete = { viewModel.deleteWorkLocation(location) }
            )
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

    // Dialog zum Bearbeiten eines Arbeitsorts
    if (showEditDialog && locationToEdit != null) {
        EditWorkLocationDialog(
            location = locationToEdit!!,
            onDismiss = {
                showEditDialog = false
                locationToEdit = null
            },
            onSave = { newName, newRadius ->
                viewModel.updateWorkLocation(locationToEdit!!, newName, newRadius)
                showEditDialog = false
                locationToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Arbeitsort erfolgreich aktualisiert")
                }
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                // Adresse anzeigen, falls vorhanden
                if (!location.address.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            location.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    "Radius: ${location.radiusMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                Switch(
                    checked = location.enabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Bearbeiten")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Löschen")
                }
            }
        }
    }

    // Bestätigungsdialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Arbeitsort löschen?") },
            text = { Text("Möchtest du den Arbeitsort \"${location.name}\" wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
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
    var plusCode by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var useCurrentLocation by remember { mutableStateOf(false) }
    var usePlusCode by remember { mutableStateOf(false) }
    var plusCodeError by remember { mutableStateOf<String?>(null) }

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

                // Plus Code Eingabe mit Zwischenablage-Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = plusCode,
                        onValueChange = { newValue ->
                            val trimmedValue = newValue.trim().uppercase()
                            plusCode = trimmedValue
                            usePlusCode = trimmedValue.isNotBlank()
                            useCurrentLocation = false

                            // Versuche Plus Code zu dekodieren
                            if (trimmedValue.isNotBlank()) {
                            try {
                                // Extrahiere den Plus Code aus dem Text
                                // Voller Code: 8 Zeichen + "+" + 2-3 Zeichen (z.B. 8FWC9RGX+2G)
                                // Kurzer Code: 4 Zeichen + "+" + 2-3 Zeichen (z.B. P4M6+473)
                                val fullCodePattern = Regex("([23456789C][23456789CFGHJMPQRV][23456789CFGHJMPQRVWX]{6}\\+[23456789CFGHJMPQRVWX]{2,3})")
                                val shortCodePattern = Regex("([23456789CFGHJMPQRVWX]{4}\\+[23456789CFGHJMPQRVWX]{2,3})")

                                val fullMatch = fullCodePattern.find(trimmedValue)
                                val shortMatch = shortCodePattern.find(trimmedValue)
                                val extractedCode = fullMatch?.value ?: shortMatch?.value ?: trimmedValue

                                android.util.Log.d("PlusCode", "Input: '$trimmedValue', Extracted: '$extractedCode'")

                                var olc = com.google.openlocationcode.OpenLocationCode(extractedCode)

                                // Wenn es ein kurzer Code ist, versuche ihn mit einer Referenz-Koordinate zu vervollständigen
                                if (!olc.isFull && olc.isShort) {
                                    android.util.Log.d("PlusCode", "Short code detected, recovering with reference location")

                                    // Versuche, Stadtnamen aus dem Text zu extrahieren
                                    val cityName = trimmedValue.replace(extractedCode, "").trim().split(",").firstOrNull()?.trim()
                                    android.util.Log.d("PlusCode", "Extracted city name: '$cityName'")

                                    var refLat = 51.0  // Default: Zentrum Deutschland
                                    var refLng = 10.5

                                    // Wenn ein Stadtname vorhanden ist, versuche Geocoding
                                    if (!cityName.isNullOrBlank()) {
                                        try {
                                            val geocoder = android.location.Geocoder(context, java.util.Locale.GERMANY)
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                // Für Android 13+ müssten wir async API verwenden, was kompliziert ist
                                                // Verwende stattdessen alte API auch hier
                                                @Suppress("DEPRECATION")
                                                val addresses = geocoder.getFromLocationName(cityName, 1)
                                                if (!addresses.isNullOrEmpty()) {
                                                    refLat = addresses[0].latitude
                                                    refLng = addresses[0].longitude
                                                    android.util.Log.d("PlusCode", "Found reference: $cityName at $refLat, $refLng")
                                                }
                                            } else {
                                                @Suppress("DEPRECATION")
                                                val addresses = geocoder.getFromLocationName(cityName, 1)
                                                if (!addresses.isNullOrEmpty()) {
                                                    refLat = addresses[0].latitude
                                                    refLng = addresses[0].longitude
                                                    android.util.Log.d("PlusCode", "Found reference: $cityName at $refLat, $refLng")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.w("PlusCode", "Could not geocode city: $cityName", e)
                                        }
                                    }

                                    val recovered = olc.recover(refLat, refLng)
                                    olc = recovered
                                    android.util.Log.d("PlusCode", "Recovered full code: ${recovered.code}")
                                }

                                if (olc.isFull) {
                                    val decoded = olc.decode()
                                    latitude = String.format(java.util.Locale.US, "%.6f", decoded.centerLatitude)
                                    longitude = String.format(java.util.Locale.US, "%.6f", decoded.centerLongitude)
                                    plusCodeError = null
                                    android.util.Log.d("PlusCode", "Successfully decoded: lat=$latitude, lng=$longitude")
                                } else {
                                    plusCodeError = "Plus Code ist unvollständig"
                                    android.util.Log.w("PlusCode", "Code is not full: $extractedCode")
                                }
                            } catch (e: Exception) {
                                plusCodeError = "Ungültiger Plus Code: ${e.message}"
                                android.util.Log.e("PlusCode", "Error decoding: '$trimmedValue'", e)
                            }
                        } else {
                            plusCodeError = null
                        }
                    },
                        label = { Text("Plus Code") },
                        placeholder = { Text("z.B. P4M6+473 Stuttgart") },
                        singleLine = true,
                        isError = plusCodeError != null,
                        supportingText = if (plusCodeError != null) {
                            { Text(plusCodeError!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        trailingIcon = if (usePlusCode && plusCodeError == null) {
                            {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = "Gültig",
                                    tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    // Paste Button
                    IconButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = clipboardManager.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                // Trigger the onValueChange to process the pasted text
                                val trimmedValue = text.trim().uppercase()
                                plusCode = trimmedValue
                                usePlusCode = trimmedValue.isNotBlank()
                                useCurrentLocation = false

                                // Copy the decode logic here
                                if (trimmedValue.isNotBlank()) {
                                    try {
                                        val fullCodePattern = Regex("([23456789C][23456789CFGHJMPQRV][23456789CFGHJMPQRVWX]{6}\\+[23456789CFGHJMPQRVWX]{2,3})")
                                        val shortCodePattern = Regex("([23456789CFGHJMPQRVWX]{4}\\+[23456789CFGHJMPQRVWX]{2,3})")
                                        val fullMatch = fullCodePattern.find(trimmedValue)
                                        val shortMatch = shortCodePattern.find(trimmedValue)
                                        val extractedCode = fullMatch?.value ?: shortMatch?.value ?: trimmedValue
                                        android.util.Log.d("PlusCode", "Pasted - Input: '$trimmedValue', Extracted: '$extractedCode'")
                                        var olc = com.google.openlocationcode.OpenLocationCode(extractedCode)
                                        if (!olc.isFull && olc.isShort) {
                                            val cityName = trimmedValue.replace(extractedCode, "").trim().split(",").firstOrNull()?.trim()
                                            var refLat = 51.0
                                            var refLng = 10.5
                                            if (!cityName.isNullOrBlank()) {
                                                try {
                                                    val geocoder = android.location.Geocoder(context, java.util.Locale.GERMANY)
                                                    @Suppress("DEPRECATION")
                                                    val addresses = geocoder.getFromLocationName(cityName, 1)
                                                    if (!addresses.isNullOrEmpty()) {
                                                        refLat = addresses[0].latitude
                                                        refLng = addresses[0].longitude
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.w("PlusCode", "Could not geocode city: $cityName", e)
                                                }
                                            }
                                            val recovered = olc.recover(refLat, refLng)
                                            olc = recovered
                                        }
                                        if (olc.isFull) {
                                            val decoded = olc.decode()
                                            latitude = String.format(java.util.Locale.US, "%.6f", decoded.centerLatitude)
                                            longitude = String.format(java.util.Locale.US, "%.6f", decoded.centerLongitude)
                                            plusCodeError = null
                                        } else {
                                            plusCodeError = "Plus Code ist unvollständig"
                                        }
                                    } catch (e: Exception) {
                                        plusCodeError = "Ungültiger Plus Code: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Aus Zwischenablage einfügen")
                    }
                }

                if (usePlusCode && plusCodeError == null && latitude.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Dekodierte Koordinaten",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "$latitude, $longitude",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                        usePlusCode = false
                        plusCode = ""
                    },
                    label = { Text("Breitengrad") },
                    placeholder = { Text("z.B. 48.775556") },
                    singleLine = true,
                    enabled = !usePlusCode
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = {
                        longitude = it
                        useCurrentLocation = false
                        usePlusCode = false
                        plusCode = ""
                    },
                    label = { Text("Längengrad") },
                    placeholder = { Text("z.B. 9.182778") },
                    singleLine = true,
                    enabled = !usePlusCode
                )
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (Meter)") },
                    placeholder = { Text("100") },
                    singleLine = true
                )
                Text(
                    "Tipp: Öffne Google Maps, tippe auf deinen Arbeitsort und kopiere den Plus Code mit Stadtnamen (z.B. \"P4M6+473 Stuttgart\").",
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
                        radius.replace(',', '.').toFloatOrNull() != null &&
                        (usePlusCode && plusCodeError == null || !usePlusCode)
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
private fun EditWorkLocationDialog(
    location: WorkLocation,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var radius by remember { mutableStateOf(location.radiusMeters.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arbeitsort bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Hauptbüro") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (Meter)") },
                    placeholder = { Text("100") },
                    singleLine = true
                )

                // Zeige aktuelle Koordinaten an (nicht editierbar)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Standort",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!location.address.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    location.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Text(
                    "Der Standort kann nicht geändert werden. Um einen anderen Standort zu verwenden, erstelle einen neuen Arbeitsort.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                enabled = name.isNotBlank() && radius.replace(',', '.').toFloatOrNull() != null && (radius.replace(',', '.').toFloatOrNull() ?: 0f) > 0
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
