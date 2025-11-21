package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    // 获取当前用户的所有预定（用于"My Bookings"页面）
    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY bookingDate DESC, timeSlot ASC")
    fun getUserBookings(userId: String): Flow<List<BookingEntity>>

    // 【修复核心】：获取指定设施、指定日期的所有预定（用于把已定时间置灰）
    @Query("SELECT * FROM bookings WHERE facilityName = :facilityName AND bookingDate = :date AND status = 'confirmed'")
    suspend fun getBookingsByFacilityAndDate(facilityName: String, date: String): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)
    
    // 如果需要其他方法保留即可
}
