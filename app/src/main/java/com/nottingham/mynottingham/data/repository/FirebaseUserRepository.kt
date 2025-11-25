package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
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
 * 数据结构:
 * - users/{firebaseUID}: 用户详细信息
 * - username_to_uid/{username}: 用户名到 UID 的映射
 *
 * 示例:
 * users/abc123xyz/ {
 *   username: "student1",
 *   fullName: "Alice Wong",
 *   email: "student1@nottingham.edu.my",
 *   role: "STUDENT",
 *   studentId: 1,
 *   matricNumber: "20123456",
 *   faculty: "Faculty of Science and Engineering"
 * }
 *
 * username_to_uid/ {
 *   student1: "abc123xyz",
 *   teacher1: "def456uvw"
 * }
 */
class FirebaseUserRepository {

    // ⚠️ 重要：必须指定数据库 URL，因为数据库在 asia-southeast1 区域
    // 如果不指定，Android SDK 会默认连接到美国服务器，导致无法读取数据
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef: DatabaseReference = database.getReference("users")
    private val usernameToUidRef: DatabaseReference = database.getReference("username_to_uid")
    private val presenceRef: DatabaseReference = database.getReference("presence")
    private val connectedRef: DatabaseReference = database.getReference(".info/connected")

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
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
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
                    val title = snapshot.child("title").getValue(String::class.java)
                    val officeRoom = snapshot.child("officeRoom").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    // 映射到 User 模型
                    val user = User(
                        id = userId,
                        username = username,
                        name = fullName,
                        email = email,
                        role = role,
                        studentId = if (role == "STUDENT") studentId else (employeeId ?: ""),
                        faculty = if (role == "STUDENT") faculty else (department ?: ""),
                        year = 2, // TODO: 从 Firebase 添加 year 字段，或从 matricNumber 解析
                        program = "Computer Science", // TODO: 从 Firebase 添加 program 字段
                        title = title,
                        officeRoom = officeRoom,
                        profileImageUrl = profileImageUrl
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

            val username = snapshot.child("username").getValue(String::class.java) ?: ""
            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: ""
            val role = snapshot.child("role").getValue(String::class.java) ?: "STUDENT"
            val studentId = snapshot.child("studentId").getValue(Long::class.java)?.toString() ?: ""
            val matricNumber = snapshot.child("matricNumber").getValue(String::class.java) ?: ""
            val faculty = snapshot.child("faculty").getValue(String::class.java) ?: ""
            val employeeId = snapshot.child("employeeId").getValue(String::class.java)
            val department = snapshot.child("department").getValue(String::class.java)

            // Teacher-specific fields
            val title = snapshot.child("title").getValue(String::class.java)
            val officeRoom = snapshot.child("officeRoom").getValue(String::class.java)
            val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

            // Delivery mode for Campus Errand
            val deliveryMode = snapshot.child("deliveryMode").getValue(Boolean::class.java) ?: false

            val user = User(
                id = userId,
                username = username,
                name = fullName,
                email = email,
                role = role,
                studentId = if (role == "STUDENT") studentId else (employeeId ?: ""),
                faculty = if (role == "STUDENT") faculty else (department ?: ""),
                year = 2,
                program = "Computer Science",
                title = title,
                officeRoom = officeRoom,
                profileImageUrl = profileImageUrl,
                deliveryMode = deliveryMode
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

            // 🔴 如果更新了头像，同步更新所有相关对话中的头像
            if (updates.containsKey("profileImageUrl")) {
                val newAvatar = updates["profileImageUrl"] as? String
                updateAvatarInConversations(userId, newAvatar)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error updating user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新用户在所有对话中的头像
     * 当用户更改头像时，需要同步更新其他用户的对话列表中显示的头像
     *
     * 🔴 数据结构：user_conversations/{otherUserId}/{conversationId}/participantId = userId
     * 我们需要找到所有其他用户的对话中 participantId 等于当前用户的记录，并更新 participantAvatar
     *
     * @param userId 更改头像的用户ID
     * @param newAvatar 新头像 key
     */
    private suspend fun updateAvatarInConversations(userId: String, newAvatar: String?) {
        try {
            val userConversationsRef = database.getReference("user_conversations")

            // 1. 获取当前用户的所有对话，从中得知参与者
            val myConversations = userConversationsRef.child(userId).get().await()

            myConversations.children.forEach { convSnapshot ->
                val conversationId = convSnapshot.key ?: return@forEach
                // 🔴 从对话数据中获取对方的 participantId
                val otherUserId = convSnapshot.child("participantId").getValue(String::class.java) ?: return@forEach

                // 2. 🔴 先检查对方的对话是否存在，避免创建不完整的记录
                try {
                    val otherConvRef = userConversationsRef.child(otherUserId).child(conversationId)
                    val otherConvSnapshot = otherConvRef.get().await()

                    // 只有当对话存在且有完整数据时才更新头像
                    if (otherConvSnapshot.exists() && otherConvSnapshot.child("participantId").exists()) {
                        otherConvRef.child("participantAvatar").setValue(newAvatar).await()
                        android.util.Log.d("FirebaseUserRepo",
                            "Updated avatar in conversation $conversationId for user $otherUserId")
                    } else {
                        android.util.Log.d("FirebaseUserRepo",
                            "Skipped updating avatar - conversation $conversationId doesn't exist for user $otherUserId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirebaseUserRepo",
                        "Failed to update avatar for $otherUserId in $conversationId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error updating avatar in conversations: ${e.message}")
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
     * 获取所有用户 (用于联系人列表)
     * @param excludeUserId 排除的用户ID (通常是当前用户)
     * @return Result<List<User>> 用户列表
     */
    suspend fun getAllUsers(excludeUserId: String? = null): Result<List<User>> {
        return try {
            val snapshot = usersRef.get().await()
            val users = mutableListOf<User>()

            for (child in snapshot.children) {
                val uid = child.key ?: continue

                // Skip the current user
                if (uid == excludeUserId) continue

                val username = child.child("username").getValue(String::class.java) ?: ""
                val fullName = child.child("fullName").getValue(String::class.java) ?: ""
                val email = child.child("email").getValue(String::class.java) ?: ""
                val role = child.child("role").getValue(String::class.java) ?: "STUDENT"

                // Skip admin users from contact list
                if (role == "ADMIN") continue

                val studentId = child.child("studentId").getValue(Long::class.java)?.toString() ?: ""
                val matricNumber = child.child("matricNumber").getValue(String::class.java) ?: ""
                val faculty = child.child("faculty").getValue(String::class.java) ?: ""
                val employeeId = child.child("employeeId").getValue(String::class.java)
                val department = child.child("department").getValue(String::class.java)
                val title = child.child("title").getValue(String::class.java)
                val officeRoom = child.child("officeRoom").getValue(String::class.java)
                val profileImageUrl = child.child("profileImageUrl").getValue(String::class.java)

                val user = User(
                    id = uid,
                    username = username,
                    name = fullName,
                    email = email,
                    role = role,
                    studentId = if (role == "STUDENT") studentId else (employeeId ?: ""),
                    faculty = if (role == "STUDENT") faculty else (department ?: ""),
                    year = 2,
                    program = if (role == "STUDENT") "Computer Science" else (department ?: ""),
                    title = title,
                    officeRoom = officeRoom,
                    profileImageUrl = profileImageUrl
                )
                users.add(user)
            }

            android.util.Log.d("FirebaseUserRepo", "Loaded ${users.size} users from Firebase")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error fetching all users: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 根据用户名查找用户 UID (用于登录 - 已废弃)
     *
     * ⚠️ DEPRECATED: 不再需要此方法
     * 现在使用 Firebase Authentication 直接登录，不需要先查找 UID
     *
     * @param username 用户名
     * @return String? Firebase UID 或 null
     */
    @Deprecated(
        message = "Use Firebase Authentication instead. Convert username to email and use FirebaseAuth.signInWithEmailAndPassword()",
        replaceWith = ReplaceWith("FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)")
    )
    suspend fun findUserIdByUsername(username: String): String? {
        return try {
            // 优先使用 username_to_uid 映射 (更高效)
            val mappingSnapshot = usernameToUidRef.child(username).get().await()
            if (mappingSnapshot.exists()) {
                return mappingSnapshot.getValue(String::class.java)
            }

            // 备用方案：查询 users 表 (较慢)
            val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
            snapshot.children.firstOrNull()?.key
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error finding user by username: ${e.message}")
            null
        }
    }

    // ==================== Presence System (Telegram-style) ====================

    /**
     * 设置用户在线状态并配置断开连接时自动离线
     * 类似Telegram的在线状态逻辑
     *
     * @param userId 用户ID
     */
    fun setupPresence(userId: String) {
        val userPresenceRef = presenceRef.child(userId)

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false

                if (connected) {
                    // 用户连接时设置在线
                    val presenceData = mapOf(
                        "isOnline" to true,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                    userPresenceRef.setValue(presenceData)

                    // 配置断开连接时自动设置离线和最后在线时间
                    userPresenceRef.onDisconnect().setValue(
                        mapOf(
                            "isOnline" to false,
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseUserRepo", "Presence error: ${error.message}")
            }
        })
    }

    /**
     * 手动设置用户离线（用于登出）
     *
     * @param userId 用户ID
     */
    suspend fun setOffline(userId: String) {
        try {
            presenceRef.child(userId).setValue(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error setting offline: ${e.message}")
        }
    }

    /**
     * 监听用户在线状态（实时）
     *
     * @param userId 要监听的用户ID
     * @return Flow<Pair<Boolean, Long>> Pair(是否在线, 最后在线时间戳)
     */
    fun observeUserPresence(userId: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                trySend(Pair(isOnline, lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseUserRepo", "Presence observe error: ${error.message}")
            }
        }

        presenceRef.child(userId).addValueEventListener(listener)

        awaitClose {
            presenceRef.child(userId).removeEventListener(listener)
        }
    }

    /**
     * 一次性获取用户在线状态
     *
     * @param userId 用户ID
     * @return Pair<Boolean, Long> Pair(是否在线, 最后在线时间戳)
     */
    suspend fun getUserPresence(userId: String): Pair<Boolean, Long> {
        return try {
            val snapshot = presenceRef.child(userId).get().await()
            val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
            val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
            Pair(isOnline, lastSeen)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error getting presence: ${e.message}")
            Pair(false, 0L)
        }
    }

    /**
     * 格式化最后在线时间（Telegram风格）
     *
     * @param lastSeen 最后在线时间戳
     * @return String 格式化的字符串，如 "last seen just now", "last seen 5 minutes ago"
     */
    fun formatLastSeen(lastSeen: Long): String {
        if (lastSeen == 0L) return "Offline"

        val now = System.currentTimeMillis()
        val diff = now - lastSeen
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1 -> "last seen just now"
            minutes < 60 -> "last seen $minutes min ago"
            hours < 24 -> "last seen $hours hr ago"
            days < 7 -> "last seen $days days ago"
            else -> "last seen recently"
        }
    }
}
