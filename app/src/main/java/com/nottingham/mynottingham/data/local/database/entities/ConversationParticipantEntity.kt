package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Conversation participant entity for Room database
 * Links users to conversations, supporting both one-on-one and group chats
 */
@Entity(
    tableName = "conversation_participants",
    primaryKeys = ["conversationId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
        // Note: Removed UserEntity foreign key constraint
        // ConversationParticipantEntity contains all necessary user info (userName, userAvatar)
        // This avoids FOREIGN KEY constraint errors when users are not cached locally
    ],
    indices = [Index("conversationId"), Index("userId")]
)
data class ConversationParticipantEntity(
    val conversationId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val joinedAt: Long = System.currentTimeMillis()
)
