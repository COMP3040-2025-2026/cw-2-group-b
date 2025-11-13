package com.nottingham.mynottingham.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CourseScheduleDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("courseId")
    val courseId: Long,

    @SerializedName("courseCode")
    val courseCode: String,

    @SerializedName("courseName")
    val courseName: String,

    @SerializedName("semester")
    val semester: String,

    @SerializedName("dayOfWeek")
    val dayOfWeek: String,

    @SerializedName("startTime")
    val startTime: String,

    @SerializedName("endTime")
    val endTime: String,

    @SerializedName("room")
    val room: String?,

    @SerializedName("building")
    val building: String?,

    @SerializedName("courseType")
    val courseType: String,

    @SerializedName("sessionStatus")
    val sessionStatus: String,

    @SerializedName("hasStudentSigned")
    val hasStudentSigned: Boolean?,

    @SerializedName("unlockedAtTimestamp")
    val unlockedAtTimestamp: Long?,

    @SerializedName("attendedClasses")
    val attendedClasses: Int?,

    @SerializedName("totalSignedClasses")
    val totalSignedClasses: Int?
)

data class UnlockSessionRequest(
    @SerializedName("courseScheduleId")
    val courseScheduleId: Long,

    @SerializedName("sessionDate")
    val sessionDate: String  // ISO format: "2025-11-12"
)

data class SignInRequest(
    @SerializedName("courseScheduleId")
    val courseScheduleId: Long,

    @SerializedName("sessionDate")
    val sessionDate: String  // ISO format: "2025-11-12"
)

data class StudentAttendanceDto(
    @SerializedName("studentId")
    val studentId: Long,

    @SerializedName("studentName")
    val studentName: String,

    @SerializedName("matricNumber")
    val matricNumber: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("hasAttended")
    val hasAttended: Boolean,

    @SerializedName("attendanceStatus")
    val attendanceStatus: String?,  // PRESENT, ABSENT, LATE, EXCUSED, or null

    @SerializedName("checkInTime")
    val checkInTime: String?  // ISO datetime format
)

data class MarkAttendanceRequest(
    @SerializedName("studentId")
    val studentId: Long,

    @SerializedName("courseScheduleId")
    val courseScheduleId: Long,

    @SerializedName("sessionDate")
    val sessionDate: String,  // ISO format: "2025-11-12"

    @SerializedName("status")
    val status: String  // PRESENT, ABSENT, LATE, EXCUSED
)
