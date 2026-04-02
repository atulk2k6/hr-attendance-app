package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

data class EmployeeWithDetails(
    val id: Long,
    val code: String,
    val empId: String,
    val name: String,
    val fatherName: String,
    val departmentId: Long?,
    val departmentName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val dob: String,
    val doj: String,
    val weeklyOffDay: Int,
    val fd: String,
    val otRrType: String,
    val otRrValue: Double,
    val grossSalary: Double,
    val isActive: Boolean
)

@Dao
interface EmployeeDao {

    @Query("""
        SELECT e.id, e.code, e.emp_id AS empId, e.name, e.father_name AS fatherName,
               e.department_id AS departmentId, d.name AS departmentName,
               e.category_id AS categoryId, c.name AS categoryName,
               e.dob, e.doj, e.weekly_off_day AS weeklyOffDay, e.fd,
               e.ot_rr_type AS otRrType, e.ot_rr_value AS otRrValue,
               e.gross_salary AS grossSalary, e.is_active AS isActive
        FROM employees e
        LEFT JOIN departments d ON e.department_id = d.id
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.is_active = 1
        ORDER BY e.code ASC
    """)
    fun getAllActive(): Flow<List<EmployeeWithDetails>>

    @Query("""
        SELECT e.id, e.code, e.emp_id AS empId, e.name, e.father_name AS fatherName,
               e.department_id AS departmentId, d.name AS departmentName,
               e.category_id AS categoryId, c.name AS categoryName,
               e.dob, e.doj, e.weekly_off_day AS weeklyOffDay, e.fd,
               e.ot_rr_type AS otRrType, e.ot_rr_value AS otRrValue,
               e.gross_salary AS grossSalary, e.is_active AS isActive
        FROM employees e
        LEFT JOIN departments d ON e.department_id = d.id
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.is_active = 1
        AND (:departmentId IS NULL OR e.department_id = :departmentId)
        AND (:categoryId IS NULL OR e.category_id = :categoryId)
        ORDER BY e.code ASC
    """)
    fun getFiltered(departmentId: Long?, categoryId: Long?): Flow<List<EmployeeWithDetails>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE emp_id = :empId")
    suspend fun getByEmpId(empId: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Query("UPDATE employees SET is_active = 0, updated_at = datetime('now') WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("SELECT COUNT(*) FROM employees WHERE is_active = 1")
    fun getActiveCount(): Flow<Int>
}
