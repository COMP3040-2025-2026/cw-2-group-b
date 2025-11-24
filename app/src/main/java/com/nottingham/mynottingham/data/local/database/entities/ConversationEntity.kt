package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Conversation entity for Room database
 * Stores conversation metadata including pinned status and group information
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupAvatar: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
