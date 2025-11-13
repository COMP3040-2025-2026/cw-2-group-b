package com.nottingham.mynottingham.data.mapper

import com.nottingham.mynottingham.data.model.*
import com.nottingham.mynottingham.data.remote.dto.CourseScheduleDto
import com.nottingham.mynottingham.data.remote.dto.StudentAttendanceDto

object CourseMapper {

    fun toCourse(dto: CourseScheduleDto): Course {
        val signInStatus = parseSignInStatus(dto.sessionStatus)
        val hasStudentSigned = dto.hasStudentSigned ?: false

        // Debug logging
        android.util.Log.d("CourseMapper", "Converting DTO: courseCode=${dto.courseCode}, " +
                "sessionStatus=${dto.sessionStatus}, hasStudentSigned=${dto.hasStudentSigned}")

        // Determine today's status for visual indicators
        val todayStatus = when {
            hasStudentSigned -> TodayClassStatus.ATTENDED  // Student has signed in or teacher marked as present
            signInStatus == SignInStatus.UNLOCKED -> TodayClassStatus.IN_PROGRESS  // Sign-in is open but student hasn't signed yet
            else -> null  // Show default locked state
        }

        android.util.Log.d("CourseMapper", "Result: signInStatus=$signInStatus, " +
                "hasStudentSigned=$hasStudentSigned, todayStatus=$todayStatus")

        return Course(
            id = dto.id.toString(),
            courseName = dto.courseName,
            courseCode = dto.courseCode,
            semester = dto.semester,
            attendedClasses = dto.attendedClasses ?: 0,
            totalClasses = dto.totalSignedClasses ?: 0,  // Only count classes where sign-in was opened
            dayOfWeek = parseDayOfWeek(dto.dayOfWeek),
            startTime = dto.startTime,
            endTime = dto.endTime,
            location = dto.room ?: dto.building ?: "TBA",
            courseType = parseCourseType(dto.courseType),
            todayStatus = todayStatus,
            signInStatus = signInStatus,
            signInUnlockedAt = dto.unlockedAtTimestamp,
            hasStudentSigned = hasStudentSigned
        )
    }

    private fun parseDayOfWeek(dayString: String): DayOfWeek {
        return try {
            DayOfWeek.valueOf(dayString.uppercase())
        } catch (e: Exception) {
            DayOfWeek.MONDAY
        }
    }

    private fun parseCourseType(typeString: String): CourseType {
        return try {
            CourseType.valueOf(typeString.uppercase())
        } catch (e: Exception) {
            CourseType.LECTURE
        }
    }

    private fun parseSignInStatus(statusString: String): SignInStatus {
        return try {
            SignInStatus.valueOf(statusString.uppercase())
        } catch (e: Exception) {
            SignInStatus.LOCKED
        }
    }

    fun toStudentAttendance(dto: StudentAttendanceDto): StudentAttendance {
        return StudentAttendance(
            studentId = dto.studentId,
            studentName = dto.studentName,
            matricNumber = dto.matricNumber,
            email = dto.email,
            hasAttended = dto.hasAttended,
            attendanceStatus = dto.attendanceStatus?.let { parseAttendanceStatus(it) },
            checkInTime = dto.checkInTime
        )
    }

    private fun parseAttendanceStatus(statusString: String): AttendanceStatus {
        return try {
            AttendanceStatus.valueOf(statusString.uppercase())
        } catch (e: Exception) {
            AttendanceStatus.ABSENT
        }
    }
}
