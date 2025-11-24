package com.nottingham.mynottingham.data.repository

import android.util.Log
import com.nottingham.mynottingham.data.local.database.entities.*
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.remote.dto.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Extension functions for converting DTOs to Entities and Models
 * This eliminates boilerplate mapper code from Repository classes
 */

// ========== Forum Mappers ==========

/**
 * Convert ForumPostDto to ForumPostEntity
 */
fun ForumPostDto.toEntity(): ForumPostEntity {
    return ForumPostEntity(
        id = this.id,
        authorId = this.authorId,
        authorName = this.authorName,
        authorAvatar = this.authorAvatar,
        title = this.title,
        content = this.content,
        category = this.category,
        imageUrl = this.imageUrl,
        likes = this.likes,
        comments = this.comments,
        views = this.views,
        isPinned = this.isPinned,
        isLocked = this.isLocked,
        isLikedByCurrentUser = this.isLikedByCurrentUser,
        tags = this.tags?.joinToString(","),
        createdAt = parseTimestamp(this.createdAt),
        updatedAt = parseTimestamp(this.updatedAt)
    )
}

/**
 * Convert ForumCommentDto to ForumCommentEntity
 */
fun ForumCommentDto.toEntity(): ForumCommentEntity {
    return ForumCommentEntity(
        id = this.id,
        postId = this.postId,
        authorId = this.authorId,
        authorName = this.authorName,
        authorAvatar = this.authorAvatar,
        content = this.content,
        likes = this.likes,
        isLikedByCurrentUser = this.isLikedByCurrentUser,
        createdAt = parseTimestamp(this.createdAt)
    )
}

// ========== Message Mappers ==========

/**
 * Convert ConversationDto to ConversationEntity
 */
fun ConversationDto.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = this.id,
        isGroup = this.isGroup,
        groupName = this.groupName,
        groupAvatar = this.groupAvatar,
        lastMessage = this.lastMessage,
        lastMessageTime = this.lastMessageTime,
        lastMessageSenderId = this.lastMessageSenderId,
        unreadCount = this.unreadCount,
        isPinned = this.isPinned,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert ParticipantDto to ConversationParticipantEntity
 */
fun ParticipantDto.toEntity(conversationId: String): ConversationParticipantEntity {
    return ConversationParticipantEntity(
        conversationId = conversationId,
        userId = this.userId,
        userName = this.userName,
        userAvatar = this.userAvatar,
        isOnline = this.isOnline,
        isTyping = this.isTyping,
        lastSeenAt = this.lastSeenAt
    )
}

/**
 * Convert MessageDto to MessageEntity
 */
fun MessageDto.toEntity(): MessageEntity {
    return MessageEntity(
        id = this.id,
        conversationId = this.conversationId,
        senderId = this.senderId,
        senderName = this.senderName,
        senderAvatar = this.senderAvatar,
        content = this.content,
        timestamp = this.timestamp,
        isRead = this.isRead,
        messageType = this.messageType,
        createdAt = this.createdAt
    )
}

/**
 * Convert MessageEntity to ChatMessage domain model
 */
fun MessageEntity.toModel(): ChatMessage {
    return ChatMessage(
        id = this.id,
        conversationId = this.conversationId,
        senderId = this.senderId,
        senderName = this.senderName,
        senderAvatar = this.senderAvatar,
        message = this.content,
        timestamp = this.timestamp,
        isRead = this.isRead,
        messageType = this.messageType
    )
}

/**
 * Convert ConversationEntity + Participants to Conversation domain model
 */
fun ConversationEntity.toModel(
    participants: List<ConversationParticipantEntity>,
    currentUserId: String
): Conversation {
    // For one-on-one chats, get the other participant's info
    val otherParticipant = if (!this.isGroup && participants.isNotEmpty()) {
        participants.firstOrNull { it.userId != currentUserId } ?: participants.first()
    } else {
        null
    }

    return Conversation(
        id = this.id,
        participantId = otherParticipant?.userId ?: "",
        participantName = if (this.isGroup) {
            this.groupName ?: "Group Chat"
        } else {
            otherParticipant?.userName ?: "Unknown"
        },
        participantAvatar = if (this.isGroup) {
            this.groupAvatar
        } else {
            otherParticipant?.userAvatar
        },
        lastMessage = this.lastMessage ?: "",
        lastMessageTime = this.lastMessageTime,
        unreadCount = this.unreadCount,
        isOnline = otherParticipant?.isOnline ?: false,
        isPinned = this.isPinned,
        isGroup = this.isGroup
    )
}

// ========== Helper Functions ==========

/**
 * Parse ISO 8601 timestamp string to millis
 * Handles both ZonedDateTime and LocalDateTime formats
 */
private fun parseTimestamp(timestamp: String): Long {
    return try {
        // Try parsing with timezone first (e.g., "2023-11-19T10:00:00Z")
        ZonedDateTime.parse(timestamp).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            // If that fails, try parsing as LocalDateTime and assume UTC
            // (e.g., "2023-11-19T10:00:00")
            LocalDateTime.parse(timestamp)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        } catch (e2: Exception) {
            Log.e("DtoMappers", "Failed to parse timestamp: $timestamp", e2)
            System.currentTimeMillis()
        }
    }
}
