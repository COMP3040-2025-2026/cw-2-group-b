package com.nottingham.mynottingham.ui.booking

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import com.nottingham.mynottingham.data.repository.FirebaseBookingRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * BookingViewModel - Firebase Migration Edition
 *
 * Fully uses Firebase Realtime Database to manage facility bookings
 * No longer relies on local Room database
 *
 * Advantages:
 * - Real-time sync: Automatic multi-device booking status synchronization
 * - Conflict detection: Server-side validation of time conflicts
 * - Data persistence: Cloud backup, no data loss
 */
class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseBookingRepo = FirebaseBookingRepository()

    // For observing already booked slots on the selected date
    private val _occupiedSlots = MutableLiveData<List<BookingEntity>>()
    val occupiedSlots: LiveData<List<BookingEntity>> = _occupiedSlots

    // For observing all user bookings
    private val _userBookings = MutableLiveData<List<BookingEntity>>()
    val userBookings: LiveData<List<BookingEntity>> = _userBookings

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Load bookings for a facility on a specific day
     * Query Firebase for bookings on specific date
     */
    fun loadOccupiedSlots(facilityName: String, date: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("BookingViewModel", "Loading occupied slots for $facilityName on $date")

                // Calculate time range for the day
                val bookingDate = LocalDate.parse(date)
                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val dateStart = bookingDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dateEnd = bookingDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                // Query from Firebase
                val result = firebaseBookingRepo.getBookingsByFacilityAndDate(facilityName, dateStart, dateEnd)

                result.onSuccess { bookings ->
                    val bookingEntities = bookings.mapNotNull { mapToBookingEntity(it) }
                    Log.d("BookingViewModel", "Found ${bookingEntities.size} occupied slots")
                    _occupiedSlots.value = bookingEntities
                }.onFailure { e ->
                    Log.e("BookingViewModel", "Error loading occupied slots: ${e.message}")
                    _occupiedSlots.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error loading occupied slots", e)
                _occupiedSlots.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save booking to Firebase
     */
    fun saveBooking(
        facilityName: String,
        date: String,
        timeSlot: Int,
        userId: String,
        userName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Creating booking: $facilityName on $date at $timeSlot:00")

                // Calculate timestamp
                val bookingDate = LocalDate.parse(date)
                val bookingTime = LocalTime.of(timeSlot, 0)
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)
                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val startTime = bookingDateTime.atZone(zoneId).toInstant().toEpochMilli()
                val endTime = bookingDateTime.plusHours(1).atZone(zoneId).toInstant().toEpochMilli()

                // Check availability
                val isAvailable = firebaseBookingRepo.checkAvailability(facilityName, startTime, endTime)
                if (!isAvailable) {
                    Log.e("BookingViewModel", "Time slot is already booked")
                    // TODO: Notify UI to show error message
                    return@launch
                }

                // Create booking data
                val bookingData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "facilityName" to facilityName,
                    "facilityType" to getFacilityType(facilityName),
                    "startTime" to startTime,
                    "endTime" to endTime
                )

                val result = firebaseBookingRepo.createBooking(bookingData)

                if (result.isSuccess) {
                    val bookingId = result.getOrNull()
                    Log.d("BookingViewModel", "Booking created successfully: $bookingId")

                    // Refresh bookings list
                    loadOccupiedSlots(facilityName, date)
                    onSuccess()
                } else {
                    Log.e("BookingViewModel", "Failed to create booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error creating booking", e)
            }
        }
    }

    /**
     * Get all bookings for current user (real-time listening)
     */
    fun getUserBookings(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Loading bookings for user: $userId")

                // Use Firebase Flow for real-time listening
                firebaseBookingRepo.getUserBookings(userId).collect { firebaseBookings ->
                    // Convert to BookingEntity
                    val bookingEntities = firebaseBookings.mapNotNull { mapToBookingEntity(it) }

                    Log.d("BookingViewModel", "Loaded ${bookingEntities.size} bookings")
                    _userBookings.postValue(bookingEntities)
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error loading user bookings", e)
                _userBookings.postValue(emptyList())
            }
        }
    }

    /**
     * Cancel booking (change status to CANCELLED)
     */
    fun cancelBooking(booking: BookingEntity) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Cancelling booking: ${booking.id}")

                val result = firebaseBookingRepo.cancelBooking(booking.id)

                if (result.isSuccess) {
                    Log.d("BookingViewModel", "Booking cancelled successfully")
                    // Firebase Flow will automatically update the list
                } else {
                    Log.e("BookingViewModel", "Failed to cancel booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error cancelling booking", e)
            }
        }
    }

    /**
     * Delete booking record (completely delete from database)
     */
    fun deleteBooking(booking: BookingEntity) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Deleting booking: ${booking.id}")

                val result = firebaseBookingRepo.deleteBooking(booking.id)

                if (result.isSuccess) {
                    Log.d("BookingViewModel", "Booking deleted successfully")
                    // Firebase Flow will automatically update the list
                } else {
                    Log.e("BookingViewModel", "Failed to delete booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error deleting booking", e)
            }
        }
    }

    /**
     * Convert Firebase Map data to BookingEntity
     */
    private fun mapToBookingEntity(firebaseData: Map<String, Any>): BookingEntity? {
        return try {
            val id = firebaseData["id"] as? String ?: return null
            val userId = firebaseData["userId"] as? String ?: ""
            val userName = firebaseData["userName"] as? String ?: ""
            val facilityName = firebaseData["facilityName"] as? String ?: ""
            val startTime = firebaseData["startTime"] as? Long ?: 0L
            val status = firebaseData["status"] as? String ?: "PENDING"

            // Convert from timestamp to date and time slot
            val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
            val dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime),
                zoneId
            )
            val date = dateTime.toLocalDate().toString()
            val timeSlot = dateTime.hour

            BookingEntity(
                id = id, // Real ID generated by Firebase
                userId = userId,
                userName = userName,
                facilityName = facilityName,
                bookingDate = date,
                timeSlot = timeSlot,
                status = status
            )
        } catch (e: Exception) {
            Log.w("BookingViewModel", "Failed to parse booking data: ${e.message}")
            null
        }
    }

    /**
     * Get facility type based on facility name
     */
    private fun getFacilityType(facilityName: String): String {
        return when {
            facilityName.contains("Basketball", ignoreCase = true) -> "Basketball Court"
            facilityName.contains("Badminton", ignoreCase = true) -> "Badminton Court"
            facilityName.contains("Tennis", ignoreCase = true) -> "Tennis Court"
            facilityName.contains("Pitch", ignoreCase = true) -> "Football Pitch"
            facilityName.contains("Squash", ignoreCase = true) -> "Squash Court"
            facilityName.contains("Table Tennis", ignoreCase = true) -> "Table Tennis"
            else -> "Sports Facility"
        }
    }
}
