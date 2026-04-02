package com.attendance.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class AttendanceCalcResult(
    val status: String,
    val totalHours: Double,
    val otHours: Double
)

object OvertimeCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun calculate(
        inTime: String,
        outTime: String,
        normalWorkHours: Double,
        weeklyOffDay: Int, // 0=Sun..6=Sat
        date: String
    ): AttendanceCalcResult {
        if (inTime.isBlank() || outTime.isBlank()) {
            val localDate = LocalDate.parse(date)
            val isWeeklyOff = dayOfWeekToIndex(localDate.dayOfWeek) == weeklyOffDay
            return AttendanceCalcResult(
                status = if (isWeeklyOff) "W" else "A",
                totalHours = 0.0,
                otHours = 0.0
            )
        }

        val inT = LocalTime.parse(inTime, timeFormatter)
        val outT = LocalTime.parse(outTime, timeFormatter)

        // Calculate total minutes worked
        var totalMinutes = ChronoUnit.MINUTES.between(inT, outT)
        if (totalMinutes < 0) {
            totalMinutes += 24 * 60 // overnight shift
        }

        val totalHours = roundToOneDecimal(totalMinutes / 60.0)

        val localDate = LocalDate.parse(date)
        val isWeeklyOff = dayOfWeekToIndex(localDate.dayOfWeek) == weeklyOffDay

        val status = if (isWeeklyOff) "W" else "P"
        val otHours = if (isWeeklyOff) {
            // All hours on weekly off day count as OT
            totalHours
        } else {
            // OT = hours beyond normal work hours
            roundToOneDecimal(maxOf(0.0, totalHours - normalWorkHours))
        }

        return AttendanceCalcResult(
            status = status,
            totalHours = totalHours,
            otHours = otHours
        )
    }

    private fun dayOfWeekToIndex(dayOfWeek: DayOfWeek): Int {
        // Convert Java DayOfWeek (Mon=1..Sun=7) to our index (Sun=0..Sat=6)
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
    }

    private fun roundToOneDecimal(value: Double): Double {
        return Math.round(value * 10.0) / 10.0
    }
}
