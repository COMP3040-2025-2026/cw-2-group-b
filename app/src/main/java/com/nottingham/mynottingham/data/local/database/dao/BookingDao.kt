package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    // Get all bookings for current user (for "My Bookings" page)
    @Query("SELECT * FROM bookings WHERE userId = :userId AND status != 'deleted' ORDER BY bookingDate DESC, timeSlot ASC")
    fun getUserBookings(userId: String): Flow<List<BookingEntity>>

    // Core fix: Get all bookings for specific facility and date (to gray out booked time slots)
    @Query("SELECT * FROM bookings WHERE facilityName = :facilityName AND bookingDate = :date AND status = 'confirmed'")
    suspend fun getBookingsByFacilityAndDate(facilityName: String, date: String): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    // Add other methods as needed
}
