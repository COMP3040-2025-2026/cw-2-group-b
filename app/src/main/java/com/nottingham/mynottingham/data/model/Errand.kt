package com.nottingham.mynottingham.data.model

/**
 * Errand data model
 */
data class Errand(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterAvatar: String? = null,
    val requesterRating: Double = 0.0,
    val requesterReviewCount: Int = 0,
    val providerId: String? = null,
    val providerName: String? = null,
    val title: String,
    val description: String,
    val type: ErrandType,
    val location: String,
    val deadline: String,
    val reward: Double,
    val additionalNotes: String? = null,
    val status: ErrandStatus,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)

enum class ErrandType(val displayName: String, val color: String) {
    SHOPPING("Shopping", "#FF6B9D"),
    PICKUP("Pickup", "#4ECDC4"),
    FOOD_DELIVERY("Food Delivery", "#FFD93D"),
    OTHER("Others", "#A8E6CF")
}

enum class ErrandStatus {
    PENDING,      // Waiting to be taken
    ACCEPTED,     // Accepted (courier accepted but not started delivery)
    DELIVERING,   // In delivery
    IN_PROGRESS,  // Compatible with old status, equivalent to ACCEPTED
    COMPLETED,    // Completed
    CANCELLED     // Cancelled
}

/**
 * User balance for Campus Errand
 */
data class ErrandBalance(
    val userId: String,
    val balance: Double,
    val totalEarned: Double,
    val totalSpent: Double
)
