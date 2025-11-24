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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * InstattRepository - 统一管理签到系统的数据访问
 *
 * 架构设计（已迁移到 Firebase）：
 * - 课程查询（getTeacherCourses, getStudentCourses）：✅ 使用 Firebase Realtime Database
 * - 实时签到操作（unlock/lock/signIn）：✅ 使用 Firebase Realtime Database
 * - 实时监听（学生名单、锁定状态）：✅ 通过 Flow 实现响应式更新
 */
class InstattRepository {

    private val apiService = RetrofitInstance.apiService
    private val firebaseManager = FirebaseInstattManager()
    // ✅ 新增：引入 Firebase 课程仓库
    private val firebaseCourseRepo = FirebaseCourseRepository()

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

    /**
     * ✅ 已迁移：使用 Firebase 获取教师课程
     */
    suspend fun getTeacherCourses(teacherId: String, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            // 直接调用 FirebaseCourseRepository
            firebaseCourseRepo.getTeacherCourses(teacherId, date)
        }
    }

    /**
     * ✅ 已迁移：使用 Firebase 获取学生课程
     */
    suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            // 直接调用 FirebaseCourseRepository
            firebaseCourseRepo.getStudentCourses(studentId, date)
        }
    }

    /**
     * 教师开启签到 - 使用 Firebase 实现实时更新
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     * ✅ 新增：返回是否首次unlock（用于增加课程总数）
     *
     * @return Result<Boolean> - true表示首次unlock，false表示重复unlock
     */
    suspend fun unlockSession(teacherId: String, courseScheduleId: String, date: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            // 直接使用 Firebase，不再调用后端 API
            // 注意：firebaseManager 不需要 teacherId
            firebaseManager.unlockSession(courseScheduleId, date)
        }
    }

    /**
     * 教师关闭签到 - 使用 Firebase 实现实时更新
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun lockSession(teacherId: String, courseScheduleId: String, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // 直接使用 Firebase，不再调用后端 API
            // 注意：firebaseManager 不需要 teacherId
            firebaseManager.lockSession(courseScheduleId, date)
        }
    }

    /**
     * 学生签到 - 使用 Firebase 实现毫秒级响应
     * ✅ 修复：支持 String UID（Firebase UID）
     * @param studentUid Firebase UID (String)
     * @param studentName 学生姓名（从 TokenManager 获取）
     * @param matricNumber 学号（可选）
     * @param email 邮箱（可选）
     */
    suspend fun signIn(
        studentUid: String,  // 🔴 改为 String UID
        courseScheduleId: String,
        date: String,
        studentName: String = "Student", // 默认值，调用时应传入真实姓名
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // 直接写入 Firebase，无需等待后端响应
            firebaseManager.signIn(
                courseScheduleId = courseScheduleId,
                date = date,
                studentUid = studentUid,  // 🔴 传递 String UID
                studentName = studentName,
                matricNumber = matricNumber,
                email = email
            )
        }
    }

    /**
     * 获取学生签到名单 - 返回 Flow 实现实时监听
     * 当学生签到时，教师端会自动收到更新
     *
     * 数据合并策略（已优化为 Firebase 优先）：
     * 1. 从 Firebase 获取所有已注册学生名单（基础数据）
     * 2. 实时监听 Firebase 签到数据（实时更新）
     * 3. 将 Firebase 数据覆盖到学生名单上，未签到学生保持 ABSENT 状态
     *
     * 优点：
     * - 教师能看到完整班级名册（包括未签到学生）
     * - Firebase 实时更新签到状态（毫秒级响应）
     * - 完全不依赖后端 MySQL 服务器
     *
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    fun getStudentAttendanceList(
        teacherId: String,  // Firebase UID
        courseScheduleId: String,
        date: String
    ): Flow<List<StudentAttendance>> = flow {
        // Step 1: 从 course ID 中提取纯课程代码（去掉 schedule number）
        // 例如: "comp2001_1" -> "comp2001"
        val courseId = courseScheduleId.substringBefore("_")

        // Step 2: 尝试从 Firebase 获取已注册学生名单（一次性查询）
        val enrolledResult = firebaseCourseRepo.getEnrolledStudents(courseId)
        val enrolledStudents = enrolledResult.getOrNull() ?: emptyList()

        android.util.Log.d(
            "InstattRepository",
            "📋 Found ${enrolledStudents.size} enrolled students for course $courseId"
        )

        // Step 3: 监听 Firebase 实时签到数据
        firebaseManager.listenToStudentAttendanceList(courseScheduleId, date)
            .collect { firebaseStudents ->
                // Step 4: 合并数据 - 🔴 使用 Firebase UID 匹配，避免重名问题
                if (enrolledStudents.isNotEmpty()) {
                    // 有注册学生数据 - 使用合并模式（完整名册 + 实时状态）
                    val mergedList = enrolledStudents.map { (studentUid, studentName) ->
                        // 🔴 修复：使用 Firebase UID 匹配（唯一标识符）
                        val firebaseRecord = firebaseStudents.find {
                            it.studentId == studentUid  // UID 对 UID 匹配
                        }

                        if (firebaseRecord != null) {
                            // Firebase 有该学生的签到记录，使用 Firebase 的实时数据
                            android.util.Log.d(
                                "InstattRepository",
                                "✅ Matched enrolled student $studentName ($studentUid) with Firebase record"
                            )
                            firebaseRecord
                        } else {
                            // Firebase 还没有该学生的签到记录，显示为 ABSENT
                            android.util.Log.d(
                                "InstattRepository",
                                "⚠️ Student $studentName ($studentUid) enrolled but not signed in yet"
                            )
                            StudentAttendance(
                                studentId = studentUid,  // 使用 Firebase UID
                                studentName = studentName,
                                matricNumber = null,
                                email = null,
                                hasAttended = false,
                                attendanceStatus = AttendanceStatus.ABSENT,
                                checkInTime = null
                            )
                        }
                    }
                    emit(mergedList)
                } else {
                    // 没有注册学生数据 - 降级为 Firebase-only 模式
                    // 这种模式下只显示已签到学生，但至少保证实时性
                    android.util.Log.w(
                        "InstattRepository",
                        "⚠️ No enrolled students found for $courseId, showing only signed-in students"
                    )
                    emit(firebaseStudents)
                }
            }
    }.flowOn(Dispatchers.IO)

    /**
     * ❌ 已废弃：不再使用 MySQL 后端
     *
     * 此方法已被 Firebase 完全替代，所有学生名单数据现在从 Firebase 获取：
     * - enrollments/{courseId}/{studentId} - 学生选课关系
     * - sessions/{scheduleId}_{date}/students/ - 实时签到记录
     *
     * 如需获取学生名单，请使用：
     * - firebaseCourseRepo.getEnrolledStudents(courseId)
     * - firebaseManager.listenToStudentAttendanceList(scheduleId, date)
     */
    @Deprecated("Use Firebase instead", ReplaceWith("firebaseCourseRepo.getEnrolledStudents(courseId)"))
    suspend fun getStudentAttendanceListOnce(
        teacherId: String,
        courseScheduleId: String,
        date: String
    ): Result<List<StudentAttendance>> {
        return Result.failure(
            Exception("Backend disabled: This method is deprecated. Use Firebase directly.")
        )
    }

    /**
     * 教师手动标记学生出勤状态 - 使用 Firebase 实现实时更新
     * 🔴 修复：使用 Firebase UID（String）作为唯一标识符，避免重名问题
     */
    suspend fun markAttendance(
        teacherId: String,  // Firebase UID (not used in Firebase operations)
        studentUid: String,  // 🔴 Firebase UID (String)
        courseScheduleId: String,
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

            // 🔴 直接使用 Firebase UID，不再转换为 Long
            firebaseManager.markStudentAttendance(
                courseScheduleId = courseScheduleId,
                date = date,
                studentUid = studentUid,  // 🔴 传递 String UID
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
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    fun listenToSessionLockStatus(
        courseScheduleId: String,
        date: String
    ): Flow<Boolean> {
        return firebaseManager.listenToSessionLockStatus(courseScheduleId, date)
    }

    /**
     * 检查学生是否已经签到
     * 🔴 修复：使用 Firebase UID（String）作为唯一标识符
     */
    suspend fun hasStudentSignedIn(
        courseScheduleId: String,
        date: String,
        studentUid: String  // 🔴 改为 String UID
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            firebaseManager.hasStudentSignedIn(courseScheduleId, date, studentUid)
        }
    }
}

