package com.attendance.app.ui.screens.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.CategoryDao
import com.attendance.app.data.local.dao.DepartmentDao
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.entity.CategoryEntity
import com.attendance.app.data.local.entity.DepartmentEntity
import com.attendance.app.data.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeeListUiState(
    val employees: List<EmployeeWithDetails> = emptyList(),
    val searchQuery: String = "",
    val selectedDepartmentId: Long? = null,
    val selectedCategoryId: Long? = null,
    val departments: List<DepartmentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val showDeactivateDialog: Boolean = false,
    val employeeToDeactivate: EmployeeWithDetails? = null
)

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val departmentDao: DepartmentDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeListUiState())
    val uiState: StateFlow<EmployeeListUiState> = _uiState.asStateFlow()

    private val _selectedDepartmentId = MutableStateFlow<Long?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredEmployees: Flow<List<EmployeeWithDetails>> =
        combine(_selectedDepartmentId, _selectedCategoryId) { deptId, catId ->
            Pair(deptId, catId)
        }.flatMapLatest { (deptId, catId) ->
            employeeRepository.getFiltered(deptId, catId)
        }

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

        viewModelScope.launch {
            combine(filteredEmployees, _searchQuery) { employees, query ->
                if (query.isBlank()) {
                    employees
                } else {
                    val q = query.trim().lowercase()
                    employees.filter { emp ->
                        emp.name.lowercase().contains(q) ||
                                emp.code.lowercase().contains(q) ||
                                emp.empId.lowercase().contains(q) ||
                                (emp.departmentName?.lowercase()?.contains(q) == true)
                    }
                }
            }.collect { filtered ->
                _uiState.update { it.copy(employees = filtered) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDepartmentFilterChanged(departmentId: Long?) {
        _selectedDepartmentId.value = departmentId
        _uiState.update { it.copy(selectedDepartmentId = departmentId) }
    }

    fun onCategoryFilterChanged(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onDeactivateRequested(employee: EmployeeWithDetails) {
        _uiState.update {
            it.copy(showDeactivateDialog = true, employeeToDeactivate = employee)
        }
    }

    fun onDeactivateConfirmed() {
        val employee = _uiState.value.employeeToDeactivate ?: return
        viewModelScope.launch {
            employeeRepository.deactivate(employee.id)
            _uiState.update {
                it.copy(showDeactivateDialog = false, employeeToDeactivate = null)
            }
        }
    }

    fun onDeactivateDismissed() {
        _uiState.update {
            it.copy(showDeactivateDialog = false, employeeToDeactivate = null)
        }
    }
}
