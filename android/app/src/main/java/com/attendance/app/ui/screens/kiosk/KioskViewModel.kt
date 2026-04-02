package com.attendance.app.ui.screens.kiosk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.dao.PunchLogDao
import com.attendance.app.data.local.dao.UnitLocationDao
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import com.attendance.app.data.local.entity.PunchLogEntity
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

data class PunchResult(
    val employeeName: String,
    val punchType: String,
    val time: String
)

data class KioskUiState(
    val unitName: String = "",
    val unitId: Long? = null,
    val inputCode: String = "",
    val matchedEmployee: EmployeeWithDetails? = null,
    val lastPunchType: String? = null,
    val suggestedPunchType: String = "IN",
    val currentTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")),
    val currentDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
    val punchSuccess: PunchResult? = null,
    val isLocked: Boolean = true,
    val pinInput: String = "",
    val showPinDialog: Boolean = false,
    val autoResetSeconds: Int = 5,
    val countdownRemaining: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class KioskViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val punchLogDao: PunchLogDao,
    private val attendanceDao: AttendanceDao,
    private val unitLocationDao: UnitLocationDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KioskUiState())
    val uiState: StateFlow<KioskUiState> = _uiState.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    private val punchTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private var allEmployees: List<EmployeeWithDetails> = emptyList()
    private var timeTickerJob: Job? = null
    private var autoResetJob: Job? = null

    companion object {
        private const val KEY_KIOSK_UNIT_ID = "kiosk_unit_id"
        private const val KEY_KIOSK_PIN = "kiosk_pin"
        private const val KEY_KIOSK_AUTO_RESET = "kiosk_auto_reset_seconds"
    }

    init {
        loadSettings()
        loadEmployees()
        startTimeTicker()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load unit
            val unitId = settingsRepository.getValue(KEY_KIOSK_UNIT_ID)?.toLongOrNull()
            if (unitId != null) {
                val unit = unitLocationDao.getById(unitId)
                if (unit != null) {
                    _uiState.update {
                        it.copy(unitId = unit.id, unitName = unit.name)
                    }
                }
            } else {
                // Fall back to first active unit
                val units = unitLocationDao.getAllActive().first()
                if (units.isNotEmpty()) {
                    val unit = units.first()
                    _uiState.update {
                        it.copy(unitId = unit.id, unitName = unit.name)
                    }
                }
            }

            // Load auto-reset seconds
            val resetSeconds = settingsRepository.getValue(KEY_KIOSK_AUTO_RESET)?.toIntOrNull() ?: 5
            _uiState.update { it.copy(autoResetSeconds = resetSeconds) }
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            employeeRepository.getAllActive().collectLatest { employees ->
                allEmployees = employees
                // Re-check match if there's an active input
                val currentInput = _uiState.value.inputCode
                if (currentInput.isNotEmpty()) {
                    findMatch(currentInput)
                }
            }
        }
    }

    private fun startTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                val today = LocalDate.now()
                _uiState.update {
                    it.copy(
                        currentTime = now.format(timeFormatter),
                        currentDate = today.format(dateFormatter)
                    )
                }
                delay(10_000L) // Update every 10 seconds for kiosk display
            }
        }
    }

    fun onCodeInput(char: Char) {
        val newCode = _uiState.value.inputCode + char
        _uiState.update { it.copy(inputCode = newCode, errorMessage = null) }
        findMatch(newCode)
    }

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(inputCode = code, errorMessage = null) }
        if (code.isBlank()) {
            _uiState.update {
                it.copy(matchedEmployee = null, lastPunchType = null, suggestedPunchType = "IN")
            }
            return
        }
        findMatch(code)
    }

    fun onBackspace() {
        val current = _uiState.value.inputCode
        if (current.isNotEmpty()) {
            val newCode = current.dropLast(1)
            _uiState.update { it.copy(inputCode = newCode, errorMessage = null) }
            if (newCode.isEmpty()) {
                _uiState.update {
                    it.copy(matchedEmployee = null, lastPunchType = null, suggestedPunchType = "IN")
                }
            } else {
                findMatch(newCode)
            }
        }
    }

    private fun findMatch(code: String) {
        val lowerCode = code.lowercase()
        val match = allEmployees.find { emp ->
            emp.code.lowercase() == lowerCode || emp.empId.lowercase() == lowerCode
        }

        if (match != null) {
            _uiState.update { it.copy(matchedEmployee = match) }
            viewModelScope.launch {
                val lastType = punchLogDao.getLastPunchType(match.id, DateUtils.today())
                val suggested = if (lastType == "IN") "OUT" else "IN"
                _uiState.update {
                    it.copy(lastPunchType = lastType, suggestedPunchType = suggested)
                }
            }
        } else {
            _uiState.update {
                it.copy(matchedEmployee = null, lastPunchType = null, suggestedPunchType = "IN")
            }
        }
    }

    fun clearInput() {
        autoResetJob?.cancel()
        _uiState.update {
            it.copy(
                inputCode = "",
                matchedEmployee = null,
                lastPunchType = null,
                suggestedPunchType = "IN",
                punchSuccess = null,
                countdownRemaining = 0,
                errorMessage = null
            )
        }
    }

    fun punch(type: String) {
        val state = _uiState.value
        val employee = state.matchedEmployee ?: return
        val unitId = state.unitId

        viewModelScope.launch {
            try {
                val today = DateUtils.today()
                val punchTime = LocalTime.now().format(punchTimeFormatter)

                // Insert punch log
                val punchLog = PunchLogEntity(
                    employeeId = employee.id,
                    date = today,
                    time = punchTime,
                    punchType = type,
                    unitId = unitId,
                    recordedBy = "self",
                    createdAt = DateUtils.now()
                )
                punchLogDao.insert(punchLog)

                // Auto-update attendance record
                updateAttendanceRecord(employee, today)

                // Show success
                val result = PunchResult(
                    employeeName = employee.name,
                    punchType = type,
                    time = punchTime
                )
                _uiState.update {
                    it.copy(
                        punchSuccess = result,
                        inputCode = "",
                        matchedEmployee = null,
                        lastPunchType = null,
                        suggestedPunchType = "IN",
                        errorMessage = null
                    )
                }

                // Start auto-reset countdown
                startAutoResetCountdown()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Punch failed: ${e.message}") }
            }
        }
    }

    private fun startAutoResetCountdown() {
        autoResetJob?.cancel()
        val seconds = _uiState.value.autoResetSeconds
        _uiState.update { it.copy(countdownRemaining = seconds) }

        autoResetJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.update { it.copy(countdownRemaining = i) }
                delay(1000L)
            }
            // Reset screen
            _uiState.update {
                it.copy(
                    punchSuccess = null,
                    countdownRemaining = 0,
                    inputCode = "",
                    matchedEmployee = null,
                    lastPunchType = null,
                    suggestedPunchType = "IN"
                )
            }
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

    fun showPinDialog() {
        _uiState.update { it.copy(showPinDialog = true, pinInput = "", errorMessage = null) }
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialog = false, pinInput = "", errorMessage = null) }
    }

    fun onPinInputChanged(pin: String) {
        _uiState.update { it.copy(pinInput = pin, errorMessage = null) }
    }

    fun exitKiosk(onExit: () -> Unit) {
        viewModelScope.launch {
            val kioskPin = settingsRepository.getValue(KEY_KIOSK_PIN) ?: ""
            if (kioskPin.isEmpty()) {
                // No PIN set, exit directly
                _uiState.update { it.copy(isLocked = false, showPinDialog = false) }
                onExit()
            } else {
                val enteredPin = _uiState.value.pinInput
                if (enteredPin == kioskPin) {
                    _uiState.update { it.copy(isLocked = false, showPinDialog = false) }
                    onExit()
                } else {
                    _uiState.update { it.copy(errorMessage = "Incorrect PIN") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeTickerJob?.cancel()
        autoResetJob?.cancel()
    }
}
