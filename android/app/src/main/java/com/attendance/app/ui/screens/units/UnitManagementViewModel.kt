package com.attendance.app.ui.screens.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.dao.UnitLocationDao
import com.attendance.app.data.local.entity.UnitLocationEntity
import com.attendance.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitDialogState(
    val name: String = "",
    val unitNumber: String = "",
    val address: String = ""
)

data class UnitManagementUiState(
    val units: List<UnitLocationEntity> = emptyList(),
    val showDialog: Boolean = false,
    val editingUnit: UnitLocationEntity? = null,
    val dialogState: UnitDialogState = UnitDialogState(),
    val dialogError: String? = null,
    val showDeactivateDialog: Boolean = false,
    val unitToDeactivate: UnitLocationEntity? = null
)

@HiltViewModel
class UnitManagementViewModel @Inject constructor(
    private val unitLocationDao: UnitLocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitManagementUiState())
    val uiState: StateFlow<UnitManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            unitLocationDao.getAll().collect { units ->
                _uiState.update { it.copy(units = units) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingUnit = null,
                dialogState = UnitDialogState(),
                dialogError = null
            )
        }
    }

    fun showEditDialog(unit: UnitLocationEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingUnit = unit,
                dialogState = UnitDialogState(
                    name = unit.name,
                    unitNumber = unit.unitNumber,
                    address = unit.address
                ),
                dialogError = null
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                dialogState = it.dialogState.copy(name = name),
                dialogError = null
            )
        }
    }

    fun onUnitNumberChanged(unitNumber: String) {
        _uiState.update {
            it.copy(
                dialogState = it.dialogState.copy(unitNumber = unitNumber),
                dialogError = null
            )
        }
    }

    fun onAddressChanged(address: String) {
        _uiState.update {
            it.copy(
                dialogState = it.dialogState.copy(address = address),
                dialogError = null
            )
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showDialog = false,
                editingUnit = null,
                dialogState = UnitDialogState(),
                dialogError = null
            )
        }
    }

    fun saveUnit() {
        val dialog = _uiState.value.dialogState
        val name = dialog.name.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(dialogError = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val now = DateUtils.now()
            val editing = _uiState.value.editingUnit

            try {
                if (editing != null) {
                    unitLocationDao.update(
                        editing.copy(
                            name = name,
                            unitNumber = dialog.unitNumber.trim(),
                            address = dialog.address.trim(),
                            updatedAt = now
                        )
                    )
                } else {
                    unitLocationDao.insert(
                        UnitLocationEntity(
                            name = name,
                            unitNumber = dialog.unitNumber.trim(),
                            address = dialog.address.trim(),
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                dismissDialog()
            } catch (e: Exception) {
                val msg = if (e.message?.contains("UNIQUE", ignoreCase = true) == true) {
                    "A unit with this name already exists"
                } else {
                    "Error saving unit: ${e.message}"
                }
                _uiState.update { it.copy(dialogError = msg) }
            }
        }
    }

    fun showDeactivateDialog(unit: UnitLocationEntity) {
        _uiState.update {
            it.copy(showDeactivateDialog = true, unitToDeactivate = unit)
        }
    }

    fun dismissDeactivateDialog() {
        _uiState.update {
            it.copy(showDeactivateDialog = false, unitToDeactivate = null)
        }
    }

    fun confirmDeactivate() {
        val unit = _uiState.value.unitToDeactivate ?: return
        viewModelScope.launch {
            unitLocationDao.deactivate(unit.id)
            dismissDeactivateDialog()
        }
    }
}
