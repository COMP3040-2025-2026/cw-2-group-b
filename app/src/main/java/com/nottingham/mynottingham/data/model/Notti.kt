package com.nottingham.mynottingham.data.model

/**
 * Notti AI chat message data model
 * For Notti AI assistant message display
 */
data class NottiMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val messageType: NottiMessageType = NottiMessageType.TEXT,
    // Card data
    val cardData: NottiCardData? = null
)

enum class NottiMessageType {
    TEXT,
    SHUTTLE_CARD,
    BOOKING_CARD
}

/**
 * Card data wrapper
 */
data class NottiCardData(
    val title: String,
    val subtitle: String? = null,
    val items: List<NottiCardItem> = emptyList()
)

/**
 * Individual item within card
 */
data class NottiCardItem(
    val label: String,
    val value: String,
    val icon: String? = null,  // emoji or icon name
    val isHighlighted: Boolean = false
)

/**
 * Quick action suggestions for Notti
 */
data class QuickAction(
    val id: String,
    val title: String,
    val icon: String,
    val action: String
)
