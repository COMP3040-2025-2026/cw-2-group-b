package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Booking entity for sports facility bookings
 */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Auto-generated unique ID
    val userId: String,
    val userName: String,       // New: Booking person's name
    val facilityName: String,   // Facility name (e.g. "Badminton Court 1")
    val bookingDate: String,    // Format "yyyy-MM-dd"
    val timeSlot: Int,          // New: Integer time slot (e.g. 10 represents 10:00-11:00)
    val status: String = "confirmed",
    val createdAt: Long = System.currentTimeMillis()
)
