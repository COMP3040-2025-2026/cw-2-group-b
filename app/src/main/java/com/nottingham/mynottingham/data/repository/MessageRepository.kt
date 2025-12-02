package com.nottingham.mynottingham.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.remote.dto.ContactSuggestionDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Repository for Message operations
 *
 * ✅ FIREBASE ONLY - No backend API dependencies
 * All messaging operations use Firebase Realtime Database
 *
 * Data structure:
 * - conversations/{conversationId}: Global conversation metadata
 * - messages/{conversationId}/{messageId}: Messages
 * - user_conversations/{userId}/{conversationId}: User's conversation list with denormalized data
 */
class MessageRepository(private val context: Context) {

    companion object {
        private const val TAG = "MessageRepository"
    }

    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val conversationsRef = database.getReference("conversations")
    private val messagesRef = database.getReference("messages")
    private val userConversationsRef = database.getReference("user_conversations")
    private val usersRef = database.getReference("users")
    private val firebaseUserRepo = FirebaseUserRepository()

    // ========== Conversation Operations ==========

    /**
     * Get all conversations for a user (real-time from Firebase)
     */
    fun getConversationsFlow(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        Log.d(TAG, "Getting conversations for user: $currentUserId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<Conversation>()

                for (child in snapshot.children) {
                    try {
                        val conversationId = child.key ?: continue
                        conversations.add(
                            Conversation(
                                id = conversationId,
                                participantId = child.child("participantId").getValue(String::class.java) ?: "",
                                participantName = child.child("participantName").getValue(String::class.java) ?: "Unknown",
                                participantAvatar = child.child("participantAvatar").getValue(String::class.java),
                                lastMessage = child.child("lastMessage").getValue(String::class.java) ?: "",
                                lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
                                unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0,
                                isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false,
                                isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false,
                                isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing conversation: ${e.message}")
                    }
                }

                // Sort: pinned first, then by last message time
                val sorted = conversations.sortedWith(
                    compareByDescending<Conversation> { it.isPinned }
                        .thenByDescending { it.lastMessageTime }
                )
                Log.d(TAG, "Loaded ${sorted.size} conversations")
                trySend(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read conversations: ${error.message}")
                close(error.toException())
            }
        }

        userConversationsRef.child(currentUserId).addValueEventListener(listener)

        awaitClose {
            userConversationsRef.child(currentUserId).removeEventListener(listener)
        }
    }

    /**
     * Sync conversations - For Firebase, this is a no-op since we use real-time listeners
     */
    suspend fun syncConversations(token: String): Result<Unit> {
        // Firebase uses real-time sync, no manual sync needed
        return Result.success(Unit)
    }

    /**
     * Create new conversation using Firebase
     */
    suspend fun createConversation(
        token: String,
        participantIds: List<String>,
        isGroup: Boolean = false,
        groupName: String? = null
    ): Result<Conversation> {
        return try {
            // Get current user info
            val tokenManager = TokenManager(context)
            val currentUserId = tokenManager.getUserId().first() ?: return Result.failure(Exception("User not logged in"))
            val currentUserName = tokenManager.getFullName().first() ?: "Unknown"
            val currentUserAvatar = tokenManager.getAvatar().first()

            if (participantIds.isEmpty()) {
                return Result.failure(Exception("No participants specified"))
            }

            // For one-on-one chats
            if (!isGroup) {
                val participantId = participantIds.first()

                // Get participant info from Firebase
                val participantSnapshot = usersRef.child(participantId).get().await()
                val participantName = participantSnapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
                val participantAvatar = participantSnapshot.child("profileImageUrl").getValue(String::class.java)

                // Check if conversation already exists
                val existingConversation = findExistingConversation(currentUserId, participantId)
                if (existingConversation != null) {
                    Log.d(TAG, "Found existing conversation: ${existingConversation.id}")
                    return Result.success(existingConversation)
                }

                val conversationId = generateConversationId(currentUserId, participantId)
                val now = System.currentTimeMillis()

                // Save global conversation data
                val conversationData = mapOf(
                    "id" to conversationId,
                    "isGroup" to false,
                    "createdAt" to now,
                    "updatedAt" to now,
                    "participants" to mapOf(
                        currentUserId to true,
                        participantId to true
                    )
                )
                conversationsRef.child(conversationId).setValue(conversationData).await()

                // Save to current user's conversations
                val currentUserData = mapOf(
                    "participantId" to participantId,
                    "participantName" to participantName,
                    "participantAvatar" to participantAvatar,
                    "lastMessage" to "",
                    "lastMessageTime" to now,
                    "unreadCount" to 0,
                    "isPinned" to false,
                    "isGroup" to false,
                    "isOnline" to false
                )
                userConversationsRef.child(currentUserId).child(conversationId).setValue(currentUserData).await()

                // Save to participant's conversations
                val participantData = mapOf(
                    "participantId" to currentUserId,
                    "participantName" to currentUserName,
                    "participantAvatar" to currentUserAvatar,
                    "lastMessage" to "",
                    "lastMessageTime" to now,
                    "unreadCount" to 0,
                    "isPinned" to false,
                    "isGroup" to false,
                    "isOnline" to false
                )
                userConversationsRef.child(participantId).child(conversationId).setValue(participantData).await()

                Log.d(TAG, "Created 1:1 conversation: $conversationId")

                return Result.success(
                    Conversation(
                        id = conversationId,
                        participantId = participantId,
                        participantName = participantName,
                        participantAvatar = participantAvatar,
                        lastMessage = "",
                        lastMessageTime = now,
                        unreadCount = 0,
                        isOnline = false,
                        isPinned = false,
                        isGroup = false
                    )
                )
            }

            // For group chats - include current user in all participants
            val allParticipantIds = (participantIds + currentUserId).distinct()
            val conversationId = conversationsRef.push().key ?: return Result.failure(Exception("Failed to generate ID"))
            val now = System.currentTimeMillis()

            // Build participants map
            val participantsMap = allParticipantIds.associateWith { true }

            // Save global conversation data with owner
            val conversationData = mapOf(
                "id" to conversationId,
                "isGroup" to true,
                "groupName" to groupName,
                "createdAt" to now,
                "updatedAt" to now,
                "participants" to participantsMap,
                "ownerId" to currentUserId,  // Set creator as owner
                "adminIds" to emptyList<String>()  // Initially no admins
            )
            conversationsRef.child(conversationId).setValue(conversationData).await()

            // Create user_conversations entry for EACH participant
            for (participantId in allParticipantIds) {
                val userConvData = mapOf(
                    "participantId" to "",  // Empty for groups
                    "participantName" to (groupName ?: "Group Chat"),  // Use group name!
                    "participantAvatar" to null,
                    "lastMessage" to "",
                    "lastMessageTime" to now,
                    "unreadCount" to 0,
                    "isPinned" to false,
                    "isGroup" to true,
                    "isOnline" to false
                )
                userConversationsRef.child(participantId).child(conversationId).setValue(userConvData).await()
                Log.d(TAG, "Created user_conversation for $participantId in group $conversationId")
            }

            Log.d(TAG, "Created group conversation: $conversationId with ${allParticipantIds.size} members")

            Result.success(
                Conversation(
                    id = conversationId,
                    participantId = "",
                    participantName = groupName ?: "Group Chat",
                    participantAvatar = null,
                    lastMessage = "",
                    lastMessageTime = now,
                    unreadCount = 0,
                    isOnline = false,
                    isPinned = false,
                    isGroup = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Find existing one-on-one conversation
     */
    private suspend fun findExistingConversation(userId1: String, userId2: String): Conversation? {
        return try {
            val conversationId = generateConversationId(userId1, userId2)
            val snapshot = userConversationsRef.child(userId1).child(conversationId).get().await()

            if (snapshot.exists()) {
                Conversation(
                    id = conversationId,
                    participantId = snapshot.child("participantId").getValue(String::class.java) ?: "",
                    participantName = snapshot.child("participantName").getValue(String::class.java) ?: "Unknown",
                    participantAvatar = snapshot.child("participantAvatar").getValue(String::class.java),
                    lastMessage = snapshot.child("lastMessage").getValue(String::class.java) ?: "",
                    lastMessageTime = snapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
                    unreadCount = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0,
                    isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false,
                    isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false,
                    isGroup = snapshot.child("isGroup").getValue(Boolean::class.java) ?: false
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding existing conversation: ${e.message}")
            null
        }
    }

    /**
     * Generate deterministic conversation ID for one-on-one chats
     */
    private fun generateConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    /**
     * Update pinned status
     */
    suspend fun updatePinnedStatus(token: String, conversationId: String, isPinned: Boolean): Result<Unit> {
        return try {
            val tokenManager = TokenManager(context)
            val currentUserId = tokenManager.getUserId().first() ?: return Result.failure(Exception("User not logged in"))

            userConversationsRef.child(currentUserId).child(conversationId)
                .child("isPinned").setValue(isPinned).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pinned status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Search conversations (local search in memory)
     */
    fun searchConversations(query: String, currentUserId: String): Flow<List<Conversation>> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conversations = mutableListOf<Conversation>()

                    for (child in snapshot.children) {
                        val conversationId = child.key ?: continue
                        val participantName = child.child("participantName").getValue(String::class.java) ?: ""
                        val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""

                        // Filter by query
                        if (participantName.contains(query, ignoreCase = true) ||
                            lastMessage.contains(query, ignoreCase = true)) {
                            conversations.add(
                                Conversation(
                                    id = conversationId,
                                    participantId = child.child("participantId").getValue(String::class.java) ?: "",
                                    participantName = participantName,
                                    participantAvatar = child.child("participantAvatar").getValue(String::class.java),
                                    lastMessage = lastMessage,
                                    lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
                                    unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0,
                                    isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false,
                                    isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false,
                                    isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                                )
                            )
                        }
                    }
                    trySend(conversations)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            userConversationsRef.child(currentUserId).addValueEventListener(listener)
            awaitClose { userConversationsRef.child(currentUserId).removeEventListener(listener) }
        }
    }

    // ========== Message Operations ==========

    /**
     * Get messages for a conversation (real-time from Firebase)
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()

                for (child in snapshot.children) {
                    try {
                        val messageId = child.key ?: continue
                        messages.add(
                            ChatMessage(
                                id = messageId,
                                conversationId = conversationId,
                                senderId = child.child("senderId").getValue(String::class.java) ?: "",
                                senderName = child.child("senderName").getValue(String::class.java) ?: "",
                                senderAvatar = child.child("senderAvatar").getValue(String::class.java),
                                message = child.child("content").getValue(String::class.java) ?: "",
                                timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                                isRead = child.child("isRead").getValue(Boolean::class.java) ?: false,
                                messageType = child.child("messageType").getValue(String::class.java) ?: "TEXT"
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: ${e.message}")
                    }
                }

                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read messages: ${error.message}")
                close(error.toException())
            }
        }

        messagesRef.child(conversationId).addValueEventListener(listener)

        awaitClose {
            messagesRef.child(conversationId).removeEventListener(listener)
        }
    }

    /**
     * Sync messages - For Firebase, this is a no-op
     */
    suspend fun syncMessages(
        token: String,
        conversationId: String,
        page: Int = 0,
        size: Int = 50
    ): Result<List<ChatMessage>> {
        // Firebase uses real-time sync
        return Result.success(emptyList())
    }

    /**
     * Send a message using Firebase
     */
    suspend fun sendMessage(
        token: String,
        conversationId: String,
        content: String,
        messageType: String = "TEXT"
    ): Result<ChatMessage> {
        return try {
            val tokenManager = TokenManager(context)
            val senderId = tokenManager.getUserId().first() ?: return Result.failure(Exception("User not logged in"))
            val senderName = tokenManager.getFullName().first() ?: "Unknown"

            val messageId = messagesRef.child(conversationId).push().key
                ?: return Result.failure(Exception("Failed to generate message ID"))

            val now = System.currentTimeMillis()

            val messageData = mapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "content" to content,
                "timestamp" to now,
                "isRead" to false,
                "messageType" to messageType
            )

            // Save message
            messagesRef.child(conversationId).child(messageId).setValue(messageData).await()

            // Update last message for all participants
            val conversationSnapshot = conversationsRef.child(conversationId).child("participants").get().await()
            for (participant in conversationSnapshot.children) {
                val participantId = participant.key ?: continue

                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to content,
                    "lastMessageTime" to now
                )

                // Increment unread count for other participants
                if (participantId != senderId) {
                    val currentUnread = userConversationsRef.child(participantId).child(conversationId)
                        .child("unreadCount").get().await().getValue(Int::class.java) ?: 0
                    updates["unreadCount"] = currentUnread + 1
                }

                userConversationsRef.child(participantId).child(conversationId).updateChildren(updates).await()
            }

            Log.d(TAG, "Message sent: $messageId")

            Result.success(
                ChatMessage(
                    id = messageId,
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    senderAvatar = null,
                    message = content,
                    timestamp = now,
                    isRead = false,
                    messageType = messageType
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(token: String, conversationId: String, currentUserId: String): Result<Unit> {
        return try {
            userConversationsRef.child(currentUserId).child(conversationId)
                .child("unreadCount").setValue(0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as read: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark conversation as unread
     */
    suspend fun markConversationAsUnread(conversationId: String) {
        try {
            val tokenManager = TokenManager(context)
            val currentUserId = tokenManager.getUserId().first() ?: return
            userConversationsRef.child(currentUserId).child(conversationId)
                .child("unreadCount").setValue(1).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as unread: ${e.message}")
        }
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(token: String, conversationId: String): Result<Unit> {
        return try {
            val tokenManager = TokenManager(context)
            val currentUserId = tokenManager.getUserId().first() ?: return Result.failure(Exception("User not logged in"))

            userConversationsRef.child(currentUserId).child(conversationId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update typing status (no-op for now, can implement with Firebase presence)
     */
    suspend fun updateTypingStatus(token: String, conversationId: String, isTyping: Boolean): Result<Unit> {
        // TODO: Implement with Firebase presence if needed
        return Result.success(Unit)
    }

    /**
     * Get contact suggestions from Firebase
     */
    suspend fun getContactSuggestions(token: String): Result<List<ContactSuggestionDto>> {
        return try {
            val tokenManager = TokenManager(context)
            val currentUserId = tokenManager.getUserId().first()

            val result = firebaseUserRepo.getAllUsers(currentUserId)
            if (result.isSuccess) {
                val users = result.getOrNull() ?: emptyList()
                val contacts = users.map { user ->
                    ContactSuggestionDto(
                        userId = user.id,
                        userName = user.name,
                        userAvatar = user.profileImageUrl,
                        program = if (user.role == "STUDENT") user.program else user.faculty,
                        year = if (user.role == "STUDENT") user.year else null,
                        isOnline = false
                    )
                }
                Log.d(TAG, "Loaded ${contacts.size} contacts from Firebase")
                Result.success(contacts)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to load contacts"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contacts: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get default contacts - Now uses Firebase
     */
    suspend fun getDefaultContacts(userId: String): Result<List<ContactSuggestionDto>> {
        return getContactSuggestions("")
    }

    /**
     * Create default conversations - Simplified for Firebase
     */
    suspend fun createDefaultConversations(token: String, userId: String): Result<List<Conversation>> {
        // No default conversations needed with Firebase
        return Result.success(emptyList())
    }

    /**
     * Delete messages older than 7 days - Firebase handles this differently
     */
    suspend fun cleanupOldMessages(): Result<Int> {
        // Firebase can use Cloud Functions for cleanup
        // For now, return success
        return Result.success(0)
    }
}
