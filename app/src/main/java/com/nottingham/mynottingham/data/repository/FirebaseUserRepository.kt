package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase User Repository
 *
 * 直接从 Firebase Realtime Database 读取用户数据
 * 不再依赖 Spring Boot 后端 API
 *
 * 数据来源: Firebase users/{userId}
 * {
 *   username: "student1",
 *   fullName: "Alice Wong",
 *   email: "student1@student.nottingham.edu.my",
 *   role: "STUDENT",
 *   studentId: 1,
 *   matricNumber: "20123456",
 *   faculty: "Faculty of Science and Engineering"
 * }
 */
class FirebaseUserRepository {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef: DatabaseReference = database.getReference("users")

    /**
     * 获取用户资料 (实时监听)
     * @param userId 用户ID (如 "student1", "teacher1")
     * @return Flow<User> 用户数据流，自动更新
     */
    fun getUserProfile(userId: String): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                try {
                    val username = snapshot.child("username").getValue(String::class.java) ?: userId
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val role = snapshot.child("role").getValue(String::class.java) ?: "STUDENT"

                    // 学生特有字段
                    val studentId = snapshot.child("studentId").getValue(Long::class.java)?.toString() ?: ""
                    val matricNumber = snapshot.child("matricNumber").getValue(String::class.java) ?: ""
                    val faculty = snapshot.child("faculty").getValue(String::class.java) ?: ""

                    // 教师特有字段
                    val employeeId = snapshot.child("employeeId").getValue(String::class.java)
                    val department = snapshot.child("department").getValue(String::class.java)

                    // 映射到 User 模型
                    val user = User(
                        id = userId,
                        name = fullName,
                        email = email,
                        studentId = if (role == "STUDENT") studentId else (employeeId ?: ""),
                        faculty = if (role == "STUDENT") faculty else (department ?: ""),
                        year = 2, // TODO: 从 Firebase 添加 year 字段，或从 matricNumber 解析
                        program = "Computer Science", // TODO: 从 Firebase 添加 program 字段
                        profileImageUrl = null // TODO: 支持头像上传到 Firebase Storage
                    )

                    trySend(user)
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseUserRepo", "Error parsing user data: ${e.message}")
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseUserRepo", "Failed to read user: ${error.message}")
                close(error.toException())
            }
        }

        usersRef.child(userId).addValueEventListener(listener)

        awaitClose {
            usersRef.child(userId).removeEventListener(listener)
        }
    }

    /**
     * 获取用户资料 (一次性读取)
     * @param userId 用户ID
     * @return Result<User> 用户数据或错误
     */
    suspend fun getUserProfileOnce(userId: String): Result<User> {
        return try {
            val snapshot = usersRef.child(userId).get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("User not found: $userId"))
            }

            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: ""
            val role = snapshot.child("role").getValue(String::class.java) ?: "STUDENT"
            val studentId = snapshot.child("studentId").getValue(Long::class.java)?.toString() ?: ""
            val matricNumber = snapshot.child("matricNumber").getValue(String::class.java) ?: ""
            val faculty = snapshot.child("faculty").getValue(String::class.java) ?: ""

            val user = User(
                id = userId,
                name = fullName,
                email = email,
                studentId = studentId,
                faculty = faculty,
                year = 2,
                program = "Computer Science",
                profileImageUrl = null
            )

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error fetching user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新用户资料
     * @param userId 用户ID
     * @param updates 要更新的字段 Map
     * @return Result<Unit> 成功或失败
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersRef.child(userId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error updating user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 检查用户是否存在
     * @param userId 用户ID
     * @return Boolean 是否存在
     */
    suspend fun userExists(userId: String): Boolean {
        return try {
            usersRef.child(userId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 根据用户名查找用户ID (用于登录)
     * @param username 用户名
     * @return String? 用户ID 或 null
     */
    suspend fun findUserIdByUsername(username: String): String? {
        return try {
            val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
            snapshot.children.firstOrNull()?.key
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error finding user by username: ${e.message}")
            null
        }
    }
}
