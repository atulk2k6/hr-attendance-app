package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.DepartmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {

    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAll(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: Long): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(department: DepartmentEntity): Long

    @Update
    suspend fun update(department: DepartmentEntity)

    @Delete
    suspend fun delete(department: DepartmentEntity)

    @Query("SELECT COUNT(*) FROM employees WHERE department_id = :departmentId AND is_active = 1")
    suspend fun getEmployeeCount(departmentId: Long): Int
}
