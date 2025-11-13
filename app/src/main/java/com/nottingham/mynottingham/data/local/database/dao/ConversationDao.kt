package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Conversation operations
 * Supports pinned conversations and search
 */
@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isPinned = 1 ORDER BY lastMessageTime DESC")
    fun getPinnedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationByIdFlow(conversationId: String): Flow<ConversationEntity?>

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updatePinnedStatus(conversationId: String, isPinned: Boolean)

    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: String, count: Int)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTime = :timestamp, lastMessageSenderId = :senderId, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateLastMessage(
        conversationId: String,
        lastMessage: String,
        timestamp: Long,
        senderId: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT * FROM conversations
        WHERE (isGroup = 0 AND EXISTS(
            SELECT 1 FROM conversation_participants
            WHERE conversation_participants.conversationId = conversations.id
            AND conversation_participants.userName LIKE '%' || :query || '%'
        ))
        OR (isGroup = 1 AND groupName LIKE '%' || :query || '%')
        OR lastMessage LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, lastMessageTime DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
