package com.attendance.app.ui.screens.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.domain.model.OtRrType
import com.attendance.app.domain.model.WeekDay
import com.attendance.app.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(
    employeeId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EmployeeFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(employeeId) {
        viewModel.loadEmployee(employeeId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditing) "Edit Employee" else "Add Employee")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading && state.isEditing && state.existingEntity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Code
                OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChanged,
                    label = { Text("Code *") },
                    isError = state.codeError != null,
                    supportingText = state.codeError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Employee ID
                OutlinedTextField(
                    value = state.empId,
                    onValueChange = viewModel::onEmpIdChanged,
                    label = { Text("Employee ID *") },
                    isError = state.empIdError != null,
                    supportingText = state.empIdError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Father's Name
                OutlinedTextField(
                    value = state.fatherName,
                    onValueChange = viewModel::onFatherNameChanged,
                    label = { Text("Father's Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Department dropdown
                DropdownField(
                    label = "Department",
                    selectedText = state.departments.find { it.id == state.departmentId }?.name ?: "None",
                    options = listOf("None" to null) + state.departments.map { it.name to it.id },
                    onSelected = { viewModel.onDepartmentChanged(it as Long?) }
                )

                // Category dropdown
                DropdownField(
                    label = "Category",
                    selectedText = state.categories.find { it.id == state.categoryId }?.name ?: "None",
                    options = listOf("None" to null) + state.categories.map { it.name to it.id },
                    onSelected = { viewModel.onCategoryChanged(it as Long?) }
                )

                // Date of Birth
                DateField(
                    label = "Date of Birth",
                    value = state.dob,
                    onDateSelected = viewModel::onDobChanged
                )

                // Date of Joining
                DateField(
                    label = "Date of Joining",
                    value = state.doj,
                    onDateSelected = viewModel::onDojChanged
                )

                // Weekly Off Day dropdown
                DropdownField(
                    label = "Weekly Off Day",
                    selectedText = WeekDay.fromIndex(state.weeklyOffDay).label,
                    options = WeekDay.entries.map { it.label to it.dayIndex },
                    onSelected = { viewModel.onWeeklyOffDayChanged(it as Int) }
                )

                // FD dropdown
                DropdownField(
                    label = "FD",
                    selectedText = if (state.fd.isBlank()) "(Blank)" else state.fd,
                    options = listOf("(Blank)" to "", "D" to "D"),
                    onSelected = { viewModel.onFdChanged(it as String) }
                )

                // OT/RR Type dropdown
                DropdownField(
                    label = "OT/RR Type",
                    selectedText = OtRrType.fromCode(state.otRrType).label,
                    options = OtRrType.entries.map { it.label to it.code },
                    onSelected = { viewModel.onOtRrTypeChanged(it as String) }
                )

                // OT/RR Value
                OutlinedTextField(
                    value = state.otRrValue,
                    onValueChange = viewModel::onOtRrValueChanged,
                    label = { Text("OT/RR Value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Gross Salary
                OutlinedTextField(
                    value = state.grossSalary,
                    onValueChange = viewModel::onGrossSalaryChanged,
                    label = { Text("Gross Salary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading &&
                            state.code.isNotBlank() &&
                            state.empId.isNotBlank() &&
                            state.name.isNotBlank()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isEditing) "Update Employee" else "Save Employee")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selectedText: String,
    options: List<Pair<String, Any?>>,
    onSelected: (Any?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (displayText, value) ->
                DropdownMenuItem(
                    text = { Text(displayText) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val displayValue = if (value.isNotBlank()) {
        try {
            DateUtils.formatForDisplay(value)
        } catch (_: Exception) {
            value
        }
    } else {
        ""
    }

    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    if (showDatePicker) {
        val initialMillis = if (value.isNotBlank()) {
            try {
                LocalDate.parse(value)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onDateSelected(localDate.toString()) // yyyy-MM-dd format
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
