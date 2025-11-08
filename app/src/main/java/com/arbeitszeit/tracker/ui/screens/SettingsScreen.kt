package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.userSettings.collectAsState()
    
    var name by remember { mutableStateOf(settings?.name ?: "") }
    var einrichtung by remember { mutableStateOf(settings?.einrichtung ?: "") }
    var prozent by remember { mutableStateOf(settings?.arbeitsumfangProzent?.toString() ?: "100") }
    var stunden by remember { mutableStateOf("40") }
    var minuten by remember { mutableStateOf("00") }
    var arbeitsTage by remember { mutableStateOf(settings?.arbeitsTageProWoche?.toString() ?: "5") }
    var ferienbetreuung by remember { mutableStateOf(settings?.ferienbetreuung ?: false) }
    
    LaunchedEffect(settings) {
        settings?.let {
            name = it.name
            einrichtung = it.einrichtung
            prozent = it.arbeitsumfangProzent.toString()
            stunden = (it.wochenStundenMinuten / 60).toString()
            minuten = (it.wochenStundenMinuten % 60).toString().padStart(2, '0')
            arbeitsTage = it.arbeitsTageProWoche.toString()
            ferienbetreuung = it.ferienbetreuung
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = einrichtung,
            onValueChange = { einrichtung = it },
            label = { Text("Einrichtung") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = prozent,
            onValueChange = { prozent = it },
            label = { Text("Arbeitsumfang (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stunden,
                onValueChange = { stunden = it },
                label = { Text("Stunden") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minuten,
                onValueChange = { minuten = it },
                label = { Text("Minuten") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = arbeitsTage,
            onValueChange = { arbeitsTage = it },
            label = { Text("Arbeitstage/Woche") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Ferienbetreuung")
            Spacer(Modifier.weight(1f))
            Switch(checked = ferienbetreuung, onCheckedChange = { ferienbetreuung = it })
        }
        
        Button(
            onClick = {
                val wochenMinuten = (stunden.toIntOrNull() ?: 0) * 60 + (minuten.toIntOrNull() ?: 0)
                viewModel.updateSettings(
                    name = name,
                    einrichtung = einrichtung,
                    arbeitsumfangProzent = prozent.toIntOrNull() ?: 100,
                    wochenStundenMinuten = wochenMinuten,
                    arbeitsTageProWoche = arbeitsTage.toIntOrNull() ?: 5,
                    ferienbetreuung = ferienbetreuung,
                    ueberstundenVorjahrMinuten = 0
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speichern")
        }
    }
}
