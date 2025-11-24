package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Errand entity for campus errand tasks
 */
@Entity(tableName = "errands")
data class ErrandEntity(
    @PrimaryKey
    val id: String,
    val requesterId: String,
    val providerId: String? = null,
    val title: String,
    val description: String,
    val type: String, // "Food Delivery", "Package Pickup", etc.
    val priority: String, // "Standard", "Express"
    val pickupLocation: String,
    val deliveryLocation: String,
    val fee: Double,
    val status: String, // "pending", "accepted", "picked_up", "completed", "cancelled"
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
