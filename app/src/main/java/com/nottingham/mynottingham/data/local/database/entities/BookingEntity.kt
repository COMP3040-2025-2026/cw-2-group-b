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
    val id: String = UUID.randomUUID().toString(), // 自动生成唯一ID
    val userId: String,
    val userName: String,       // 新增：预定人名字
    val facilityName: String,   // 设施名称 (例如 "Badminton Court 1")
    val bookingDate: String,    // 格式 "yyyy-MM-dd"
    val timeSlot: Int,          // 新增：整数时间段 (例如 10 代表 10:00-11:00)
    val status: String = "confirmed",
    val createdAt: Long = System.currentTimeMillis()
)
