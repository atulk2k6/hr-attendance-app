package com.attendance.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.CategoryDao
import com.attendance.app.data.local.entity.CategoryEntity
import com.attendance.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val employeeCounts: Map<Long, Int> = emptyMap(),
    val showDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val dialogText: String = "",
    val dialogError: String? = null,
    val showDeleteDialog: Boolean = false,
    val categoryToDelete: CategoryEntity? = null,
    val deleteError: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                val counts = mutableMapOf<Long, Int>()
                for (cat in categories) {
                    counts[cat.id] = categoryDao.getEmployeeCount(cat.id)
                }
                _uiState.update {
                    it.copy(categories = categories, employeeCounts = counts)
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingCategory = null,
                dialogText = "",
                dialogError = null
            )
        }
    }

    fun showEditDialog(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingCategory = category,
                dialogText = category.name,
                dialogError = null
            )
        }
    }

    fun onDialogTextChanged(text: String) {
        _uiState.update { it.copy(dialogText = text, dialogError = null) }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(showDialog = false, editingCategory = null, dialogText = "", dialogError = null)
        }
    }

    fun saveCategory() {
        val name = _uiState.value.dialogText.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(dialogError = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val now = DateUtils.now()
            val editing = _uiState.value.editingCategory
            if (editing != null) {
                categoryDao.update(editing.copy(name = name, updatedAt = now))
            } else {
                categoryDao.insert(CategoryEntity(name = name, createdAt = now, updatedAt = now))
            }
            dismissDialog()
        }
    }

    fun showDeleteDialog(category: CategoryEntity) {
        _uiState.update {
            it.copy(showDeleteDialog = true, categoryToDelete = category, deleteError = null)
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(showDeleteDialog = false, categoryToDelete = null, deleteError = null)
        }
    }

    fun confirmDelete() {
        val category = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            val count = categoryDao.getEmployeeCount(category.id)
            if (count > 0) {
                _uiState.update {
                    it.copy(deleteError = "Cannot delete: $count employee(s) assigned to this category")
                }
            } else {
                categoryDao.delete(category)
                dismissDeleteDialog()
            }
        }
    }
}
