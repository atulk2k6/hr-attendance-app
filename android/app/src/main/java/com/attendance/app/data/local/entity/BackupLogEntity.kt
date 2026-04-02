package com.attendance.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_log")
data class BackupLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "backup_type")
    val backupType: String, // local, google_drive, manual
    @ColumnInfo(name = "file_path")
    val filePath: String = "",
    val status: String, // success, failed
    @ColumnInfo(name = "error_message")
    val errorMessage: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: String = ""
)
