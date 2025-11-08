package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbeitszeit.tracker.viewmodel.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: ExportViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedKW by viewModel.selectedKW.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Excel Export", style = MaterialTheme.typography.titleLarge)
        
        Text("Kalenderwoche: $selectedKW")
        
        Slider(
            value = selectedKW.toFloat(),
            onValueChange = { viewModel.selectKW(it.toInt()) },
            valueRange = 1f..52f,
            steps = 51
        )
        
        Button(
            onClick = { viewModel.exportExcel() },
            enabled = !uiState.isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Excel exportieren")
            }
        }
        
        if (uiState.exportSuccess) {
            Text("Export erfolgreich: ${viewModel.getExpectedFileName()}")
        }
        
        uiState.error?.let {
            Text("Fehler: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}
