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
 * Firebase Realtime Database Manager for Instatt (签到) System
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

    // 使用新加坡区域的数据库 URL（从错误日志中获取）
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val sessionsRef = database.getReference("sessions")

    /**
     * 生成 session key: {courseScheduleId}_{date}
     * Example: "comp2001_1_2025-01-15"
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    private fun getSessionKey(courseScheduleId: String, date: String): String {
        return "${courseScheduleId}_$date"
    }

    // ==================== Teacher Functions ====================

    /**
     * 教师开启签到：将 session 标记为 unlocked
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     * ✅ 新增：检测是否首次unlock，记录firstUnlockTime和unlockCount
     * ✅ 新增：设置20分钟自动锁定时间
     *
     * @return Result<Boolean> - true表示首次unlock（需要增加totalClasses），false表示重复unlock
     */
    suspend fun unlockSession(courseScheduleId: String, date: String): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val sessionRef = sessionsRef.child(sessionKey)

            // 检查session是否已存在firstUnlockTime
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

            // 只有首次unlock才设置firstUnlockTime
            if (isFirstTime) {
                updates["firstUnlockTime"] = currentTime
                updates["startTime"] = currentTime
            }

            // 设置20分钟后自动锁定的时间戳（每次unlock都更新）
            updates["autoLockTime"] = currentTime + (20 * 60 * 1000) // 20分钟

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
     * 教师关闭签到：将 session 标记为 locked
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun lockSession(courseScheduleId: String, date: String): Result<Unit> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val updates = mapOf(
                "isLocked" to true,
                "isActive" to false,
                "endTime" to System.currentTimeMillis()
            )
            sessionsRef.child(sessionKey).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 教师手动标记学生出勤状态
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun markStudentAttendance(
        courseScheduleId: String,
        date: String,
        studentId: Long,
        status: AttendanceStatus,
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val studentData = mapOf(
                "studentId" to studentId,
                "studentName" to studentName,
                "matricNumber" to matricNumber,
                "email" to email,
                "status" to status.name,
                "checkInTime" to java.time.Instant.now().toString(),
                "timestamp" to System.currentTimeMillis()
            )
            sessionsRef.child(sessionKey).child("students").child(studentId.toString())
                .setValue(studentData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 实时监听签到名单（Flow）- 教师端使用
     * 当有学生签到时，实时更新列表
     *
     * 重要：即使 Firebase 没有数据或未连接，也会立即发送空列表，避免 UI 一直 loading
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    fun listenToStudentAttendanceList(
        courseScheduleId: String,
        date: String
    ): Flow<List<StudentAttendance>> = callbackFlow {
        val sessionKey = getSessionKey(courseScheduleId, date)
        val studentsRef = sessionsRef.child(sessionKey).child("students")

        // 立即发送空列表，避免 UI 一直转圈
        trySend(emptyList())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val students = mutableListOf<StudentAttendance>()

                // 如果 snapshot 不存在或为空，返回空列表
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { child ->
                    try {
                        val studentIdLong = child.child("studentId").getValue(Long::class.java) ?: 0L
                        val studentId = studentIdLong.toString()  // Convert to String for Firebase UID compatibility
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
                                studentId = studentId,
                                studentName = studentName,
                                matricNumber = matricNumber,
                                email = email,
                                hasAttended = status == AttendanceStatus.PRESENT,
                                attendanceStatus = status,
                                checkInTime = checkInTime
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid entries
                        android.util.Log.w("FirebaseInstatt", "Failed to parse student: ${e.message}")
                    }
                }

                // 发送最新的学生列表（可能为空）
                trySend(students)
            }

            override fun onCancelled(error: DatabaseError) {
                // Firebase 连接失败时，也发送空列表，而不是让 UI 一直等待
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
     * 学生签到
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun signIn(
        courseScheduleId: String,
        date: String,
        studentId: Long,
        studentName: String,
        matricNumber: String? = null,
        email: String? = null
    ): Result<Unit> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)

            // 先检查 session 是否 unlocked
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

            // 写入签到数据
            val studentData = mapOf(
                "studentId" to studentId,
                "studentName" to studentName,
                "matricNumber" to matricNumber,
                "email" to email,
                "status" to AttendanceStatus.PRESENT.name,
                "checkInTime" to java.time.Instant.now().toString(),
                "timestamp" to System.currentTimeMillis()
            )

            sessionsRef.child(sessionKey).child("students").child(studentId.toString())
                .setValue(studentData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 实时监听 session 的锁定状态（Flow）- 学生端使用
     * 当教师 unlock session 时，学生端的签到按钮立即变亮
     *
     * 重要：如果 session 不存在，默认返回 true (locked)
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    fun listenToSessionLockStatus(
        courseScheduleId: String,
        date: String
    ): Flow<Boolean> = callbackFlow {
        val sessionKey = getSessionKey(courseScheduleId, date)
        val lockRef = sessionsRef.child(sessionKey).child("isLocked")

        // 立即发送默认值 (locked)，避免 UI 卡住
        trySend(true)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 如果 session 不存在或 isLocked 字段不存在，默认为 locked (true)
                val isLocked = if (snapshot.exists()) {
                    snapshot.getValue(Boolean::class.java) ?: true
                } else {
                    true // session 还没创建，默认锁定
                }

                trySend(isLocked)

                // 日志输出
                android.util.Log.d(
                    "FirebaseInstatt",
                    "Session $sessionKey: isLocked=$isLocked (exists=${snapshot.exists()})"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                // 连接失败时，也返回 locked 状态
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
     * 检查学生是否已经签到
     * ✅ 修复：courseScheduleId 改为 String 以支持 Firebase ID
     */
    suspend fun hasStudentSignedIn(
        courseScheduleId: String,
        date: String,
        studentId: Long
    ): Result<Boolean> {
        return try {
            val sessionKey = getSessionKey(courseScheduleId, date)
            val snapshot = sessionsRef.child(sessionKey)
                .child("students")
                .child(studentId.toString())
                .get()
                .await()

            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Utility Functions ====================

    /**
     * 检查并执行20分钟自动锁定
     * 如果session超过autoLockTime且仍未锁定，则自动锁定并标记未签到学生为缺席
     *
     * @param courseScheduleId 课程排课ID
     * @param date 日期
     * @param enrolledStudents 所有选课学生列表（用于标记缺席）
     * @return Result<Boolean> - true表示执行了自动锁定，false表示无需锁定
     */
    suspend fun checkAndAutoLockSession(
        courseScheduleId: String,
        date: String,
        enrolledStudents: List<Pair<Long, String>>  // List of (studentId, studentName)
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

            // 如果已经锁定或没有设置autoLockTime，无需操作
            if (isLocked || autoLockTime == 0L) {
                return Result.success(false)
            }

            // 检查是否超过autoLockTime
            if (currentTime >= autoLockTime) {
                android.util.Log.d(
                    "FirebaseInstatt",
                    "Auto-locking session $sessionKey (exceeded 20 minutes)"
                )

                // 1. 锁定session
                val lockUpdates = mapOf(
                    "isLocked" to true,
                    "isActive" to false,
                    "autoLockedAt" to currentTime
                )
                sessionRef.updateChildren(lockUpdates).await()

                // 2. 标记所有未签到学生为缺席
                val studentsSnapshot = sessionRef.child("students").get().await()
                val signedInStudentIds = studentsSnapshot.children.mapNotNull {
                    it.child("studentId").getValue(Long::class.java)
                }.toSet()

                for ((studentId, studentName) in enrolledStudents) {
                    if (studentId !in signedInStudentIds) {
                        // 标记为缺席
                        val absentData = mapOf(
                            "studentId" to studentId,
                            "studentName" to studentName,
                            "status" to AttendanceStatus.ABSENT.name,
                            "markedAt" to currentTime,
                            "autoMarked" to true  // 标记这是自动标记的缺席
                        )
                        sessionRef.child("students").child(studentId.toString())
                            .setValue(absentData).await()

                        android.util.Log.d(
                            "FirebaseInstatt",
                            "Auto-marked student $studentId ($studentName) as ABSENT"
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
     * 删除过期的 session（可选的清理功能）
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
