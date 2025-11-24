package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Message Repository
 *
 * 直接从 Firebase Realtime Database 读取和管理聊天消息
 * 不再依赖 Spring Boot 后端 API
 *
 * Firebase 数据结构：
 * conversations/{conversationId}/
 *   metadata/
 *     - isGroup: boolean
 *     - groupName: string (optional)
 *     - createdAt: timestamp
 *     - lastMessage: string
 *     - lastMessageTime: timestamp
 *     - participants: {uid1: true, uid2: true}
 *   messages/{messageId}/
 *     - senderId: string
 *     - senderName: string
 *     - message: string
 *     - timestamp: timestamp
 *     - isRead: boolean
 *     - messageType: "TEXT"
 *
 * user_conversations/{userId}/{conversationId}/
 *   - unreadCount: number
 *   - isPinned: boolean
 *   - participantIds: [uid1, uid2]
 *   - participantName: string
 *   - participantAvatar: string
 */
class FirebaseMessageRepository {

    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val conversationsRef: DatabaseReference = database.getReference("conversations")
    private val userConversationsRef: DatabaseReference = database.getReference("user_conversations")

    /**
     * 获取当前用户的对话列表（实时监听）
     * @param userId 当前用户ID
     * @return Flow<List<Conversation>> 对话列表流
     */
    fun getConversationsFlow(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<Conversation>()

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { child ->
                    try {
                        val conversationId = child.key ?: return@forEach
                        val unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0
                        val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                        val participantIds = child.child("participantIds").children.mapNotNull { it.getValue(String::class.java) }
                        val participantName = child.child("participantName").getValue(String::class.java) ?: "Unknown"
                        val participantAvatar = child.child("participantAvatar").getValue(String::class.java)
                        val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false

                        // 获取对方用户ID（非群组情况）
                        val participantId = if (!isGroup && participantIds.size >= 2) {
                            participantIds.find { it != userId } ?: participantIds.first()
                        } else {
                            participantIds.firstOrNull() ?: ""
                        }

                        conversations.add(
                            Conversation(
                                id = conversationId,
                                participantId = participantId,
                                participantName = participantName,
                                participantAvatar = participantAvatar,
                                lastMessage = lastMessage,
                                lastMessageTime = lastMessageTime,
                                unreadCount = unreadCount,
                                isOnline = isOnline,
                                isPinned = isPinned,
                                isGroup = isGroup
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing conversation: ${e.message}")
                    }
                }

                // 按置顶和最后消息时间排序
                conversations.sortWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime })
                trySend(conversations)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseMessageRepo", "Error listening to conversations: ${error.message}")
                close(error.toException())
            }
        }

        userConversationsRef.child(userId).addValueEventListener(listener)

        awaitClose {
            userConversationsRef.child(userId).removeEventListener(listener)
        }
    }

    /**
     * 获取指定对话的消息列表（实时监听）
     * @param conversationId 对话ID
     * @return Flow<List<ChatMessage>> 消息列表流
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { child ->
                    try {
                        val messageId = child.key ?: return@forEach
                        val senderId = child.child("senderId").getValue(String::class.java) ?: ""
                        val senderName = child.child("senderName").getValue(String::class.java) ?: "Unknown"
                        val senderAvatar = child.child("senderAvatar").getValue(String::class.java)
                        val message = child.child("message").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        val messageType = child.child("messageType").getValue(String::class.java) ?: "TEXT"

                        messages.add(
                            ChatMessage(
                                id = messageId,
                                conversationId = conversationId,
                                senderId = senderId,
                                senderName = senderName,
                                senderAvatar = senderAvatar,
                                message = message,
                                timestamp = timestamp,
                                isRead = isRead,
                                messageType = messageType
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing message: ${e.message}")
                    }
                }

                // 按时间顺序排列
                messages.sortBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseMessageRepo", "Error listening to messages: ${error.message}")
                close(error.toException())
            }
        }

        conversationsRef.child(conversationId).child("messages").addValueEventListener(listener)

        awaitClose {
            conversationsRef.child(conversationId).child("messages").removeEventListener(listener)
        }
    }

    /**
     * 发送消息
     * @param conversationId 对话ID
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param senderAvatar 发送者头像URL
     * @param message 消息内容
     * @param messageType 消息类型（TEXT, IMAGE, FILE）
     * @return Result<String> 消息ID或错误
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String?,
        message: String,
        messageType: String = "TEXT"
    ): Result<String> {
        return try {
            val timestamp = System.currentTimeMillis()
            val newMessageRef = conversationsRef.child(conversationId).child("messages").push()
            val messageId = newMessageRef.key ?: throw Exception("Failed to generate message ID")

            val messageData = mapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "message" to message,
                "timestamp" to timestamp,
                "isRead" to false,
                "messageType" to messageType
            )

            // 保存消息
            newMessageRef.setValue(messageData).await()

            // 更新对话的最后消息信息
            val metadataUpdates = mapOf(
                "lastMessage" to message,
                "lastMessageTime" to timestamp
            )
            conversationsRef.child(conversationId).child("metadata").updateChildren(metadataUpdates).await()

            // 更新所有参与者的未读计数（除了发送者自己）
            updateUnreadCountForParticipants(conversationId, senderId)

            Result.success(messageId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 创建新对话（一对一或群组）
     * @param participantIds 参与者ID列表
     * @param currentUserId 当前用户ID
     * @param currentUserName 当前用户名称
     * @param isGroup 是否为群组
     * @param groupName 群组名称（可选）
     * @return Result<String> 对话ID或错误
     */
    suspend fun createConversation(
        participantIds: List<String>,
        currentUserId: String,
        currentUserName: String,
        isGroup: Boolean = false,
        groupName: String? = null
    ): Result<String> {
        return try {
            // 检查是否已存在相同参与者的对话
            val existingConvId = findExistingConversation(currentUserId, participantIds)
            if (existingConvId != null) {
                return Result.success(existingConvId)
            }

            val newConvRef = conversationsRef.push()
            val conversationId = newConvRef.key ?: throw Exception("Failed to generate conversation ID")
            val timestamp = System.currentTimeMillis()

            // 创建对话元数据
            val participantsMap = participantIds.associateWith { true }
            val metadataMap = mutableMapOf<String, Any>(
                "isGroup" to isGroup,
                "createdAt" to timestamp,
                "lastMessage" to "",
                "lastMessageTime" to timestamp,
                "participants" to participantsMap
            )
            if (isGroup && groupName != null) {
                metadataMap["groupName"] = groupName
            }

            newConvRef.child("metadata").setValue(metadataMap).await()

            // 为每个参与者创建用户对话记录
            participantIds.forEach { participantId ->
                val otherParticipants = participantIds.filter { it != participantId }
                val userConvData = mutableMapOf<String, Any>(
                    "unreadCount" to 0,
                    "isPinned" to false,
                    "participantIds" to participantIds,
                    "lastMessage" to "",
                    "lastMessageTime" to timestamp,
                    "isGroup" to isGroup,
                    "isOnline" to false
                )

                // 如果不是群组，设置对方用户的名称和头像
                if (!isGroup && otherParticipants.isNotEmpty()) {
                    val otherUserId = otherParticipants.first()
                    // 这里需要从 users 节点获取对方的名称和头像
                    // 暂时使用占位符，后续可以通过 getUserInfo 方法获取
                    userConvData["participantName"] = "Loading..."
                    // 不设置 participantAvatar，保持为空
                } else if (isGroup && groupName != null) {
                    userConvData["participantName"] = groupName
                }

                userConversationsRef.child(participantId).child(conversationId).setValue(userConvData).await()
            }

            Result.success(conversationId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error creating conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 标记消息为已读
     * @param conversationId 对话ID
     * @param userId 当前用户ID
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Result<Unit> {
        return try {
            // 重置未读计数
            userConversationsRef.child(userId).child(conversationId).child("unreadCount").setValue(0).await()

            // 标记所有非当前用户发送的消息为已读
            val messagesRef = conversationsRef.child(conversationId).child("messages")
            val snapshot = messagesRef.get().await()

            snapshot.children.forEach { child ->
                val senderId = child.child("senderId").getValue(String::class.java)
                val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false

                if (senderId != userId && !isRead) {
                    child.ref.child("isRead").setValue(true)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error marking messages as read: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 置顶/取消置顶对话
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @param pinned 是否置顶
     */
    suspend fun togglePinConversation(userId: String, conversationId: String, pinned: Boolean): Result<Unit> {
        return try {
            userConversationsRef.child(userId).child(conversationId).child("isPinned").setValue(pinned).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error toggling pin: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 删除对话
     * @param userId 用户ID
     * @param conversationId 对话ID
     */
    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        return try {
            // 只删除用户的对话记录，不删除实际对话数据
            userConversationsRef.child(userId).child(conversationId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error deleting conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 搜索用户（用于创建新对话时的联系人建议）
     * @param query 搜索关键词
     * @return Flow<List<Map<String, String>>> 用户列表
     */
    fun searchUsers(query: String): Flow<List<Map<String, String>>> = callbackFlow {
        val usersRef = database.getReference("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<Map<String, String>>()

                snapshot.children.forEach { child ->
                    try {
                        val uid = child.key ?: return@forEach
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val username = child.child("username").getValue(String::class.java) ?: ""
                        val email = child.child("email").getValue(String::class.java) ?: ""
                        val avatar = child.child("profileImageUrl").getValue(String::class.java)

                        // 简单的模糊搜索
                        if (name.contains(query, ignoreCase = true) ||
                            username.contains(query, ignoreCase = true) ||
                            email.contains(query, ignoreCase = true)) {
                            users.add(
                                mapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "username" to username,
                                    "email" to email,
                                    "avatar" to (avatar ?: "")
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing user: ${e.message}")
                    }
                }

                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        usersRef.addValueEventListener(listener)

        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 查找已存在的对话（相同参与者）
     */
    private suspend fun findExistingConversation(currentUserId: String, participantIds: List<String>): String? {
        return try {
            val snapshot = userConversationsRef.child(currentUserId).get().await()
            val allParticipants = (participantIds + currentUserId).sorted()

            snapshot.children.forEach { child ->
                val convParticipants = child.child("participantIds").children
                    .mapNotNull { it.getValue(String::class.java) }
                    .sorted()

                if (convParticipants == allParticipants) {
                    return child.key
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error finding existing conversation: ${e.message}")
            null
        }
    }

    /**
     * 更新所有参与者的未读计数（除了发送者）
     */
    private suspend fun updateUnreadCountForParticipants(conversationId: String, senderId: String) {
        try {
            val metadataSnapshot = conversationsRef.child(conversationId).child("metadata").get().await()
            val participants = metadataSnapshot.child("participants").children.mapNotNull { it.key }

            participants.filter { it != senderId }.forEach { participantId ->
                val userConvRef = userConversationsRef.child(participantId).child(conversationId).child("unreadCount")
                val currentCount = userConvRef.get().await().getValue(Int::class.java) ?: 0
                userConvRef.setValue(currentCount + 1)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating unread counts: ${e.message}")
        }
    }

    /**
     * 更新用户对话中的参与者信息（名称和头像）
     * 应该在创建对话后调用，传入实际的用户数据
     */
    suspend fun updateConversationParticipantInfo(
        userId: String,
        conversationId: String,
        participantName: String,
        participantAvatar: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "participantName" to participantName
            )
            if (participantAvatar != null) {
                updates["participantAvatar"] = participantAvatar
            }

            userConversationsRef.child(userId).child(conversationId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating participant info: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新用户打字状态
     * @param conversationId 对话ID
     * @param userId 用户ID
     * @param isTyping 是否正在输入
     * @return Result<Unit> 成功或错误
     */
    suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            // 在对话元数据中更新打字状态
            // 使用临时节点存储，设置过期时间（例如3秒后自动清除）
            val typingRef = conversationsRef.child(conversationId).child("typing").child(userId)

            if (isTyping) {
                val typingData = mapOf(
                    "isTyping" to true,
                    "timestamp" to System.currentTimeMillis()
                )
                typingRef.setValue(typingData).await()
            } else {
                typingRef.removeValue().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating typing status: ${e.message}")
            Result.failure(e)
        }
    }
}
