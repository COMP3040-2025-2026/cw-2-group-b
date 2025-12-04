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
 * Reads and manages chat messages directly from Firebase Realtime Database.
 * No longer depends on Spring Boot backend API.
 *
 * Firebase data structure:
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
     * Get current user's conversation list (real-time listener)
     * @param userId Current user ID
     * @return Flow<List<Conversation>> Conversation list flow
     *
     * Fix: Real-time fetch of other user's latest avatar and online status
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

                // Refetch avatar and online status on each data change
                val avatarCache = mutableMapOf<String, String?>()
                val presenceCache = mutableMapOf<String, Boolean>()

                // Step 1: Parse all conversations, collect user IDs that need avatar fetch
                val rawConversations = mutableListOf<Map<String, Any?>>()
                val participantIdsToFetch = mutableSetOf<String>()

                snapshot.children.forEach { child ->
                    try {
                        val conversationId = child.key ?: return@forEach
                        val unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0
                        val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                        // Read participantId directly, database doesn't have participantIds array
                        val participantId = child.child("participantId").getValue(String::class.java) ?: ""
                        val participantName = child.child("participantName").getValue(String::class.java) ?: "Unknown"
                        val participantAvatar = child.child("participantAvatar").getValue(String::class.java)
                        val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false

                        // Get other user's latest avatar and online status (needed for all conversation types)
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

                // Step 2: Fetch needed avatars and online status
                if (participantIdsToFetch.isEmpty()) {
                    // No conversations need avatar fetch (all groups)
                    val conversations = buildConversations(rawConversations, avatarCache, presenceCache)
                    trySend(conversations)
                } else {
                    // Total requests needed: avatar + online status, 2 requests per participant
                    val totalRequests = participantIdsToFetch.size * 2
                    var fetchedCount = 0

                    participantIdsToFetch.forEach { participantId ->
                        // Fetch avatar
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

                        // Fetch online status from presence node
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
     * Real-time listener for current user's total unread message count
     * Iterates through all conversations under user_conversations and sums unreadCount
     */
    fun getTotalUnreadCountFlow(userId: String): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalUnread = 0
                // Iterate through all user conversations
                for (conversationSnapshot in snapshot.children) {
                    val count = conversationSnapshot.child("unreadCount").getValue(Int::class.java) ?: 0
                    totalUnread += count
                }
                // Send the latest total
                trySend(totalUnread)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseMessageRepo", "Failed to calculate unread count: ${error.message}")
                trySend(0)
            }
        }

        // Listen to user_conversations/{userId} node changes
        userConversationsRef.child(userId).addValueEventListener(listener)

        // Remove listener when Flow stops collecting (e.g., page destroyed)
        awaitClose {
            userConversationsRef.child(userId).removeEventListener(listener)
        }
    }

    /**
     * Helper method: Build conversation list with latest avatar and online status
     */
    private fun buildConversations(
        rawConversations: List<Map<String, Any?>>,
        avatarCache: Map<String, String?>,
        presenceCache: Map<String, Boolean> = emptyMap()
    ): List<Conversation> {
        val conversations = rawConversations.map { raw ->
            val participantId = raw["participantId"] as String
            val isGroup = raw["isGroup"] as Boolean
            // Use cached latest avatar (fetched from users table)
            val currentAvatar = if (participantId.isNotEmpty()) {
                avatarCache[participantId] ?: raw["participantAvatar"] as? String
            } else {
                raw["participantAvatar"] as? String
            }
            // Use cached online status (fetched from presence node)
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

        // Sort by pinned status and last message time
        return conversations.sortedWith(
            compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTime }
        )
    }

    /**
     * Get message list for specified conversation (real-time listener)
     * @param conversationId Conversation ID
     * @return Flow<List<ChatMessage>> Message list flow
     *
     * Fix: Real-time fetch of sender's latest avatar instead of using stored old avatar
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

                // Refetch avatar on each data change to ensure it's latest
                val avatarCache = mutableMapOf<String, String?>()

                // Step 1: Parse all messages, collect unique senderIds
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
                        val imageUrl = child.child("imageUrl").getValue(String::class.java)

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
                                "messageType" to messageType,
                                "imageUrl" to imageUrl
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseMessageRepo", "Error parsing message: ${e.message}")
                    }
                }

                // Step 2: Fetch all senders' latest avatars (async)
                if (uniqueSenderIds.isEmpty()) {
                    // No messages need avatar fetch
                    buildAndSendMessages(rawMessages, avatarCache, conversationId, messages)
                    messages.sortBy { it.timestamp }
                    trySend(messages)
                } else {
                    // Need to fetch avatars
                    var fetchedCount = 0
                    uniqueSenderIds.forEach { senderId ->
                        usersRef.child(senderId).child("profileImageUrl").get()
                            .addOnSuccessListener { avatarSnapshot ->
                                val currentAvatar = avatarSnapshot.getValue(String::class.java)
                                avatarCache[senderId] = currentAvatar
                                fetchedCount++

                                // Build message list after all avatars are fetched
                                if (fetchedCount == uniqueSenderIds.size) {
                                    buildAndSendMessages(rawMessages, avatarCache, conversationId, messages)
                                    messages.sortBy { it.timestamp }
                                    trySend(messages)
                                }
                            }
                            .addOnFailureListener {
                                // Use stored avatar when fetch fails
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
     * Helper method: Build message list with latest avatars
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
            // Prioritize cached latest avatar, fallback to stored avatar in message
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
                    messageType = raw["messageType"] as String,
                    imageUrl = raw["imageUrl"] as? String
                )
            )
        }
    }

    /**
     * Send message
     * @param conversationId Conversation ID
     * @param senderId Sender ID
     * @param senderName Sender name
     * @param senderAvatar Sender avatar URL
     * @param message Message content
     * @param messageType Message type (TEXT, IMAGE, FILE)
     * @param imageUrl Image URL (only used when messageType is IMAGE)
     * @return Result<String> Message ID or error
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String?,
        message: String,
        messageType: String = "TEXT",
        imageUrl: String? = null
    ): Result<String> {
        return try {
            // Trim whitespace from message
            val trimmedMessage = message.trim()

            val timestamp = System.currentTimeMillis()
            val newMessageRef = conversationsRef.child(conversationId).child("messages").push()
            val messageId = newMessageRef.key ?: throw Exception("Failed to generate message ID")

            val messageData = mutableMapOf<String, Any?>(
                "senderId" to senderId,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "message" to trimmedMessage,
                "timestamp" to timestamp,
                "isRead" to false,
                "messageType" to messageType
            )

            // Add image URL if it's an image message
            if (messageType == "IMAGE" && imageUrl != null) {
                messageData["imageUrl"] = imageUrl
            }

            // Save message
            newMessageRef.setValue(messageData).await()

            // Set last message display text
            val lastMessageText = if (messageType == "IMAGE") "[Image]" else trimmedMessage

            // Update conversation's last message info (metadata)
            val metadataUpdates = mapOf(
                "lastMessage" to lastMessageText,
                "lastMessageTime" to timestamp
            )
            conversationsRef.child(conversationId).child("metadata").updateChildren(metadataUpdates).await()

            // Update lastMessage and lastMessageTime in all participants' user_conversations
            updateLastMessageForAllParticipants(conversationId, lastMessageText, timestamp, senderId)

            // Send FCM push notification to other participants
            sendPushNotificationToParticipants(conversationId, senderId, senderName, lastMessageText)

            Result.success(messageId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send FCM push notification to other participants in conversation
     */
    private suspend fun sendPushNotificationToParticipants(
        conversationId: String,
        senderId: String,
        senderName: String,
        message: String
    ) {
        try {
            // Get conversation participants
            var participants: List<String> = emptyList()

            // Try to get participants from multiple paths
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

            // Send push notification to each participant (except sender)
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
     * Create new conversation (one-on-one or group)
     * @param participantIds Participant ID list (excluding current user)
     * @param currentUserId Current user ID
     * @param currentUserName Current user name
     * @param isGroup Whether it's a group chat
     * @param groupName Group name (optional)
     * @return Result<String> Conversation ID or error
     */
    suspend fun createConversation(
        participantIds: List<String>,
        currentUserId: String,
        currentUserName: String,
        isGroup: Boolean = false,
        groupName: String? = null
    ): Result<String> {
        return try {
            // Add current user to participants list
            val allParticipantIds = (participantIds + currentUserId).distinct()

            // Check if conversation with same participants already exists
            val existingConvId = findExistingConversation(currentUserId, participantIds)
            if (existingConvId != null) {
                return Result.success(existingConvId)
            }

            // Use deterministic ID for one-on-one chats (prevents duplicates)
            // Use push() ID for group chats
            val conversationId = if (!isGroup && participantIds.size == 1) {
                generateDeterministicConversationId(currentUserId, participantIds.first())
            } else {
                conversationsRef.push().key ?: throw Exception("Failed to generate conversation ID")
            }
            val newConvRef = conversationsRef.child(conversationId)
            val timestamp = System.currentTimeMillis()

            // Create participants map (including current user)
            val participantsMap = allParticipantIds.associateWith { true }

            // Create conversation metadata
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

            // Also create record at conversations/{id}/participants (for sendMessage to read)
            newConvRef.child("participants").setValue(participantsMap).await()

            // Set group owner (creator)
            if (isGroup) {
                newConvRef.child("ownerId").setValue(currentUserId).await()
                newConvRef.child("adminIds").setValue(emptyList<String>()).await()
                if (groupName != null) {
                    newConvRef.child("groupName").setValue(groupName).await()
                }
            }

            // Create user conversation record for each participant (including current user)
            for (participantId in allParticipantIds) {
                val userConvData = mutableMapOf<String, Any>(
                    "unreadCount" to 0,
                    "isPinned" to false,
                    "lastMessage" to "",
                    "lastMessageTime" to timestamp,
                    "isGroup" to isGroup,
                    "isOnline" to false
                )

                if (isGroup) {
                    // Group: use group name
                    userConvData["participantName"] = groupName ?: "Group Chat"
                    userConvData["participantId"] = ""
                } else {
                    // One-on-one: set other user's info
                    val otherUserId = allParticipantIds.first { it != participantId }
                    userConvData["participantId"] = otherUserId
                    // Get other user's info
                    try {
                        val otherUserSnapshot = database.getReference("users").child(otherUserId).get().await()
                        val otherUserName = otherUserSnapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
                        val otherUserAvatar = otherUserSnapshot.child("profileImageUrl").getValue(String::class.java)
                        userConvData["participantName"] = otherUserName
                        if (otherUserAvatar != null) {
                            userConvData["participantAvatar"] = otherUserAvatar
                        }
                    } catch (e: Exception) {
                        userConvData["participantName"] = "Unknown"
                    }
                }

                userConversationsRef.child(participantId).child(conversationId).setValue(userConvData).await()
                android.util.Log.d("FirebaseMessageRepo", "Created user_conversation for $participantId")
            }

            android.util.Log.d("FirebaseMessageRepo", "Created conversation: $conversationId with ${allParticipantIds.size} participants")
            Result.success(conversationId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error creating conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark messages as read
     * @param conversationId Conversation ID
     * @param userId Current user ID
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Result<Unit> {
        return try {
            // Reset unread count
            userConversationsRef.child(userId).child(conversationId).child("unreadCount").setValue(0).await()

            // Mark all messages not sent by current user as read
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
     * Pin/unpin conversation
     * @param userId User ID
     * @param conversationId Conversation ID
     * @param pinned Whether to pin
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
     * Delete conversation
     * @param userId User ID
     * @param conversationId Conversation ID
     */
    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        return try {
            // Only delete user's conversation record, not actual conversation data
            userConversationsRef.child(userId).child(conversationId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error deleting conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Search users (for contact suggestions when creating new conversation)
     * @param query Search keyword
     * @return Flow<List<Map<String, String>>> User list
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

                        // Simple fuzzy search
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
     * Find existing conversation (same participants)
     * For one-on-one chats: check participantId (singular string)
     * For group chats: check participantIds array if exists
     */
    private suspend fun findExistingConversation(currentUserId: String, participantIds: List<String>): String? {
        return try {
            val snapshot = userConversationsRef.child(currentUserId).get().await()

            // For one-on-one chat (single participant)
            if (participantIds.size == 1) {
                val targetParticipantId = participantIds.first()
                snapshot.children.forEach { child ->
                    val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                    if (!isGroup) {
                        val participantId = child.child("participantId").getValue(String::class.java)
                        if (participantId == targetParticipantId) {
                            android.util.Log.d("FirebaseMessageRepo", "Found existing conversation: ${child.key}")
                            return child.key
                        }
                    }
                }
            } else {
                // For group chats - check participantIds array if exists
                val allParticipants = (participantIds + currentUserId).sorted()
                snapshot.children.forEach { child ->
                    val convParticipants = child.child("participantIds").children
                        .mapNotNull { it.getValue(String::class.java) }
                        .sorted()

                    if (convParticipants == allParticipants) {
                        return child.key
                    }
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error finding existing conversation: ${e.message}")
            null
        }
    }

    /**
     * Generate deterministic conversation ID for one-on-one chats
     * This ensures the same two users always get the same conversation ID
     */
    private fun generateDeterministicConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    /**
     * Update lastMessage, lastMessageTime and unreadCount in all participants' user_conversations
     */
    private suspend fun updateLastMessageForAllParticipants(
        conversationId: String,
        message: String,
        timestamp: Long,
        senderId: String
    ) {
        try {
            // Try reading participants from multiple possible paths
            var participants: List<String> = emptyList()

            // Method 1: conversations/{id}/participants (new structure)
            val directSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            if (directSnapshot.exists() && directSnapshot.childrenCount > 0) {
                participants = directSnapshot.children.mapNotNull { it.key }
                android.util.Log.d("FirebaseMessageRepo", "Found participants in conversations/{id}/participants: $participants")
            }

            // Method 2: conversations/{id}/metadata/participants (old structure)
            if (participants.isEmpty()) {
                val metadataSnapshot = conversationsRef.child(conversationId).child("metadata").child("participants").get().await()
                if (metadataSnapshot.exists() && metadataSnapshot.childrenCount > 0) {
                    participants = metadataSnapshot.children.mapNotNull { it.key }
                    android.util.Log.d("FirebaseMessageRepo", "Found participants in metadata: $participants")
                }
            }

            // Method 3: Parse from conversationId (format: {userId1}_{userId2})
            if (participants.isEmpty() && conversationId.contains("_")) {
                participants = conversationId.split("_")
                android.util.Log.d("FirebaseMessageRepo", "Parsed participants from conversationId: $participants")
            }

            android.util.Log.d("FirebaseMessageRepo", "Updating lastMessage for ${participants.size} participants")

            participants.forEach { participantId ->
                val userConvRef = userConversationsRef.child(participantId).child(conversationId)

                // Check if user conversation exists
                val exists = userConvRef.get().await().exists()
                if (!exists) {
                    android.util.Log.w("FirebaseMessageRepo", "User conversation not found for $participantId, skipping")
                    return@forEach
                }

                // Update lastMessage and lastMessageTime
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to message,
                    "lastMessageTime" to timestamp
                )

                // Increment unread count if not the sender
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
     * Update participant info in user conversation (name and avatar)
     * Should be called after creating conversation with actual user data
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
     * Update participant ID in user conversation
     * Used to ensure participantId field is correctly set
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
     * Update user typing status
     * @param conversationId Conversation ID
     * @param userId User ID
     * @param isTyping Whether user is typing
     * @return Result<Unit> Success or error
     */
    suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            // Update typing status in conversation metadata
            // Use temporary node storage, with expiration time (e.g., auto-clear after 3 seconds)
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

    // ========== Group Management Methods ==========

    /**
     * Get group details
     */
    suspend fun getGroupInfo(conversationId: String): Result<Map<String, Any?>> {
        return try {
            val snapshot = conversationsRef.child(conversationId).get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Group not found"))
            }

            val groupName = snapshot.child("groupName").getValue(String::class.java)
                ?: snapshot.child("metadata").child("groupName").getValue(String::class.java)
                ?: "Group"
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                ?: snapshot.child("metadata").child("createdAt").getValue(Long::class.java)
                ?: 0L
            val ownerId = snapshot.child("ownerId").getValue(String::class.java) ?: ""
            val adminIds = snapshot.child("adminIds").children.mapNotNull { it.getValue(String::class.java) }
            val participants = snapshot.child("participants").children.mapNotNull { it.key }
                .ifEmpty { snapshot.child("metadata").child("participants").children.mapNotNull { it.key } }

            val result = mapOf(
                "id" to conversationId,
                "groupName" to groupName,
                "createdAt" to createdAt,
                "ownerId" to ownerId,
                "adminIds" to adminIds,
                "participantIds" to participants
            )

            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error getting group info: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update group name
     */
    suspend fun updateGroupName(conversationId: String, newName: String): Result<Unit> {
        return try {
            // Update group name in conversations
            conversationsRef.child(conversationId).child("groupName").setValue(newName).await()
            conversationsRef.child(conversationId).child("metadata").child("groupName").setValue(newName).await()

            // Update participantName in all members' user_conversations
            val participantsSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            val participants = participantsSnapshot.children.mapNotNull { it.key }

            participants.forEach { participantId ->
                userConversationsRef.child(participantId).child(conversationId)
                    .child("participantName").setValue(newName).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error updating group name: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Set group owner (called when creating group)
     */
    suspend fun setGroupOwner(conversationId: String, ownerId: String): Result<Unit> {
        return try {
            conversationsRef.child(conversationId).child("ownerId").setValue(ownerId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error setting group owner: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add admin
     */
    suspend fun addAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            val adminIdsRef = conversationsRef.child(conversationId).child("adminIds")
            val snapshot = adminIdsRef.get().await()
            val currentAdmins = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()

            if (!currentAdmins.contains(userId)) {
                currentAdmins.add(userId)
                adminIdsRef.setValue(currentAdmins).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error adding admin: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove admin
     */
    suspend fun removeAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            val adminIdsRef = conversationsRef.child(conversationId).child("adminIds")
            val snapshot = adminIdsRef.get().await()
            val currentAdmins = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()

            currentAdmins.remove(userId)
            adminIdsRef.setValue(currentAdmins).await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error removing admin: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add group member
     */
    suspend fun addGroupMember(conversationId: String, userId: String, groupName: String): Result<Unit> {
        return try {
            // Add to participants
            conversationsRef.child(conversationId).child("participants").child(userId).setValue(true).await()
            conversationsRef.child(conversationId).child("metadata").child("participants").child(userId).setValue(true).await()

            // Create user's user_conversations record
            val lastMessageSnapshot = conversationsRef.child(conversationId).child("metadata").child("lastMessage").get().await()
            val lastMessage = lastMessageSnapshot.getValue(String::class.java) ?: ""
            val lastTimeSnapshot = conversationsRef.child(conversationId).child("metadata").child("lastMessageTime").get().await()
            val lastTime = lastTimeSnapshot.getValue(Long::class.java) ?: System.currentTimeMillis()

            val userConvData = mapOf(
                "participantId" to "",
                "participantName" to groupName,
                "participantAvatar" to null,
                "lastMessage" to lastMessage,
                "lastMessageTime" to lastTime,
                "unreadCount" to 0,
                "isPinned" to false,
                "isGroup" to true,
                "isOnline" to false
            )
            userConversationsRef.child(userId).child(conversationId).setValue(userConvData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error adding group member: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove group member
     */
    suspend fun removeGroupMember(conversationId: String, userId: String): Result<Unit> {
        return try {
            // Remove from participants
            conversationsRef.child(conversationId).child("participants").child(userId).removeValue().await()
            conversationsRef.child(conversationId).child("metadata").child("participants").child(userId).removeValue().await()

            // Remove from adminIds (if admin)
            removeAdmin(conversationId, userId)

            // Delete user's user_conversations record
            userConversationsRef.child(userId).child(conversationId).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error removing group member: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Transfer ownership
     */
    suspend fun transferOwnership(conversationId: String, newOwnerId: String): Result<Unit> {
        return try {
            // Set new owner
            conversationsRef.child(conversationId).child("ownerId").setValue(newOwnerId).await()

            // If new owner is admin, remove from admin list (owner doesn't need to be in admin list)
            removeAdmin(conversationId, newOwnerId)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error transferring ownership: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Dissolve group
     */
    suspend fun dissolveGroup(conversationId: String): Result<Unit> {
        return try {
            // Get all members
            val participantsSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            val participants = participantsSnapshot.children.mapNotNull { it.key }

            // Delete all members' user_conversations records
            participants.forEach { participantId ->
                userConversationsRef.child(participantId).child(conversationId).removeValue().await()
            }

            // Delete entire conversation
            conversationsRef.child(conversationId).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessageRepo", "Error dissolving group: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Leave group
     */
    suspend fun leaveGroup(conversationId: String, userId: String): Result<Unit> {
        return removeGroupMember(conversationId, userId)
    }
}
