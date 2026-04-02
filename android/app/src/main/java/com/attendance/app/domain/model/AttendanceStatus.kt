package com.attendance.app.domain.model

enum class AttendanceStatus(val code: String, val label: String) {
    PRESENT("P", "Present"),
    ABSENT("A", "Absent"),
    WEEKLY_OFF("W", "Weekly Off"),
    COMP_OFF("CO", "Comp Off");

    companion object {
        fun fromCode(code: String): AttendanceStatus {
            return entries.find { it.code == code } ?: PRESENT
        }
    }
}

enum class OtRrType(val code: String, val label: String) {
    COMP("COMP", "Compensatory"),
    RR("RR", "Regular Rate");

    companion object {
        fun fromCode(code: String): OtRrType {
            return entries.find { it.code == code } ?: COMP
        }
    }
}

enum class WeekDay(val dayIndex: Int, val label: String, val shortLabel: String) {
    SUNDAY(0, "Sunday", "SUN"),
    MONDAY(1, "Monday", "MON"),
    TUESDAY(2, "Tuesday", "TUE"),
    WEDNESDAY(3, "Wednesday", "WED"),
    THURSDAY(4, "Thursday", "THU"),
    FRIDAY(5, "Friday", "FRI"),
    SATURDAY(6, "Saturday", "SAT");

    companion object {
        fun fromIndex(index: Int): WeekDay {
            return entries.find { it.dayIndex == index } ?: SUNDAY
        }
    }
}
