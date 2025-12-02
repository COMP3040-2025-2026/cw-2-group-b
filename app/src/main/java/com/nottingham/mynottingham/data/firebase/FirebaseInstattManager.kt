package com.nottingham.mynottingham.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.StudentAttendance
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Firebase Realtime Database Manager for Instatt (Attendance Check-in) System
 *
 * Data Structure:
 * sessions/
 *   {courseScheduleId}_{date}/
 *     isLocked: Boolean
 *     isActive: Boolean
 *     startTime: Long (timestamp)
 *     endTime: Long? (timestamp)
 *     students/
 *       {studentId}/
 *         studentId: Long
 *         studentName: String
 *         matricNumber: String?
 *         email: String?
 *         status: String ("PRESENT", "ABSENT", "LATE", "EXCUSED")
 *         checkInTime: String (ISO datetime)
 *         timestamp: Long
 */
class FirebaseInstattManager {

    // Using Singapore region database URL
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val sessionsRef = database.getReference("sessions")

    /**
     * Generate session key: {courseScheduleId}_{date}
     * Example: "comp2001_1_2025-01-15"
     * Fix: courseScheduleId changed to String to support Firebase ID
     */
    private fun getSessionKey(courseScheduleId: String, date: String): String {
        return "${courseScheduleId}_$date"
    }

    // ==================== Teacher Functions ====================

    /**
     * Teacher opens attendance check-in: marks session as unlocked
     * Fix: courseScheduleId changed to String to support Firebase ID
     * New: Detect if first unlock, record firstUnlockTime and unlockCount
     * New: Set 20-minute auto-lock time
     *
     * @return Result<Boolean> - true indicates first unlock (needs to increase totalClasses), false indicates repeated unlock
     */
    suspend fun unlockSession(courseScheduleId: String, date: String): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val sessionRef = sessionsRef.child(sessionKey)

            // Check if session already has firstUnlockTime
            val snapshot = sessionRef.get().await()
            val isFirstTime = !snapshot.hasChild("firstUnlockTime")
            val currentUnlockCount = snapshot.child("unlockCount").getValue(Long::class.java) ?: 0L

            val currentTime = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any>(
                "isLocked" to false,
                "isActive" to true,
                "lastUnlockTime" to currentTime,
                "unlockCount" to (currentUnlockCount + 1)
            )

            // Only set firstUnlockTime on first unlock
            if (isFirstTime) {
                updates["firstUnlockTime"] = currentTime
                updates["startTime"] = currentTime
            }

            // Set auto-lock timestamp 20 minutes later (updated each unlock)
            updates["autoLockTime"] = currentTime + (20 * 60 * 1000) // 20 minutes

            sessionRef.updateChildren(updates).await()

            android.util.Log.d(
                "FirebaseInstatt",
                "Session $sessionKey unlocked. First time: $isFirstTime, unlock count: ${currentUnlockCount + 1}"
            )

            Result.success(isFirstTime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Teacher closes attendance check-in: marks session as locked
     * Fix: courseScheduleId changed to String to support Firebase ID
     * New: Automatically mark all students who didn't sign in as ABSENT
     *
     * @param enrolledStudents List of all enrolled students, used to mark absent students
     */
    suspend fun lockSession(
        courseScheduleId: String,
        date: String,
        enrolledStudents: List<Pair<String, String>> = emptyList()  // (studentUid, studentName)
    ): Result<Unit> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val sessionRef = sessionsRef.child(sessionKey)

            // Check if session has firstUnlockTime (whether check-in was opened)
            val sessionSnapshot = sessionRef.get().await()
            val hasFirstUnlock = sessionSnapshot.hasChild("firstUnlockTime")

            // Only mark absent students if check-in was opened
            if (hasFirstUnlock && enrolledStudents.isNotEmpty()) {
                // Get list of students who signed in
                val studentsSnapshot = sessionRef.child("students").get().await()
                val signedStudentUids = studentsSnapshot.children.mapNotNull {
                    it.child("studentUid").getValue(String::class.java) ?: it.key
                }.toSet()

                val currentTime = System.currentTimeMillis()

                // Mark all students who didn't sign in as ABSENT
                for ((studentUid, studentName) in enrolledStudents) {
                    if (studentUid !in signedStudentUids) {
                        val absentData = mapOf(
                            "studentUid" to studentUid,
                            "studentName" to studentName,
                            "status" to AttendanceStatus.ABSENT.name,
                            "markedAt" to currentTime,
                            "autoMarked" to true  // Mark this as auto-marked absence
                        )
                        sessionRef.child("students").child(studentUid)
                            .setValue(absentData).await()

                        android.util.Log.d(
                            "FirebaseInstatt",
                            "Auto-marked student $studentName ($studentUid) as ABSENT"
                        )
                    }
                }
            }

            // Lock the session
            val updates = mapOf(
                "isLocked" to true,
                "isActive" to false,
                "endTime" to System.currentTimeMillis()
            )
            sessionRef.updateChildren(updates).await()

            android.util.Log.d("FirebaseInstatt", "Session $sessionKey locked")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInstatt", "Failed to lock session: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Teacher manually marks student attendance status
     * Fix: Use String UID (Firebase UID) as unique identifier
     * New: If first mark (session has no firstUnlockTime), set firstUnlockTime to increase totalClasses
     *
     * @return Result<Boolean> - true indicates first mark (totalClasses +1), false indicates not first
     */
    suspend fun markStudentAttendance(
        courseScheduleId: String,
        date: String,
        studentUid: String,  // Changed to String UID
        status: AttendanceStatus,
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val sessionRef = sessionsRef.child(sessionKey)

            // Check if this is the first mark (session has no firstUnlockTime)
            val sessionSnapshot = sessionRef.get().await()
            val isFirstMark = !sessionSnapshot.hasChild("firstUnlockTime")

            val currentTime = System.currentTimeMillis()

            // If first mark, set firstUnlockTime (this increases totalClasses +1)
            if (isFirstMark) {
                val sessionUpdates = mapOf(
                    "firstUnlockTime" to currentTime,
                    "startTime" to currentTime,
                    "manualMarkSession" to true  // Mark this as a session created by manual marking
                )
                sessionRef.updateChildren(sessionUpdates).await()
                android.util.Log.d(
                    "FirebaseInstatt",
                    "First mark for session $sessionKey - totalClasses will increase"
                )
            }

            // Save student attendance data
            val studentData = mapOf(
                "studentUid" to studentUid,
                "studentName" to studentName,
                "matricNumber" to matricNumber,
                "email" to email,
                "status" to status.name,
                "checkInTime" to java.time.Instant.now().toString(),
                "timestamp" to currentTime,
                "manuallyMarked" to true  // Mark this as teacher manually marked
            )
            sessionRef.child("students").child(studentUid)
                .setValue(studentData).await()

            android.util.Log.d(
                "FirebaseInstatt",
                "Teacher marked $studentName ($studentUid) as ${status.name} (firstMark=$isFirstMark)"
            )

            Result.success(isFirstMark)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInstatt", "Failed to mark attendance: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listen to attendance list (Flow) - used by teacher
     * Updates list in real-time when students sign in
     *
     * Important: Even if Firebase has no data or disconnected, sends empty list immediately to avoid UI loading
     * Fix: courseScheduleId changed to String to support Firebase ID
     */
    fun listenToStudentAttendanceList(
        courseScheduleId: String,
        date: String
    ): Flow<List<StudentAttendance>> = callbackFlow {
        val sessionKey = getSessionKey(courseScheduleId, date)
        val studentsRef = sessionsRef.child(sessionKey).child("students")

        // Send empty list immediately to avoid UI spinning indefinitely
        trySend(emptyList())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val students = mutableListOf<StudentAttendance>()

                // If snapshot doesn't exist or is empty, return empty list
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { child ->
                    try {
                        // Fix: Support both old and new data formats
                        // New format: studentUid (String)
                        // Old format: studentId (Long)
                        val studentUid = child.child("studentUid").getValue(String::class.java)
                            ?: child.child("studentId").getValue(Long::class.java)?.toString()
                            ?: child.key  // If both missing, use node key as UID
                            ?: ""

                        val studentName = child.child("studentName").getValue(String::class.java) ?: ""
                        val matricNumber = child.child("matricNumber").getValue(String::class.java)
                        val email = child.child("email").getValue(String::class.java)
                        val statusStr = child.child("status").getValue(String::class.java) ?: "ABSENT"
                        val checkInTime = child.child("checkInTime").getValue(String::class.java)

                        val status = try {
                            AttendanceStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            AttendanceStatus.ABSENT
                        }

                        students.add(
                            StudentAttendance(
                                studentId = studentUid,  // Use UID
                                studentName = studentName,
                                matricNumber = matricNumber,
                                email = email,
                                hasAttended = status == AttendanceStatus.PRESENT,
                                attendanceStatus = status,
                                checkInTime = checkInTime
                            )
                        )

                        android.util.Log.d(
                            "FirebaseInstatt",
                            "Parsed student: $studentName ($studentUid) - $status"
                        )
                    } catch (e: Exception) {
                        // Skip invalid entries
                        android.util.Log.w("FirebaseInstatt", "Failed to parse student: ${e.message}")
                    }
                }

                // Send latest student list (may be empty)
                android.util.Log.d("FirebaseInstatt", "Emitting ${students.size} students to listener")
                trySend(students)
            }

            override fun onCancelled(error: DatabaseError) {
                // If Firebase connection fails, send empty list instead of making UI wait
                android.util.Log.e("FirebaseInstatt", "Firebase cancelled: ${error.message}")
                trySend(emptyList())
                close(error.toException())
            }
        }

        studentsRef.addValueEventListener(listener)

        awaitClose {
            studentsRef.removeEventListener(listener)
        }
    }

    // ==================== Student Functions ====================

    /**
     * Student signs in
     * Fix: Support String UID (Firebase UID)
     * @param studentUid Firebase UID (String)
     * @param studentName Student name
     */
    suspend fun signIn(
        courseScheduleId: String,
        date: String,
        studentUid: String,  // Changed to String UID
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)

            // First check if session is unlocked
            val isUnlocked = suspendCoroutine<Boolean> { continuation ->
                sessionsRef.child(sessionKey).child("isLocked")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val locked = snapshot.getValue(Boolean::class.java) ?: true
                        continuation.resume(!locked) // unlocked = !locked
                    }
                    .addOnFailureListener {
                        continuation.resumeWithException(it)
                    }
            }

            if (!isUnlocked) {
                return Result.failure(Exception("Session is locked"))
            }

            // Write check-in data - use Firebase UID as key
            val studentData = mapOf(
                "studentUid" to studentUid,  // Save Firebase UID
                "studentName" to studentName,
                "matricNumber" to matricNumber,
                "email" to email,
                "status" to AttendanceStatus.PRESENT.name,
                "checkInTime" to java.time.Instant.now().toString(),
                "timestamp" to System.currentTimeMillis()
            )

            // Use Firebase UID as key (not numeric ID)
            sessionsRef.child(sessionKey).child("students").child(studentUid)
                .setValue(studentData).await()

            android.util.Log.d(
                "FirebaseInstatt",
                "Student $studentName ($studentUid) signed in to $sessionKey"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInstatt", "Sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listen to session lock status (Flow) - used by student
     * When teacher unlocks session, student's sign-in button becomes bright immediately
     *
     * Important: If session doesn't exist, defaults to true (locked)
     * Fix: courseScheduleId changed to String to support Firebase ID
     */
    fun listenToSessionLockStatus(
        courseScheduleId: String,
        date: String
    ): Flow<Boolean> = callbackFlow {
        val sessionKey = getSessionKey(courseScheduleId, date)
        val lockRef = sessionsRef.child(sessionKey).child("isLocked")

        // Send default value (locked) immediately to prevent UI from being stuck
        trySend(true)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // If session doesn't exist or isLocked field doesn't exist, defaults to locked (true)
                val isLocked = if (snapshot.exists()) {
                    snapshot.getValue(Boolean::class.java) ?: true
                } else {
                    true // Session not created yet, default to locked
                }

                trySend(isLocked)

                // Log output
                android.util.Log.d(
                    "FirebaseInstatt",
                    "Session $sessionKey: isLocked=$isLocked (exists=${snapshot.exists()})"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                // If connection fails, also return locked status
                android.util.Log.e("FirebaseInstatt", "Listen cancelled: ${error.message}")
                trySend(true)
                close(error.toException())
            }
        }

        lockRef.addValueEventListener(listener)

        awaitClose {
            lockRef.removeEventListener(listener)
        }
    }

    /**
     * Check if student has already signed in
     * Fix: Use Firebase UID (String) as unique identifier
     */
    suspend fun hasStudentSignedIn(
        courseScheduleId: String,
        date: String,
        studentUid: String  // Changed to String UID
    ): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val snapshot = sessionsRef.child(sessionKey)
                .child("students")
                .child(studentUid)  // Use String UID
                .get()
                .await()

            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Utility Functions ====================

    /**
     * Check and execute 20-minute auto-lock
     * If session exceeds autoLockTime and still unlocked, auto-lock and mark unsigned students as absent
     * Fix: Use Firebase UID (String) as unique identifier
     *
     * @param courseScheduleId Course schedule ID
     * @param date Date
     * @param enrolledStudents List of all enrolled students (UID, name)
     * @return Result<Boolean> - true indicates auto-lock executed, false means no lock needed
     */
    suspend fun checkAndAutoLockSession(
        courseScheduleId: String,
        date: String,
        enrolledStudents: List<Pair<String, String>>  // List of (studentUid, studentName)
    ): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val sessionRef = sessionsRef.child(sessionKey)

            val snapshot = sessionRef.get().await()
            if (!snapshot.exists()) {
                return Result.success(false)
            }

            val isLocked = snapshot.child("isLocked").getValue(Boolean::class.java) ?: true
            val autoLockTime = snapshot.child("autoLockTime").getValue(Long::class.java) ?: 0L
            val currentTime = System.currentTimeMillis()

            // If already locked or no autoLockTime set, no action needed
            if (isLocked || autoLockTime == 0L) {
                return Result.success(false)
            }

            // Check if exceeded autoLockTime
            if (currentTime >= autoLockTime) {
                android.util.Log.d(
                    "FirebaseInstatt",
                    "Auto-locking session $sessionKey (exceeded 20 minutes)"
                )

                // 1. Lock session
                val lockUpdates = mapOf(
                    "isLocked" to true,
                    "isActive" to false,
                    "autoLockedAt" to currentTime
                )
                sessionRef.updateChildren(lockUpdates).await()

                // 2. Mark all unsigned students as absent
                val studentsSnapshot = sessionRef.child("students").get().await()
                // Use studentUid (String) as key
                val signedInStudentUids = studentsSnapshot.children.mapNotNull {
                    it.child("studentUid").getValue(String::class.java) ?: it.key
                }.toSet()

                for ((studentUid, studentName) in enrolledStudents) {
                    if (studentUid !in signedInStudentUids) {
                        // Mark as absent
                        val absentData = mapOf(
                            "studentUid" to studentUid,  // Use Firebase UID
                            "studentName" to studentName,
                            "status" to AttendanceStatus.ABSENT.name,
                            "markedAt" to currentTime,
                            "autoMarked" to true  // Mark this as auto-marked absence
                        )
                        // Use studentUid as key
                        sessionRef.child("students").child(studentUid)
                            .setValue(absentData).await()

                        android.util.Log.d(
                            "FirebaseInstatt",
                            "Auto-marked student $studentUid ($studentName) as ABSENT"
                        )
                    }
                }

                return Result.success(true)
            }

            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete expired sessions (optional cleanup function)
     */
    suspend fun cleanupExpiredSessions(daysToKeep: Int = 7): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            var deletedCount = 0

            val snapshot = sessionsRef.get().await()
            snapshot.children.forEach { sessionSnapshot ->
                val startTime = sessionSnapshot.child("startTime").getValue(Long::class.java) ?: 0L
                if (startTime > 0 && startTime < cutoffTime) {
                    sessionSnapshot.ref.removeValue().await()
                    deletedCount++
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
