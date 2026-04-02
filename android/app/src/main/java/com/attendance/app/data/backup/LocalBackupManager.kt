package com.attendance.app.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.attendance.app.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    companion object {
        private const val BACKUP_FOLDER = "AttendanceApp/backups"
        private const val BACKUP_PREFIX = "attendance_"
        private const val BACKUP_EXTENSION = ".db"
    }

    fun getBackupDir(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(documentsDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    fun backup(): File {
        // Close open connections so the WAL is checkpointed
        database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFileName = "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION"
        val backupFile = File(getBackupDir(), backupFileName)

        FileInputStream(dbFile).use { input ->
            FileOutputStream(backupFile).use { output ->
                input.copyTo(output)
            }
        }

        return backupFile
    }

    fun getBackupFiles(): List<File> {
        val backupDir = getBackupDir()
        return backupDir.listFiles { file ->
            file.isFile && file.name.endsWith(BACKUP_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean {
        return file.exists() && file.delete()
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun restoreBackup(backupFile: File): Result<Unit> {
        return try {
            // Checkpoint WAL and close the database before overwriting the file
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            database.close()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
