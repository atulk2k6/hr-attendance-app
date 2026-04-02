package com.attendance.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.util.DateUtils
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private val DAY_ABBREVIATIONS = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyViewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmployeeDetail: ((employeeId: Long, year: Int, month: Int) -> Unit)? = null,
    viewModel: MonthlyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Day detail bottom sheet
    if (state.dayDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDayDetail() },
            sheetState = sheetState
        ) {
            DayDetailSheet(detail = state.dayDetail!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly View") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month/Year navigation
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
                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = "${DateUtils.getMonthName(state.selectedMonth)} ${state.selectedYear}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.onNextMonth() }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search employee name or code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                singleLine = true
            )

            // Department + Category filter chips
            if (state.departments.isNotEmpty() || state.categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Dept chips
                    if (state.departments.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = state.selectedDeptId == null,
                                onClick = { viewModel.onDeptFilterChanged(null) },
                                label = { Text("All Depts") }
                            )
                        }
                        items(state.departments.size) { idx ->
                            val dept = state.departments[idx]
                            FilterChip(
                                selected = state.selectedDeptId == dept.id,
                                onClick = { viewModel.onDeptFilterChanged(dept.id) },
                                label = { Text(dept.name) }
                            )
                        }
                    }
                    // Cat chips
                    if (state.categories.isNotEmpty()) {
                        item { VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)) }
                        item {
                            FilterChip(
                                selected = state.selectedCatId == null,
                                onClick = { viewModel.onCatFilterChanged(null) },
                                label = { Text("All Cats") }
                            )
                        }
                        items(state.categories.size) { idx ->
                            val cat = state.categories[idx]
                            FilterChip(
                                selected = state.selectedCatId == cat.id,
                                onClick = { viewModel.onCatFilterChanged(cat.id) },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val employees = state.filteredEmployees

            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.allEmployees.isEmpty()) "No active employees found"
                               else "No employees match the filter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val nameColumnWidth = 110.dp
                val dayCellWidth = 38.dp
                val summaryColumnWidth = 56.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Header row
                    item(key = "header") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Name column header
                            Box(
                                modifier = Modifier
                                    .width(nameColumnWidth)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Employee",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Scrollable day headers
                            Row(modifier = Modifier.horizontalScroll(horizontalScrollState).weight(1f)) {
                                for (day in 1..state.daysInMonth) {
                                    val dateStr = String.format(
                                        "%04d-%02d-%02d",
                                        state.selectedYear, state.selectedMonth, day
                                    )
                                    val dayOfWeekIndex = DateUtils.getDayOfWeekIndex(dateStr)
                                    val isSunday = dayOfWeekIndex == 0

                                    Box(
                                        modifier = Modifier
                                            .width(dayCellWidth)
                                            .background(
                                                if (isSunday) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = DAY_ABBREVIATIONS[dayOfWeekIndex],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            // Summary column header
                            Box(
                                modifier = Modifier
                                    .width(summaryColumnWidth)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "P/A/OT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Employee rows
                    itemsIndexed(employees, key = { _, emp -> emp.id }) { _, employee ->
                        val empRecords = state.attendanceMap[employee.id] ?: emptyMap()
                        val summary = state.summaryMap[employee.id]

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Fixed employee name — tappable for drill-down
                            Box(
                                modifier = Modifier
                                    .width(nameColumnWidth)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .clickable(enabled = onNavigateToEmployeeDetail != null) {
                                        onNavigateToEmployeeDetail?.invoke(
                                            employee.id,
                                            state.selectedYear,
                                            state.selectedMonth
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text(
                                        text = employee.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (onNavigateToEmployeeDetail != null)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = employee.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            // Scrollable day cells
                            Row(modifier = Modifier.horizontalScroll(horizontalScrollState).weight(1f)) {
                                for (day in 1..state.daysInMonth) {
                                    val dateStr = String.format(
                                        "%04d-%02d-%02d",
                                        state.selectedYear, state.selectedMonth, day
                                    )
                                    val record = empRecords[dateStr]
                                    val statusCode = record?.status ?: ""
                                    val (bgColor, textColor) = statusCellColors(statusCode)

                                    Box(
                                        modifier = Modifier
                                            .width(dayCellWidth)
                                            .height(44.dp)
                                            .background(bgColor)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .clickable { viewModel.showDayDetail(employee, dateStr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = statusCode.ifBlank { "-" },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                fontSize = 10.sp
                                            )
                                            if (record != null && record.otHours > 0) {
                                                Text(
                                                    text = "+${record.otHours.let { if (it == it.toLong().toDouble()) it.toLong().toString() else String.format("%.1f", it) }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 7.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Summary column
                            Box(
                                modifier = Modifier
                                    .width(summaryColumnWidth)
                                    .height(44.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (summary != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "P:${summary.presentDays} A:${summary.absentDays}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (summary.totalOtHours > 0) {
                                            Text(
                                                text = "OT:${String.format("%.1f", summary.totalOtHours)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailSheet(detail: DayDetailState) {
    val dateDisplay = try {
        val ld = LocalDate.parse(detail.date)
        ld.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
    } catch (e: Exception) { detail.date }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = detail.employee.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${detail.employee.code} · $dateDisplay",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        val att = detail.attendance
        if (att != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(label = "Status", value = att.status)
                if (att.inTime.isNotBlank()) InfoChip(label = "In", value = att.inTime)
                if (att.outTime.isNotBlank()) InfoChip(label = "Out", value = att.outTime)
                if (att.totalHours > 0) InfoChip(label = "Hours", value = "${att.totalHours}h")
                if (att.otHours > 0) InfoChip(label = "OT", value = "${att.otHours}h")
            }
            if (att.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Remarks: ${att.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No attendance record for this day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (detail.punches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Punch Log",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            detail.punches.forEach { punch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = punch.punchType,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (punch.punchType == "IN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = punch.time,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "via ${punch.recordedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun statusCellColors(status: String): Pair<Color, Color> {
    return when (status) {
        "P" -> Pair(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "A" -> Pair(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onErrorContainer
        )
        "W" -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        "CO" -> Pair(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
