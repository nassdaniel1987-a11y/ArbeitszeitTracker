package com.arbeitszeit.tracker.wear.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.arbeitszeit.tracker.wear.R

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()
    val focusRequester = FocusRequester()

    // Request focus for rotary input
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                state = listState,
                contentPadding = PaddingValues(
                    top = 32.dp,
                    start = 10.dp,
                    end = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                autoCentering = AutoCenteringParams(itemIndex = 1)
            ) {
            // Header with status
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isWorking) {
                            stringResource(R.string.working)
                        } else {
                            stringResource(R.string.not_working)
                        },
                        style = MaterialTheme.typography.caption1,
                        color = if (uiState.isWorking) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Time info
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Today
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.today_hours),
                            style = MaterialTheme.typography.body2
                        )
                        Text(
                            text = formatMinutes(uiState.todayMinutes),
                            style = MaterialTheme.typography.title2
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Week
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.week_hours),
                            style = MaterialTheme.typography.body2
                        )
                        Text(
                            text = formatMinutes(uiState.weekMinutes),
                            style = MaterialTheme.typography.title2
                        )
                    }
                }
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
