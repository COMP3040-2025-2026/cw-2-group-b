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
     */
    suspend fun unlockSession(teacherId: String, courseScheduleId: String, date: String): Result<Unit> {
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
     *
     * 数据合并策略：
     * 1. 从 MySQL 获取所有已注册学生名单（基础数据）
     * 2. 实时监听 Firebase 签到数据（实时更新）
     * 3. 将 Firebase 数据覆盖到 MySQL 名单上，未签到学生保持 ABSENT 状态
     *
     * 优点：
     * - 教师能看到完整班级名册（包括未签到学生）
     * - Firebase 实时更新签到状态（毫秒级响应）
     * - 离线场景自动降级为 Firebase-only 模式
     *
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    fun getStudentAttendanceList(
        teacherId: String,  // Firebase UID
        courseScheduleId: String,
        date: String
    ): Flow<List<StudentAttendance>> = flow {
        // Step 1: 尝试从 MySQL 获取已注册学生名单（一次性查询）
        val enrolledResult = getStudentAttendanceListOnce(teacherId, courseScheduleId, date)
        val enrolledStudents = enrolledResult.getOrNull() ?: emptyList()

        // Step 2: 监听 Firebase 实时签到数据
        firebaseManager.listenToStudentAttendanceList(courseScheduleId, date)
            .collect { firebaseStudents ->
                // Step 3: 合并数据
                if (enrolledStudents.isNotEmpty()) {
                    // 有 MySQL 数据 - 使用合并模式（完整名册 + 实时状态）
                    val mergedList = enrolledStudents.map { enrolled ->
                        // 查找该学生在 Firebase 中的实时签到记录
                        val firebaseRecord = firebaseStudents.find { it.studentId == enrolled.studentId }

                        if (firebaseRecord != null) {
                            // Firebase 有该学生的签到记录，使用 Firebase 的实时数据
                            firebaseRecord
                        } else {
                            // Firebase 还没有该学生的签到记录，保留 MySQL 的默认状态
                            enrolled
                        }
                    }
                    emit(mergedList)
                } else {
                    // MySQL 查询失败或返回空（可能后端离线）- 降级为 Firebase-only 模式
                    // 这种模式下只显示已签到学生，但至少保证实时性
                    emit(firebaseStudents)
                }
            }
    }.flowOn(Dispatchers.IO)

    /**
     * 一次性获取 MySQL 中的已注册学生名单
     * 用于内部实现：提供基础名册数据，供 getStudentAttendanceList() 合并使用
     *
     * 此方法返回从 MySQL 查询的完整班级花名册，包含所有已注册学生及其历史签到状态
     * 通常在教师端用于显示"应到学生"基准线
     *
     * ✅ 修复：courseScheduleId 改为 String，但需要转换为 Long 调用后端
     */
    suspend fun getStudentAttendanceListOnce(
        teacherId: String,  // Firebase UID
        courseScheduleId: String,
        date: String
    ): Result<List<StudentAttendance>> {
        return withContext(Dispatchers.IO) {
            try {
                // 尝试将 Firebase UID 转换为 Long（仅当后端仍在使用时）
                val teacherIdLong = teacherId.toLongOrNull()
                    ?: return@withContext Result.failure(Exception("Backend disabled: teacher ID is Firebase UID"))

                // 尝试将 courseScheduleId 转换为 Long
                val scheduleIdLong = courseScheduleId.toLongOrNull()
                    ?: return@withContext Result.failure(Exception("Backend disabled: schedule ID is Firebase string ID"))

                val response = apiService.getStudentAttendanceList(teacherIdLong, scheduleIdLong, date)
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
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun markAttendance(
        teacherId: String,  // Firebase UID (not used in Firebase operations)
        studentId: String,  // Firebase UID
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

            // 尝试将 studentId 转换为 Long（Firebase Manager 可能仍需要）
            val studentIdLong = studentId.toLongOrNull()
                ?: return@withContext Result.failure(Exception("Invalid student ID format"))

            firebaseManager.markStudentAttendance(
                courseScheduleId = courseScheduleId,
                date = date,
                studentId = studentIdLong,
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

