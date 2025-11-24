package com.nottingham.mynottingham.data.repository

import com.nottingham.mynottingham.data.firebase.FirebaseInstattManager
import com.nottingham.mynottingham.data.mapper.CourseMapper
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.StudentAttendance
import com.nottingham.mynottingham.data.model.SystemTime
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.MarkAttendanceRequest
import com.nottingham.mynottingham.data.remote.dto.SignInRequest
import com.nottingham.mynottingham.data.remote.dto.UnlockSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * InstattRepository - 统一管理签到系统的数据访问
 *
 * 架构设计：
 * - 课程查询（getTeacherCourses, getStudentCourses）：继续使用 HTTP + MySQL
 * - 实时签到操作（unlock/lock/signIn）：使用 Firebase Realtime Database
 * - 实时监听（学生名单、锁定状态）：通过 Flow 实现响应式更新
 */
class InstattRepository {

    private val apiService = RetrofitInstance.apiService
    private val firebaseManager = FirebaseInstattManager()

    suspend fun getSystemTime(): Result<SystemTime> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSystemTime()
                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data
                    if (dto != null) {
                        Result.success(SystemTime.fromDto(dto))
                    } else {
                        Result.failure(Exception("System time data is null"))
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to get system time"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getTeacherCourses(teacherId: Long, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTeacherCourses(teacherId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val courses = response.body()?.data?.map { CourseMapper.toCourse(it) } ?: emptyList()
                    Result.success(courses)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load courses"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStudentCourses(studentId: Long, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentCourses(studentId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val courses = response.body()?.data?.map { CourseMapper.toCourse(it) } ?: emptyList()
                    Result.success(courses)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load courses"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 教师开启签到 - 使用 Firebase 实现实时更新
     */
    suspend fun unlockSession(teacherId: Long, courseScheduleId: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // 直接使用 Firebase，不再调用后端 API
            firebaseManager.unlockSession(courseScheduleId, date)
        }
    }

    /**
     * 教师关闭签到 - 使用 Firebase 实现实时更新
     */
    suspend fun lockSession(teacherId: Long, courseScheduleId: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // 直接使用 Firebase，不再调用后端 API
            firebaseManager.lockSession(courseScheduleId, date)
        }
    }

    /**
     * 学生签到 - 使用 Firebase 实现毫秒级响应
     * @param studentName 学生姓名（从 TokenManager 获取）
     * @param matricNumber 学号（可选）
     * @param email 邮箱（可选）
     */
    suspend fun signIn(
        studentId: Long,
        courseScheduleId: Long,
        date: String,
        studentName: String = "Student $studentId", // 默认值，调用时应传入真实姓名
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // 直接写入 Firebase，无需等待后端响应
            firebaseManager.signIn(
                courseScheduleId = courseScheduleId,
                date = date,
                studentId = studentId,
                studentName = studentName,
                matricNumber = matricNumber,
                email = email
            )
        }
    }

    /**
     * 获取学生签到名单 - 返回 Flow 实现实时监听
     * 当学生签到时，教师端会自动收到更新
     */
    fun getStudentAttendanceList(
        teacherId: Long,
        courseScheduleId: Long,
        date: String
    ): Flow<List<StudentAttendance>> {
        // 返回 Firebase 的实时监听 Flow
        return firebaseManager.listenToStudentAttendanceList(courseScheduleId, date)
    }

    /**
     * 兼容方法：一次性获取学生名单（非实时）
     * 保留此方法以防某些场景需要一次性查询
     */
    @Deprecated(
        "Use getStudentAttendanceList() Flow version for real-time updates",
        ReplaceWith("getStudentAttendanceList(teacherId, courseScheduleId, date)")
    )
    suspend fun getStudentAttendanceListOnce(
        teacherId: Long,
        courseScheduleId: Long,
        date: String
    ): Result<List<StudentAttendance>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentAttendanceList(teacherId, courseScheduleId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val students = response.body()?.data?.map { CourseMapper.toStudentAttendance(it) } ?: emptyList()
                    Result.success(students)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load student list"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 教师手动标记学生出勤状态 - 使用 Firebase 实现实时更新
     */
    suspend fun markAttendance(
        teacherId: Long,
        studentId: Long,
        courseScheduleId: Long,
        date: String,
        status: String,
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val attendanceStatus = try {
                AttendanceStatus.valueOf(status)
            } catch (e: Exception) {
                AttendanceStatus.ABSENT
            }

            firebaseManager.markStudentAttendance(
                courseScheduleId = courseScheduleId,
                date = date,
                studentId = studentId,
                status = attendanceStatus,
                studentName = studentName,
                matricNumber = matricNumber,
                email = email
            )
        }
    }

    /**
     * 学生端：监听 session 的锁定状态（实时）
     * 当教师 unlock session 时，学生端的签到按钮立即变亮
     */
    fun listenToSessionLockStatus(
        courseScheduleId: Long,
        date: String
    ): Flow<Boolean> {
        return firebaseManager.listenToSessionLockStatus(courseScheduleId, date)
    }

    /**
     * 检查学生是否已经签到
     */
    suspend fun hasStudentSignedIn(
        courseScheduleId: Long,
        date: String,
        studentId: Long
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            firebaseManager.hasStudentSignedIn(courseScheduleId, date, studentId)
        }
    }
}

