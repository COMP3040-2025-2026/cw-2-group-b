package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Booking entity for sports facility bookings
 */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val bookingId: Long = 0,
    val facilityId: String,
    val facilityName: String,
    val bookingDate: String, // 格式: "yyyy-MM-dd"
    val timeSlot: Int,       // 例如: 10 代表 10:00 - 11:00
    val userId: String,
    val userName: String,    // 预定人名字 (直接存储方便显示)
    val bookingTime: Long = System.currentTimeMillis(), // 下单时间
    val status: String, // "confirmed", "cancelled", "completed"
    val notes: String? = null
)
