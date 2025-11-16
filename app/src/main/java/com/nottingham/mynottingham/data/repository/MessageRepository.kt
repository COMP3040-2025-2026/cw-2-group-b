package com.nottingham.mynottingham.data.repository

import android.content.Context
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.local.database.entities.ConversationEntity
import com.nottingham.mynottingham.data.local.database.entities.ConversationParticipantEntity
import com.nottingham.mynottingham.data.local.database.entities.MessageEntity
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.*
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for Message operations
 * Coordinates between local database and remote API
 */
class MessageRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val participantDao = database.conversationParticipantDao()
    private val apiService = RetrofitInstance.apiService

    // ========== Conversation Operations ==========

    /**
     * Get all conversations from local database as Flow
     * Ordered by pinned status and last message time
     */
    fun getConversationsFlow(currentUserId: String): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { entity ->
                val participants = participantDao.getParticipantsForConversationSync(entity.id)
                entityToConversation(entity, participants, currentUserId)
            }
        }
    }

    /**
     * Sync conversations from API and save to local database
     */
    suspend fun syncConversations(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversations("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    val conversations = response.body()?.data ?: emptyList()

                    // Save conversations to database
                    conversations.forEach { dto ->
                        val entity = dtoToConversationEntity(dto)
                        conversationDao.insertConversation(entity)

                        // Save participants
                        val participantEntities = dto.participants.map { participantDto ->
                            ConversationParticipantEntity(
                                conversationId = dto.id,
                                userId = participantDto.userId,
                                userName = participantDto.userName,
                                userAvatar = participantDto.userAvatar,
                                isOnline = participantDto.isOnline,
                                isTyping = participantDto.isTyping,
                                lastSeenAt = participantDto.lastSeenAt
                            )
                        }
                        participantDao.insertParticipants(participantEntities)
                    }

                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to sync conversations"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create new conversation
     */
    suspend fun createConversation(
        token: String,
        participantIds: List<String>,
        isGroup: Boolean = false,
        groupName: String? = null
    ): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateConversationRequest(participantIds, isGroup, groupName)
                val response = apiService.createConversation("Bearer $token", request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data
                    if (dto != null) {
                        // Save to local database
                        val entity = dtoToConversationEntity(dto)
                        conversationDao.insertConversation(entity)

                        val participantEntities = dto.participants.map { p ->
                            ConversationParticipantEntity(
                                conversationId = dto.id,
                                userId = p.userId,
                                userName = p.userName,
                                userAvatar = p.userAvatar,
                                isOnline = p.isOnline,
                                isTyping = p.isTyping,
                                lastSeenAt = p.lastSeenAt
                            )
                        }
                        participantDao.insertParticipants(participantEntities)

                        // Get current user ID from TokenManager
                        val currentUserId = getCurrentUserId()
                        val conversation = entityToConversation(entity, participantEntities, currentUserId)
                        Result.success(conversation)
                    } else {
                        Result.failure(Exception("Conversation data is null"))
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to create conversation"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get current user ID from DataStore
     */
    private suspend fun getCurrentUserId(): String {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.KEY_USER_ID, "") ?: ""
    }

    /**
     * Update conversation pinned status
     */
    suspend fun updatePinnedStatus(token: String, conversationId: String, isPinned: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdatePinnedStatusRequest(isPinned)
                val response = apiService.updatePinnedStatus("Bearer $token", conversationId, request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Update local database
                    conversationDao.updatePinnedStatus(conversationId, isPinned)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to update pinned status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Search conversations locally
     */
    suspend fun searchConversations(query: String): Flow<List<Conversation>> {
        val currentUserId = getCurrentUserId()
        return conversationDao.searchConversations(query).map { entities ->
            entities.map { entity ->
                val participants = participantDao.getParticipantsForConversationSync(entity.id)
                entityToConversation(entity, participants, currentUserId)
            }
        }
    }

    // ========== Message Operations ==========

    /**
     * Get messages for a conversation from local database
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { entityToChatMessage(it) }
        }
    }

    /**
     * Sync messages from API and save to local database
     */
    suspend fun syncMessages(token: String, conversationId: String, limit: Int = Constants.MESSAGE_PAGE_SIZE): Result<List<ChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMessages("Bearer $token", conversationId, limit)
                if (response.isSuccessful && response.body()?.success == true) {
                    val messages = response.body()?.data?.messages ?: emptyList()

                    // Save messages to database
                    val entities = messages.map { dtoToMessageEntity(it) }
                    messageDao.insertMessages(entities)

                    val chatMessages = entities.map { entityToChatMessage(it) }
                    Result.success(chatMessages)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to sync messages"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        token: String,
        conversationId: String,
        content: String,
        messageType: String = Constants.MessageTypes.TEXT
    ): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SendMessageRequest(conversationId, content, messageType)
                val response = apiService.sendMessage("Bearer $token", request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data
                    if (dto != null) {
                        // Save to local database
                        val entity = dtoToMessageEntity(dto)
                        messageDao.insertMessage(entity)

                        // Update conversation's last message
                        conversationDao.updateLastMessage(
                            conversationId = conversationId,
                            lastMessage = content,
                            timestamp = dto.timestamp,
                            senderId = dto.senderId
                        )

                        val chatMessage = entityToChatMessage(entity)
                        Result.success(chatMessage)
                    } else {
                        Result.failure(Exception("Message data is null"))
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to send message"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(token: String, conversationId: String, currentUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.markAsRead("Bearer $token", conversationId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Update local database
                    messageDao.markMessagesAsRead(conversationId, currentUserId)
                    conversationDao.updateUnreadCount(conversationId, 0)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to mark as read"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update typing status
     */
    suspend fun updateTypingStatus(token: String, conversationId: String, isTyping: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TypingStatusRequest(conversationId, isTyping)
                val response = apiService.updateTypingStatus("Bearer $token", request)

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update typing status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get contact suggestions
     */
    suspend fun getContactSuggestions(token: String): Result<List<ContactSuggestionDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getContactSuggestions("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    val contacts = response.body()?.data ?: emptyList()
                    Result.success(contacts)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to get contact suggestions"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get default contacts for a user (teachers for students, students for teachers)
     */
    suspend fun getDefaultContacts(userId: String): Result<List<ContactSuggestionDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDefaultContacts(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val contacts = response.body()?.data ?: emptyList()
                    Result.success(contacts)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to get default contacts"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create default conversations for a user
     * Returns the list of created conversations
     */
    suspend fun createDefaultConversations(token: String, userId: String): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                val defaultContactsResult = getDefaultContacts(userId)
                if (defaultContactsResult.isFailure) {
                    return@withContext defaultContactsResult.exceptionOrNull()?.let { Result.failure<List<Conversation>>(it) }
                        ?: Result.failure(Exception("Failed to get default contacts"))
                }

                val contacts = defaultContactsResult.getOrNull() ?: emptyList()
                val createdConversations = mutableListOf<Conversation>()

                // Create conversation with each default contact
                for (contact in contacts) {
                    val createResult = createConversation(
                        token = token,
                        participantIds = listOf(contact.userId),
                        isGroup = false
                    )

                    if (createResult.isSuccess) {
                        createResult.getOrNull()?.let { createdConversations.add(it) }
                    }
                }

                Result.success(createdConversations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========== Data Retention ==========

    /**
     * Delete messages older than 7 days
     */
    suspend fun cleanupOldMessages(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - Constants.MESSAGE_RETENTION_MILLIS
                val deletedCount = messageDao.deleteOldMessages(cutoffTime)
                Result.success(deletedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========== Mapper Functions ==========

    private fun dtoToConversationEntity(dto: ConversationDto): ConversationEntity {
        return ConversationEntity(
            id = dto.id,
            isGroup = dto.isGroup,
            groupName = dto.groupName,
            groupAvatar = dto.groupAvatar,
            lastMessage = dto.lastMessage,
            lastMessageTime = dto.lastMessageTime,
            lastMessageSenderId = dto.lastMessageSenderId,
            unreadCount = dto.unreadCount,
            isPinned = dto.isPinned,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    private fun entityToConversation(
        entity: ConversationEntity,
        participants: List<ConversationParticipantEntity>,
        currentUserId: String
    ): Conversation {
        // For one-on-one chats, get the other participant's info
        val otherParticipant = if (!entity.isGroup && participants.isNotEmpty()) {
            participants.firstOrNull { it.userId != currentUserId } ?: participants.first()
        } else {
            null
        }

        return Conversation(
            id = entity.id,
            participantId = otherParticipant?.userId ?: "",
            participantName = if (entity.isGroup) {
                entity.groupName ?: "Group Chat"
            } else {
                otherParticipant?.userName ?: "Unknown"
            },
            participantAvatar = if (entity.isGroup) {
                entity.groupAvatar
            } else {
                otherParticipant?.userAvatar
            },
            lastMessage = entity.lastMessage ?: "",
            lastMessageTime = entity.lastMessageTime,
            unreadCount = entity.unreadCount,
            isOnline = otherParticipant?.isOnline ?: false,
            isPinned = entity.isPinned,
            isGroup = entity.isGroup
        )
    }

    private fun dtoToMessageEntity(dto: MessageDto): MessageEntity {
        return MessageEntity(
            id = dto.id,
            conversationId = dto.conversationId,
            senderId = dto.senderId,
            senderName = dto.senderName,
            senderAvatar = dto.senderAvatar,
            content = dto.content,
            timestamp = dto.timestamp,
            isRead = dto.isRead,
            messageType = dto.messageType,
            createdAt = dto.createdAt
        )
    }

    private fun entityToChatMessage(entity: MessageEntity): ChatMessage {
        return ChatMessage(
            id = entity.id,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            message = entity.content,
            timestamp = entity.timestamp,
            isRead = entity.isRead,
            messageType = entity.messageType
        )
    }
}
