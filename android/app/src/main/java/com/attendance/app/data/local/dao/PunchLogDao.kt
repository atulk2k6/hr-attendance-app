package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.PunchLogEntity
import kotlinx.coroutines.flow.Flow

data class PunchLogWithEmployee(
    val id: Long,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val empId: String,
    val date: String,
    val time: String,
    val punchType: String,
    val unitId: Long?,
    val unitName: String?,
    val recordedBy: String,
    val createdAt: String
)

@Dao
interface PunchLogDao {

    @Query("""
        SELECT pl.id, pl.employee_id AS employeeId,
               e.code AS employeeCode, e.name AS employeeName, e.emp_id AS empId,
               pl.date, pl.time, pl.punch_type AS punchType,
               pl.unit_id AS unitId, u.name AS unitName,
               pl.recorded_by AS recordedBy, pl.created_at AS createdAt
        FROM punch_log pl
        JOIN employees e ON pl.employee_id = e.id
        LEFT JOIN unit_locations u ON pl.unit_id = u.id
        WHERE pl.date = :date
        ORDER BY pl.time DESC, pl.id DESC
    """)
    fun getByDate(date: String): Flow<List<PunchLogWithEmployee>>

    @Query("""
        SELECT pl.id, pl.employee_id AS employeeId,
               e.code AS employeeCode, e.name AS employeeName, e.emp_id AS empId,
               pl.date, pl.time, pl.punch_type AS punchType,
               pl.unit_id AS unitId, u.name AS unitName,
               pl.recorded_by AS recordedBy, pl.created_at AS createdAt
        FROM punch_log pl
        JOIN employees e ON pl.employee_id = e.id
        LEFT JOIN unit_locations u ON pl.unit_id = u.id
        WHERE pl.date = :date
        ORDER BY pl.time DESC, pl.id DESC
        LIMIT :limit
    """)
    fun getRecentByDate(date: String, limit: Int = 20): Flow<List<PunchLogWithEmployee>>

    @Query("""
        SELECT * FROM punch_log
        WHERE employee_id = :employeeId AND date = :date
        ORDER BY time ASC
    """)
    suspend fun getByEmployeeAndDate(employeeId: Long, date: String): List<PunchLogEntity>

    @Query("""
        SELECT pl.punch_type FROM punch_log pl
        WHERE pl.employee_id = :employeeId AND pl.date = :date
        ORDER BY pl.time DESC, pl.id DESC
        LIMIT 1
    """)
    suspend fun getLastPunchType(employeeId: Long, date: String): String?

    @Insert
    suspend fun insert(punch: PunchLogEntity): Long

    @Delete
    suspend fun delete(punch: PunchLogEntity)

    @Query("DELETE FROM punch_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT COUNT(*) FROM punch_log
        WHERE date = :date AND punch_type = 'IN'
    """)
    fun getInCountForDate(date: String): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT employee_id) FROM punch_log
        WHERE date = :date AND punch_type = 'IN'
    """)
    fun getUniqueEmployeesInForDate(date: String): Flow<Int>
}
