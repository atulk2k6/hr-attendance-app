package com.attendance.app.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.dao.PunchLogDao
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import com.attendance.app.data.local.entity.DepartmentEntity
import com.attendance.app.data.local.entity.PunchLogEntity
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.EmployeeRepository
import com.attendance.app.data.local.dao.DepartmentDao
import com.attendance.app.data.local.dao.CategoryDao
import com.attendance.app.data.local.entity.CategoryEntity
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

data class DayDetailState(
    val employee: EmployeeWithDetails,
    val date: String,
    val attendance: AttendanceRecordEntity?,
    val punches: List<PunchLogEntity>
)

data class EmployeeMonthSummary(
    val presentDays: Int,
    val absentDays: Int,
    val weeklyOffDays: Int,
    val coDays: Int,
    val totalOtHours: Double
)

data class MonthlyViewUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val daysInMonth: Int = YearMonth.now().lengthOfMonth(),
    val allEmployees: List<EmployeeWithDetails> = emptyList(),
    val filteredEmployees: List<EmployeeWithDetails> = emptyList(),
    // employeeId -> (date "YYYY-MM-DD" -> record)
    val attendanceMap: Map<Long, Map<String, AttendanceRecordEntity>> = emptyMap(),
    // employeeId -> summary
    val summaryMap: Map<Long, EmployeeMonthSummary> = emptyMap(),
    val departments: List<DepartmentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedDeptId: Long? = null,
    val selectedCatId: Long? = null,
    val dayDetail: DayDetailState? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class MonthlyViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val departmentDao: DepartmentDao,
    private val categoryDao: CategoryDao,
    private val punchLogDao: PunchLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyViewUiState())
    val uiState: StateFlow<MonthlyViewUiState> = _uiState.asStateFlow()

    init {
        loadFilters()
        loadData()
    }

    private fun loadFilters() {
        viewModelScope.launch {
            val depts = departmentDao.getAll().first()
            val cats = categoryDao.getAll().first()
            _uiState.update { it.copy(departments = depts, categories = cats) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val year = _uiState.value.selectedYear
            val month = _uiState.value.selectedMonth
            val daysInMonth = DateUtils.getDaysInMonth(year, month)
            val (startDate, endDate) = DateUtils.getMonthDateRange(year, month)

            val employees = employeeRepository.getAllActive().first()
            val records = attendanceRepository.getByDateRange(startDate, endDate).first()

            val attendanceMap = mutableMapOf<Long, MutableMap<String, AttendanceRecordEntity>>()
            for (record in records) {
                attendanceMap
                    .getOrPut(record.employeeId) { mutableMapOf() }[record.date] = record
            }

            // Build summary per employee
            val summaryMap = employees.associate { emp ->
                val empRecords = attendanceMap[emp.id]?.values ?: emptyList()
                val summary = EmployeeMonthSummary(
                    presentDays = empRecords.count { it.status == "P" },
                    absentDays = empRecords.count { it.status == "A" },
                    weeklyOffDays = empRecords.count { it.status == "W" },
                    coDays = empRecords.count { it.status == "CO" },
                    totalOtHours = empRecords.sumOf { it.otHours }
                )
                emp.id to summary
            }

            _uiState.update {
                it.copy(
                    daysInMonth = daysInMonth,
                    allEmployees = employees,
                    attendanceMap = attendanceMap,
                    summaryMap = summaryMap,
                    isLoading = false
                )
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val q = state.searchQuery.lowercase().trim()
        val filtered = state.allEmployees.filter { emp ->
            val matchesDept = state.selectedDeptId == null || emp.departmentId == state.selectedDeptId
            val matchesCat = state.selectedCatId == null || emp.categoryId == state.selectedCatId
            val matchesSearch = q.isBlank() ||
                emp.name.lowercase().contains(q) ||
                emp.code.lowercase().contains(q)
            matchesDept && matchesCat && matchesSearch
        }
        _uiState.update { it.copy(filteredEmployees = filtered) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onDeptFilterChanged(deptId: Long?) {
        _uiState.update { it.copy(selectedDeptId = deptId) }
        applyFilters()
    }

    fun onCatFilterChanged(catId: Long?) {
        _uiState.update { it.copy(selectedCatId = catId) }
        applyFilters()
    }

    fun showDayDetail(employee: EmployeeWithDetails, dateStr: String) {
        viewModelScope.launch {
            val attendance = _uiState.value.attendanceMap[employee.id]?.get(dateStr)
            val punches = punchLogDao.getByEmployeeAndDate(employee.id, dateStr)
            _uiState.update {
                it.copy(dayDetail = DayDetailState(employee, dateStr, attendance, punches))
            }
        }
    }

    fun dismissDayDetail() {
        _uiState.update { it.copy(dayDetail = null) }
    }

    fun onPreviousMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val prev = current.minusMonths(1)
        _uiState.update {
            it.copy(
                selectedYear = prev.year,
                selectedMonth = prev.monthValue,
                daysInMonth = prev.lengthOfMonth()
            )
        }
        loadData()
    }

    fun onNextMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val next = current.plusMonths(1)
        _uiState.update {
            it.copy(
                selectedYear = next.year,
                selectedMonth = next.monthValue,
                daysInMonth = next.lengthOfMonth()
            )
        }
        loadData()
    }
}
