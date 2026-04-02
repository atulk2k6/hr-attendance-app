package com.attendance.app.ui.screens.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.DepartmentDao
import com.attendance.app.data.local.entity.DepartmentEntity
import com.attendance.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentUiState(
    val departments: List<DepartmentEntity> = emptyList(),
    val employeeCounts: Map<Long, Int> = emptyMap(),
    val showDialog: Boolean = false,
    val editingDepartment: DepartmentEntity? = null,
    val dialogText: String = "",
    val dialogError: String? = null,
    val showDeleteDialog: Boolean = false,
    val departmentToDelete: DepartmentEntity? = null,
    val deleteError: String? = null
)

@HiltViewModel
class DepartmentViewModel @Inject constructor(
    private val departmentDao: DepartmentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepartmentUiState())
    val uiState: StateFlow<DepartmentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            departmentDao.getAll().collect { departments ->
                val counts = mutableMapOf<Long, Int>()
                for (dept in departments) {
                    counts[dept.id] = departmentDao.getEmployeeCount(dept.id)
                }
                _uiState.update {
                    it.copy(departments = departments, employeeCounts = counts)
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingDepartment = null,
                dialogText = "",
                dialogError = null
            )
        }
    }

    fun showEditDialog(department: DepartmentEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingDepartment = department,
                dialogText = department.name,
                dialogError = null
            )
        }
    }

    fun onDialogTextChanged(text: String) {
        _uiState.update { it.copy(dialogText = text, dialogError = null) }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(showDialog = false, editingDepartment = null, dialogText = "", dialogError = null)
        }
    }

    fun saveDepartment() {
        val name = _uiState.value.dialogText.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(dialogError = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val now = DateUtils.now()
            val editing = _uiState.value.editingDepartment
            if (editing != null) {
                departmentDao.update(editing.copy(name = name, updatedAt = now))
            } else {
                departmentDao.insert(DepartmentEntity(name = name, createdAt = now, updatedAt = now))
            }
            dismissDialog()
        }
    }

    fun showDeleteDialog(department: DepartmentEntity) {
        _uiState.update {
            it.copy(showDeleteDialog = true, departmentToDelete = department, deleteError = null)
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(showDeleteDialog = false, departmentToDelete = null, deleteError = null)
        }
    }

    fun confirmDelete() {
        val department = _uiState.value.departmentToDelete ?: return
        viewModelScope.launch {
            val count = departmentDao.getEmployeeCount(department.id)
            if (count > 0) {
                _uiState.update {
                    it.copy(deleteError = "Cannot delete: $count employee(s) assigned to this department")
                }
            } else {
                departmentDao.delete(department)
                dismissDeleteDialog()
            }
        }
    }
}
