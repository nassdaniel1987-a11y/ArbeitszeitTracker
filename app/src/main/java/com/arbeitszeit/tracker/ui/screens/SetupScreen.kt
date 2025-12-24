package com.arbeitszeit.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbeitszeit.tracker.viewmodel.SetupStep
import com.arbeitszeit.tracker.viewmodel.SetupViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Setup Wizard Screen für neue Benutzer
 *
 * Führt durch die erste Einrichtung:
 * 1. Willkommen
 * 2. Excel-Vorlage hochladen (optional)
 * 3. Benutzerdaten eingeben/bestätigen
 * 4. Abschluss
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // DatePicker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Wenn Setup abgeschlossen, navigiere zur App
    LaunchedEffect(uiState.setupComplete) {
        if (uiState.setupComplete) {
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einrichtung") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Progress Indicator
            SetupProgressIndicator(
                currentStep = uiState.currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Content
            when (uiState.currentStep) {
                SetupStep.WELCOME -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                SetupStep.TEMPLATE_UPLOAD -> TemplateUploadStep(
                    viewModel = viewModel,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onNext = { viewModel.nextStep() },
                    onSkip = { viewModel.skipTemplateUpload() },
                    onBack = { viewModel.previousStep() }
                )
                SetupStep.USER_DATA -> UserDataStep(
                    viewModel = viewModel,
                    uiState = uiState,
                    showDatePicker = showDatePicker,
                    onShowDatePickerChange = { showDatePicker = it },
                    datePickerState = datePickerState,
                    onComplete = { viewModel.completeSetup() },
                    onBack = { viewModel.previousStep() }
                )
                SetupStep.COMPLETION -> CompletionStep(
                    onFinish = onSetupComplete
                )
            }

            // Error Snackbar
            uiState.error?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                ) {
                    Text(message, color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

/**
 * Fortschrittsanzeige
 */
@Composable
private fun SetupProgressIndicator(
    currentStep: SetupStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Willkommen" to SetupStep.WELCOME,
        "Vorlage" to SetupStep.TEMPLATE_UPLOAD,
        "Daten" to SetupStep.USER_DATA,
        "Fertig" to SetupStep.COMPLETION
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (label, step) ->
            val isActive = step == currentStep
            val isPassed = step.ordinal < currentStep.ordinal

            // Step Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isPassed -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPassed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = (index + 1).toString(),
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Label
            if (index < steps.size - 1) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Schritt 1: Willkommen
 */
@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Icon
        Icon(
            Icons.Default.Celebration,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            "Willkommen!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Arbeitszeit Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Diese App hilft dir dabei, deine Arbeitszeiten zu erfassen und zu verwalten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(
                    "Lass uns jetzt deine App einrichten. Der Prozess dauert nur wenige Minuten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Los geht's!")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

/**
 * Schritt 2: Template Upload
 */
@Composable
private fun TemplateUploadStep(
    viewModel: SetupViewModel,
    isLoading: Boolean,
    error: String?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadTemplate(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title
        Text(
            "Excel-Vorlage hochladen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        "Warum eine Vorlage hochladen?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Die Excel-Vorlage enthält deine Stammdaten (Name, Einrichtung, Arbeitsumfang, etc.) und wird für den Export benötigt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Upload Button
        Button(
            onClick = {
                filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Lade...")
            } else {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Excel-Vorlage auswählen")
            }
        }

        HorizontalDivider()

        // Skip Option
        Text(
            "Keine Vorlage vorhanden?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onSkip,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Überspringen und manuell eingeben")
        }

        Spacer(Modifier.weight(1f))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zurück")
            }
        }
    }
}

/**
 * Schritt 3: User Data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDataStep(
    viewModel: SetupViewModel,
    uiState: com.arbeitszeit.tracker.viewmodel.SetupUiState,
    showDatePicker: Boolean,
    onShowDatePickerChange: (Boolean) -> Unit,
    datePickerState: DatePickerState,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            if (uiState.hasTemplate) "Daten bestätigen" else "Daten eingeben",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (uiState.hasTemplate) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "Daten wurden aus der Excel-Vorlage geladen. Bitte überprüfen und ggf. anpassen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Name
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Einrichtung
        OutlinedTextField(
            value = uiState.einrichtung,
            onValueChange = { viewModel.updateEinrichtung(it) },
            label = { Text("Einrichtung") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            isError = uiState.einrichtungError != null,
            supportingText = uiState.einrichtungError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Arbeitsumfang
        OutlinedTextField(
            value = uiState.arbeitsumfangProzent.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { prozent -> viewModel.updateArbeitsumfang(prozent) }
            },
            label = { Text("Arbeitsumfang (%)") },
            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Wochenstunden
        var wochenStundenText by remember {
            mutableStateOf(String.format("%02d:%02d", uiState.wochenStundenMinuten / 60, uiState.wochenStundenMinuten % 60))
        }

        OutlinedTextField(
            value = wochenStundenText,
            onValueChange = { newValue ->
                wochenStundenText = newValue
                // Parse HH:MM format
                val parts = newValue.split(":")
                if (parts.size == 2) {
                    val hours = parts[0].toIntOrNull() ?: 0
                    val minutes = parts[1].toIntOrNull() ?: 0
                    viewModel.updateWochenStunden(hours * 60 + minutes)
                }
            },
            label = { Text("Wochenstunden (HH:MM)") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            supportingText = { Text("z.B. 40:00 für 40 Stunden/Woche") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Arbeitstage/Woche
        OutlinedTextField(
            value = uiState.arbeitsTageProWoche.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { tage -> viewModel.updateArbeitsTage(tage) }
            },
            label = { Text("Arbeitstage/Woche") },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Urlaubsanspruch
        OutlinedTextField(
            value = uiState.urlaubsanspruchTage.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { tage -> viewModel.updateUrlaubsanspruch(tage) }
            },
            label = { Text("Urlaubsanspruch (Tage)") },
            leadingIcon = { Icon(Icons.Default.BeachAccess, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Erster Montag
        val currentYear = LocalDate.now().year
        val ersterMontagFormatted = try {
            val date = LocalDate.parse(uiState.ersterMontagImJahr)
            date.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy"))
        } catch (e: Exception) {
            "Format: yyyy-MM-dd"
        }

        Text(
            "Jahr $currentYear wird angelegt",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.ersterMontagImJahr,
            onValueChange = { viewModel.updateErsterMontag(it) },
            label = { Text("Erster Montag im Jahr $currentYear") },
            leadingIcon = {
                IconButton(onClick = { onShowDatePickerChange(true) }) {
                    Icon(Icons.Default.Event, contentDescription = "Datum auswählen")
                }
            },
            isError = uiState.ersterMontagError != null,
            supportingText = {
                Text(uiState.ersterMontagError ?: ersterMontagFormatted)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Ferienbetreuung
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ChildCare, contentDescription = null)
                Column {
                    Text("Ferienbetreuung", fontWeight = FontWeight.Medium)
                    Text(
                        "Zeige Ferien in Übersicht an",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = uiState.ferienbetreuung,
                onCheckedChange = { viewModel.updateFerienbetreuung(it) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zurück")
            }

            Button(
                onClick = {
                    android.util.Log.d("SetupScreen", "Abschließen clicked")
                    onComplete()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Abschließen")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }

        // DatePicker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { onShowDatePickerChange(false) },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.updateErsterMontag(date.toString())
                        }
                        onShowDatePickerChange(false)
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onShowDatePickerChange(false) }) {
                        Text("Abbrechen")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

/**
 * Schritt 4: Abschluss
 */
@Composable
private fun CompletionStep(
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Success Icon
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            "Einrichtung abgeschlossen!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Alles bereit!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "Deine Daten wurden gespeichert und das aktuelle Jahr wurde angelegt. Du kannst jetzt mit der Zeiterfassung beginnen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Finish Button
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zur App")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
