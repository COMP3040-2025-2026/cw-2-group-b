package com.nottingham.mynottingham.data.remote.dto

/**
 * Data Transfer Objects for Booking
 */

data class FacilityResponse(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val capacity: Int
)

data class TimeSlotResponse(
    val id: String,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)

data class CreateBookingRequest(
    val facilityId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val notes: String?
)

data class BookingResponse(
    val id: String,
    val userId: String,
    val facilityType: String,
    val facilityName: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val notes: String?,
    val createdAt: Long
)
