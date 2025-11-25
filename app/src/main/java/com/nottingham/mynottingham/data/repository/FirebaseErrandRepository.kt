package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Errand Repository
 *
 * 直接从 Firebase Realtime Database 读取和管理跑腿任务数据
 * 不再依赖 Spring Boot 后端 API
 *
 * Firebase 数据结构：
 * errands/{errandId}: {
 *   title: "Pickup Food from Cafeteria",
 *   description: "Need lunch from main cafeteria",
 *   requesterId: "student1",
 *   requesterName: "Alice Wong",
 *   providerId: "student2" (optional),
 *   providerName: "Bob Chen" (optional),
 *   type: "FOOD_DELIVERY" | "PICKUP" | "SHOPPING",
 *   status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED",
 *   reward: 5.00,
 *   pickupLocation: "Main Cafeteria",
 *   deliveryLocation: "Library 3rd Floor",
 *   timestamp: 1732443000000,
 *   completedAt: 1732443000000 (optional)
 * }
 */
class FirebaseErrandRepository {

    // ⚠️ 重要：必须指定数据库 URL，因为数据库在 asia-southeast1 区域
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val errandsRef: DatabaseReference = database.getReference("errands")

    /**
     * 创建新的跑腿任务
     * @param errand 任务数据
     * @return Result<String> 任务ID或错误
     */
    suspend fun createErrand(errand: Map<String, Any>): Result<String> {
        return try {
            val newErrandRef = errandsRef.push()
            val errandId = newErrandRef.key ?: throw Exception("Failed to generate errand ID")

            val errandData = errand.toMutableMap()
            errandData["timestamp"] = System.currentTimeMillis()
            errandData["status"] = "PENDING"

            newErrandRef.setValue(errandData).await()

            Result.success(errandId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error creating errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取所有可用的跑腿任务（状态为 PENDING）
     * @return Flow<List<Map<String, Any>>> 任务列表流
     */
    fun getAvailableErrands(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val errands = mutableListOf<Map<String, Any>>()

                for (child in snapshot.children) {
                    val errand = child.value as? Map<String, Any> ?: continue
                    val status = errand["status"] as? String ?: ""

                    if (status == "PENDING") {
                        val errandWithId = errand.toMutableMap()
                        errandWithId["id"] = child.key ?: ""
                        errands.add(errandWithId)
                    }
                }

                // 按时间倒序排列
                errands.sortByDescending { it["timestamp"] as? Long ?: 0L }
                trySend(errands)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        errandsRef.addValueEventListener(listener)

        awaitClose {
            errandsRef.removeEventListener(listener)
        }
    }

    /**
     * 获取用户发布的跑腿任务
     * @param userId 用户ID
     * @return Flow<List<Map<String, Any>>> 任务列表流
     */
    fun getUserRequestedErrands(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val errands = mutableListOf<Map<String, Any>>()

                for (child in snapshot.children) {
                    val errand = child.value as? Map<String, Any> ?: continue
                    val errandWithId = errand.toMutableMap()
                    errandWithId["id"] = child.key ?: ""
                    errands.add(errandWithId)
                }

                errands.sortByDescending { it["timestamp"] as? Long ?: 0L }
                trySend(errands)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        errandsRef.orderByChild("requesterId").equalTo(userId).addValueEventListener(listener)

        awaitClose {
            errandsRef.removeEventListener(listener)
        }
    }

    /**
     * 获取用户接受的跑腿任务
     * @param userId 用户ID
     * @return Flow<List<Map<String, Any>>> 任务列表流
     */
    fun getUserProvidedErrands(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val errands = mutableListOf<Map<String, Any>>()

                for (child in snapshot.children) {
                    val errand = child.value as? Map<String, Any> ?: continue
                    val errandWithId = errand.toMutableMap()
                    errandWithId["id"] = child.key ?: ""
                    errands.add(errandWithId)
                }

                errands.sortByDescending { it["timestamp"] as? Long ?: 0L }
                trySend(errands)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        errandsRef.orderByChild("providerId").equalTo(userId).addValueEventListener(listener)

        awaitClose {
            errandsRef.removeEventListener(listener)
        }
    }

    /**
     * 接受跑腿任务
     * @param errandId 任务ID
     * @param providerId 接受者ID
     * @param providerName 接受者姓名
     * @return Result<Unit>
     */
    suspend fun acceptErrand(errandId: String, providerId: String, providerName: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "providerId" to providerId,
                "providerName" to providerName,
                "status" to "IN_PROGRESS",
                "acceptedAt" to System.currentTimeMillis()
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error accepting errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 完成跑腿任务
     * @param errandId 任务ID
     * @return Result<Unit>
     */
    suspend fun completeErrand(errandId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "COMPLETED",
                "completedAt" to System.currentTimeMillis()
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error completing errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 取消跑腿任务
     * @param errandId 任务ID
     * @return Result<Unit>
     */
    suspend fun cancelErrand(errandId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledAt" to System.currentTimeMillis()
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error cancelling errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取单个跑腿任务详情
     * @param errandId 任务ID
     * @return Result<Map<String, Any>?> 任务数据或null
     */
    suspend fun getErrandById(errandId: String): Result<Map<String, Any>?> {
        return try {
            val snapshot = errandsRef.child(errandId).get().await()
            if (snapshot.exists()) {
                val errand = snapshot.value as? Map<String, Any>
                if (errand != null) {
                    val errandWithId = errand.toMutableMap()
                    errandWithId["id"] = errandId
                    Result.success(errandWithId)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error getting errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 放弃任务（接单者放弃，任务回到可用池）
     * @param errandId 任务ID
     * @return Result<Unit>
     */
    suspend fun dropErrand(errandId: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>(
                "providerId" to null,
                "providerName" to null,
                "status" to "PENDING",
                "acceptedAt" to null
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error dropping errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 删除跑腿任务（发布者永久删除）
     * @param errandId 任务ID
     * @return Result<Unit>
     */
    suspend fun deleteErrand(errandId: String): Result<Unit> {
        return try {
            errandsRef.child(errandId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error deleting errand: ${e.message}")
            Result.failure(e)
        }
    }
}
