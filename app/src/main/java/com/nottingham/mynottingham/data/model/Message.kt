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
    val isOnline: Boolean = false
)

/**
 * Chat message data model
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE
}
