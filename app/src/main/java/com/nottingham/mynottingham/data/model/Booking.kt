package com.nottingham.mynottingham.data.model

/**
 * Booking data model for sports facility bookings
 * Used by UI components to display and manage bookings
 */
data class Booking(
    val id: String,
    val userId: String,
    val userName: String,
    val facilityName: String,
    val bookingDate: String,
    val timeSlot: Int,
    val status: String
)
