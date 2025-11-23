package com.arbeitszeit.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.arbeitszeit.tracker.wear.R

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        timeText = {
            TimeText()
        }
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 32.dp,
                    start = 10.dp,
                    end = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 0)
            ) {
            // Title
            item {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center
                )
            }

            // Status
            item {
                Text(
                    text = if (uiState.isWorking) {
                        stringResource(R.string.working)
                    } else {
                        stringResource(R.string.not_working)
                    },
                    style = MaterialTheme.typography.body2,
                    color = if (uiState.isWorking) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                    textAlign = TextAlign.Center
                )
            }

            // Today's hours
            item {
                CompactChip(
                    onClick = { },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.today_hours))
                            Text(
                                text = formatMinutes(uiState.todayMinutes),
                                style = MaterialTheme.typography.title3
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Week's hours
            item {
                CompactChip(
                    onClick = { },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.week_hours))
                            Text(
                                text = formatMinutes(uiState.weekMinutes),
                                style = MaterialTheme.typography.title3
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Check-in button
            if (!uiState.isWorking) {
                item {
                    Chip(
                        onClick = { viewModel.onCheckIn() },
                        label = {
                            Text(
                                text = stringResource(R.string.quick_start),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.button
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Check-out button
            if (uiState.isWorking) {
                item {
                    Chip(
                        onClick = { viewModel.onCheckOut() },
                        label = {
                            Text(
                                text = stringResource(R.string.quick_end),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.button
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Cancel button (only when working)
            if (uiState.isWorking) {
                item {
                    Chip(
                        onClick = { viewModel.onCancelWork() },
                        label = {
                            Text(
                                text = stringResource(R.string.cancel),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.button
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d:%02d", hours, mins)
}
