package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.nottingham.mynottingham.data.firebase.FirebaseInstattManager
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.StudentAttendance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * InstattRepository - Unified management of attendance system data access
 *
 * Architecture design (migrated to Firebase):
 * - Course queries (getTeacherCourses, getStudentCourses): Using Firebase Realtime Database
 * - Real-time attendance operations (unlock/lock/signIn): Using Firebase Realtime Database
 * - Real-time listeners (student list, lock status): Implementing reactive updates through Flow
 */
class InstattRepository {

    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val firebaseManager = FirebaseInstattManager()
    private val firebaseCourseRepo = FirebaseCourseRepository()

    /**
     * Get server time - uses Firebase server time offset
     * Firebase provides .info/serverTimeOffset to calculate the difference between server and local time
     */
    suspend fun getSystemTime(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val offsetRef = database.getReference(".info/serverTimeOffset")
                val offset = offsetRef.get().await().getValue(Long::class.java) ?: 0L
                System.currentTimeMillis() + offset
            } catch (e: Exception) {
                // If unable to get server time, fall back to local time
                android.util.Log.w("InstattRepository", "Failed to get server time offset, using local time", e)
                System.currentTimeMillis()
            }
        }
    }

    /**
     * Migrated: Get teacher courses using Firebase
     */
    suspend fun getTeacherCourses(teacherId: String, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            // Directly call FirebaseCourseRepository
            firebaseCourseRepo.getTeacherCourses(teacherId, date)
        }
    }

    /**
     * Migrated: Get student courses using Firebase
     */
    suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            // Directly call FirebaseCourseRepository
            firebaseCourseRepo.getStudentCourses(studentId, date)
        }
    }

    /**
     * Teacher unlocks attendance - uses Firebase for real-time updates
     * FIX: Changed courseScheduleId to String to support Firebase ID
     * NEW: Returns whether this is first unlock (used to increment total classes)
     *
     * @return Result<Boolean> - true indicates first unlock, false indicates repeat unlock
     */
    suspend fun unlockSession(teacherId: String, courseScheduleId: String, date: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            // Use Firebase directly, no longer calling backend API
            // Note: firebaseManager does not need teacherId
            firebaseManager.unlockSession(courseScheduleId, date)
        }
    }

    /**
     * Teacher locks attendance - uses Firebase for real-time updates
     * FIX: Changed courseScheduleId to String to support Firebase ID
     * NEW: Automatically marks all unsigned students as ABSENT
     */
    suspend fun lockSession(teacherId: String, courseScheduleId: String, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Get list of all enrolled students
            val courseId = courseScheduleId.substringBefore("_")
            val enrolledResult = firebaseCourseRepo.getEnrolledStudents(courseId)
            val enrolledStudents = enrolledResult.getOrNull() ?: emptyList()

            android.util.Log.d(
                "InstattRepository",
                "Locking session $courseScheduleId with ${enrolledStudents.size} enrolled students"
            )

            // Lock session and automatically mark unsigned students as absent
            firebaseManager.lockSession(courseScheduleId, date, enrolledStudents)
        }
    }

    /**
     * Student sign-in - uses Firebase for millisecond-level response
     * FIX: Supports String UID (Firebase UID)
     * @param studentUid Firebase UID (String)
     * @param studentName Student name (obtained from TokenManager)
     * @param matricNumber Student ID (optional)
     * @param email Email (optional)
     */
    suspend fun signIn(
        studentUid: String,  // Changed to String UID
        courseScheduleId: String,
        date: String,
        studentName: String = "Student", // Default value, should pass real name when calling
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Write directly to Firebase, no need to wait for backend response
            firebaseManager.signIn(
                courseScheduleId = courseScheduleId,
                date = date,
                studentUid = studentUid,  // Pass String UID
                studentName = studentName,
                matricNumber = matricNumber,
                email = email
            )
        }
    }

    /**
     * Get student attendance list - returns Flow for real-time listening
     * Teacher side automatically receives updates when students sign in
     *
     * Data merge strategy (optimized to Firebase-first):
     * 1. Get all enrolled student list from Firebase (base data)
     * 2. Listen to Firebase attendance data in real-time (real-time updates)
     * 3. Overlay Firebase data on student list, unsigned students remain ABSENT status
     *
     * Advantages:
     * - Teacher can see complete class roster (including unsigned students)
     * - Firebase real-time attendance status updates (millisecond-level response)
     * - Completely independent of backend MySQL server
     *
     * FIX: Changed courseScheduleId to String to support Firebase ID
     */
    fun getStudentAttendanceList(
        teacherId: String,  // Firebase UID
        courseScheduleId: String,
        date: String
    ): Flow<List<StudentAttendance>> = flow {
        // Step 1: Extract pure course code from course ID (remove schedule number)
        // Example: "comp2001_1" -> "comp2001"
        val courseId = courseScheduleId.substringBefore("_")

        // Step 2: Try to get enrolled student list from Firebase (one-time query)
        val enrolledResult = firebaseCourseRepo.getEnrolledStudents(courseId)
        val enrolledStudents = enrolledResult.getOrNull() ?: emptyList()

        android.util.Log.d(
            "InstattRepository",
            "Found ${enrolledStudents.size} enrolled students for course $courseId"
        )

        // Step 3: Listen to Firebase real-time attendance data
        firebaseManager.listenToStudentAttendanceList(courseScheduleId, date)
            .collect { firebaseStudents ->
                // Step 4: Merge data - Use Firebase UID matching to avoid duplicate name issues
                if (enrolledStudents.isNotEmpty()) {
                    // Has enrolled student data - use merge mode (complete roster + real-time status)
                    val mergedList = enrolledStudents.map { (studentUid, studentName) ->
                        // FIX: Use Firebase UID matching (unique identifier)
                        val firebaseRecord = firebaseStudents.find {
                            it.studentId == studentUid  // UID to UID matching
                        }

                        if (firebaseRecord != null) {
                            // Firebase has this student's attendance record, use Firebase's real-time data
                            android.util.Log.d(
                                "InstattRepository",
                                "Matched enrolled student $studentName ($studentUid) with Firebase record"
                            )
                            firebaseRecord
                        } else {
                            // Firebase does not yet have this student's attendance record, show as ABSENT
                            android.util.Log.d(
                                "InstattRepository",
                                "Student $studentName ($studentUid) enrolled but not signed in yet"
                            )
                            StudentAttendance(
                                studentId = studentUid,  // Use Firebase UID
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
                    // No enrolled student data - fall back to Firebase-only mode
                    // In this mode only show signed-in students, but at least guarantee real-time
                    android.util.Log.w(
                        "InstattRepository",
                        "No enrolled students found for $courseId, showing only signed-in students"
                    )
                    emit(firebaseStudents)
                }
            }
    }.flowOn(Dispatchers.IO)

    /**
     * DEPRECATED: No longer using MySQL backend
     *
     * This method has been completely replaced by Firebase, all student list data is now obtained from Firebase:
     * - enrollments/{courseId}/{studentId} - Student enrollment relationships
     * - sessions/{scheduleId}_{date}/students/ - Real-time attendance records
     *
     * To get student list, please use:
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
     * Teacher manually marks student attendance status - uses Firebase for real-time updates
     * FIX: Use Firebase UID (String) as unique identifier to avoid duplicate name issues
     * NEW: If this is first marking, will automatically increment totalClasses
     *
     * @return Result<Boolean> - true indicates first marking (totalClasses +1), false indicates not first
     */
    suspend fun markAttendance(
        teacherId: String,  // Firebase UID (not used in Firebase operations)
        studentUid: String,  // Firebase UID (String)
        courseScheduleId: String,
        date: String,
        status: String,
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val attendanceStatus = try {
                AttendanceStatus.valueOf(status)
            } catch (e: Exception) {
                AttendanceStatus.ABSENT
            }

            // Use Firebase UID directly, no longer converting to Long
            firebaseManager.markStudentAttendance(
                courseScheduleId = courseScheduleId,
                date = date,
                studentUid = studentUid,  // Pass String UID
                status = attendanceStatus,
                studentName = studentName,
                matricNumber = matricNumber,
                email = email
            )
        }
    }

    /**
     * Student side: Listen to session lock status (real-time)
     * When teacher unlocks session, student's sign-in button immediately lights up
     * FIX: Changed courseScheduleId to String to support Firebase ID
     */
    fun listenToSessionLockStatus(
        courseScheduleId: String,
        date: String
    ): Flow<Boolean> {
        return firebaseManager.listenToSessionLockStatus(courseScheduleId, date)
    }

    /**
     * Check if student has already signed in
     * FIX: Use Firebase UID (String) as unique identifier
     */
    suspend fun hasStudentSignedIn(
        courseScheduleId: String,
        date: String,
        studentUid: String  // Changed to String UID
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            firebaseManager.hasStudentSignedIn(courseScheduleId, date, studentUid)
        }
    }
}

