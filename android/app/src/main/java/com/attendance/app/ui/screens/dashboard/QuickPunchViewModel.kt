package com.attendance.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.dao.PunchLogDao
import com.attendance.app.data.local.dao.PunchLogWithEmployee
import com.attendance.app.data.local.dao.UnitLocationDao
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import com.attendance.app.data.local.entity.PunchLogEntity
import com.attendance.app.data.local.entity.UnitLocationEntity
import com.attendance.app.data.repository.EmployeeRepository
import com.attendance.app.data.repository.SettingsRepository
import com.attendance.app.util.DateUtils
import com.attendance.app.util.OvertimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TodayStats(
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val totalOt: Double = 0.0,
    val totalPunchedIn: Int = 0
)

data class QuickPunchUiState(
    val units: List<UnitLocationEntity> = emptyList(),
    val selectedUnitId: Long? = null,
    val searchQuery: String = "",
    val searchResults: List<EmployeeWithDetails> = emptyList(),
    val selectedEmployee: EmployeeWithDetails? = null,
    val currentDate: String = DateUtils.today(),
    val currentTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val adjustedTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val timeAdjustmentMinutes: Int = 0,
    val lastPunchType: String? = null,
    val suggestedPunchType: String = "IN",
    val recentPunches: List<PunchLogWithEmployee> = emptyList(),
    val todayStats: TodayStats = TodayStats(),
    val isPunching: Boolean = false,
    val punchSuccess: String? = null,
    val duplicatePunchWarning: String? = null,
    val pendingPunchType: String? = null,
    val dailyReportText: String? = null
)

@HiltViewModel
class QuickPunchViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val punchLogDao: PunchLogDao,
    private val attendanceDao: AttendanceDao,
    private val unitLocationDao: UnitLocationDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickPunchUiState())
    val uiState: StateFlow<QuickPunchUiState> = _uiState.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var allEmployees: List<EmployeeWithDetails> = emptyList()
    private var timeTickerJob: Job? = null
    private var recentPunchesJob: Job? = null
    private var statsJob: Job? = null

    companion object {
        private const val KEY_LAST_SELECTED_UNIT_ID = "last_selected_unit_id"
        private const val MAX_SEARCH_RESULTS = 5
        private const val MAX_TIME_ADJUSTMENT = 30
        private const val MAX_RECENT_PUNCHES = 20
    }

    init {
        loadUnits()
        loadEmployees()
        startTimeTicker()
        observeRecentPunches()
        observeTodayStats()
    }

    private fun loadUnits() {
        viewModelScope.launch {
            unitLocationDao.getAllActive().collectLatest { units ->
                val savedUnitId = settingsRepository.getValue(KEY_LAST_SELECTED_UNIT_ID)?.toLongOrNull()
                val selectedId = when {
                    savedUnitId != null && units.any { it.id == savedUnitId } -> savedUnitId
                    units.size == 1 -> units.first().id
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        units = units,
                        selectedUnitId = selectedId ?: it.selectedUnitId
                    )
                }
            }
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            employeeRepository.getAllActive().collectLatest { employees ->
                allEmployees = employees
            }
        }
    }

    private fun startTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch {
            while (true) {
                val now = LocalTime.now().format(timeFormatter)
                val adjusted = computeAdjustedTime(now, _uiState.value.timeAdjustmentMinutes)
                _uiState.update {
                    it.copy(
                        currentTime = now,
                        adjustedTime = adjusted,
                        currentDate = DateUtils.today()
                    )
                }
                delay(5_000L)
            }
        }
    }

    private fun observeRecentPunches() {
        recentPunchesJob?.cancel()
        recentPunchesJob = viewModelScope.launch {
            punchLogDao.getRecentByDate(DateUtils.today(), MAX_RECENT_PUNCHES).collectLatest { punches ->
                _uiState.update { it.copy(recentPunches = punches) }
            }
        }
    }

    private fun observeTodayStats() {
        val today = DateUtils.today()

        statsJob?.cancel()

        viewModelScope.launch {
            attendanceDao.getPresentCountForDate(today).collectLatest { count ->
                _uiState.update { it.copy(todayStats = it.todayStats.copy(presentCount = count)) }
            }
        }

        viewModelScope.launch {
            attendanceDao.getAbsentCountForDate(today).collectLatest { count ->
                _uiState.update { it.copy(todayStats = it.todayStats.copy(absentCount = count)) }
            }
        }

        viewModelScope.launch {
            attendanceDao.getTotalOtForDate(today).collectLatest { total ->
                _uiState.update { it.copy(todayStats = it.todayStats.copy(totalOt = total ?: 0.0)) }
            }
        }

        viewModelScope.launch {
            punchLogDao.getUniqueEmployeesInForDate(today).collectLatest { count ->
                _uiState.update { it.copy(todayStats = it.todayStats.copy(totalPunchedIn = count)) }
            }
        }
    }

    fun onUnitSelected(unitId: Long) {
        _uiState.update { it.copy(selectedUnitId = unitId) }
        viewModelScope.launch {
            settingsRepository.setValue(KEY_LAST_SELECTED_UNIT_ID, unitId.toString())
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedEmployee = null, lastPunchType = null) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        val lowerQuery = query.lowercase()
        val results = allEmployees
            .filter { emp ->
                emp.code.lowercase().contains(lowerQuery) ||
                        emp.empId.lowercase().contains(lowerQuery) ||
                        emp.name.lowercase().contains(lowerQuery)
            }
            .take(MAX_SEARCH_RESULTS)
        _uiState.update { it.copy(searchResults = results) }
    }

    fun onEmployeeSelected(employee: EmployeeWithDetails) {
        _uiState.update {
            it.copy(
                selectedEmployee = employee,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        viewModelScope.launch {
            val lastType = punchLogDao.getLastPunchType(employee.id, DateUtils.today())
            val suggested = if (lastType == "IN") "OUT" else "IN"
            _uiState.update {
                it.copy(
                    lastPunchType = lastType,
                    suggestedPunchType = suggested
                )
            }
        }
    }

    fun clearSelectedEmployee() {
        _uiState.update {
            it.copy(
                selectedEmployee = null,
                searchQuery = "",
                searchResults = emptyList(),
                lastPunchType = null,
                suggestedPunchType = "IN"
            )
        }
    }

    fun adjustTime(minutes: Int) {
        val current = _uiState.value.timeAdjustmentMinutes + minutes
        val clamped = current.coerceIn(-MAX_TIME_ADJUSTMENT, MAX_TIME_ADJUSTMENT)
        val adjusted = computeAdjustedTime(_uiState.value.currentTime, clamped)
        _uiState.update {
            it.copy(
                timeAdjustmentMinutes = clamped,
                adjustedTime = adjusted
            )
        }
    }

    fun resetTime() {
        val now = LocalTime.now().format(timeFormatter)
        _uiState.update {
            it.copy(
                timeAdjustmentMinutes = 0,
                currentTime = now,
                adjustedTime = now
            )
        }
    }

    fun punch(type: String) {
        val state = _uiState.value
        val employee = state.selectedEmployee ?: return
        val unitId = state.selectedUnitId

        _uiState.update { it.copy(isPunching = true) }

        viewModelScope.launch {
            try {
                val today = DateUtils.today()

                // Check for duplicate punch
                val lastType = punchLogDao.getLastPunchType(employee.id, today)
                if (lastType == type) {
                    val lastTime = punchLogDao.getByEmployeeAndDate(employee.id, today)
                        .lastOrNull { it.punchType == type }?.time ?: ""
                    val timeDisplay = if (lastTime.isNotBlank()) " at $lastTime" else ""
                    _uiState.update {
                        it.copy(
                            isPunching = false,
                            duplicatePunchWarning = "Already punched $type$timeDisplay. Record another $type?",
                            pendingPunchType = type
                        )
                    }
                    return@launch
                }

                doPunch(employee.id, type, unitId, state.adjustedTime, today, employee.name)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPunching = false,
                        punchSuccess = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmDuplicatePunch() {
        val state = _uiState.value
        val employee = state.selectedEmployee ?: return
        val type = state.pendingPunchType ?: return
        _uiState.update { it.copy(duplicatePunchWarning = null, pendingPunchType = null, isPunching = true) }
        viewModelScope.launch {
            try {
                doPunch(employee.id, type, state.selectedUnitId, state.adjustedTime, DateUtils.today(), employee.name)
            } catch (e: Exception) {
                _uiState.update { it.copy(isPunching = false, punchSuccess = "Error: ${e.message}") }
            }
        }
    }

    fun dismissDuplicatePunch() {
        _uiState.update { it.copy(duplicatePunchWarning = null, pendingPunchType = null, isPunching = false) }
    }

    private suspend fun doPunch(
        employeeId: Long,
        type: String,
        unitId: Long?,
        punchTime: String,
        today: String,
        employeeName: String
    ) {
        val employee = allEmployees.find { it.id == employeeId } ?: return

        // 1. Insert punch log
        val punchLog = PunchLogEntity(
            employeeId = employeeId,
            date = today,
            time = punchTime,
            punchType = type,
            unitId = unitId,
            recordedBy = "supervisor",
            createdAt = DateUtils.now()
        )
        punchLogDao.insert(punchLog)

        // 2. Auto-update attendance record
        updateAttendanceRecord(employee, today)

        // 3. Show success
        _uiState.update {
            it.copy(
                isPunching = false,
                punchSuccess = "$employeeName punched $type at $punchTime",
                selectedEmployee = null,
                searchQuery = "",
                searchResults = emptyList(),
                lastPunchType = null,
                suggestedPunchType = "IN",
                timeAdjustmentMinutes = 0,
                adjustedTime = it.currentTime
            )
        }
    }

    private suspend fun updateAttendanceRecord(employee: EmployeeWithDetails, date: String) {
        val punches = punchLogDao.getByEmployeeAndDate(employee.id, date)
        if (punches.isEmpty()) return

        val firstIn = punches.filter { it.punchType == "IN" }.minByOrNull { it.time }?.time ?: ""
        val lastOut = punches.filter { it.punchType == "OUT" }.maxByOrNull { it.time }?.time ?: ""

        val normalWorkHours = settingsRepository.getNormalWorkHours()
        val calcResult = OvertimeCalculator.calculate(
            inTime = firstIn,
            outTime = lastOut,
            normalWorkHours = normalWorkHours,
            weeklyOffDay = employee.weeklyOffDay,
            date = date
        )

        val existing = attendanceDao.getByEmployeeAndDate(employee.id, date)
        val now = DateUtils.now()
        val record = AttendanceRecordEntity(
            id = existing?.id ?: 0,
            employeeId = employee.id,
            date = date,
            inTime = firstIn,
            outTime = lastOut,
            status = calcResult.status,
            totalHours = calcResult.totalHours,
            otHours = calcResult.otHours,
            remarks = existing?.remarks ?: "",
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        attendanceDao.upsert(record)
    }

    fun deletePunch(punch: PunchLogWithEmployee) {
        viewModelScope.launch {
            punchLogDao.deleteById(punch.id)
            // Recalculate attendance for this employee
            val employee = allEmployees.find { it.id == punch.employeeId }
            if (employee != null) {
                val remainingPunches = punchLogDao.getByEmployeeAndDate(punch.employeeId, punch.date)
                if (remainingPunches.isEmpty()) {
                    val existing = attendanceDao.getByEmployeeAndDate(punch.employeeId, punch.date)
                    if (existing != null) {
                        attendanceDao.delete(existing)
                    }
                } else {
                    updateAttendanceRecord(employee, punch.date)
                }
            }
        }
    }

    fun clearPunchSuccess() {
        _uiState.update { it.copy(punchSuccess = null) }
    }

    fun generateDailyReport() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val punches = punchLogDao.getRecentByDate(today, 200).first()
            val companyName = settingsRepository.getCompanyName()

            val sb = StringBuilder()
            sb.appendLine("*ATTENDANCE REPORT — $today*")
            if (companyName.isNotBlank()) sb.appendLine(companyName)
            sb.appendLine("Present: ${_uiState.value.todayStats.presentCount} | Punched In: ${_uiState.value.todayStats.totalPunchedIn}")
            sb.appendLine()

            val grouped = punches.groupBy { it.employeeCode }
            grouped.forEach { (code, empPunches) ->
                val name = empPunches.first().employeeName
                val firstIn = empPunches.filter { it.punchType == "IN" }.minByOrNull { it.time }?.time ?: "-"
                val lastOut = empPunches.filter { it.punchType == "OUT" }.maxByOrNull { it.time }?.time ?: "-"
                sb.appendLine("$code $name  IN:$firstIn  OUT:$lastOut")
            }

            _uiState.update { it.copy(dailyReportText = sb.toString()) }
        }
    }

    fun clearDailyReport() {
        _uiState.update { it.copy(dailyReportText = null) }
    }

    private fun computeAdjustedTime(baseTime: String, adjustmentMinutes: Int): String {
        return try {
            val base = LocalTime.parse(baseTime, timeFormatter)
            base.plusMinutes(adjustmentMinutes.toLong()).format(timeFormatter)
        } catch (e: Exception) {
            baseTime
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeTickerJob?.cancel()
        recentPunchesJob?.cancel()
    }
}
