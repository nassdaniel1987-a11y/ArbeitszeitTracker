package com.arbeitszeit.tracker.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbeitszeit.tracker.data.entity.YearSettings
import com.arbeitszeit.tracker.ui.components.DeleteYearConfirmDialog
import com.arbeitszeit.tracker.ui.components.EditYearDialog
import com.arbeitszeit.tracker.viewmodel.YearViewModel

/**
 * Screen zur Verwaltung aller Jahre
 *
 * Features:
 * - Liste aller Jahre anzeigen
 * - Jahr bearbeiten (ersterMontag, Urlaubsanspruch, Vorjahresübertrag)
 * - Jahr löschen (Zeiteinträge bleiben erhalten)
 * - Excel-Vorlage hochladen/löschen
 * - Aktives Jahr markieren
 * - Zu Jahr wechseln
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearManagementScreen(
    viewModel: YearViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jahre verwalten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showNewYearDialog() }) {
                        Icon(Icons.Default.Add, "Neues Jahr anlegen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            "Jahres-Verwaltung",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Hier kannst du alle Jahre verwalten, bearbeiten und Excel-Vorlagen hochladen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Jahre Liste
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.allYears) { year ->
                        YearCard(
                            year = year,
                            isActive = year.isActive,
                            onEdit = { viewModel.showEditYearDialog(year) },
                            onDelete = { viewModel.showDeleteConfirmDialog(year) },
                            onSwitch = { viewModel.switchToYear(year.year) }
                        )
                    }

                    // Hinweis wenn leer
                    if (uiState.allYears.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Keine Jahre vorhanden. Bitte lege ein neues Jahr an.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialoge
        if (uiState.showEditYearDialog && uiState.editYear != null) {
            EditYearDialog(
                year = uiState.editYear!!,
                availableTemplates = uiState.availableTemplates,
                onDismiss = { viewModel.dismissEditYearDialog() },
                onSave = { ersterMontag, urlaubsanspruch, vorjahresUebertrag ->
                    viewModel.updateYear(
                        year = uiState.editYear!!.year,
                        ersterMontagImJahr = ersterMontag,
                        urlaubsanspruch = urlaubsanspruch,
                        vorjahresUebertrag = vorjahresUebertrag
                    )
                },
                onTemplateUpload = { uri ->
                    viewModel.uploadTemplate(uiState.editYear!!.year, uri)
                },
                onTemplateDelete = {
                    viewModel.deleteTemplate(uiState.editYear!!.year)
                }
            )
        }

        if (uiState.showDeleteConfirmDialog && uiState.deleteYear != null) {
            DeleteYearConfirmDialog(
                year = uiState.deleteYear!!,
                onDismiss = { viewModel.dismissDeleteConfirmDialog() },
                onConfirm = { viewModel.confirmDeleteYear() }
            )
        }

        // Success/Error Snackbars
        uiState.success?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(message)
            }
        }

        uiState.error?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text(message)
            }
        }
    }
}

/**
 * Card für ein einzelnes Jahr
 */
@Composable
private fun YearCard(
    year: YearSettings,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Jahr + Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Jahr ${year.year}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isActive) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Aktiv") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            HorizontalDivider()

            // Details
            YearDetailRow(
                icon = Icons.Default.CalendarMonth,
                label = "Erster Montag",
                value = formatDateGerman(year.ersterMontagImJahr)
            )

            YearDetailRow(
                icon = Icons.Default.BeachAccess,
                label = "Urlaubsanspruch",
                value = "${year.urlaubsanspruchTage} Tage"
            )

            YearDetailRow(
                icon = Icons.Default.Timeline,
                label = "Vorjahresübertrag",
                value = year.getVorjahresUebertragFormatted() + " Std"
            )

            YearDetailRow(
                icon = Icons.Default.Description,
                label = "Excel-Vorlage",
                value = if (year.hasExcelTemplate)
                    "✓ template_${year.year}.xlsx"
                else
                    "Standard"
            )

            HorizontalDivider()

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Bearbeiten")
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !isActive,  // Aktives Jahr kann nicht gelöscht werden
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Löschen")
                }
            }

            // Switch Button (wenn nicht aktiv)
            if (!isActive) {
                Button(
                    onClick = onSwitch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Zu diesem Jahr wechseln")
                }
            }
        }
    }
}

/**
 * Detail-Zeile für Jahr-Informationen
 */
@Composable
private fun YearDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Formatiert yyyy-MM-dd → dd.MM.yyyy
 */
private fun formatDateGerman(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) {
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } else {
        isoDate
    }
}
