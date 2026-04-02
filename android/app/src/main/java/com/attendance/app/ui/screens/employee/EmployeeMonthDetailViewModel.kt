package com.attendance.app.ui.screens.employee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.EmployeeRepository
import com.attendance.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class EmployeeMonthStats(
    val presentDays: Int,
    val absentDays: Int,
    val weeklyOffDays: Int,
    val coDays: Int,
    val totalOtHours: Double,
    val avgDailyHours: Double
)

data class EmployeeMonthDetailUiState(
    val employee: EmployeeWithDetails? = null,
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val records: List<AttendanceRecordEntity> = emptyList(),
    val stats: EmployeeMonthStats? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class EmployeeMonthDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val employeeId: Long = checkNotNull(savedStateHandle["employeeId"])

    private val _uiState = MutableStateFlow(
        EmployeeMonthDetailUiState(
            selectedYear = savedStateHandle["year"] ?: LocalDate.now().year,
            selectedMonth = savedStateHandle["month"] ?: LocalDate.now().monthValue
        )
    )
    val uiState: StateFlow<EmployeeMonthDetailUiState> = _uiState.asStateFlow()

    init {
        loadEmployee()
        loadData()
    }

    private fun loadEmployee() {
        viewModelScope.launch {
            val employees = employeeRepository.getAllActive().first()
            val emp = employees.find { it.id == employeeId }
            _uiState.update { it.copy(employee = emp) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val year = _uiState.value.selectedYear
            val month = _uiState.value.selectedMonth
            val (startDate, endDate) = DateUtils.getMonthDateRange(year, month)

            val records = attendanceRepository.getByEmployeeAndDateRange(
                employeeId, startDate, endDate
            ).first()

            val presentDays = records.count { it.status == "P" }
            val absentDays = records.count { it.status == "A" }
            val weeklyOffDays = records.count { it.status == "W" }
            val coDays = records.count { it.status == "CO" }
            val totalOt = records.sumOf { it.otHours }
            val workDayRecords = records.filter { it.totalHours > 0 }
            val avgHours = if (workDayRecords.isNotEmpty())
                workDayRecords.sumOf { it.totalHours } / workDayRecords.size
            else 0.0

            _uiState.update {
                it.copy(
                    records = records,
                    stats = EmployeeMonthStats(
                        presentDays = presentDays,
                        absentDays = absentDays,
                        weeklyOffDays = weeklyOffDays,
                        coDays = coDays,
                        totalOtHours = totalOt,
                        avgDailyHours = avgHours
                    ),
                    isLoading = false
                )
            }
        }
    }

    fun onPreviousMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val prev = current.minusMonths(1)
        _uiState.update { it.copy(selectedYear = prev.year, selectedMonth = prev.monthValue) }
        loadData()
    }

    fun onNextMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val next = current.plusMonths(1)
        _uiState.update { it.copy(selectedYear = next.year, selectedMonth = next.monthValue) }
        loadData()
    }
}
