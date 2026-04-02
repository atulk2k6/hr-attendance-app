package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.EmployeeDao
import com.attendance.app.data.local.dao.EmployeeWithDetails
import com.attendance.app.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeRepository @Inject constructor(
    private val employeeDao: EmployeeDao
) {

    fun getAllActive(): Flow<List<EmployeeWithDetails>> =
        employeeDao.getAllActive()

    fun getFiltered(departmentId: Long?, categoryId: Long?): Flow<List<EmployeeWithDetails>> =
        employeeDao.getFiltered(departmentId, categoryId)

    suspend fun getById(id: Long): EmployeeEntity? =
        employeeDao.getById(id)

    suspend fun save(entity: EmployeeEntity) {
        if (entity.id == 0L) {
            employeeDao.insert(entity)
        } else {
            employeeDao.update(entity)
        }
    }

    suspend fun deactivate(id: Long) =
        employeeDao.deactivate(id)

    fun getActiveCount(): Flow<Int> =
        employeeDao.getActiveCount()
}
