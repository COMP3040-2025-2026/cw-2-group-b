package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.ConversationParticipantEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ConversationParticipant operations
 * Manages participants in conversations including online status and typing indicators
 */
@Dao
interface ConversationParticipantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ConversationParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ConversationParticipantEntity>)

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    fun getParticipantsForConversation(conversationId: String): Flow<List<ConversationParticipantEntity>>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    suspend fun getParticipantsForConversationSync(conversationId: String): List<ConversationParticipantEntity>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun getParticipant(conversationId: String, userId: String): ConversationParticipantEntity?

    @Query("UPDATE conversation_participants SET isOnline = :isOnline, lastSeenAt = :lastSeenAt WHERE userId = :userId")
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean, lastSeenAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversation_participants SET isTyping = :isTyping WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean)

    @Query("SELECT * FROM conversation_participants WHERE userId = :userId")
    fun getConversationsForUser(userId: String): Flow<List<ConversationParticipantEntity>>

    @Delete
    suspend fun deleteParticipant(participant: ConversationParticipantEntity)

    @Query("DELETE FROM conversation_participants WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun removeParticipant(conversationId: String, userId: String)

    @Query("DELETE FROM conversation_participants WHERE conversationId = :conversationId")
    suspend fun deleteParticipantsForConversation(conversationId: String)

    @Query("DELETE FROM conversation_participants")
    suspend fun deleteAllParticipants()
}
