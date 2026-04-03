package com.attendance.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.local.dao.PunchLogWithEmployee
import com.attendance.app.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPunchScreen(
    onNavigateToEmployees: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToMonthlyView: () -> Unit,
    onNavigateToDepartments: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToKioskMode: () -> Unit = {},
    onNavigateToUnits: () -> Unit = {},
    viewModel: QuickPunchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Flash color for punch feedback
    var punchFlashColor by remember { mutableStateOf(Color.Transparent) }

    // Auto-focus search on launch and after punch
    LaunchedEffect(state.selectedEmployee) {
        if (state.selectedEmployee == null) {
            try {
                delay(300)
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Show snackbar on punch success
    LaunchedEffect(state.punchSuccess) {
        state.punchSuccess?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearPunchSuccess()
        }
    }

    // Share daily report when ready
    LaunchedEffect(state.dailyReportText) {
        state.dailyReportText?.let { reportText ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Attendance Report ${state.currentDate}")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Report"))
            viewModel.clearDailyReport()
        }
    }

    // Duplicate punch warning dialog
    if (state.duplicatePunchWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicatePunch() },
            title = { Text("Duplicate Punch") },
            text = { Text(state.duplicatePunchWarning!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDuplicatePunch() }) { Text("Yes, Record") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicatePunch() }) { Text("Cancel") }
            }
        )
    }

    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                actions = {
                    IconButton(onClick = { viewModel.generateDailyReport() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share today's report")
                    }
                    IconButton(onClick = onNavigateToKioskMode) {
                        Icon(Icons.Default.Tablet, contentDescription = "Kiosk Mode")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Monthly View") },
                                onClick = { showOverflowMenu = false; onNavigateToMonthlyView() }
                            )
                            DropdownMenuItem(
                                text = { Text("Departments") },
                                onClick = { showOverflowMenu = false; onNavigateToDepartments() }
                            )
                            DropdownMenuItem(
                                text = { Text("Categories") },
                                onClick = { showOverflowMenu = false; onNavigateToCategories() }
                            )
                            DropdownMenuItem(
                                text = { Text("Units / Locations") },
                                onClick = { showOverflowMenu = false; onNavigateToUnits() }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. UNIT SELECTOR
            if (state.units.size > 1) {
                UnitSelector(
                    units = state.units,
                    selectedUnitId = state.selectedUnitId,
                    onUnitSelected = viewModel::onUnitSelected
                )
            }

            // 2. QUICK PUNCH CARD
            QuickPunchCard(
                state = state,
                punchFlashColor = punchFlashColor,
                focusRequester = focusRequester,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onEmployeeSelected = viewModel::onEmployeeSelected,
                onClearEmployee = viewModel::clearSelectedEmployee,
                onAdjustTime = viewModel::adjustTime,
                onResetTime = viewModel::resetTime,
                onPunch = { type ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    punchFlashColor = if (type == "IN") Color(0x3300C853) else Color(0x33FF1744)
                    viewModel.punch(type)
                    scope.launch {
                        delay(500)
                        punchFlashColor = Color.Transparent
                    }
                }
            )

            // 3. TODAY'S SUMMARY
            TodaySummaryRow(stats = state.todayStats)

            // 4. RECENT PUNCHES
            if (state.recentPunches.isNotEmpty()) {
                RecentPunchesSection(
                    punches = state.recentPunches,
                    onDeletePunch = { punch ->
                        viewModel.deletePunch(punch)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Punch deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            // Note: Undo would require caching the deleted entity.
                            // For now, deletion is immediate.
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSelector(
    units: List<com.attendance.app.data.local.entity.UnitLocationEntity>,
    selectedUnitId: Long?,
    onUnitSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = units.find { it.id == selectedUnitId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedUnit?.let { "${it.name} (${it.unitNumber})" } ?: "Select Unit",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text("${unit.name} (${unit.unitNumber})")
                    },
                    onClick = {
                        onUnitSelected(unit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickPunchCard(
    state: QuickPunchUiState,
    punchFlashColor: Color,
    focusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    onEmployeeSelected: (com.attendance.app.data.local.dao.EmployeeWithDetails) -> Unit,
    onClearEmployee: () -> Unit,
    onAdjustTime: (Int) -> Unit,
    onResetTime: () -> Unit,
    onPunch: (String) -> Unit
) {
    val flashColor by animateColorAsState(
        targetValue = punchFlashColor,
        animationSpec = tween(300),
        label = "punchFlash"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Flash overlay
            if (flashColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(flashColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                if (state.selectedEmployee == null) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Type code or name...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Search results dropdown
                    AnimatedVisibility(
                        visible = state.searchResults.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column {
                                state.searchResults.forEachIndexed { index, employee ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEmployeeSelected(employee) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "[${employee.code}] ${employee.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = employee.empId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (index < state.searchResults.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected employee display
                if (state.selectedEmployee != null) {
                    val emp = state.selectedEmployee
                    SelectedEmployeeCard(
                        name = emp.name,
                        code = emp.code,
                        empId = emp.empId,
                        departmentName = emp.departmentName,
                        lastPunchType = state.lastPunchType,
                        lastPunchTime = state.recentPunches
                            .filter { it.employeeId == emp.id }
                            .maxByOrNull { it.time }
                            ?.let { "${it.punchType} @ ${it.time}" },
                        onClear = onClearEmployee
                    )

                    // Date & Time row
                    DateTimeSection(
                        currentDate = state.currentDate,
                        adjustedTime = state.adjustedTime,
                        timeAdjustmentMinutes = state.timeAdjustmentMinutes,
                        onAdjustTime = onAdjustTime,
                        onResetTime = onResetTime
                    )

                    // Punch buttons
                    PunchButtons(
                        lastPunchType = state.lastPunchType,
                        isPunching = state.isPunching,
                        onPunchIn = { onPunch("IN") },
                        onPunchOut = { onPunch("OUT") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedEmployeeCard(
    name: String,
    code: String,
    empId: String,
    departmentName: String?,
    lastPunchType: String?,
    lastPunchTime: String?,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = buildString {
                    append(code)
                    append(" | ")
                    append(empId)
                    if (!departmentName.isNullOrBlank()) {
                        append(" | ")
                        append(departmentName)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Last punch chip
            val chipText = if (lastPunchTime != null) "Last: $lastPunchTime" else "No punch today"
            val chipColor = when (lastPunchType) {
                "IN" -> MaterialTheme.colorScheme.primaryContainer
                "OUT" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(chipColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = "Clear selection")
        }
    }
}

@Composable
private fun DateTimeSection(
    currentDate: String,
    adjustedTime: String,
    timeAdjustmentMinutes: Int,
    onAdjustTime: (Int) -> Unit,
    onResetTime: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = DateUtils.formatForDisplay(currentDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Time display
            Text(
                text = adjustedTime,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (timeAdjustmentMinutes != 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        // Time adjustment buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            OutlinedButton(
                onClick = { onAdjustTime(-15) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("-15m", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { onAdjustTime(-5) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("-5m", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = onResetTime,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Now", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onAdjustTime(5) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("+5m", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { onAdjustTime(15) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("+15m", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (timeAdjustmentMinutes != 0) {
            Text(
                text = "${if (timeAdjustmentMinutes > 0) "+" else ""}${timeAdjustmentMinutes}min from now",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PunchButtons(
    lastPunchType: String?,
    isPunching: Boolean,
    onPunchIn: () -> Unit,
    onPunchOut: () -> Unit
) {
    val inEnabled = !isPunching && lastPunchType != "IN"
    val outEnabled = !isPunching && lastPunchType == "IN"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // PUNCH IN button
        androidx.compose.material3.Button(
            onClick = onPunchIn,
            enabled = inEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00C853),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF00C853).copy(alpha = 0.3f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "PUNCH IN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // PUNCH OUT button
        androidx.compose.material3.Button(
            onClick = onPunchOut,
            enabled = outEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF1744),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFFF1744).copy(alpha = 0.3f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "PUNCH OUT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodaySummaryRow(stats: TodayStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "Punched In",
            value = stats.totalPunchedIn.toString(),
            icon = Icons.Default.Login,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "Present",
            value = stats.presentCount.toString(),
            icon = Icons.Default.People,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "Total OT",
            value = String.format("%.1fh", stats.totalOt),
            icon = Icons.Default.Timer,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentPunchesSection(
    punches: List<PunchLogWithEmployee>,
    onDeletePunch: (PunchLogWithEmployee) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                punches.forEachIndexed { index, punch ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                onDeletePunch(punch)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Time
                            Text(
                                text = punch.time,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(48.dp)
                            )

                            // Name (code)
                            Text(
                                text = "${punch.employeeName} (${punch.employeeCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // IN/OUT chip
                            val isIn = punch.punchType == "IN"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isIn) Color(0xFF00C853).copy(alpha = 0.15f)
                                        else Color(0xFFFF1744).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = punch.punchType,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIn) Color(0xFF00C853) else Color(0xFFFF1744)
                                )
                            }
                        }
                    }

                    if (index < punches.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}
