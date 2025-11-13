package com.nottingham.mynottingham.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Instatt (Attendance System) Models
 */

// Course with attendance information
@Parcelize
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
    val location: String? = null,
    val courseType: CourseType = CourseType.LECTURE,
    val todayStatus: TodayClassStatus? = null,
    // Sign-in system fields (for teacher/student interaction)
    var signInStatus: SignInStatus = SignInStatus.LOCKED,
    var signInUnlockedAt: Long? = null,  // Timestamp when unlocked
    var hasStudentSigned: Boolean = false  // Whether current student has signed
) : Parcelable

// Day of week enum
@Parcelize
enum class DayOfWeek(val displayName: String) : Parcelable {
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

// Course type enum
@Parcelize
enum class CourseType(val displayName: String) : Parcelable {
    LECTURE("lecture"),
    TUTORIAL("tutorial"),
    COMPUTING("computing"),
    LAB("lab")
}

// Today's class status for visual indicators
@Parcelize
enum class TodayClassStatus : Parcelable {
    UPCOMING,      // Blue line - class hasn't started yet
    IN_PROGRESS,   // Green line - currently in class
    ATTENDED,      // Green check - attended the class
    MISSED         // Red X - didn't attend the class
}

// Sign-in status for attendance system
@Parcelize
enum class SignInStatus : Parcelable {
    LOCKED,        // Not yet available for sign-in (show lock icon)
    UNLOCKED,      // Available for sign-in (show pencil icon for students)
    CLOSED         // Sign-in period ended (lock again)
}

// User role
enum class UserRole {
    STUDENT,
    TEACHER
}

// Student attendance information (for teacher's view of student list)
data class StudentAttendance(
    val studentId: Long,
    val studentName: String,
    val matricNumber: String,
    val email: String,
    val hasAttended: Boolean,
    val attendanceStatus: AttendanceStatus?,
    val checkInTime: String?  // ISO datetime format
)
