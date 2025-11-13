package com.nottingham.mynottingham.data.mapper

import com.nottingham.mynottingham.data.model.*
import com.nottingham.mynottingham.data.remote.dto.CourseScheduleDto
import com.nottingham.mynottingham.data.remote.dto.StudentAttendanceDto

object CourseMapper {

    fun toCourse(dto: CourseScheduleDto): Course {
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
            signInStatus = parseSignInStatus(dto.sessionStatus),
            signInUnlockedAt = dto.unlockedAtTimestamp,
            hasStudentSigned = dto.hasStudentSigned ?: false
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
