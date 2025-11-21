package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Booking operations
 */
@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY bookingTime DESC")
    fun getUserBookings(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId")
    suspend fun getBookingById(bookingId: Long): BookingEntity?

    @Query("SELECT * FROM bookings WHERE userId = :userId AND status = 'confirmed' ORDER BY bookingDate ASC")
    fun getUpcomingBookings(userId: String): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE userId = :userId")
    suspend fun deleteUserBookings(userId: String)
}
