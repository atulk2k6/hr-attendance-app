package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.UnitLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitLocationDao {

    @Query("SELECT * FROM unit_locations WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<UnitLocationEntity>>

    @Query("SELECT * FROM unit_locations ORDER BY name ASC")
    fun getAll(): Flow<List<UnitLocationEntity>>

    @Query("SELECT * FROM unit_locations WHERE id = :id")
    suspend fun getById(id: Long): UnitLocationEntity?

    @Query("SELECT COUNT(*) FROM unit_locations WHERE is_active = 1")
    fun getActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: UnitLocationEntity): Long

    @Update
    suspend fun update(unit: UnitLocationEntity)

    @Query("UPDATE unit_locations SET is_active = 0, updated_at = datetime('now') WHERE id = :id")
    suspend fun deactivate(id: Long)
}
