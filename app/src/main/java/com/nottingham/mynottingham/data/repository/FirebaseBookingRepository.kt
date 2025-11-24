package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Booking Repository
 *
 * 直接从 Firebase Realtime Database 读取和创建预订数据
 * 不再依赖 Spring Boot 后端 API
 *
 * Firebase 数据结构：
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

    private val database = FirebaseDatabase.getInstance()
    private val bookingsRef: DatabaseReference = database.getReference("bookings")

    /**
     * 创建新的预订
     * @param booking 预订数据
     * @return Result<String> 预订ID或错误
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
     * 获取用户的所有预订
     * @param userId 用户ID
     * @return Flow<List<Map<String, Any>>> 预订列表流
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

                // 按创建时间倒序排列
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
     * 取消预订
     * @param bookingId 预订ID
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
     * 获取特定时间段的可用性
     * @param facilityName 场地名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Boolean 是否可用
     */
    suspend fun checkAvailability(facilityName: String, startTime: Long, endTime: Long): Boolean {
        return try {
            val snapshot = bookingsRef.orderByChild("facilityName").equalTo(facilityName).get().await()

            // 检查是否有冲突的预订
            for (child in snapshot.children) {
                val bookingStart = child.child("startTime").getValue(Long::class.java) ?: 0L
                val bookingEnd = child.child("endTime").getValue(Long::class.java) ?: 0L
                val status = child.child("status").getValue(String::class.java) ?: ""

                // 跳过已取消的预订
                if (status == "CANCELLED") continue

                // 检查时间冲突
                if (startTime < bookingEnd && endTime > bookingStart) {
                    return false
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseBookingRepo", "Error checking availability: ${e.message}")
            false
        }
    }
}
