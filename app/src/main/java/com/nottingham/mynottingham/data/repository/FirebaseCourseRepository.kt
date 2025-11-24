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
     * 获取学生的所有课程（包含今日排课）
     * @param studentId 学生用户名 (如 "student1")
     * @param date 日期 (格式: yyyy-MM-dd)
     * @return Result<List<Course>> 课程列表
     */
    suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
        return try {
            android.util.Log.d("FirebaseCourseRepo", "🔍 Fetching courses for studentId: $studentId")
            android.util.Log.d("FirebaseCourseRepo", "📅 Date: $date")
            android.util.Log.d("FirebaseCourseRepo", "🔗 Database URL: ${database.reference.database.app.name}")

            // 1. 获取学生选修的课程ID列表
            val studentCoursesSnapshot = studentCoursesRef.child(studentId).get().await()

            android.util.Log.d("FirebaseCourseRepo", "📊 Snapshot exists: ${studentCoursesSnapshot.exists()}")
            android.util.Log.d("FirebaseCourseRepo", "📊 Snapshot value: ${studentCoursesSnapshot.value}")
            android.util.Log.d("FirebaseCourseRepo", "📊 Children count: ${studentCoursesSnapshot.childrenCount}")

            val courseIds = studentCoursesSnapshot.children.mapNotNull { it.key }

            android.util.Log.d("FirebaseCourseRepo", "📚 Found ${courseIds.size} courses: $courseIds")

            if (courseIds.isEmpty()) {
                android.util.Log.w("FirebaseCourseRepo", "⚠️ No courses found for student: $studentId")
                return Result.success(emptyList())
            }

            // 2. 获取每门课程的详细信息
            val courses = mutableListOf<Course>()

            for (courseId in courseIds) {
                try {
                    android.util.Log.d("FirebaseCourseRepo", "📖 Loading course: $courseId")
                    val course = getCourseWithSchedules(courseId, date)
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
     * 获取课程及其所有排课
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

            val course = Course(
                id = scheduleId,  // 使用 scheduleId 作为唯一标识
                courseName = name,
                courseCode = code,
                semester = semester,
                attendedClasses = 0,  // TODO: 从 attendance records 计算
                totalClasses = 15,    // TODO: 从 Firebase 添加字段
                dayOfWeek = parseDayOfWeek(dayOfWeek),
                startTime = startTime,
                endTime = endTime,
                location = room,
                courseType = parseCourseType(type),
                todayStatus = TodayClassStatus.UPCOMING, // TODO: 根据时间判断
                signInStatus = SignInStatus.LOCKED,
                signInUnlockedAt = null,
                hasStudentSigned = false
            )

            courses.add(course)
        }

        return courses
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
