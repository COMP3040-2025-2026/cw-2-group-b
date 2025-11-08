package com.nottingham.mynottingham.data.model

/**
 * Errand data model
 */
data class Errand(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val providerId: String? = null,
    val providerName: String? = null,
    val title: String,
    val description: String,
    val type: String,
    val priority: String,
    val pickupLocation: String,
    val deliveryLocation: String,
    val fee: Double,
    val status: ErrandStatus,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ErrandStatus {
    PENDING,
    ACCEPTED,
    PICKED_UP,
    COMPLETED,
    CANCELLED
}
