package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Message operations
 * Includes 7-day retention policy queries
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesForConversationPaged(conversationId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND isRead = 0")
    suspend fun getUnreadCount(conversationId: String, currentUserId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND isRead = 0")
    fun getUnreadCountFlow(conversationId: String, currentUserId: String): Flow<Int>

    @Query("DELETE FROM messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldMessages(cutoffTimestamp: Long): Int

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
