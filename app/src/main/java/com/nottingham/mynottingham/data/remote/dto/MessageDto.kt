package com.nottingham.mynottingham.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for Messaging
 */

// Message DTOs
data class MessageDto(
    val id: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_name")
    val senderName: String,
    @SerializedName("sender_avatar")
    val senderAvatar: String?,
    val content: String,
    val timestamp: Long,
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("message_type")
    val messageType: String,
    @SerializedName("created_at")
    val createdAt: Long
)

data class ConversationDto(
    val id: String,
    @SerializedName("is_group")
    val isGroup: Boolean,
    @SerializedName("group_name")
    val groupName: String?,
    @SerializedName("group_avatar")
    val groupAvatar: String?,
    @SerializedName("last_message")
    val lastMessage: String?,
    @SerializedName("last_message_time")
    val lastMessageTime: Long,
    @SerializedName("last_message_sender_id")
    val lastMessageSenderId: String?,
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("is_pinned")
    val isPinned: Boolean,
    val participants: List<ParticipantDto>,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long
)

data class ParticipantDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_avatar")
    val userAvatar: String?,
    @SerializedName("is_online")
    val isOnline: Boolean,
    @SerializedName("is_typing")
    val isTyping: Boolean,
    @SerializedName("last_seen_at")
    val lastSeenAt: Long
)

// Request DTOs
data class SendMessageRequest(
    val content: String,
    @SerializedName("message_type")
    val messageType: String = "TEXT",
    @SerializedName("attachment_url")
    val attachmentUrl: String? = null
)

data class CreateConversationRequest(
    @SerializedName("participant_ids")
    val participantIds: List<String>,
    @SerializedName("is_group")
    val isGroup: Boolean = false,
    @SerializedName("group_name")
    val groupName: String? = null
)

data class UpdatePinnedStatusRequest(
    @SerializedName("is_pinned")
    val isPinned: Boolean
)

data class MarkAsReadRequest(
    @SerializedName("conversation_id")
    val conversationId: String
)

data class TypingStatusRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("is_typing")
    val isTyping: Boolean
)

// Response DTOs
data class ConversationListResponse(
    val success: Boolean,
    val message: String,
    val data: List<ConversationDto>?
)

data class MessageListResponse(
    val success: Boolean,
    val message: String,
    val data: MessageListData?
)

data class MessageListData(
    val messages: List<MessageDto>,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("total_count")
    val totalCount: Int
)

data class SendMessageResponse(
    val success: Boolean,
    val message: String,
    val data: MessageDto?
)

data class ConversationResponse(
    val success: Boolean,
    val message: String,
    val data: ConversationDto?
)

data class ContactSuggestionDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_avatar")
    val userAvatar: String?,
    val program: String?,
    val year: Int?,
    @SerializedName("is_online")
    val isOnline: Boolean
)

data class ContactSuggestionResponse(
    val success: Boolean,
    val message: String,
    val data: List<ContactSuggestionDto>?
)
