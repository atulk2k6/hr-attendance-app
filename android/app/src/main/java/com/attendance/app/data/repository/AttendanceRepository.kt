package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.AttendanceWithEmployee
import com.attendance.app.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao
) {

    fun getByDate(date: String): Flow<List<AttendanceWithEmployee>> =
        attendanceDao.getByDate(date)

    fun getByEmployeeAndDateRange(
        employeeId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getByEmployeeAndDateRange(employeeId, startDate, endDate)

    fun getByDateRange(
        startDate: String,
        endDate: String
    ): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getByDateRange(startDate, endDate)

    suspend fun getByEmployeeAndDate(
        employeeId: Long,
        date: String
    ): AttendanceRecordEntity? =
        attendanceDao.getByEmployeeAndDate(employeeId, date)

    suspend fun save(record: AttendanceRecordEntity) {
        attendanceDao.upsert(record)
    }

    suspend fun saveAll(records: List<AttendanceRecordEntity>) {
        attendanceDao.upsertAll(records)
    }

    suspend fun delete(record: AttendanceRecordEntity) {
        attendanceDao.delete(record)
    }

    fun getPresentCount(date: String): Flow<Int> =
        attendanceDao.getPresentCountForDate(date)

    fun getAbsentCount(date: String): Flow<Int> =
        attendanceDao.getAbsentCountForDate(date)

    fun getTotalOt(date: String): Flow<Double?> =
        attendanceDao.getTotalOtForDate(date)
}
