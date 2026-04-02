package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.BackupLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupLogDao {

    @Query("SELECT * FROM backup_log ORDER BY created_at DESC LIMIT 50")
    fun getRecent(): Flow<List<BackupLogEntity>>

    @Query("SELECT * FROM backup_log ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatest(): BackupLogEntity?

    @Insert
    suspend fun insert(log: BackupLogEntity): Long

    @Query("DELETE FROM backup_log WHERE id NOT IN (SELECT id FROM backup_log ORDER BY created_at DESC LIMIT 100)")
    suspend fun pruneOld()
}
