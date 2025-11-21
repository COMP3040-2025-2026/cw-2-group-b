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
        return bookingDao.getUserBookings(userId).asLiveData()
    }

    // 取消预定
    fun cancelBooking(booking: BookingEntity) {
        viewModelScope.launch {
            bookingDao.deleteBooking(booking)
            // 此时 LiveData 会自动更新，界面会自动刷新
        }
    }
}
