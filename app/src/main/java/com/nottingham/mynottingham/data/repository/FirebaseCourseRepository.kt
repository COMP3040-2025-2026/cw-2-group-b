package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Firebase Course Repository
 *
 * 直接从 Firebase Realtime Database 读取课程和排课数据
 * 不再依赖 Spring Boot 后端 API
 *
 * Firebase 数据结构：
 * - courses/{courseId}: 课程基本信息
 * - schedules/{scheduleId}: 排课信息
 * - enrollments/{courseId}/{studentId}: 学生选课关系
 * - student_courses/{studentId}/{courseId}: 反向索引
 */
class FirebaseCourseRepository {

    // ⚠️ 重要：必须指定数据库 URL，因为数据库在 asia-southeast1 区域
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val coursesRef: DatabaseReference = database.getReference("courses")
    private val schedulesRef: DatabaseReference = database.getReference("schedules")
    private val enrollmentsRef: DatabaseReference = database.getReference("enrollments")
    private val studentCoursesRef: DatabaseReference = database.getReference("student_courses")

    /**
     * 获取学生的所有课程（包含今日排课和签到状态）
     * @param studentId 学生Firebase UID
     * @param date 日期 (格式: yyyy-MM-dd)
     * @return Result<List<Course>> 课程列表（包含签到状态和统计数据）
     */
    suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
        return try {
            android.util.Log.d("FirebaseCourseRepo", "🔍 Fetching courses for studentId: $studentId")
            android.util.Log.d("FirebaseCourseRepo", "📅 Date: $date")

            // 1. 获取学生选修的课程ID列表
            val studentCoursesSnapshot = studentCoursesRef.child(studentId).get().await()

            val courseIds = studentCoursesSnapshot.children.mapNotNull { it.key }

            android.util.Log.d("FirebaseCourseRepo", "📚 Found ${courseIds.size} courses: $courseIds")

            if (courseIds.isEmpty()) {
                android.util.Log.w("FirebaseCourseRepo", "⚠️ No courses found for student: $studentId")
                return Result.success(emptyList())
            }

            // 2. 获取每门课程的详细信息（包含签到状态）
            val courses = mutableListOf<Course>()

            for (courseId in courseIds) {
                try {
                    android.util.Log.d("FirebaseCourseRepo", "📖 Loading course: $courseId")
                    val course = getCourseWithSchedulesAndAttendance(courseId, date, studentId)
                    android.util.Log.d("FirebaseCourseRepo", "✅ Loaded ${course.size} schedules for $courseId")
                    courses.addAll(course)
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseCourseRepo", "❌ Failed to load course $courseId: ${e.message}", e)
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "✅ Total courses loaded: ${courses.size}")
            Result.success(courses)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "❌ Error fetching student courses: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取教师的所有课程
     * @param teacherId 教师用户名 (如 "teacher1")
     * @param date 日期
     * @return Result<List<Course>> 课程列表
     */
    suspend fun getTeacherCourses(teacherId: String, date: String): Result<List<Course>> {
        return try {
            // 查询 teacherId 字段匹配的所有课程
            val coursesSnapshot = coursesRef.orderByChild("teacherId").equalTo(teacherId).get().await()

            val courses = mutableListOf<Course>()

            for (courseSnapshot in coursesSnapshot.children) {
                val courseId = courseSnapshot.key ?: continue
                try {
                    val course = getCourseWithSchedules(courseId, date)
                    courses.addAll(course)
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseCourseRepo", "Failed to load course $courseId: ${e.message}")
                }
            }

            Result.success(courses)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error fetching teacher courses: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取课程及其所有排课（包含学生签到状态和统计数据）
     * @param courseId 课程ID (如 "comp3040")
     * @param date 日期 (用于判断今日状态和过滤星期几)
     * @param studentId 学生Firebase UID（用于检查签到状态）
     * @return List<Course> 一门课程在指定日期的排课
     */
    private suspend fun getCourseWithSchedulesAndAttendance(
        courseId: String,
        date: String,
        studentId: String
    ): List<Course> {
        // 1. 获取课程基本信息
        val courseSnapshot = coursesRef.child(courseId).get().await()
        if (!courseSnapshot.exists()) {
            return emptyList()
        }

        val code = courseSnapshot.child("code").getValue(String::class.java) ?: ""
        val name = courseSnapshot.child("name").getValue(String::class.java) ?: ""
        val semester = courseSnapshot.child("semester").getValue(String::class.java) ?: "25-26"

        // 2. 从日期解析出星期几
        val targetDayOfWeek = getDayOfWeekFromDate(date)

        // 3. 查询该课程的所有排课
        val schedulesSnapshot = schedulesRef.orderByChild("courseId").equalTo(courseId).get().await()

        val courses = mutableListOf<Course>()

        for (scheduleSnapshot in schedulesSnapshot.children) {
            val scheduleId = scheduleSnapshot.key ?: continue

            val dayOfWeek = scheduleSnapshot.child("dayOfWeek").getValue(String::class.java) ?: continue

            // ✅ 只添加匹配当天星期的课程
            if (dayOfWeek.uppercase() != targetDayOfWeek.name) {
                continue
            }

            val startTime = scheduleSnapshot.child("startTime").getValue(String::class.java) ?: "00:00"
            val endTime = scheduleSnapshot.child("endTime").getValue(String::class.java) ?: "00:00"
            val room = scheduleSnapshot.child("room").getValue(String::class.java) ?: ""
            val type = scheduleSnapshot.child("type").getValue(String::class.java) ?: "LECTURE"

            // ✅ 从 Firebase sessions 查询真实的签到状态
            val sessionKey = "${scheduleId}_$date"
            val sessionSnapshot = database.getReference("sessions").child(sessionKey).get().await()
            val isLocked = sessionSnapshot.child("isLocked").getValue(Boolean::class.java) ?: true

            // ✅ 检查学生在此 session 中的状态
            val studentRecord = sessionSnapshot.child("students").child(studentId)
            val hasStudentRecord = studentRecord.exists()
            val studentStatus = if (hasStudentRecord) {
                studentRecord.child("status").getValue(String::class.java) ?: "NOT_MARKED"
            } else {
                "NOT_MARKED"  // 学生初始状态为"未标记"
            }

            // ✅ 检查 session 是否曾经开放过签到
            val hasFirstUnlock = sessionSnapshot.hasChild("firstUnlockTime")

            // ✅ 确定签到状态和今日状态
            val signInStatus: SignInStatus
            val todayStatus: TodayClassStatus
            val hasStudentSigned: Boolean

            when {
                studentStatus == "PRESENT" -> {
                    // 学生已签到或被标记为出席 - 显示绿色勾
                    signInStatus = SignInStatus.SIGNED
                    todayStatus = TodayClassStatus.ATTENDED
                    hasStudentSigned = true
                    android.util.Log.d("FirebaseCourseRepo", "✅ Student PRESENT for $scheduleId")
                }
                studentStatus == "ABSENT" || studentStatus == "LATE" || studentStatus == "EXCUSED" -> {
                    // 学生被标记为缺席/迟到/请假 - 显示红色叉叉
                    signInStatus = SignInStatus.CLOSED
                    todayStatus = TodayClassStatus.MISSED
                    hasStudentSigned = false
                    android.util.Log.d("FirebaseCourseRepo", "❌ Student $studentStatus for $scheduleId")
                }
                !isLocked -> {
                    // Session 解锁但未签到 - 显示可签到（铅笔图标）
                    signInStatus = SignInStatus.UNLOCKED
                    todayStatus = TodayClassStatus.IN_PROGRESS
                    hasStudentSigned = false
                }
                isLocked && hasFirstUnlock -> {
                    // Session 已锁定且曾经开放过 - 学生可能错过了签到
                    // 如果学生没有任何记录，显示灰色锁（等待系统标记为缺席）
                    signInStatus = SignInStatus.CLOSED
                    todayStatus = TodayClassStatus.UPCOMING
                    hasStudentSigned = false
                }
                else -> {
                    // Session 从未开放过签到 - 显示灰色锁
                    signInStatus = SignInStatus.LOCKED
                    todayStatus = TodayClassStatus.UPCOMING
                    hasStudentSigned = false
                }
            }

            // ✅ 计算签到统计数据
            val (attendedCount, totalCount) = calculateAttendanceStats(scheduleId, studentId)

            val course = Course(
                id = scheduleId,
                courseName = name,
                courseCode = code,
                semester = semester,
                attendedClasses = attendedCount,
                totalClasses = totalCount,
                dayOfWeek = parseDayOfWeek(dayOfWeek),
                startTime = startTime,
                endTime = endTime,
                location = room,
                courseType = parseCourseType(type),
                todayStatus = todayStatus,
                signInStatus = signInStatus,
                signInUnlockedAt = null,
                hasStudentSigned = hasStudentSigned
            )

            courses.add(course)
        }

        return courses
    }

    /**
     * 计算学生的签到统计数据
     * @param scheduleId 排课ID (如 "comp3040_1")
     * @param studentId 学生Firebase UID
     * @return Pair<Int, Int> (已签到次数, 总课程数)
     */
    private suspend fun calculateAttendanceStats(scheduleId: String, studentId: String): Pair<Int, Int> {
        return try {
            val sessionsRef = database.getReference("sessions")
            val sessionsSnapshot = sessionsRef.get().await()

            var attendedCount = 0
            var totalCount = 0

            // 遍历所有 session，找出与该 scheduleId 相关的
            for (sessionSnapshot in sessionsSnapshot.children) {
                val sessionKey = sessionSnapshot.key ?: continue

                // 检查 session key 是否以 scheduleId 开头 (格式: scheduleId_date)
                if (!sessionKey.startsWith("${scheduleId}_")) {
                    continue
                }

                // 检查是否有 firstUnlockTime（只有首次解锁才计入总数）
                val hasFirstUnlock = sessionSnapshot.hasChild("firstUnlockTime")
                if (hasFirstUnlock) {
                    totalCount++

                    // 检查学生是否签到
                    val studentRecord = sessionSnapshot.child("students").child(studentId)
                    if (studentRecord.exists()) {
                        val status = studentRecord.child("status").getValue(String::class.java)
                        if (status == "PRESENT") {
                            attendedCount++
                        }
                    }
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "📊 Attendance for $scheduleId: $attendedCount / $totalCount")
            Pair(attendedCount, totalCount)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error calculating attendance: ${e.message}")
            Pair(0, 0)
        }
    }

    /**
     * 获取课程及其所有排课（教师端使用，不检查学生签到状态）
     * @param courseId 课程ID (如 "comp3040")
     * @param date 日期 (用于判断今日状态和过滤星期几)
     * @return List<Course> 一门课程在指定日期的排课
     */
    private suspend fun getCourseWithSchedules(courseId: String, date: String): List<Course> {
        // 1. 获取课程基本信息
        val courseSnapshot = coursesRef.child(courseId).get().await()
        if (!courseSnapshot.exists()) {
            return emptyList()
        }

        val code = courseSnapshot.child("code").getValue(String::class.java) ?: ""
        val name = courseSnapshot.child("name").getValue(String::class.java) ?: ""
        val semester = courseSnapshot.child("semester").getValue(String::class.java) ?: "25-26"

        // 2. 从日期解析出星期几
        val targetDayOfWeek = getDayOfWeekFromDate(date)

        // 3. 查询该课程的所有排课
        val schedulesSnapshot = schedulesRef.orderByChild("courseId").equalTo(courseId).get().await()

        val courses = mutableListOf<Course>()

        for (scheduleSnapshot in schedulesSnapshot.children) {
            val scheduleId = scheduleSnapshot.key ?: continue

            val dayOfWeek = scheduleSnapshot.child("dayOfWeek").getValue(String::class.java) ?: continue

            // ✅ 只添加匹配当天星期的课程
            if (dayOfWeek.uppercase() != targetDayOfWeek.name) {
                continue
            }

            val startTime = scheduleSnapshot.child("startTime").getValue(String::class.java) ?: "00:00"
            val endTime = scheduleSnapshot.child("endTime").getValue(String::class.java) ?: "00:00"
            val room = scheduleSnapshot.child("room").getValue(String::class.java) ?: ""
            val type = scheduleSnapshot.child("type").getValue(String::class.java) ?: "LECTURE"

            // ✅ 从 Firebase sessions 查询真实的签到状态
            val sessionKey = "${scheduleId}_$date"
            val sessionSnapshot = database.getReference("sessions").child(sessionKey).get().await()
            val isLocked = sessionSnapshot.child("isLocked").getValue(Boolean::class.java) ?: true
            val signInStatus = if (isLocked) SignInStatus.LOCKED else SignInStatus.UNLOCKED

            // 计算总课程数（用于教师端显示）
            val totalCount = calculateTotalSessions(scheduleId)

            val course = Course(
                id = scheduleId,
                courseName = name,
                courseCode = code,
                semester = semester,
                attendedClasses = 0,
                totalClasses = totalCount,
                dayOfWeek = parseDayOfWeek(dayOfWeek),
                startTime = startTime,
                endTime = endTime,
                location = room,
                courseType = parseCourseType(type),
                todayStatus = TodayClassStatus.UPCOMING,
                signInStatus = signInStatus,
                signInUnlockedAt = null,
                hasStudentSigned = false
            )

            courses.add(course)
        }

        return courses
    }

    /**
     * 计算排课的总课程数（首次解锁次数）
     */
    private suspend fun calculateTotalSessions(scheduleId: String): Int {
        return try {
            val sessionsRef = database.getReference("sessions")
            val sessionsSnapshot = sessionsRef.get().await()

            var count = 0
            for (sessionSnapshot in sessionsSnapshot.children) {
                val sessionKey = sessionSnapshot.key ?: continue
                if (sessionKey.startsWith("${scheduleId}_") && sessionSnapshot.hasChild("firstUnlockTime")) {
                    count++
                }
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 从日期字符串解析出星期几
     * @param date 日期字符串 (格式: yyyy-MM-dd)
     * @return DayOfWeek 星期几
     */
    private fun getDayOfWeekFromDate(date: String): DayOfWeek {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val localDate = java.time.LocalDate.parse(date)
                when (localDate.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
                    java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
                    java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
                    java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
                    java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
                    java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
                    java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
                }
            } else {
                // Fallback for older Android versions
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val parsedDate = sdf.parse(date)
                val calendar = java.util.Calendar.getInstance()
                calendar.time = parsedDate ?: return DayOfWeek.MONDAY
                when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
                    java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
                    java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                    java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
                    java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
                    java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
                    java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
                    else -> DayOfWeek.MONDAY
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error parsing date: ${e.message}")
            DayOfWeek.MONDAY
        }
    }

    /**
     * 解析星期几
     */
    private fun parseDayOfWeek(day: String): DayOfWeek {
        return try {
            DayOfWeek.valueOf(day.uppercase())
        } catch (e: Exception) {
            DayOfWeek.MONDAY
        }
    }

    /**
     * 解析课程类型
     */
    private fun parseCourseType(type: String): CourseType {
        return when (type.uppercase()) {
            "LECTURE" -> CourseType.LECTURE
            "LAB" -> CourseType.LAB
            "TUTORIAL" -> CourseType.TUTORIAL
            else -> CourseType.LECTURE
        }
    }

    /**
     * 获取课程的注册学生数量
     * @param courseId 课程ID
     * @return Int 学生数量
     */
    suspend fun getEnrolledStudentCount(courseId: String): Int {
        return try {
            val snapshot = enrollmentsRef.child(courseId).get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取课程的所有注册学生列表（从 Firebase）
     * 🔴 修复：使用 Firebase UID 作为唯一标识符，避免重名问题
     *
     * Firebase 数据结构：
     * - enrollments/{courseId}/{studentUid}: true (注意: key是Firebase UID字符串)
     * - users/{uid}/fullName: "Student Name"
     *
     * @param courseId 课程ID 或 排课ID (如 "comp2001" 或 "comp2001_1")
     * @return Result<List<Pair<String, String>>> Firebase UID 和姓名的配对列表
     */
    suspend fun getEnrolledStudents(courseId: String): Result<List<Pair<String, String>>> {
        return try {
            android.util.Log.d("FirebaseCourseRepo", "🔍 Fetching enrolled students for input ID: $courseId")

            // [FIX 1] 处理 Schedule ID (如 "comp3040_1" -> "comp3040")
            // 数据库中的 enrollments 使用的是通用课程 ID，不带排课后缀
            val realCourseId = if (courseId.contains("_")) {
                courseId.substringBefore("_")
            } else {
                courseId
            }

            android.util.Log.d("FirebaseCourseRepo", "🔍 Using Real Course ID: $realCourseId")

            // Step 1: 从 enrollments 获取所有选修该课程的学生 UID (String)
            val enrollmentSnapshot = enrollmentsRef.child(realCourseId).get().await()

            if (!enrollmentSnapshot.exists()) {
                android.util.Log.w("FirebaseCourseRepo", "⚠️ No enrollments found for course: $realCourseId")
                return Result.success(emptyList())
            }

            // [FIX 2] Enrollments 的 Key 是 Firebase UID (String)，不是 Long
            // 不要使用 toLongOrNull()，否则会返回 null 导致列表为空
            val studentUids = enrollmentSnapshot.children.mapNotNull { it.key }

            android.util.Log.d("FirebaseCourseRepo", "📋 Found ${studentUids.size} student UIDs: $studentUids")

            if (studentUids.isEmpty()) {
                return Result.success(emptyList())
            }

            // Step 2: 从 users 节点获取每个学生的详细信息
            val usersRef = database.getReference("users")
            val students = mutableListOf<Pair<String, String>>()  // 🔴 改为 String (UID), String (Name)

            for (uid in studentUids) {
                try {
                    val userSnapshot = usersRef.child(uid).get().await()

                    if (userSnapshot.exists()) {
                        val fullName = userSnapshot.child("fullName").getValue(String::class.java)
                            ?: userSnapshot.child("username").getValue(String::class.java)
                            ?: "Student"

                        // 🔴 使用 Firebase UID 作为唯一标识符（避免重名问题）
                        students.add(Pair(uid, fullName))
                        android.util.Log.d("FirebaseCourseRepo", "✅ Loaded Student: $fullName (UID: $uid)")
                    } else {
                        android.util.Log.w("FirebaseCourseRepo", "⚠️ User profile not found for UID: $uid")
                        // 即使找不到详细信息，也添加一个占位符，避免列表为空
                        students.add(Pair(uid, "Unknown Student"))
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseCourseRepo", "Failed to fetch user $uid: ${e.message}")
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "✅ Total enrolled students loaded: ${students.size}")
            Result.success(students)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "❌ Error fetching enrolled students: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 检查学生是否选修了某门课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return Boolean 是否选修
     */
    suspend fun isStudentEnrolled(studentId: String, courseId: String): Boolean {
        return try {
            enrollmentsRef.child(courseId).child(studentId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 实时监听课程的签到状态 (用于 INSTATT)
     * @param scheduleId 排课ID
     * @param date 日期
     * @return Flow<SignInStatus> 签到状态流
     */
    fun listenToSignInStatus(scheduleId: String, date: String): Flow<SignInStatus> = callbackFlow {
        val sessionKey = "${scheduleId}_$date"
        val sessionRef = database.getReference("sessions").child(sessionKey)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.child("isLocked").getValue(Boolean::class.java) ?: true
                val status = if (isLocked) SignInStatus.LOCKED else SignInStatus.UNLOCKED
                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        sessionRef.addValueEventListener(listener)

        awaitClose {
            sessionRef.removeEventListener(listener)
        }
    }
}
