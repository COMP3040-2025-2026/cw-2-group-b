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
 * Reads user data directly from Firebase Realtime Database.
 * No longer depends on Spring Boot backend API.
 *
 * Data structure:
 * - users/{firebaseUID}: User details
 * - username_to_uid/{username}: Username to UID mapping
 *
 * Example:
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

    // Important: Must specify database URL as database is in asia-southeast1 region
    // If not specified, Android SDK defaults to US server, causing data read failures
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef: DatabaseReference = database.getReference("users")
    private val usernameToUidRef: DatabaseReference = database.getReference("username_to_uid")
    private val presenceRef: DatabaseReference = database.getReference("presence")
    private val connectedRef: DatabaseReference = database.getReference(".info/connected")

    /**
     * Get user profile (real-time listener)
     * @param userId User ID (e.g. "student1", "teacher1")
     * @return Flow<User> User data flow with auto-updates
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

                    // Student-specific fields
                    val studentId = snapshot.child("studentId").getValue(Long::class.java)?.toString() ?: ""
                    val matricNumber = snapshot.child("matricNumber").getValue(String::class.java) ?: ""
                    val faculty = snapshot.child("faculty").getValue(String::class.java) ?: ""

                    // Teacher-specific fields
                    val employeeId = snapshot.child("employeeId").getValue(String::class.java)
                    val department = snapshot.child("department").getValue(String::class.java)
                    val title = snapshot.child("title").getValue(String::class.java)
                    val officeRoom = snapshot.child("officeRoom").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    // Map to User model
                    val user = User(
                        id = userId,
                        username = username,
                        name = fullName,
                        email = email,
                        role = role,
                        studentId = if (role == "STUDENT") studentId else (employeeId ?: ""),
                        faculty = if (role == "STUDENT") faculty else (department ?: ""),
                        year = 2, // TODO: Add year field from Firebase or parse from matricNumber
                        program = "Computer Science", // TODO: Add program field from Firebase
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
     * Get user profile (one-time read)
     * @param userId User ID
     * @return Result<User> User data or error
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
     * Update user profile
     * @param userId User ID
     * @param updates Map of fields to update
     * @return Result<Unit> Success or failure
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersRef.child(userId).updateChildren(updates).await()

            // If avatar was updated, sync avatar across all related conversations
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
     * Update user avatar in all conversations
     * When user changes avatar, sync to all other users' conversation lists
     *
     * Data structure: user_conversations/{otherUserId}/{conversationId}/participantId = userId
     * Find all records where participantId equals current user and update participantAvatar
     *
     * @param userId User ID who changed avatar
     * @param newAvatar New avatar key
     */
    private suspend fun updateAvatarInConversations(userId: String, newAvatar: String?) {
        try {
            val userConversationsRef = database.getReference("user_conversations")

            // 1. Get all conversations of current user to find participants
            val myConversations = userConversationsRef.child(userId).get().await()

            myConversations.children.forEach { convSnapshot ->
                val conversationId = convSnapshot.key ?: return@forEach
                // Get the other user's participantId from conversation data
                val otherUserId = convSnapshot.child("participantId").getValue(String::class.java) ?: return@forEach

                // 2. Check if other user's conversation exists first to avoid creating incomplete records
                try {
                    val otherConvRef = userConversationsRef.child(otherUserId).child(conversationId)
                    val otherConvSnapshot = otherConvRef.get().await()

                    // Only update avatar when conversation exists with complete data
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
     * Check if user exists
     * @param userId User ID
     * @return Boolean Whether user exists
     */
    suspend fun userExists(userId: String): Boolean {
        return try {
            usersRef.child(userId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all users (for contact list)
     * @param excludeUserId User ID to exclude (usually current user)
     * @return Result<List<User>> User list
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
     * Find user UID by username (for login - deprecated)
     *
     * DEPRECATED: This method is no longer needed
     * Now using Firebase Authentication directly, no need to lookup UID first
     *
     * @param username Username
     * @return String? Firebase UID or null
     */
    @Deprecated(
        message = "Use Firebase Authentication instead. Convert username to email and use FirebaseAuth.signInWithEmailAndPassword()",
        replaceWith = ReplaceWith("FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)")
    )
    suspend fun findUserIdByUsername(username: String): String? {
        return try {
            // Prefer using username_to_uid mapping (more efficient)
            val mappingSnapshot = usernameToUidRef.child(username).get().await()
            if (mappingSnapshot.exists()) {
                return mappingSnapshot.getValue(String::class.java)
            }

            // Fallback: query users table (slower)
            val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
            snapshot.children.firstOrNull()?.key
        } catch (e: Exception) {
            android.util.Log.e("FirebaseUserRepo", "Error finding user by username: ${e.message}")
            null
        }
    }

    // ==================== Presence System (Telegram-style) ====================

    /**
     * Set up user presence and configure auto-offline on disconnect
     * Telegram-style online status logic
     *
     * @param userId User ID
     */
    fun setupPresence(userId: String) {
        val userPresenceRef = presenceRef.child(userId)

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false

                if (connected) {
                    // Set online when user connects
                    val presenceData = mapOf(
                        "isOnline" to true,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                    userPresenceRef.setValue(presenceData)

                    // Configure auto-offline and last seen time on disconnect
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
     * Manually set user offline (for logout)
     *
     * @param userId User ID
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
     * Observe user online status (real-time)
     *
     * @param userId User ID to observe
     * @return Flow<Pair<Boolean, Long>> Pair(isOnline, lastSeenTimestamp)
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
     * Get user online status (one-time)
     *
     * @param userId User ID
     * @return Pair<Boolean, Long> Pair(isOnline, lastSeenTimestamp)
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
     * Format last seen time (Telegram-style)
     *
     * @param lastSeen Last seen timestamp
     * @return String Formatted string like "last seen just now", "last seen 5 minutes ago"
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
