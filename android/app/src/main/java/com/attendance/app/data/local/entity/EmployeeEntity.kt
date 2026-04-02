package com.attendance.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["emp_id"], unique = true),
        Index(value = ["code"], unique = true),
        Index(value = ["department_id"]),
        Index(value = ["category_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["department_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    @ColumnInfo(name = "emp_id")
    val empId: String,
    val name: String,
    @ColumnInfo(name = "father_name")
    val fatherName: String = "",
    @ColumnInfo(name = "department_id")
    val departmentId: Long? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    val dob: String = "",
    val doj: String = "",
    @ColumnInfo(name = "weekly_off_day")
    val weeklyOffDay: Int = 0, // 0=Sun..6=Sat
    val fd: String = "",
    @ColumnInfo(name = "ot_rr_type")
    val otRrType: String = "COMP",
    @ColumnInfo(name = "ot_rr_value")
    val otRrValue: Double = 0.0,
    @ColumnInfo(name = "gross_salary")
    val grossSalary: Double = 0.0,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
)
