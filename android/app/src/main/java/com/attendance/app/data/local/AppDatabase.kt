package com.attendance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.attendance.app.data.local.dao.*
import com.attendance.app.data.local.entity.*

@Database(
    entities = [
        DepartmentEntity::class,
        CategoryEntity::class,
        EmployeeEntity::class,
        AttendanceRecordEntity::class,
        SettingEntity::class,
        BackupLogEntity::class,
        UnitLocationEntity::class,
        PunchLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun departmentDao(): DepartmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun settingsDao(): SettingsDao
    abstract fun backupLogDao(): BackupLogDao
    abstract fun unitLocationDao(): UnitLocationDao
    abstract fun punchLogDao(): PunchLogDao

    companion object {
        const val DATABASE_NAME = "attendance.db"

        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('normal_work_hours', '8.0', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('auto_backup_enabled', '1', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('company_name', '', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('backup_time', '23:00', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('kiosk_pin', '', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('kiosk_auto_clear_seconds', '5', datetime('now'))")
                db.execSQL("INSERT OR IGNORE INTO settings (key, value, updated_at) VALUES ('time_adjustment_limit_minutes', '30', datetime('now'))")
            }
        }
    }
}
