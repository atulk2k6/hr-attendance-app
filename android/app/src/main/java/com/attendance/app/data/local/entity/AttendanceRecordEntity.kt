package com.attendance.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["employee_id", "date"], unique = true),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "employee_id")
    val employeeId: Long,
    val date: String, // YYYY-MM-DD
    @ColumnInfo(name = "in_time")
    val inTime: String = "",
    @ColumnInfo(name = "out_time")
    val outTime: String = "",
    val status: String = "P", // P, A, W, CO
    @ColumnInfo(name = "total_hours")
    val totalHours: Double = 0.0,
    @ColumnInfo(name = "ot_hours")
    val otHours: Double = 0.0,
    val remarks: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
)
