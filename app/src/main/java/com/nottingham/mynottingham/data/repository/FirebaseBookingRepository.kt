package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Booking Repository
 *
 * Reads and creates booking data directly from Firebase Realtime Database.
 * No longer depends on Spring Boot backend API.
 *
 * Firebase data structure:
 * bookings/{bookingId}: {
 *   userId: "student1",
 *   userName: "Alice Wong",
 *   facilityName: "Basketball Court 1",
 *   facilityType: "Basketball Court",
 *   status: "CONFIRMED" | "PENDING" | "CANCELLED",
 *   startTime: 1732443000000 (timestamp),
 *   endTime: 1732446600000 (timestamp),
 *   fee: 10.00,
 *   createdAt: 1732443000000
 * }
 */
class FirebaseBookingRepository {

    // Important: Must specify database URL as database is in asia-southeast1 region
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val bookingsRef: DatabaseReference = database.getReference("bookings")

    /**
     * Create new booking
     * @param booking Booking data
     * @return Result<String> Booking ID or error
     */
    suspend fun createBooking(booking: Map<String, Any>): Result<String> {
        return try {
            val newBookingRef = bookingsRef.push()
            val bookingId = newBookingRef.key ?: throw Exception("Failed to generate booking ID")

            val bookingData = booking.toMutableMap()
            bookingData["createdAt"] = System.currentTimeMillis()
            bookingData["status"] = "PENDING"

            newBookingRef.setValue(bookingData).await()

            Result.success(bookingId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error creating booking: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get user's all bookings
     * @param userId User ID
     * @return Flow<List<Map<String, Any>>> Booking list flow
     */
    fun getUserBookings(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = mutableListOf<Map<String, Any>>()

                for (child in snapshot.children) {
                    val booking = child.value as? Map<String, Any> ?: continue
                    val bookingWithId = booking.toMutableMap()
                    bookingWithId["id"] = child.key ?: ""
                    bookings.add(bookingWithId)
                }

                // Sort by creation time descending
                bookings.sortByDescending { it["createdAt"] as? Long ?: 0L }
                trySend(bookings)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        bookingsRef.orderByChild("userId").equalTo(userId).addValueEventListener(listener)

        awaitClose {
            bookingsRef.removeEventListener(listener)
        }
    }

    /**
     * Cancel booking
     * @param bookingId Booking ID
     * @return Result<Unit>
     */
    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledAt" to System.currentTimeMillis()
            )
            bookingsRef.child(bookingId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error cancelling booking: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete booking record (completely remove from database)
     * @param bookingId Booking ID
     * @return Result<Unit>
     */
    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            bookingsRef.child(bookingId).removeValue().await()
            android.util.Log.d("FirebaseBookingRepo", "Booking deleted: $bookingId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error deleting booking: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all bookings for a specific facility on a specific date
     * Uses client-side filtering to avoid needing Firebase indexes
     *
     * @param facilityName Facility name
     * @param dateStart Date start timestamp (day's 00:00:00)
     * @param dateEnd Date end timestamp (day's 23:59:59)
     * @return Result<List<Map<String, Any>>> Booking list
     */
    suspend fun getBookingsByFacilityAndDate(
        facilityName: String,
        dateStart: Long,
        dateEnd: Long
    ): Result<List<Map<String, Any>>> {
        return try {
            // Get all bookings, then filter on client side (avoids needing Firebase indexes)
            val snapshot = bookingsRef.get().await()
            val bookings = mutableListOf<Map<String, Any>>()

            for (child in snapshot.children) {
                val booking = child.value as? Map<String, Any> ?: continue
                val bookingFacility = booking["facilityName"] as? String ?: continue
                val startTime = (booking["startTime"] as? Long) ?: continue
                val status = booking["status"] as? String ?: ""

                // Match facility name
                if (bookingFacility != facilityName) continue

                // Skip cancelled bookings
                if (status == "CANCELLED") continue

                // Check if booking is within specified date
                if (startTime >= dateStart && startTime < dateEnd) {
                    val bookingWithId = booking.toMutableMap()
                    bookingWithId["id"] = child.key ?: ""
                    bookings.add(bookingWithId)
                }
            }

            Result.success(bookings)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error getting bookings by date: ${e.message}")
            // Return empty list instead of failure so user can still try to book
            Result.success(emptyList())
        }
    }

    /**
     * Get availability for a specific time slot
     * Uses client-side filtering to avoid needing Firebase indexes
     *
     * @param facilityName Facility name
     * @param startTime Start time
     * @param endTime End time
     * @return Boolean Whether available
     */
    suspend fun checkAvailability(facilityName: String, startTime: Long, endTime: Long): Boolean {
        return try {
            // Get all bookings, then filter on client side
            val snapshot = bookingsRef.get().await()

            // Check for conflicting bookings
            for (child in snapshot.children) {
                val booking = child.value as? Map<String, Any> ?: continue
                val bookingFacility = booking["facilityName"] as? String ?: continue

                // Only check bookings for the same facility
                if (bookingFacility != facilityName) continue

                val bookingStart = (booking["startTime"] as? Long) ?: 0L
                val bookingEnd = (booking["endTime"] as? Long) ?: 0L
                val status = booking["status"] as? String ?: ""

                // Skip cancelled bookings
                if (status == "CANCELLED") continue

                // Check for time conflict
                if (startTime < bookingEnd && endTime > bookingStart) {
                    android.util.Log.d("FirebaseBookingRepo", "Time conflict found with existing booking")
                    return false
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error checking availability: ${e.message}")
            // Return true on error to allow user to try booking
            true
        }
    }
}
