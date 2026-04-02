package com.attendance.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "punch_log",
    indices = [
        Index(value = ["employee_id", "date"]),
        Index(value = ["date"]),
        Index(value = ["unit_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnitLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["unit_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PunchLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "employee_id")
    val employeeId: Long,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    @ColumnInfo(name = "punch_type")
    val punchType: String, // "IN" or "OUT"
    @ColumnInfo(name = "unit_id")
    val unitId: Long? = null,
    @ColumnInfo(name = "recorded_by")
    val recordedBy: String = "supervisor", // "supervisor" or "self"
    @ColumnInfo(name = "created_at")
    val createdAt: String = ""
)
