package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter

/**
 * Firebase Course Repository
 *
 * Reads course and schedule data directly from Firebase Realtime Database
 * No longer depends on Spring Boot backend API
 *
 * Firebase data structure:
 * - courses/{courseId}: Basic course information
 * - schedules/{scheduleId}: Schedule information
 * - enrollments/{courseId}/{studentId}: Student enrollment relationships
 * - student_courses/{studentId}/{courseId}: Reverse index
 */
class FirebaseCourseRepository {

    // IMPORTANT: Must specify database URL because database is in asia-southeast1 region
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val coursesRef: DatabaseReference = database.getReference("courses")
    private val schedulesRef: DatabaseReference = database.getReference("schedules")
    private val enrollmentsRef: DatabaseReference = database.getReference("enrollments")
    private val studentCoursesRef: DatabaseReference = database.getReference("student_courses")

    /**
     * Get all courses for a student (including today's schedule and attendance status)
     * @param studentId Student Firebase UID
     * @param date Date (format: yyyy-MM-dd)
     * @return Result<List<Course>> Course list (including attendance status and statistics)
     */
    suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
        return try {
            android.util.Log.d("FirebaseCourseRepo", "Fetching courses for studentId: $studentId")
            android.util.Log.d("FirebaseCourseRepo", "Date: $date")

            // 1. Get list of course IDs enrolled by the student
            val studentCoursesSnapshot = studentCoursesRef.child(studentId).get().await()

            val courseIds = studentCoursesSnapshot.children.mapNotNull { it.key }

            android.util.Log.d("FirebaseCourseRepo", "Found ${courseIds.size} courses: $courseIds")

            if (courseIds.isEmpty()) {
                android.util.Log.w("FirebaseCourseRepo", "No courses found for student: $studentId")
                return Result.success(emptyList())
            }

            // 2. Get detailed information for each course (including attendance status)
            val courses = mutableListOf<Course>()

            for (courseId in courseIds) {
                try {
                    android.util.Log.d("FirebaseCourseRepo", "Loading course: $courseId")
                    val course = getCourseWithSchedulesAndAttendance(courseId, date, studentId)
                    android.util.Log.d("FirebaseCourseRepo", "Loaded ${course.size} schedules for $courseId")
                    courses.addAll(course)
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseCourseRepo", "Failed to load course $courseId: ${e.message}", e)
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "Total courses loaded: ${courses.size}")
            Result.success(courses)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error fetching student courses: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all courses for a teacher
     * @param teacherId Teacher username (e.g. "teacher1")
     * @param date Date
     * @return Result<List<Course>> Course list
     */
    suspend fun getTeacherCourses(teacherId: String, date: String): Result<List<Course>> {
        return try {
            // Query all courses where teacherId field matches
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
     * Get course and all its schedules (including student attendance status and statistics)
     * @param courseId Course ID (e.g. "comp3040")
     * @param date Date (used to determine today's status and filter day of week)
     * @param studentId Student Firebase UID (used to check attendance status)
     * @return List<Course> Schedules for a course on the specified date
     */
    private suspend fun getCourseWithSchedulesAndAttendance(
        courseId: String,
        date: String,
        studentId: String
    ): List<Course> {
        // 1. Get basic course information
        val courseSnapshot = coursesRef.child(courseId).get().await()
        if (!courseSnapshot.exists()) {
            return emptyList()
        }

        val code = courseSnapshot.child("code").getValue(String::class.java) ?: ""
        val name = courseSnapshot.child("name").getValue(String::class.java) ?: ""
        val semester = courseSnapshot.child("semester").getValue(String::class.java) ?: "25-26"

        // 2. Parse day of week from date
        val targetDayOfWeek = getDayOfWeekFromDate(date)

        // 3. Query all schedules for this course
        val schedulesSnapshot = schedulesRef.orderByChild("courseId").equalTo(courseId).get().await()

        val courses = mutableListOf<Course>()

        for (scheduleSnapshot in schedulesSnapshot.children) {
            val scheduleId = scheduleSnapshot.key ?: continue

            val dayOfWeek = scheduleSnapshot.child("dayOfWeek").getValue(String::class.java) ?: continue

            // Only add courses matching today's day of week
            if (dayOfWeek.uppercase() != targetDayOfWeek.name) {
                continue
            }

            val startTime = scheduleSnapshot.child("startTime").getValue(String::class.java) ?: "00:00"
            val endTime = scheduleSnapshot.child("endTime").getValue(String::class.java) ?: "00:00"
            val room = scheduleSnapshot.child("room").getValue(String::class.java) ?: ""
            val type = scheduleSnapshot.child("type").getValue(String::class.java) ?: "LECTURE"

            // Query actual attendance status from Firebase sessions
            val sessionKey = "${scheduleId}_$date"
            val sessionSnapshot = database.getReference("sessions").child(sessionKey).get().await()
            val isLocked = sessionSnapshot.child("isLocked").getValue(Boolean::class.java) ?: true

            // Check student's status in this session
            val studentRecord = sessionSnapshot.child("students").child(studentId)
            val hasStudentRecord = studentRecord.exists()
            val studentStatus = if (hasStudentRecord) {
                studentRecord.child("status").getValue(String::class.java) ?: "NOT_MARKED"
            } else {
                "NOT_MARKED"  // Student's initial status is "not marked"
            }

            // Check if session has ever been unlocked for attendance
            val hasFirstUnlock = sessionSnapshot.hasChild("firstUnlockTime")

            // Determine attendance status and today's status
            val signInStatus: SignInStatus
            val todayStatus: TodayClassStatus
            val hasStudentSigned: Boolean

            when {
                studentStatus == "PRESENT" -> {
                    // Student has signed in or been marked as present - show green checkmark
                    signInStatus = SignInStatus.SIGNED
                    todayStatus = TodayClassStatus.ATTENDED
                    hasStudentSigned = true
                    android.util.Log.d("FirebaseCourseRepo", "Student PRESENT for $scheduleId")
                }
                studentStatus == "ABSENT" || studentStatus == "LATE" || studentStatus == "EXCUSED" -> {
                    // Student marked as absent/late/excused - show red X
                    signInStatus = SignInStatus.CLOSED
                    todayStatus = TodayClassStatus.MISSED
                    hasStudentSigned = false
                    android.util.Log.d("FirebaseCourseRepo", "Student $studentStatus for $scheduleId")
                }
                !isLocked -> {
                    // Session unlocked but not signed in - show available for sign-in (pencil icon)
                    signInStatus = SignInStatus.UNLOCKED
                    todayStatus = TodayClassStatus.IN_PROGRESS
                    hasStudentSigned = false
                }
                isLocked && hasFirstUnlock -> {
                    // Session locked and was previously unlocked - student may have missed sign-in
                    // If student has no record, show gray lock (waiting for system to mark as absent)
                    signInStatus = SignInStatus.CLOSED
                    todayStatus = TodayClassStatus.UPCOMING
                    hasStudentSigned = false
                }
                else -> {
                    // Session never unlocked for attendance - show gray lock
                    signInStatus = SignInStatus.LOCKED
                    todayStatus = TodayClassStatus.UPCOMING
                    hasStudentSigned = false
                }
            }

            // Calculate attendance statistics
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
     * Calculate student's attendance statistics
     * @param scheduleId Schedule ID (e.g. "comp3040_1")
     * @param studentId Student Firebase UID
     * @return Pair<Int, Int> (attended count, total classes)
     */
    private suspend fun calculateAttendanceStats(scheduleId: String, studentId: String): Pair<Int, Int> {
        return try {
            val sessionsRef = database.getReference("sessions")
            val sessionsSnapshot = sessionsRef.get().await()

            var attendedCount = 0
            var totalCount = 0

            // Loop through all sessions and find those related to this scheduleId
            for (sessionSnapshot in sessionsSnapshot.children) {
                val sessionKey = sessionSnapshot.key ?: continue

                // Check if session key starts with scheduleId (format: scheduleId_date)
                if (!sessionKey.startsWith("${scheduleId}_")) {
                    continue
                }

                // Check if firstUnlockTime exists (only count sessions that were unlocked)
                val hasFirstUnlock = sessionSnapshot.hasChild("firstUnlockTime")
                if (hasFirstUnlock) {
                    totalCount++

                    // Check if student signed in
                    val studentRecord = sessionSnapshot.child("students").child(studentId)
                    if (studentRecord.exists()) {
                        val status = studentRecord.child("status").getValue(String::class.java)
                        if (status == "PRESENT") {
                            attendedCount++
                        }
                    }
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "Attendance for $scheduleId: $attendedCount / $totalCount")
            Pair(attendedCount, totalCount)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error calculating attendance: ${e.message}")
            Pair(0, 0)
        }
    }

    /**
     * Get course and all its schedules (for teacher use, does not check student attendance status)
     * @param courseId Course ID (e.g. "comp3040")
     * @param date Date (used to determine today's status and filter day of week)
     * @return List<Course> Schedules for a course on the specified date
     */
    private suspend fun getCourseWithSchedules(courseId: String, date: String): List<Course> {
        // 1. Get basic course information
        val courseSnapshot = coursesRef.child(courseId).get().await()
        if (!courseSnapshot.exists()) {
            return emptyList()
        }

        val code = courseSnapshot.child("code").getValue(String::class.java) ?: ""
        val name = courseSnapshot.child("name").getValue(String::class.java) ?: ""
        val semester = courseSnapshot.child("semester").getValue(String::class.java) ?: "25-26"

        // 2. Parse day of week from date
        val targetDayOfWeek = getDayOfWeekFromDate(date)

        // 3. Query all schedules for this course
        val schedulesSnapshot = schedulesRef.orderByChild("courseId").equalTo(courseId).get().await()

        val courses = mutableListOf<Course>()

        for (scheduleSnapshot in schedulesSnapshot.children) {
            val scheduleId = scheduleSnapshot.key ?: continue

            val dayOfWeek = scheduleSnapshot.child("dayOfWeek").getValue(String::class.java) ?: continue

            // Only add courses matching today's day of week
            if (dayOfWeek.uppercase() != targetDayOfWeek.name) {
                continue
            }

            val startTime = scheduleSnapshot.child("startTime").getValue(String::class.java) ?: "00:00"
            val endTime = scheduleSnapshot.child("endTime").getValue(String::class.java) ?: "00:00"
            val room = scheduleSnapshot.child("room").getValue(String::class.java) ?: ""
            val type = scheduleSnapshot.child("type").getValue(String::class.java) ?: "LECTURE"

            // Query actual attendance status from Firebase sessions
            val sessionKey = "${scheduleId}_$date"
            val sessionSnapshot = database.getReference("sessions").child(sessionKey).get().await()
            val isLocked = sessionSnapshot.child("isLocked").getValue(Boolean::class.java) ?: true
            val signInStatus = if (isLocked) SignInStatus.LOCKED else SignInStatus.UNLOCKED

            // Calculate total sessions (for teacher view display)
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
     * Calculate total number of sessions for a schedule (number of times first unlocked)
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
     * Parse day of week from date string
     * @param date Date string (format: yyyy-MM-dd)
     * @return DayOfWeek Day of week
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
     * Parse day of week
     */
    private fun parseDayOfWeek(day: String): DayOfWeek {
        return try {
            DayOfWeek.valueOf(day.uppercase())
        } catch (e: Exception) {
            DayOfWeek.MONDAY
        }
    }

    /**
     * Parse course type
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
     * Get number of enrolled students for a course
     * @param courseId Course ID
     * @return Int Number of students
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
     * Get list of all enrolled students for a course (from Firebase)
     * FIX: Use Firebase UID as unique identifier to avoid duplicate name issues
     *
     * Firebase data structure:
     * - enrollments/{courseId}/{studentUid}: true (note: key is Firebase UID string)
     * - users/{uid}/fullName: "Student Name"
     *
     * @param courseId Course ID or Schedule ID (e.g. "comp2001" or "comp2001_1")
     * @return Result<List<Pair<String, String>>> List of pairs of Firebase UID and name
     */
    suspend fun getEnrolledStudents(courseId: String): Result<List<Pair<String, String>>> {
        return try {
            android.util.Log.d("FirebaseCourseRepo", "Fetching enrolled students for input ID: $courseId")

            // [FIX 1] Handle Schedule ID (e.g. "comp3040_1" -> "comp3040")
            // enrollments in database use generic course ID without schedule suffix
            val realCourseId = if (courseId.contains("_")) {
                courseId.substringBefore("_")
            } else {
                courseId
            }

            android.util.Log.d("FirebaseCourseRepo", "Using Real Course ID: $realCourseId")

            // Step 1: Get all student UIDs (String) enrolled in this course from enrollments
            val enrollmentSnapshot = enrollmentsRef.child(realCourseId).get().await()

            if (!enrollmentSnapshot.exists()) {
                android.util.Log.w("FirebaseCourseRepo", "No enrollments found for course: $realCourseId")
                return Result.success(emptyList())
            }

            // [FIX 2] Enrollments key is Firebase UID (String), not Long
            // Do not use toLongOrNull(), or it will return null and result in empty list
            val studentUids = enrollmentSnapshot.children.mapNotNull { it.key }

            android.util.Log.d("FirebaseCourseRepo", "Found ${studentUids.size} student UIDs: $studentUids")

            if (studentUids.isEmpty()) {
                return Result.success(emptyList())
            }

            // Step 2: Get detailed information for each student from users node
            val usersRef = database.getReference("users")
            val students = mutableListOf<Pair<String, String>>()  // Changed to String (UID), String (Name)

            for (uid in studentUids) {
                try {
                    val userSnapshot = usersRef.child(uid).get().await()

                    if (userSnapshot.exists()) {
                        val fullName = userSnapshot.child("fullName").getValue(String::class.java)
                            ?: userSnapshot.child("username").getValue(String::class.java)
                            ?: "Student"

                        // Use Firebase UID as unique identifier (avoid duplicate name issues)
                        students.add(Pair(uid, fullName))
                        android.util.Log.d("FirebaseCourseRepo", "Loaded Student: $fullName (UID: $uid)")
                    } else {
                        android.util.Log.w("FirebaseCourseRepo", "User profile not found for UID: $uid")
                        // Add a placeholder even if details not found, to avoid empty list
                        students.add(Pair(uid, "Unknown Student"))
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseCourseRepo", "Failed to fetch user $uid: ${e.message}")
                }
            }

            android.util.Log.d("FirebaseCourseRepo", "Total enrolled students loaded: ${students.size}")
            Result.success(students)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCourseRepo", "Error fetching enrolled students: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if student is enrolled in a course
     * @param studentId Student ID
     * @param courseId Course ID
     * @return Boolean Whether enrolled
     */
    suspend fun isStudentEnrolled(studentId: String, courseId: String): Boolean {
        return try {
            enrollmentsRef.child(courseId).child(studentId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Listen to course sign-in status in real-time (for INSTATT)
     * @param scheduleId Schedule ID
     * @param date Date
     * @return Flow<SignInStatus> Sign-in status flow
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
