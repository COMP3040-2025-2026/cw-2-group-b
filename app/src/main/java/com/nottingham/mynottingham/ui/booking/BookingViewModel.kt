package com.nottingham.mynottingham.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import kotlinx.coroutines.launch
import androidx.lifecycle.asLiveData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val bookingDao = AppDatabase.getInstance(application).bookingDao()

    // 用于观察当前选定日期的已被预定列表
    private val _occupiedSlots = MutableLiveData<List<BookingEntity>>()
    val occupiedSlots: LiveData<List<BookingEntity>> = _occupiedSlots

    // 加载某设施在某天的预定情况
    fun loadOccupiedSlots(facilityName: String, date: String) {
        viewModelScope.launch {
            val bookings = bookingDao.getBookingsByFacilityAndDate(facilityName, date)
            _occupiedSlots.value = bookings
        }
    }

    // 保存预定
    fun saveBooking(
        facilityName: String,
        date: String,
        timeSlot: Int,
        userId: String,
        userName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val newBooking = BookingEntity(
                userId = userId,
                userName = userName,
                facilityName = facilityName,
                bookingDate = date,
                timeSlot = timeSlot
            )
            bookingDao.insertBooking(newBooking)
            // 保存成功后，重新刷新当前日期的占用列表，以便界面立即变灰
            loadOccupiedSlots(facilityName, date)
            onSuccess()
        }
    }

    // 获取当前用户的所有预定
    fun getUserBookings(userId: String): LiveData<List<BookingEntity>> {
        // Only show "confirmed" and "deleted" bookings, hide fully deleted ones
        return bookingDao.getUserBookings(userId).asLiveData()
    }

    // 取消预定或标记为删除
    fun cancelBooking(booking: BookingEntity) {
        viewModelScope.launch {
            try {
                val bookingDate = LocalDate.parse(booking.bookingDate)
                val bookingTime = LocalTime.of(booking.timeSlot, 0)
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)

                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val currentDateTime = LocalDateTime.now(zoneId)
                
                // Assuming each booking slot is 1 hour
                val endTime = bookingDateTime.plusHours(1)

                if (currentDateTime.isAfter(endTime)) {
                    // If the booking is in the past, update its status to "deleted"
                    val updatedBooking = booking.copy(status = "deleted")
                    bookingDao.updateBooking(updatedBooking)
                } else {
                    // If the booking is in the future, delete it
                    bookingDao.deleteBooking(booking)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: if there's an error in date parsing, just delete the booking
                bookingDao.deleteBooking(booking)
            }
        }
    }
}
