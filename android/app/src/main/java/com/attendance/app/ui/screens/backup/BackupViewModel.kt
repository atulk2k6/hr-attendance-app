package com.attendance.app.ui.screens.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.backup.LocalBackupManager
import com.attendance.app.data.local.AppDatabase
import com.attendance.app.data.local.dao.BackupLogDao
import com.attendance.app.data.local.entity.BackupLogEntity
import com.attendance.app.util.DateUtils
import com.attendance.app.util.FileShareUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val isBackingUp: Boolean = false,
    val backupFiles: List<File> = emptyList(),
    val snackbarMessage: String? = null,
    val showRestoreDialog: Boolean = false,
    val restoreTargetFile: File? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val localBackupManager: LocalBackupManager,
    private val backupLogDao: BackupLogDao,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    val backupLogs: StateFlow<List<BackupLogEntity>> = backupLogDao.getRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshFiles()
    }

    fun performBackup() {
        if (_uiState.value.isBackingUp) return

        _uiState.update { it.copy(isBackingUp = true) }

        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    localBackupManager.backup()
                }
                backupLogDao.insert(
                    BackupLogEntity(
                        backupType = "manual",
                        filePath = file.absolutePath,
                        status = "success",
                        createdAt = DateUtils.now()
                    )
                )
                backupLogDao.pruneOld()
                refreshFiles()
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        snackbarMessage = "Backup created successfully"
                    )
                }
            } catch (e: Exception) {
                backupLogDao.insert(
                    BackupLogEntity(
                        backupType = "manual",
                        filePath = "",
                        status = "failed",
                        errorMessage = e.message ?: "Unknown error",
                        createdAt = DateUtils.now()
                    )
                )
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        snackbarMessage = "Backup failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                localBackupManager.getBackupFiles()
            }
            _uiState.update { it.copy(backupFiles = files) }
        }
    }

    fun deleteBackup(file: File) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                localBackupManager.deleteBackup(file)
            }
            if (deleted) {
                refreshFiles()
                _uiState.update { it.copy(snackbarMessage = "Backup deleted") }
            } else {
                _uiState.update { it.copy(snackbarMessage = "Failed to delete backup") }
            }
        }
    }

    fun shareFile(context: Context, file: File) {
        FileShareUtil.shareFile(context, file, "application/x-sqlite3")
    }

    fun shareCurrentDatabase(context: Context) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (dbFile.exists()) {
            FileShareUtil.shareDatabase(context, dbFile)
        } else {
            _uiState.update { it.copy(snackbarMessage = "Database file not found") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onRestoreClicked(file: File) {
        _uiState.update { it.copy(showRestoreDialog = true, restoreTargetFile = file) }
    }

    fun confirmRestore() {
        val file = _uiState.value.restoreTargetFile ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                localBackupManager.restoreBackup(file)
            }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        showRestoreDialog = false,
                        restoreTargetFile = null,
                        snackbarMessage = "Restore complete. Please restart the app."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showRestoreDialog = false,
                        restoreTargetFile = null,
                        snackbarMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false, restoreTargetFile = null) }
    }
}
