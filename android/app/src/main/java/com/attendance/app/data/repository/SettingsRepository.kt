package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.SettingsDao
import com.attendance.app.data.local.entity.SettingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {

    companion object {
        private const val KEY_NORMAL_WORK_HOURS = "normal_work_hours"
        private const val KEY_UNIT_NUMBER = "unit_number"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        const val KEY_KIOSK_PIN = "kiosk_pin"
        const val KEY_KIOSK_AUTO_RESET = "kiosk_auto_clear_seconds"

        private const val DEFAULT_WORK_HOURS = 8.0
        private const val DEFAULT_UNIT_NUMBER = "1"
        private const val DEFAULT_COMPANY_NAME = ""
        private const val DEFAULT_AUTO_BACKUP = true
    }

    suspend fun getValue(key: String): String? =
        settingsDao.getValue(key)

    fun observeValue(key: String): Flow<String?> =
        settingsDao.observeValue(key)

    suspend fun setValue(key: String, value: String) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        settingsDao.set(SettingEntity(key = key, value = value, updatedAt = now))
    }

    suspend fun getNormalWorkHours(): Double =
        settingsDao.getValue(KEY_NORMAL_WORK_HOURS)?.toDoubleOrNull() ?: DEFAULT_WORK_HOURS

    suspend fun setNormalWorkHours(hours: Double) =
        setValue(KEY_NORMAL_WORK_HOURS, hours.toString())

    suspend fun getUnitNumber(): String =
        settingsDao.getValue(KEY_UNIT_NUMBER) ?: DEFAULT_UNIT_NUMBER

    suspend fun setUnitNumber(num: String) =
        setValue(KEY_UNIT_NUMBER, num)

    suspend fun getCompanyName(): String =
        settingsDao.getValue(KEY_COMPANY_NAME) ?: DEFAULT_COMPANY_NAME

    suspend fun setCompanyName(name: String) =
        setValue(KEY_COMPANY_NAME, name)

    suspend fun isAutoBackupEnabled(): Boolean =
        settingsDao.getValue(KEY_AUTO_BACKUP)?.let { it == "1" || it.equals("true", ignoreCase = true) }
            ?: DEFAULT_AUTO_BACKUP

    suspend fun setAutoBackupEnabled(enabled: Boolean) =
        setValue(KEY_AUTO_BACKUP, if (enabled) "1" else "0")

    suspend fun getKioskPin(): String =
        settingsDao.getValue(KEY_KIOSK_PIN) ?: ""

    suspend fun setKioskPin(pin: String) =
        setValue(KEY_KIOSK_PIN, pin)

    suspend fun getKioskAutoResetSeconds(): Int =
        settingsDao.getValue(KEY_KIOSK_AUTO_RESET)?.toIntOrNull() ?: 5

    suspend fun setKioskAutoResetSeconds(seconds: Int) =
        setValue(KEY_KIOSK_AUTO_RESET, seconds.toString())
}
