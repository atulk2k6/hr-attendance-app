package com.attendance.app.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.EmployeeRepository
import com.attendance.app.data.repository.SettingsRepository
import com.attendance.app.util.DateUtils
import com.attendance.app.util.OvertimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AttendanceEntry(
    val employeeId: Long,
    val name: String,
    val code: String,
    val weeklyOffDay: Int,
    val inTime: String = "",
    val outTime: String = "",
    val status: String = "A",
    val otHours: Double = 0.0,
    val totalHours: Double = 0.0,
    val existingRecordId: Long = 0,
    val createdAt: String = "",
    val remarks: String = "",
    val timeError: String? = null
)

data class AttendanceUiState(
    val selectedDate: String = DateUtils.today(),
    val displayDate: String = DateUtils.formatForDisplay(DateUtils.today()),
    val entries: List<AttendanceEntry> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val timePickerTarget: TimePickerTarget? = null
) {
    val hasTimeErrors: Boolean get() = entries.any { it.timeError != null }
}

data class TimePickerTarget(
    val entryIndex: Int,
    val isInTime: Boolean
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val employees: List<EmployeeWithDetails> = employeeRepository.getAllActive().first()
            val existingRecords: List<AttendanceRecordEntity> = attendanceRepository
                .getByDateRange(date, date).first()

            val recordMap = existingRecords.associateBy { it.employeeId }
            val normalHours = settingsRepository.getNormalWorkHours()

            val entries = employees.map { emp ->
                val existing = recordMap[emp.id]
                if (existing != null) {
                    AttendanceEntry(
                        employeeId = emp.id,
                        name = emp.name,
                        code = emp.code,
                        weeklyOffDay = emp.weeklyOffDay,
                        inTime = existing.inTime,
                        outTime = existing.outTime,
                        status = existing.status,
                        otHours = existing.otHours,
                        totalHours = existing.totalHours,
                        existingRecordId = existing.id,
                        createdAt = existing.createdAt,
                        remarks = existing.remarks
                    )
                } else {
                    val calc = OvertimeCalculator.calculate(
                        inTime = "",
                        outTime = "",
                        normalWorkHours = normalHours,
                        weeklyOffDay = emp.weeklyOffDay,
                        date = date
                    )
                    AttendanceEntry(
                        employeeId = emp.id,
                        name = emp.name,
                        code = emp.code,
                        weeklyOffDay = emp.weeklyOffDay,
                        status = calc.status,
                        otHours = calc.otHours,
                        totalHours = calc.totalHours
                    )
                }
            }

            _uiState.update {
                it.copy(
                    entries = entries,
                    saveSuccess = false
                )
            }
        }
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val date = LocalDate.of(year, month + 1, dayOfMonth)
        val isoDate = date.format(isoDateFormatter)
        _uiState.update {
            it.copy(
                selectedDate = isoDate,
                displayDate = DateUtils.formatForDisplay(isoDate),
                showDatePicker = false,
                saveSuccess = false
            )
        }
        loadData()
    }

    fun onPreviousDay() {
        val current = LocalDate.parse(_uiState.value.selectedDate, isoDateFormatter)
        val prev = current.minusDays(1)
        val isoDate = prev.format(isoDateFormatter)
        _uiState.update {
            it.copy(
                selectedDate = isoDate,
                displayDate = DateUtils.formatForDisplay(isoDate),
                saveSuccess = false
            )
        }
        loadData()
    }

    fun onNextDay() {
        val current = LocalDate.parse(_uiState.value.selectedDate, isoDateFormatter)
        val next = current.plusDays(1)
        val isoDate = next.format(isoDateFormatter)
        _uiState.update {
            it.copy(
                selectedDate = isoDate,
                displayDate = DateUtils.formatForDisplay(isoDate),
                saveSuccess = false
            )
        }
        loadData()
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun showTimePicker(entryIndex: Int, isInTime: Boolean) {
        _uiState.update {
            it.copy(
                showTimePicker = true,
                timePickerTarget = TimePickerTarget(entryIndex, isInTime)
            )
        }
    }

    fun dismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false, timePickerTarget = null) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val target = _uiState.value.timePickerTarget ?: return
        val timeStr = String.format("%02d:%02d", hour, minute)

        viewModelScope.launch {
            val normalHours = settingsRepository.getNormalWorkHours()
            val entries = _uiState.value.entries.toMutableList()
            val entry = entries[target.entryIndex]
            val updatedEntry = if (target.isInTime) {
                entry.copy(inTime = timeStr)
            } else {
                entry.copy(outTime = timeStr)
            }

            // Recalculate with new times
            val calc = OvertimeCalculator.calculate(
                inTime = updatedEntry.inTime,
                outTime = updatedEntry.outTime,
                normalWorkHours = normalHours,
                weeklyOffDay = updatedEntry.weeklyOffDay,
                date = _uiState.value.selectedDate
            )

            entries[target.entryIndex] = updatedEntry.copy(
                status = calc.status,
                totalHours = calc.totalHours,
                otHours = calc.otHours
            )

            _uiState.update {
                it.copy(
                    entries = entries,
                    showTimePicker = false,
                    timePickerTarget = null
                )
            }
        }
    }

    fun onStatusOverride(entryIndex: Int, newStatus: String) {
        val entries = _uiState.value.entries.toMutableList()
        entries[entryIndex] = entries[entryIndex].copy(status = newStatus)
        _uiState.update { it.copy(entries = entries) }
    }

    private fun isValidTime(time: String): Boolean {
        if (time.isBlank()) return true
        return Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$").matches(time)
    }

    fun onInTimeManualChanged(entryIndex: Int, time: String) {
        val entries = _uiState.value.entries.toMutableList()
        val entry = entries[entryIndex]
        val error = if (isValidTime(time)) null else "Use HH:MM format"
        entries[entryIndex] = entry.copy(inTime = time, timeError = error)
        _uiState.update { it.copy(entries = entries) }
    }

    fun onOutTimeManualChanged(entryIndex: Int, time: String) {
        val entries = _uiState.value.entries.toMutableList()
        val entry = entries[entryIndex]
        val error = if (isValidTime(time)) null else "Use HH:MM format"
        entries[entryIndex] = entry.copy(outTime = time, timeError = error)
        _uiState.update { it.copy(entries = entries) }
    }

    fun onRemarksChanged(entryIndex: Int, remarks: String) {
        val entries = _uiState.value.entries.toMutableList()
        entries[entryIndex] = entries[entryIndex].copy(remarks = remarks)
        _uiState.update { it.copy(entries = entries) }
    }

    fun getCurrentTime(): String {
        val now = java.time.LocalTime.now()
        return String.format("%02d:%02d", now.hour, now.minute)
    }

    fun applyCurrentTime(entryIndex: Int, isInTime: Boolean) {
        viewModelScope.launch {
            val normalHours = settingsRepository.getNormalWorkHours()
            val timeStr = getCurrentTime()
            val entries = _uiState.value.entries.toMutableList()
            val entry = entries[entryIndex]
            val updatedEntry = if (isInTime) entry.copy(inTime = timeStr) else entry.copy(outTime = timeStr)
            val calc = OvertimeCalculator.calculate(
                inTime = updatedEntry.inTime,
                outTime = updatedEntry.outTime,
                normalWorkHours = normalHours,
                weeklyOffDay = updatedEntry.weeklyOffDay,
                date = _uiState.value.selectedDate
            )
            entries[entryIndex] = updatedEntry.copy(
                status = calc.status,
                totalHours = calc.totalHours,
                otHours = calc.otHours,
                timeError = null
            )
            _uiState.update { it.copy(entries = entries) }
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            val normalHours = settingsRepository.getNormalWorkHours()
            val entries = _uiState.value.entries.map { entry ->
                val calc = OvertimeCalculator.calculate(
                    inTime = entry.inTime,
                    outTime = entry.outTime,
                    normalWorkHours = normalHours,
                    weeklyOffDay = entry.weeklyOffDay,
                    date = _uiState.value.selectedDate
                )
                entry.copy(status = if (calc.status == "W") "W" else "P")
            }
            _uiState.update { it.copy(entries = entries) }
        }
    }

    // Marks ONLY employees with no existing record and no punched times as Absent
    fun markUnpunchedAbsent() {
        viewModelScope.launch {
            val normalHours = settingsRepository.getNormalWorkHours()
            val entries = _uiState.value.entries.map { entry ->
                if (entry.existingRecordId == 0L && entry.inTime.isBlank() && entry.outTime.isBlank()) {
                    val calc = OvertimeCalculator.calculate(
                        inTime = "",
                        outTime = "",
                        normalWorkHours = normalHours,
                        weeklyOffDay = entry.weeklyOffDay,
                        date = _uiState.value.selectedDate
                    )
                    entry.copy(status = if (calc.status == "W") "W" else "A")
                } else {
                    entry  // don't touch employees who already have a record or times
                }
            }
            _uiState.update { it.copy(entries = entries) }
        }
    }

    fun saveAll() {
        if (_uiState.value.hasTimeErrors) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val date = _uiState.value.selectedDate
            val now = DateUtils.now()

            val records = _uiState.value.entries
                .filter { entry ->
                    // Save existing records (always update), and new records that have meaningful data
                    entry.existingRecordId != 0L ||
                    entry.inTime.isNotBlank() ||
                    entry.outTime.isNotBlank() ||
                    entry.remarks.isNotBlank() ||
                    entry.status == "W" ||
                    entry.status == "CO" ||
                    entry.status == "P"
                }
                .map { entry ->
                    AttendanceRecordEntity(
                        id = entry.existingRecordId,
                        employeeId = entry.employeeId,
                        date = date,
                        inTime = entry.inTime,
                        outTime = entry.outTime,
                        status = entry.status,
                        totalHours = entry.totalHours,
                        otHours = entry.otHours,
                        remarks = entry.remarks,
                        createdAt = if (entry.existingRecordId == 0L) now else entry.createdAt,
                        updatedAt = now
                    )
                }

            attendanceRepository.saveAll(records)

            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            loadData()
        }
    }
}
