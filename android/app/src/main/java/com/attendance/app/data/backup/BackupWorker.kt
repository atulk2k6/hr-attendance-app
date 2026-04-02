package com.attendance.app.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.app.data.local.dao.BackupLogDao
import com.attendance.app.data.local.entity.BackupLogEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localBackupManager: LocalBackupManager,
    private val backupLogDao: BackupLogDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "BackupWorker"
        const val WORK_NAME = "daily_backup"
    }

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return try {
            val backupFile = localBackupManager.backup()
            Log.i(TAG, "Backup completed: ${backupFile.absolutePath}")

            backupLogDao.insert(
                BackupLogEntity(
                    backupType = "local",
                    filePath = backupFile.absolutePath,
                    status = "success",
                    createdAt = now
                )
            )
            backupLogDao.pruneOld()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)

            backupLogDao.insert(
                BackupLogEntity(
                    backupType = "local",
                    filePath = "",
                    status = "failed",
                    errorMessage = e.message ?: "Unknown error",
                    createdAt = now
                )
            )

            Result.retry()
        }
    }
}
