package com.nottingham.mynottingham.data.model

/**
 * Notti AI chat message data model
 */
data class NottiMessage(
    val id: String,
    val message: String,
    val timestamp: Long,
    val isUserMessage: Boolean,
    val messageType: NottiMessageType = NottiMessageType.TEXT
)

enum class NottiMessageType {
    TEXT,
    SUGGESTION,
    QUICK_ACTION
}

/**
 * Quick action suggestions for Notti
 */
data class QuickAction(
    val id: String,
    val title: String,
    val icon: String,
    val action: String
)
