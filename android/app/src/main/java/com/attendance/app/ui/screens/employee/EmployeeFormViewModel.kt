package com.attendance.app.ui.screens.employee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.CategoryDao
import com.attendance.app.data.local.dao.DepartmentDao
import com.attendance.app.data.local.entity.CategoryEntity
import com.attendance.app.data.local.entity.DepartmentEntity
import com.attendance.app.data.local.entity.EmployeeEntity
import com.attendance.app.data.repository.EmployeeRepository
import com.attendance.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeeFormUiState(
    val code: String = "",
    val empId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val departmentId: Long? = null,
    val categoryId: Long? = null,
    val dob: String = "",
    val doj: String = "",
    val weeklyOffDay: Int = 0,
    val fd: String = "",
    val otRrType: String = "COMP",
    val otRrValue: String = "0.0",
    val grossSalary: String = "0.0",
    val departments: List<DepartmentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val existingEntity: EmployeeEntity? = null,
    val codeError: String? = null,
    val empIdError: String? = null
)

@HiltViewModel
class EmployeeFormViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val departmentDao: DepartmentDao,
    private val categoryDao: CategoryDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeFormUiState())
    val uiState: StateFlow<EmployeeFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            departmentDao.getAll().collect { departments ->
                _uiState.update { it.copy(departments = departments) }
            }
        }

        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun loadEmployee(employeeId: Long?) {
        if (employeeId == null || employeeId == -1L) return
        _uiState.update { it.copy(isLoading = true, isEditing = true) }
        viewModelScope.launch {
            val entity = employeeRepository.getById(employeeId)
            if (entity != null) {
                _uiState.update {
                    it.copy(
                        code = entity.code,
                        empId = entity.empId,
                        name = entity.name,
                        fatherName = entity.fatherName,
                        departmentId = entity.departmentId,
                        categoryId = entity.categoryId,
                        dob = entity.dob,
                        doj = entity.doj,
                        weeklyOffDay = entity.weeklyOffDay,
                        fd = entity.fd,
                        otRrType = entity.otRrType,
                        otRrValue = entity.otRrValue.toString(),
                        grossSalary = entity.grossSalary.toString(),
                        isLoading = false,
                        existingEntity = entity
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCodeChanged(value: String) {
        _uiState.update { it.copy(code = value, codeError = null) }
    }

    fun onEmpIdChanged(value: String) {
        _uiState.update { it.copy(empId = value, empIdError = null) }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onFatherNameChanged(value: String) {
        _uiState.update { it.copy(fatherName = value) }
    }

    fun onDepartmentChanged(departmentId: Long?) {
        _uiState.update { it.copy(departmentId = departmentId) }
    }

    fun onCategoryChanged(categoryId: Long?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onDobChanged(value: String) {
        _uiState.update { it.copy(dob = value) }
    }

    fun onDojChanged(value: String) {
        _uiState.update { it.copy(doj = value) }
    }

    fun onWeeklyOffDayChanged(value: Int) {
        _uiState.update { it.copy(weeklyOffDay = value) }
    }

    fun onFdChanged(value: String) {
        _uiState.update { it.copy(fd = value) }
    }

    fun onOtRrTypeChanged(value: String) {
        _uiState.update { it.copy(otRrType = value) }
    }

    fun onOtRrValueChanged(value: String) {
        _uiState.update { it.copy(otRrValue = value) }
    }

    fun onGrossSalaryChanged(value: String) {
        _uiState.update { it.copy(grossSalary = value) }
    }

    fun clearCodeError() {
        _uiState.update { it.copy(codeError = null) }
    }

    fun clearEmpIdError() {
        _uiState.update { it.copy(empIdError = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.code.isBlank() || state.empId.isBlank() || state.name.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val now = DateUtils.now()
            val entity = EmployeeEntity(
                id = state.existingEntity?.id ?: 0,
                code = state.code.trim(),
                empId = state.empId.trim(),
                name = state.name.trim(),
                fatherName = state.fatherName.trim(),
                departmentId = state.departmentId,
                categoryId = state.categoryId,
                dob = state.dob,
                doj = state.doj,
                weeklyOffDay = state.weeklyOffDay,
                fd = state.fd,
                otRrType = state.otRrType,
                otRrValue = state.otRrValue.toDoubleOrNull() ?: 0.0,
                grossSalary = state.grossSalary.toDoubleOrNull() ?: 0.0,
                isActive = state.existingEntity?.isActive ?: true,
                createdAt = state.existingEntity?.createdAt ?: now,
                updatedAt = now
            )
            try {
                employeeRepository.save(entity)
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                val msg = e.message?.lowercase() ?: ""
                when {
                    msg.contains("code") -> _uiState.update {
                        it.copy(isLoading = false, codeError = "This code is already used by another employee")
                    }
                    msg.contains("emp_id") -> _uiState.update {
                        it.copy(isLoading = false, empIdError = "This Employee ID is already in use")
                    }
                    else -> _uiState.update {
                        it.copy(isLoading = false, codeError = "Duplicate entry — check Code and Employee ID")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
