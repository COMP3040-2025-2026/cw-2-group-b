package com.nottingham.mynottingham.data.remote.dto

/**
 * Data Transfer Objects for Errand
 */

data class CreateErrandRequest(
    val title: String,
    val description: String,
    val type: String,
    val priority: String,
    val pickupLocation: String,
    val deliveryLocation: String,
    val fee: Double,
    val imageUrl: String?
)

data class ErrandResponse(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val providerId: String?,
    val providerName: String?,
    val title: String,
    val description: String,
    val type: String,
    val priority: String,
    val pickupLocation: String,
    val deliveryLocation: String,
    val fee: Double,
    val status: String,
    val imageUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class UpdateStatusRequest(
    val status: String
)
