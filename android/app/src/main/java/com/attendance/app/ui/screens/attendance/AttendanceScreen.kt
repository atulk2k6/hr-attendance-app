package com.attendance.app.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.domain.model.AttendanceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Attendance saved successfully")
        }
    }

    // Date picker dialog
    if (state.showDatePicker) {
        val currentDate = LocalDate.parse(state.selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / 86400000L)
                            viewModel.onDateSelected(date.year, date.monthValue - 1, date.dayOfMonth)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDatePicker() }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (state.showTimePicker && state.timePickerTarget != null) {
        val target = state.timePickerTarget!!
        val entry = state.entries.getOrNull(target.entryIndex)
        val existingTime = if (target.isInTime) entry?.inTime else entry?.outTime
        val initialHour: Int
        val initialMinute: Int
        if (!existingTime.isNullOrBlank() && existingTime.contains(":")) {
            val parts = existingTime.split(":")
            initialHour = parts[0].toIntOrNull() ?: 9
            initialMinute = parts[1].toIntOrNull() ?: 0
        } else {
            initialHour = if (target.isInTime) 9 else 18
            initialMinute = 0
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.dismissTimePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onTimeSelected(timePickerState.hour, timePickerState.minute)
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTimePicker() }) { Text("Cancel") }
            },
            title = {
                Text(if (target.isInTime) "Select In-Time" else "Select Out-Time")
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Attendance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showDatePicker() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val fabEnabled = !state.hasTimeErrors && !state.isSaving
            ExtendedFloatingActionButton(
                onClick = { if (fabEnabled) viewModel.saveAll() },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Save All") },
                containerColor = if (fabEnabled) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (fabEnabled) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date navigation bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.onPreviousDay() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    Text(
                        text = state.displayDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.onNextDay() }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }
            }

            // Quick action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.markAllPresent() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark All Present")
                }
                OutlinedButton(
                    onClick = { viewModel.markAllAbsent() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark All Absent")
                }
            }

            // Time-error banner
            if (state.hasTimeErrors) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fix time format errors before saving",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Employee attendance list
            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active employees found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.entries, key = { _, entry -> entry.employeeId }) { index, entry ->
                        AttendanceEntryCard(
                            entry = entry,
                            onInTimeTap = { viewModel.showTimePicker(index, isInTime = true) },
                            onOutTimeTap = { viewModel.showTimePicker(index, isInTime = false) },
                            onStatusOverride = { newStatus -> viewModel.onStatusOverride(index, newStatus) },
                            onNowInTime = { viewModel.applyCurrentTime(index, isInTime = true) },
                            onNowOutTime = { viewModel.applyCurrentTime(index, isInTime = false) },
                            onRemarksChanged = { viewModel.onRemarksChanged(index, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceEntryCard(
    entry: AttendanceEntry,
    onInTimeTap: () -> Unit,
    onOutTimeTap: () -> Unit,
    onStatusOverride: (String) -> Unit,
    onNowInTime: () -> Unit,
    onNowOutTime: () -> Unit,
    onRemarksChanged: (String) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showRemarks by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Name and code row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = entry.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip (tappable for override)
                Box {
                    val statusColor = when (entry.status) {
                        "P" -> MaterialTheme.colorScheme.primary
                        "A" -> MaterialTheme.colorScheme.error
                        "W" -> MaterialTheme.colorScheme.tertiary
                        "CO" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    AssistChip(
                        onClick = { showStatusMenu = true },
                        label = {
                            Text(
                                text = AttendanceStatus.fromCode(entry.status).label,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusColor.copy(alpha = 0.12f),
                            labelColor = statusColor
                        )
                    )

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        AttendanceStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text("${status.code} - ${status.label}") },
                                onClick = {
                                    onStatusOverride(status.code)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Time fields and OT row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // In-Time column
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeField(
                        label = "In",
                        value = entry.inTime,
                        onClick = onInTimeTap,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // "Now" button
                    TextButton(
                        onClick = onNowInTime,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Now", style = MaterialTheme.typography.labelSmall)
                    }
                    // Time error
                    entry.timeError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Out-Time column
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeField(
                        label = "Out",
                        value = entry.outTime,
                        onClick = onOutTimeTap,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // "Now" button
                    TextButton(
                        onClick = onNowOutTime,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Now", style = MaterialTheme.typography.labelSmall)
                    }
                    // Time error (shared — both in/out share the same timeError field)
                    entry.timeError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Total hours
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.7f)
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (entry.totalHours > 0) "${entry.totalHours}h" else "-",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                // OT hours
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(
                        text = "OT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (entry.otHours > 0) "${entry.otHours}h" else "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (entry.otHours > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (entry.otHours > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Remarks section
            if (entry.remarks.isNotBlank() || showRemarks) {
                OutlinedTextField(
                    value = entry.remarks,
                    onValueChange = onRemarksChanged,
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Sick leave, On duty...") }
                )
            } else {
                TextButton(
                    onClick = { showRemarks = true },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text("+ Add remark", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "--:--" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
