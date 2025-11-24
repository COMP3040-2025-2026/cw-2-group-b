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
 * 完全使用 Firebase Realtime Database 管理场地预订
 * 不再依赖本地 Room 数据库
 *
 * 优势：
 * - 实时同步：多设备自动同步预订状态
 * - 冲突检测：服务器端验证时间冲突
 * - 数据持久化：云端备份，不怕丢失
 */
class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseBookingRepo = FirebaseBookingRepository()

    // 用于观察当前选定日期的已被预定列表
    private val _occupiedSlots = MutableLiveData<List<BookingEntity>>()
    val occupiedSlots: LiveData<List<BookingEntity>> = _occupiedSlots

    // 用于观察用户的所有预订
    private val _userBookings = MutableLiveData<List<BookingEntity>>()
    val userBookings: LiveData<List<BookingEntity>> = _userBookings

    /**
     * 加载某设施在某天的预定情况
     * TODO: 需要从 Firebase 查询特定日期的预订
     */
    fun loadOccupiedSlots(facilityName: String, date: String) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "📥 Loading occupied slots for $facilityName on $date")
                // TODO: 实现 Firebase 查询逻辑
                // 暂时返回空列表
                _occupiedSlots.value = emptyList()
            } catch (e: Exception) {
                Log.e("BookingViewModel", "❌ Error loading occupied slots", e)
                _occupiedSlots.value = emptyList()
            }
        }
    }

    /**
     * 保存预定到 Firebase
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
                Log.d("BookingViewModel", "📤 Creating booking: $facilityName on $date at $timeSlot:00")

                // 计算时间戳
                val bookingDate = LocalDate.parse(date)
                val bookingTime = LocalTime.of(timeSlot, 0)
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)
                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val startTime = bookingDateTime.atZone(zoneId).toInstant().toEpochMilli()
                val endTime = bookingDateTime.plusHours(1).atZone(zoneId).toInstant().toEpochMilli()

                // 检查可用性
                val isAvailable = firebaseBookingRepo.checkAvailability(facilityName, startTime, endTime)
                if (!isAvailable) {
                    Log.e("BookingViewModel", "❌ Time slot is already booked")
                    // TODO: 通知 UI 显示错误消息
                    return@launch
                }

                // 创建预订数据
                val bookingData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "facilityName" to facilityName,
                    "facilityType" to getFacilityType(facilityName),
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "fee" to getFacilityFee(facilityName)
                )

                val result = firebaseBookingRepo.createBooking(bookingData)

                if (result.isSuccess) {
                    val bookingId = result.getOrNull()
                    Log.d("BookingViewModel", "✅ Booking created successfully: $bookingId")

                    // 刷新预订列表
                    loadOccupiedSlots(facilityName, date)
                    onSuccess()
                } else {
                    Log.e("BookingViewModel", "❌ Failed to create booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "❌ Error creating booking", e)
            }
        }
    }

    /**
     * 获取当前用户的所有预定（实时监听）
     */
    fun getUserBookings(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "📥 Loading bookings for user: $userId")

                // 使用 Firebase Flow 实时监听
                firebaseBookingRepo.getUserBookings(userId).collect { firebaseBookings ->
                    // 转换为 BookingEntity
                    val bookingEntities = firebaseBookings.mapNotNull { mapToBookingEntity(it) }

                    Log.d("BookingViewModel", "✅ Loaded ${bookingEntities.size} bookings")
                    _userBookings.postValue(bookingEntities)
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "❌ Error loading user bookings", e)
                _userBookings.postValue(emptyList())
            }
        }
    }

    /**
     * 取消预定
     */
    fun cancelBooking(booking: BookingEntity) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "🗑️ Cancelling booking: ${booking.id}")

                val result = firebaseBookingRepo.cancelBooking(booking.id.toString())

                if (result.isSuccess) {
                    Log.d("BookingViewModel", "✅ Booking cancelled successfully")
                    // Firebase Flow 会自动更新列表
                } else {
                    Log.e("BookingViewModel", "❌ Failed to cancel booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "❌ Error cancelling booking", e)
            }
        }
    }

    /**
     * 将 Firebase Map 数据转换为 BookingEntity
     */
    private fun mapToBookingEntity(firebaseData: Map<String, Any>): BookingEntity? {
        return try {
            val id = firebaseData["id"] as? String ?: return null
            val userId = firebaseData["userId"] as? String ?: ""
            val userName = firebaseData["userName"] as? String ?: ""
            val facilityName = firebaseData["facilityName"] as? String ?: ""
            val startTime = firebaseData["startTime"] as? Long ?: 0L
            val status = firebaseData["status"] as? String ?: "PENDING"

            // 从 timestamp 转换为日期和时间槽
            val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
            val dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime),
                zoneId
            )
            val date = dateTime.toLocalDate().toString()
            val timeSlot = dateTime.hour

            BookingEntity(
                id = id, // Firebase 生成的真实 ID
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
     * 根据设施名称获取类型
     */
    private fun getFacilityType(facilityName: String): String {
        return when {
            facilityName.contains("Basketball", ignoreCase = true) -> "Basketball Court"
            facilityName.contains("Badminton", ignoreCase = true) -> "Badminton Court"
            facilityName.contains("Tennis", ignoreCase = true) -> "Tennis Court"
            else -> "Sports Facility"
        }
    }

    /**
     * 根据设施名称获取费用
     */
    private fun getFacilityFee(facilityName: String): Double {
        return when {
            facilityName.contains("Basketball", ignoreCase = true) -> 10.0
            facilityName.contains("Badminton", ignoreCase = true) -> 15.0
            facilityName.contains("Tennis", ignoreCase = true) -> 20.0
            else -> 10.0
        }
    }
}
