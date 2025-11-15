package com.arbeitszeit.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeCard(
    settings: UserSettings?,
    viewModel: SettingsViewModel
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Erscheinungsbild",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Wähle zwischen hellem und dunklem Modus",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            var expanded by remember { mutableStateOf(false) }
            val darkModeOptions = mapOf(
                "system" to "Systemeinstellung",
                "light" to "Hell",
                "dark" to "Dunkel"
            )
            val currentDarkMode = settings?.darkMode ?: "system"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = darkModeOptions[currentDarkMode] ?: "Systemeinstellung",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    darkModeOptions.forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        when (mode) {
                                            "system" -> Icons.Default.SettingsBrightness
                                            "light" -> Icons.Default.LightMode
                                            "dark" -> Icons.Default.DarkMode
                                            else -> Icons.Default.Settings
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(label)
                                }
                            },
                            onClick = {
                                viewModel.updateDarkMode(mode)
                                expanded = false
                            },
                            leadingIcon = if (currentDarkMode == mode) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
