package com.attendance.app.data.local.dao

import androidx.room.*
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

data class AttendanceWithEmployee(
    val id: Long,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val empId: String,
    val departmentName: String?,
    val weeklyOffDay: Int,
    val date: String,
    val inTime: String,
    val outTime: String,
    val status: String,
    val totalHours: Double,
    val otHours: Double,
    val remarks: String
)

@Dao
interface AttendanceDao {

    @Query("""
        SELECT ar.id, ar.employee_id AS employeeId,
               e.code AS employeeCode, e.name AS employeeName, e.emp_id AS empId,
               d.name AS departmentName, e.weekly_off_day AS weeklyOffDay,
               ar.date, ar.in_time AS inTime, ar.out_time AS outTime,
               ar.status, ar.total_hours AS totalHours, ar.ot_hours AS otHours,
               ar.remarks
        FROM attendance_records ar
        JOIN employees e ON ar.employee_id = e.id
        LEFT JOIN departments d ON e.department_id = d.id
        WHERE ar.date = :date
        ORDER BY e.code ASC
    """)
    fun getByDate(date: String): Flow<List<AttendanceWithEmployee>>

    @Query("""
        SELECT * FROM attendance_records
        WHERE employee_id = :employeeId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    fun getByEmployeeAndDateRange(
        employeeId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<AttendanceRecordEntity>>

    @Query("""
        SELECT * FROM attendance_records
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY employee_id, date ASC
    """)
    fun getByDateRange(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employee_id = :employeeId AND date = :date")
    suspend fun getByEmployeeAndDate(employeeId: Long, date: String): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceRecordEntity>)

    @Delete
    suspend fun delete(record: AttendanceRecordEntity)

    @Query("""
        SELECT COUNT(*) FROM attendance_records
        WHERE date = :date AND status = 'P'
    """)
    fun getPresentCountForDate(date: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM attendance_records
        WHERE date = :date AND status = 'A'
    """)
    fun getAbsentCountForDate(date: String): Flow<Int>

    @Query("""
        SELECT SUM(ot_hours) FROM attendance_records
        WHERE date = :date
    """)
    fun getTotalOtForDate(date: String): Flow<Double?>
}
