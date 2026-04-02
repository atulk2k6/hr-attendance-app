package com.attendance.app.di

import android.content.Context
import androidx.room.Room
import com.attendance.app.data.local.AppDatabase
import com.attendance.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addCallback(AppDatabase.SEED_CALLBACK)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDepartmentDao(db: AppDatabase): DepartmentDao = db.departmentDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideEmployeeDao(db: AppDatabase): EmployeeDao = db.employeeDao()

    @Provides
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideBackupLogDao(db: AppDatabase): BackupLogDao = db.backupLogDao()

    @Provides
    fun provideUnitLocationDao(db: AppDatabase): UnitLocationDao = db.unitLocationDao()

    @Provides
    fun providePunchLogDao(db: AppDatabase): PunchLogDao = db.punchLogDao()
}
