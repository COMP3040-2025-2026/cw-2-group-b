package com.nottingham.mynottingham.data.model

/**
 * Booking data model
 */
data class Booking(
    val id: String,
    val userId: String,
    val facilityType: String,
    val facilityName: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String,
    val status: BookingStatus,
    val notes: String? = null,
    val createdAt: Long
)

enum class BookingStatus {
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
