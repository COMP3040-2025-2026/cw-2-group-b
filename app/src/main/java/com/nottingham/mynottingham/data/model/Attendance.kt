package com.nottingham.mynottingham.data.model

import java.util.Date

/**
 * Instatt (Attendance System) Models
 */

// Course with attendance information
data class Course(
    val id: String,
    val courseName: String,
    val courseCode: String,
    val semester: String,  // e.g., "25-26"
    val attendedClasses: Int,
    val totalClasses: Int,
    val dayOfWeek: DayOfWeek,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null
)

// Day of week enum
enum class DayOfWeek(val displayName: String) {
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday")
}

// Attendance record
data class AttendanceRecord(
    val id: String,
    val courseId: String,
    val date: Date,
    val status: AttendanceStatus,
    val markedAt: Date? = null
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED
}

// Weekly schedule
data class WeeklySchedule(
    val dayOfWeek: DayOfWeek,
    val courses: List<Course>
)
