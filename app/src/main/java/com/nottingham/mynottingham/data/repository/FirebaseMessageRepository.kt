package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.util.FcmNotificationSender
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
     *
     * 🔴 修复：实时获取对方用户的最新头像和在线状态
     */
    fun getConversationsFlow(userId: String): Flow<List<Conversation>> = callbackFlow {
        val usersRef = database.getReference("users")
        val presenceRef = database.getReference("presence")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // 🔴 每次数据变化都重新获取头像和在线状态
                val avatarCache = mutableMapOf<String, String?>()
                val presenceCache = mutableMapOf<String, Boolean>()

                // Step 1: 解析所有对话，收集需要获取头像的用户ID
                val rawConversations = mutableListOf<Map<String, Any?>>()
                val participantIdsToFetch = mutableSetOf<String>()

                snapshot.children.forEach { child ->
                    try {
                        val conversationId = child.key ?: return@forEach
                        val unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0
                        val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                        // 🔴 直接读取 participantId，数据库中没有 participantIds 数组
                        val participantId = child.child("participantId").getValue(String::class.java) ?: ""
                        val participantName = child.child("participantName").getValue(String::class.java) ?: "Unknown"
                        val participantAvatar = child.child("participantAvatar").getValue(String::class.java)
                        val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false

                        // 🔴 获取对方的最新头像和在线状态（无论是否群组，都需要获取）
                        if (participantId.isNotEmpty()) {
                            participantIdsToFetch.add(participantId)
                        }

                        rawConversations.add(
                            mapOf(
                                "id" to conversationId,
                                "participantId" to participantId,
                                "participantName" to participantName,
                                "participantAvatar" to participantAvatar,
                                "lastMessage" to lastMessage,
                                "lastMessageTime" to lastMessageTime,
                                "unreadCount" to unreadCount,
                                "isPinned" to isPinned,
                                "isGroup" to isGroup
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing conversation: ${e.message}")
                    }
                }

                // Step 2: 获取需要的头像和在线状态
                if (participantIdsToFetch.isEmpty()) {
                    // 没有需要获取头像的对话（全是群组）
                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                    trySend(conversations)
                } else {
                    // 需要获取的总数：头像 + 在线状态，每个 participant 需要 2 次请求
                    val totalRequests = participantIdsToFetch.size * 2
                    var fetchedCount = 0

                    participantIdsToFetch.forEach { participantId ->
                        // 获取头像
                        usersRef.child(participantId).child("profileImageUrl").get()
                            .addOnSuccessListener { avatarSnapshot ->
                                avatarCache[participantId] = avatarSnapshot.getValue(String::class.java)
                                fetchedCount++
                                if (fetchedCount == totalRequests) {
                                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                                    trySend(conversations)
                                }
                            }
                            .addOnFailureListener {
                                avatarCache[participantId] = null
                                fetchedCount++
                                if (fetchedCount == totalRequests) {
                                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                                    trySend(conversations)
                                }
                            }

                        // 🔴 获取在线状态 from presence node
                        presenceRef.child(participantId).child("isOnline").get()
                            .addOnSuccessListener { presenceSnapshot ->
                                presenceCache[participantId] = presenceSnapshot.getValue(Boolean::class.java) ?: false
                                fetchedCount++
                                if (fetchedCount == totalRequests) {
                                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                                    trySend(conversations)
                                }
                            }
                            .addOnFailureListener {
                                presenceCache[participantId] = false
                                fetchedCount++
                                if (fetchedCount == totalRequests) {
                                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                                    trySend(conversations)
                                }
                            }
                    }
                }
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
     * 🔥 新增功能：实时监听当前用户的【总未读消息数量】
     * 遍历 user_conversations 下的所有会话，累加 unreadCount
     */
    fun getTotalUnreadCountFlow(userId: String): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalUnread = 0
                // 遍历用户的所有会话
                for (conversationSnapshot in snapshot.children) {
                    val count = conversationSnapshot.child("unreadCount").getValue(Int::class.java) ?: 0
                    totalUnread += count
                }
                // 发送最新的总数
                trySend(totalUnread)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseMessageRepo", "计算未读数量失败: ${error.message}")
                trySend(0)
            }
        }

        // 监听 user_conversations/{userId} 节点的变化
        userConversationsRef.child(userId).addValueEventListener(listener)

        // 当 Flow 停止收集时（例如页面销毁），移除监听器
        awaitClose {
            userConversationsRef.child(userId).removeEventListener(listener)
        }
    }

    /**
     * 辅助方法：使用最新头像和在线状态构建对话列表
     */
    private fun buildConversations(
        rawConversations: List<Map<String, Any?>>,
        avatarCache: Map<String, String?>,
        presenceCache: Map<String, Boolean> = emptyMap()
    ): List<Conversation> {
        val conversations = rawConversations.map { raw ->
            val participantId = raw["participantId"] as String
            val isGroup = raw["isGroup"] as Boolean
            // 🔴 使用缓存的最新头像（从 users 表获取）
            val currentAvatar = if (participantId.isNotEmpty()) {
                avatarCache[participantId] ?: raw["participantAvatar"] as? String
            } else {
                raw["participantAvatar"] as? String
            }
            // 🔴 使用缓存的在线状态（从 presence 节点获取）
            val isOnline = if (participantId.isNotEmpty()) {
                presenceCache[participantId] ?: false
            } else {
                false
            }

            Conversation(
                id = raw["id"] as String,
                participantId = participantId,
                participantName = raw["participantName"] as String,
                participantAvatar = currentAvatar,
                lastMessage = raw["lastMessage"] as String,
                lastMessageTime = raw["lastMessageTime"] as Long,
                unreadCount = raw["unreadCount"] as Int,
                isOnline = isOnline,
                isPinned = raw["isPinned"] as Boolean,
                isGroup = isGroup
            )
        }

        // 按置顶和最后消息时间排序
        return conversations.sortedWith(
            compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime }
        )
    }

    /**
     * 获取指定对话的消息列表（实时监听）
     * @param conversationId 对话ID
     * @return Flow<List<ChatMessage>> 消息列表流
     *
     * 🔴 修复：实时获取发送者的最新头像，而不是使用消息中存储的旧头像
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // 🔴 每次数据变化都重新获取头像，确保头像是最新的
                val avatarCache = mutableMapOf<String, String?>()

                // Step 1: 解析所有消息，收集唯一的 senderId
                val uniqueSenderIds = mutableSetOf<String>()
                val rawMessages = mutableListOf<Map<String, Any?>>()

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

                        if (senderId.isNotEmpty()) {
                            uniqueSenderIds.add(senderId)
                        }
                        rawMessages.add(
                            mapOf(
                                "id" to messageId,
                                "senderId" to senderId,
                                "senderName" to senderName,
                                "senderAvatar" to senderAvatar,
                                "message" to message,
                                "timestamp" to timestamp,
                                "isRead" to isRead,
                                "messageType" to messageType
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing message: ${e.message}")
                    }
                }

                // Step 2: 获取所有发送者的最新头像（异步）
                if (uniqueSenderIds.isEmpty()) {
                    // 没有需要获取头像的消息
                    buildAndSendMessages(rawMessages, avatarCache, conversationId, messages)
                    messages.sortBy { it.timestamp }
                    trySend(messages)
                } else {
                    // 需要获取头像
                    var fetchedCount = 0
                    uniqueSenderIds.forEach { senderId ->
                        usersRef.child(senderId).child("profileImageUrl").get()
                            .addOnSuccessListener { avatarSnapshot ->
                                val currentAvatar = avatarSnapshot.getValue(String::class.java)
                                avatarCache[senderId] = currentAvatar
                                fetchedCount++

                                // 所有头像都获取完成后，构建消息列表
                                if (fetchedCount == uniqueSenderIds.size) {
                                    buildAndSendMessages(rawMessages, avatarCache, conversationId, messages)
                                    messages.sortBy { it.timestamp }
                                    trySend(messages)
                                }
                            }
                            .addOnFailureListener {
                                // 获取失败时使用消息中存储的头像
                                avatarCache[senderId] = null
                                fetchedCount++

                                if (fetchedCount == uniqueSenderIds.size) {
                                    buildAndSendMessages(rawMessages, avatarCache, conversationId, messages)
                                    messages.sortBy { it.timestamp }
                                    trySend(messages)
                                }
                            }
                    }
                }
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
     * 辅助方法：使用最新头像构建消息列表
     */
    private fun buildAndSendMessages(
        rawMessages: List<Map<String, Any?>>,
        avatarCache: Map<String, String?>,
        conversationId: String,
        messages: MutableList<ChatMessage>
    ) {
        messages.clear()
        rawMessages.forEach { raw ->
            val senderId = raw["senderId"] as String
            // 🔴 优先使用缓存中的最新头像，如果没有则使用消息中存储的头像
            val currentAvatar = avatarCache[senderId] ?: raw["senderAvatar"] as? String

            messages.add(
                ChatMessage(
                    id = raw["id"] as String,
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = raw["senderName"] as String,
                    senderAvatar = currentAvatar,
                    message = raw["message"] as String,
                    timestamp = raw["timestamp"] as Long,
                    isRead = raw["isRead"] as Boolean,
                    messageType = raw["messageType"] as String
                )
            )
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
            // 去除消息首尾空白
            val trimmedMessage = message.trim()

            val timestamp = System.currentTimeMillis()
            val newMessageRef = conversationsRef.child(conversationId).child("messages").push()
            val messageId = newMessageRef.key ?: throw Exception("Failed to generate message ID")

            val messageData = mapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "message" to trimmedMessage,
                "timestamp" to timestamp,
                "isRead" to false,
                "messageType" to messageType
            )

            // 保存消息
            newMessageRef.setValue(messageData).await()

            // 更新对话的最后消息信息 (metadata)
            val metadataUpdates = mapOf(
                "lastMessage" to trimmedMessage,
                "lastMessageTime" to timestamp
            )
            conversationsRef.child(conversationId).child("metadata").updateChildren(metadataUpdates).await()

            // 更新所有参与者的 user_conversations 中的 lastMessage 和 lastMessageTime
            updateLastMessageForAllParticipants(conversationId, trimmedMessage, timestamp, senderId)

            // 发送 FCM 推送通知给其他参与者
            sendPushNotificationToParticipants(conversationId, senderId, senderName, trimmedMessage)

            Result.success(messageId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 发送 FCM 推送通知给对话中的其他参与者
     */
    private suspend fun sendPushNotificationToParticipants(
        conversationId: String,
        senderId: String,
        senderName: String,
        message: String
    ) {
        try {
            // 获取对话参与者
            var participants: List<String> = emptyList()

            // 尝试从多种路径获取参与者
            val directSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            if (directSnapshot.exists() && directSnapshot.childrenCount > 0) {
                participants = directSnapshot.children.mapNotNull { it.key }
            }

            if (participants.isEmpty()) {
                val metadataSnapshot = conversationsRef.child(conversationId).child("metadata").child("participants").get().await()
                if (metadataSnapshot.exists() && metadataSnapshot.childrenCount > 0) {
                    participants = metadataSnapshot.children.mapNotNull { it.key }
                }
            }

            if (participants.isEmpty() && conversationId.contains("_")) {
                participants = conversationId.split("_")
            }

            android.util.Log.d("FirebaseMessageRepo", "Sending push to participants: $participants (except $senderId)")

            // 给每个参与者（除了发送者）发送推送通知
            val usersRef = database.getReference("users")
            participants.filter { it != senderId }.forEach { recipientId ->
                try {
                    val tokenSnapshot = usersRef.child(recipientId).child("fcmToken").get().await()
                    val fcmToken = tokenSnapshot.getValue(String::class.java)

                    if (!fcmToken.isNullOrEmpty()) {
                        val success = FcmNotificationSender.sendChatNotification(
                            recipientToken = fcmToken,
                            senderName = senderName,
                            messageContent = message,
                            conversationId = conversationId,
                            senderId = senderId
                        )
                        android.util.Log.d("FirebaseMessageRepo", "Push notification to $recipientId: ${if (success) "sent" else "failed"}")
                    } else {
                        android.util.Log.d("FirebaseMessageRepo", "No FCM token for user $recipientId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseMessageRepo", "Error sending push to $recipientId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error in sendPushNotificationToParticipants: ${e.message}")
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
     * 更新所有参与者的 user_conversations 中的 lastMessage, lastMessageTime 和 unreadCount
     */
    private suspend fun updateLastMessageForAllParticipants(
        conversationId: String,
        message: String,
        timestamp: Long,
        senderId: String
    ) {
        try {
            // 尝试从多种可能的路径读取 participants
            var participants: List<String> = emptyList()

            // 方法1: conversations/{id}/participants (新结构)
            val directSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            if (directSnapshot.exists() && directSnapshot.childrenCount > 0) {
                participants = directSnapshot.children.mapNotNull { it.key }
                android.util.Log.d("FirebaseMessageRepo", "Found participants in conversations/{id}/participants: $participants")
            }

            // 方法2: conversations/{id}/metadata/participants (旧结构)
            if (participants.isEmpty()) {
                val metadataSnapshot = conversationsRef.child(conversationId).child("metadata").child("participants").get().await()
                if (metadataSnapshot.exists() && metadataSnapshot.childrenCount > 0) {
                    participants = metadataSnapshot.children.mapNotNull { it.key }
                    android.util.Log.d("FirebaseMessageRepo", "Found participants in metadata: $participants")
                }
            }

            // 方法3: 从 conversationId 解析 (格式: {userId1}_{userId2})
            if (participants.isEmpty() && conversationId.contains("_")) {
                participants = conversationId.split("_")
                android.util.Log.d("FirebaseMessageRepo", "Parsed participants from conversationId: $participants")
            }

            android.util.Log.d("FirebaseMessageRepo", "Updating lastMessage for ${participants.size} participants")

            participants.forEach { participantId ->
                val userConvRef = userConversationsRef.child(participantId).child(conversationId)

                // 检查用户会话是否存在
                val exists = userConvRef.get().await().exists()
                if (!exists) {
                    android.util.Log.w("FirebaseMessageRepo", "User conversation not found for $participantId, skipping")
                    return@forEach
                }

                // 更新 lastMessage 和 lastMessageTime
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to message,
                    "lastMessageTime" to timestamp
                )

                // 如果不是发送者，增加未读计数
                if (participantId != senderId) {
                    val currentCount = userConvRef.child("unreadCount").get().await().getValue(Int::class.java) ?: 0
                    updates["unreadCount"] = currentCount + 1
                    android.util.Log.d("FirebaseMessageRepo", "Incrementing unread for $participantId: ${currentCount + 1}")
                }

                userConvRef.updateChildren(updates).await()
                android.util.Log.d("FirebaseMessageRepo", "Updated user_conversations for $participantId")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating last message for participants: ${e.message}", e)
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
     * 更新用户对话中的参与者ID
     * 用于确保 participantId 字段正确设置
     */
    suspend fun updateConversationParticipantId(
        userId: String,
        conversationId: String,
        participantId: String
    ): Result<Unit> {
        return try {
            userConversationsRef.child(userId).child(conversationId)
                .child("participantId").setValue(participantId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating participant id: ${e.message}")
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
