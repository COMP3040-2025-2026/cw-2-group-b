package com.nottingham.mynottingham.data.model

/**
 * Message conversation data model
 */
data class Conversation(
    val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isGroup: Boolean = false
)

/**
 * Chat message data model
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val messageType: String = "TEXT", // TEXT, IMAGE, FILE
    val imageUrl: String? = null // URL for image messages
)
