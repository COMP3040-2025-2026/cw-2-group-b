package com.nottingham.mynottingham.data.model

import java.util.Date

/**
 * Sports Complex Booking Models
 */

// Facility Type
data class Facility(
    val id: String,
    val name: String,
    val type: FacilityType,
    val location: String = "Sports Complex",
    val durationPerSlot: Int = 60, // minutes
    val minPlayers: Int? = null,
    val notes: String? = null
)

enum class FacilityType {
    PITCH,
    BADMINTON_COURT,
    FIELD,
    OUTDOOR_COURT,
    SPORTS_HALL,
    SQUASH_COURT,
    TABLE_TENNIS_COURT,
    TENNIS_COURT
}

// Time Slot
data class TimeSlot(
    val time: String,
    val isAvailable: Boolean,
    val startHour: Int,
    val startMinute: Int = 0
)

// Booking
data class SportsBooking(
    val id: String,
    val facilityId: String,
    val facilityName: String,
    val date: Date,
    val timeSlot: String,
    val startTime: String,
    val endTime: String,
    val location: String = "Sports Complex",
    val status: SportsBookingStatus = SportsBookingStatus.UPCOMING,
    val userId: String
)

enum class SportsBookingStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

// Equipment
data class Equipment(
    val id: String,
    val name: String,
    val rate: Double,
    val type: EquipmentType
)

enum class EquipmentType {
    RENTAL,
    SALES
}

// Operating Hours
data class OperatingHours(
    val dayType: OperatingDayType,
    val openTime: String,
    val closeTime: String,
    val isClosed: Boolean = false
)

enum class OperatingDayType {
    WEEKDAY,    // Mon - Sat
    SUNDAY,
    PUBLIC_HOLIDAY
}

// Guideline
data class BookingGuideline(
    val venue: String,
    val sport: String,
    val courtField: String,
    val minPax: String,
    val maxPax: String
)
