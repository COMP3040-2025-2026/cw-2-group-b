package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Errand Repository
 *
 * Reads and manages errand task data directly from Firebase Realtime Database.
 * No longer depends on Spring Boot backend API.
 *
 * Firebase data structure:
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

    // Important: Must specify database URL as database is in asia-southeast1 region
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val errandsRef: DatabaseReference = database.getReference("errands")
    private val usersRef: DatabaseReference = database.getReference("users")

    // ==================== Balance Methods ====================

    /**
     * Get rider balance
     * @param userId User ID
     * @return Result<Double> Balance amount
     */
    suspend fun getRiderBalance(userId: String): Result<Double> {
        return try {
            val snapshot = usersRef.child(userId).child("errandBalance").get().await()
            val balance = snapshot.getValue(Double::class.java) ?: 0.0
            Result.success(balance)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error getting rider balance: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Observe rider balance in real-time
     * @param userId User ID
     * @return Flow<Double> Balance flow
     */
    fun getRiderBalanceFlow(userId: String): Flow<Double> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.getValue(Double::class.java) ?: 0.0
                trySend(balance)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseErrandRepo", "Error listening to balance: ${error.message}")
                trySend(0.0)
            }
        }

        usersRef.child(userId).child("errandBalance").addValueEventListener(listener)

        awaitClose {
            usersRef.child(userId).child("errandBalance").removeEventListener(listener)
        }
    }

    /**
     * Add to rider balance (called when order is completed)
     * @param userId User ID
     * @param amount Amount to add
     * @return Result<Double> New balance
     */
    suspend fun addToRiderBalance(userId: String, amount: Double): Result<Double> {
        return try {
            val balanceRef = usersRef.child(userId).child("errandBalance")
            val snapshot = balanceRef.get().await()
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = currentBalance + amount

            balanceRef.setValue(newBalance).await()

            // Also update total earned
            val totalEarnedRef = usersRef.child(userId).child("errandTotalEarned")
            val totalEarnedSnapshot = totalEarnedRef.get().await()
            val currentTotalEarned = totalEarnedSnapshot.getValue(Double::class.java) ?: 0.0
            totalEarnedRef.setValue(currentTotalEarned + amount).await()

            android.util.Log.d("FirebaseErrandRepo", "Added $amount to balance. New balance: $newBalance")
            Result.success(newBalance)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error adding to balance: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Withdraw balance (clear balance)
     * @param userId User ID
     * @return Result<Double> Withdrawn amount
     */
    suspend fun withdrawBalance(userId: String): Result<Double> {
        return try {
            val balanceRef = usersRef.child(userId).child("errandBalance")
            val snapshot = balanceRef.get().await()
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0

            if (currentBalance <= 0) {
                return Result.failure(Exception("No balance to withdraw"))
            }

            // Clear balance
            balanceRef.setValue(0.0).await()

            // Update total withdrawn
            val totalWithdrawnRef = usersRef.child(userId).child("errandTotalWithdrawn")
            val totalWithdrawnSnapshot = totalWithdrawnRef.get().await()
            val currentTotalWithdrawn = totalWithdrawnSnapshot.getValue(Double::class.java) ?: 0.0
            totalWithdrawnRef.setValue(currentTotalWithdrawn + currentBalance).await()

            android.util.Log.d("FirebaseErrandRepo", "Withdrawn $currentBalance")
            Result.success(currentBalance)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error withdrawing balance: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== Errand Methods ====================

    /**
     * Create new errand task
     * @param errand Task data
     * @return Result<String> Task ID or error
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
     * Get all available errands (PENDING status)
     * @return Flow<List<Map<String, Any>>> Task list flow
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

                // Sort by timestamp descending
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
     * Get errands posted by user
     * @param userId User ID
     * @return Flow<List<Map<String, Any>>> Task list flow
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
     * Get errands accepted by user (as provider)
     * @param userId User ID
     * @return Flow<List<Map<String, Any>>> Task list flow
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
     * Accept errand task
     * @param errandId Task ID
     * @param providerId Provider ID
     * @param providerName Provider name
     * @param providerAvatar Provider avatar (optional)
     * @return Result<Unit>
     */
    suspend fun acceptErrand(
        errandId: String,
        providerId: String,
        providerName: String,
        providerAvatar: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "providerId" to providerId,
                "providerName" to providerName,
                "status" to "ACCEPTED",
                "acceptedAt" to System.currentTimeMillis()
            )
            providerAvatar?.let { updates["providerAvatar"] = it }

            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error accepting errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Start delivering (status changes from ACCEPTED to DELIVERING)
     * @param errandId Task ID
     * @return Result<Unit>
     */
    suspend fun startDelivering(errandId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "DELIVERING",
                "deliveringStartedAt" to System.currentTimeMillis()
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error starting delivery: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get rider's current active order count (ACCEPTED or DELIVERING status)
     * @param providerId Rider ID
     * @return Result<Int> Active order count
     */
    suspend fun getActiveErrandCount(providerId: String): Result<Int> {
        return try {
            val snapshot = errandsRef.orderByChild("providerId").equalTo(providerId).get().await()
            var count = 0

            for (child in snapshot.children) {
                val errand = child.value as? Map<String, Any> ?: continue
                val status = errand["status"] as? String ?: ""
                if (status == "ACCEPTED" || status == "DELIVERING") {
                    count++
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error getting active errand count: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Complete errand task and add reward to rider balance
     * @param errandId Task ID
     * @return Result<Unit>
     */
    suspend fun completeErrand(errandId: String): Result<Unit> {
        return try {
            // First get the errand to get providerId and reward
            val errandSnapshot = errandsRef.child(errandId).get().await()
            val errand = errandSnapshot.value as? Map<String, Any>
                ?: return Result.failure(Exception("Errand not found"))

            val providerId = errand["providerId"] as? String
                ?: return Result.failure(Exception("No provider assigned"))
            val reward = (errand["reward"] as? Number)?.toDouble() ?: 0.0

            // Update errand status
            val updates = mapOf(
                "status" to "COMPLETED",
                "completedAt" to System.currentTimeMillis()
            )
            errandsRef.child(errandId).updateChildren(updates).await()

            // Add reward to rider's balance
            if (reward > 0) {
                addToRiderBalance(providerId, reward)
                android.util.Log.d("FirebaseErrandRepo", "Added reward $reward to rider $providerId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error completing errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancel errand task
     * @param errandId Task ID
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
     * Get single errand task details
     * @param errandId Task ID
     * @return Result<Map<String, Any>?> Task data or null
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
     * Drop errand (provider drops task, returns to available pool)
     * Rider can drop at any status (ACCEPTED or DELIVERING)
     * @param errandId Task ID
     * @return Result<Unit>
     */
    suspend fun dropErrand(errandId: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>(
                "providerId" to null,
                "providerName" to null,
                "providerAvatar" to null,
                "status" to "PENDING",
                "acceptedAt" to null,
                "deliveringStartedAt" to null
            )
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error dropping errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete errand (permanent deletion by poster)
     * @param errandId Task ID
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

    /**
     * Update errand task
     * @param errandId Task ID
     * @param updates Map of fields to update
     * @return Result<Unit>
     */
    suspend fun updateErrand(errandId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            errandsRef.child(errandId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error updating errand: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get user's history errands (COMPLETED or CANCELLED status)
     * @param userId User ID
     * @return Flow<List<Map<String, Any>>> History errands list flow
     */
    fun getUserHistoryErrands(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val errands = mutableListOf<Map<String, Any>>()

                for (child in snapshot.children) {
                    val errand = child.value as? Map<String, Any> ?: continue
                    val status = errand["status"] as? String ?: ""
                    val requesterId = errand["requesterId"] as? String ?: ""
                    val providerId = errand["providerId"] as? String

                    // Include if user is requester or provider AND status is COMPLETED or CANCELLED
                    val isUserInvolved = requesterId == userId || providerId == userId
                    val isHistoryStatus = status == "COMPLETED" || status == "CANCELLED"

                    if (isUserInvolved && isHistoryStatus) {
                        val errandWithId = errand.toMutableMap()
                        errandWithId["id"] = child.key ?: ""
                        errands.add(errandWithId)
                    }
                }

                // Sort by completedAt or cancelledAt, newest first
                errands.sortByDescending {
                    (it["completedAt"] as? Long) ?: (it["cancelledAt"] as? Long) ?: (it["timestamp"] as? Long) ?: 0L
                }
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
     * Cleanup expired history errands (older than specified days)
     * @param daysOld Number of days (default 7)
     * @return Result<Int> Number of deleted errands
     */
    suspend fun cleanupOldErrands(daysOld: Int = 7): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            val snapshot = errandsRef.get().await()
            var deletedCount = 0

            for (child in snapshot.children) {
                val errand = child.value as? Map<String, Any> ?: continue
                val status = errand["status"] as? String ?: ""

                // Only clean up COMPLETED or CANCELLED tasks
                if (status != "COMPLETED" && status != "CANCELLED") continue

                val completedAt = (errand["completedAt"] as? Long)
                    ?: (errand["cancelledAt"] as? Long)
                    ?: continue

                if (completedAt < cutoffTime) {
                    errandsRef.child(child.key ?: "").removeValue().await()
                    deletedCount++
                }
            }

            android.util.Log.d("FirebaseErrandRepo", "Cleaned up $deletedCount old errands")
            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseErrandRepo", "Error cleaning up old errands: ${e.message}")
            Result.failure(e)
        }
    }
}
