package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Booking entity for sports facility bookings
 */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val facilityType: String,
    val facilityName: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String,
    val status: String, // "confirmed", "cancelled", "completed"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
