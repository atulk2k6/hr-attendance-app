package com.attendance.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val companyName: String = "",
    val unitNumber: String = "1",
    val normalWorkHours: String = "8.0",
    val autoBackupEnabled: Boolean = true,
    val backupFolderPath: String = "Documents/AttendanceApp/backups/",
    val appVersion: String = "1.0",
    val kioskPin: String = "",
    val kioskAutoResetSeconds: String = "5",
    val showKioskPin: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val companyName = settingsRepository.getCompanyName()
            val unitNumber = settingsRepository.getUnitNumber()
            val normalWorkHours = settingsRepository.getNormalWorkHours()
            val autoBackup = settingsRepository.isAutoBackupEnabled()
            val kioskPin = settingsRepository.getKioskPin()
            val kioskAutoReset = settingsRepository.getKioskAutoResetSeconds()

            _uiState.update {
                it.copy(
                    companyName = companyName,
                    unitNumber = unitNumber,
                    normalWorkHours = normalWorkHours.toString(),
                    autoBackupEnabled = autoBackup,
                    kioskPin = kioskPin,
                    kioskAutoResetSeconds = kioskAutoReset.toString(),
                    isLoading = false
                )
            }
        }
    }

    fun setCompanyName(name: String) {
        _uiState.update { it.copy(companyName = name) }
        viewModelScope.launch {
            settingsRepository.setCompanyName(name)
        }
    }

    fun setUnitNumber(number: String) {
        _uiState.update { it.copy(unitNumber = number) }
        viewModelScope.launch {
            settingsRepository.setUnitNumber(number)
        }
    }

    fun setNormalWorkHours(hours: String) {
        _uiState.update { it.copy(normalWorkHours = hours) }
        val parsed = hours.toDoubleOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setNormalWorkHours(parsed)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(enabled)
        }
    }

    fun setKioskPin(pin: String) {
        _uiState.update { it.copy(kioskPin = pin) }
        viewModelScope.launch {
            settingsRepository.setKioskPin(pin)
        }
    }

    fun setKioskAutoResetSeconds(seconds: String) {
        _uiState.update { it.copy(kioskAutoResetSeconds = seconds) }
        val parsed = seconds.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setKioskAutoResetSeconds(parsed)
        }
    }

    fun toggleKioskPinVisibility() {
        _uiState.update { it.copy(showKioskPin = !it.showKioskPin) }
    }
}
