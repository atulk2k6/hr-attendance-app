package com.attendance.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH)

    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun today(): String =
        LocalDate.now().format(ISO_DATE)

    fun now(): String =
        LocalDateTime.now().format(ISO_DATETIME)

    fun formatForDisplay(isoDate: String): String {
        val date = LocalDate.parse(isoDate, ISO_DATE)
        return date.format(DISPLAY_DATE)
    }

    fun parseDisplayDate(display: String): String {
        val date = LocalDate.parse(display, DISPLAY_DATE)
        return date.format(ISO_DATE)
    }

    fun getMonthName(month: Int): String {
        require(month in 1..12) { "Month must be between 1 and 12, got $month" }
        return MONTH_NAMES[month - 1]
    }

    fun getDaysInMonth(year: Int, month: Int): Int =
        YearMonth.of(year, month).lengthOfMonth()

    fun getDayOfWeekIndex(date: String): Int {
        val localDate = LocalDate.parse(date, ISO_DATE)
        // Convert from DayOfWeek (Mon=1..Sun=7) to 0=Sun..6=Sat
        return when (localDate.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
    }

    fun getMonthDateRange(year: Int, month: Int): Pair<String, String> {
        val yearMonth = YearMonth.of(year, month)
        val first = yearMonth.atDay(1).format(ISO_DATE)
        val last = yearMonth.atEndOfMonth().format(ISO_DATE)
        return Pair(first, last)
    }
}
