package com.nottingham.mynottingham.ui.errand

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ErrandViewModel - Firebase Migration Edition
 *
 * Fully uses Firebase Realtime Database to manage errand tasks
 * No longer relies on Spring Boot backend API
 *
 * Features:
 * - Real-time loading of available tasks (PENDING status)
 * - Create new task
 * - Accept task (TODO: Need to add UI)
 * - Complete task (TODO: Need to add UI)
 */
class ErrandViewModel(application: Application) : AndroidViewModel(application) {

    private val _tasks = MutableLiveData<List<ErrandTask>>()
    val tasks: LiveData<List<ErrandTask>> = _tasks

    private val tokenManager = TokenManager(application)
    private val firebaseErrandRepo = FirebaseErrandRepository()

    init {
        loadTasks()
    }

    /**
     * Real-time loading of available tasks (PENDING status)
     *
     * Filter based on delivery mode:
     * - Delivery mode OFF (regular user): Only see own published orders
     * - Delivery mode ON (delivery rider): See others' published orders (can accept)
     */
    fun loadTasks() {
        viewModelScope.launch {
            try {
                val currentUserId = tokenManager.getUserId().first() ?: ""
                val isDeliveryMode = tokenManager.getDeliveryMode().first()

                Log.d("ErrandViewModel", "Loading tasks from Firebase... (deliveryMode=$isDeliveryMode, userId=$currentUserId)")

                // Use Firebase Flow for real-time listening
                firebaseErrandRepo.getAvailableErrands().collect { firebaseErrands ->
                    // Convert Firebase data to ErrandTask
                    val allTasks = firebaseErrands.mapNotNull { mapToErrandTask(it) }

                    // Filter tasks based on delivery mode
                    val filteredTasks = if (isDeliveryMode) {
                        // Delivery mode: Show others' published orders (exclude own)
                        allTasks.filter { it.requesterId != currentUserId }
                    } else {
                        // Regular user mode: Only show own published orders
                        allTasks.filter { it.requesterId == currentUserId }
                    }

                    Log.d("ErrandViewModel", "Loaded ${filteredTasks.size} tasks (filtered from ${allTasks.size})")
                    _tasks.postValue(filteredTasks)
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error loading tasks", e)
                // Show empty list on error
                _tasks.postValue(emptyList())
            }
        }
    }

    /**
     * Create new task
     */
    fun addTask(task: ErrandTask) {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId().first() ?: ""
                val userName = tokenManager.getFullName().first() ?: "Unknown User"
                val userAvatar = tokenManager.getAvatar().first() ?: ""

                Log.d("ErrandViewModel", "Creating new task: ${task.title}")

                val errandData = mapOf<String, Any>(
                    "title" to task.title,
                    "description" to task.description,
                    "requesterId" to userId,
                    "requesterName" to userName,
                    "requesterAvatar" to userAvatar,
                    "type" to "SHOPPING",
                    "reward" to (task.reward.toDoubleOrNull() ?: 0.0),
                    "pickupLocation" to task.location,
                    "deliveryLocation" to task.location,
                    "deadline" to (task.deadline ?: "")
                )

                val result = firebaseErrandRepo.createErrand(errandData)

                if (result.isSuccess) {
                    val errandId = result.getOrNull()
                    Log.d("ErrandViewModel", "Task created successfully: $errandId")
                } else {
                    Log.e("ErrandViewModel", "Failed to create task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error creating task", e)
            }
        }
    }

    /**
     * Accept task (for other users)
     */
    fun acceptTask(taskId: String) {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId().first() ?: ""
                val userName = tokenManager.getFullName().first() ?: "Unknown User"

                Log.d("ErrandViewModel", "Accepting task: $taskId")

                val result = firebaseErrandRepo.acceptErrand(taskId, userId, userName)

                if (result.isSuccess) {
                    Log.d("ErrandViewModel", "Task accepted successfully")
                } else {
                    Log.e("ErrandViewModel", "Failed to accept task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error accepting task", e)
            }
        }
    }

    /**
     * Complete task
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d("ErrandViewModel", "Completing task: $taskId")

                val result = firebaseErrandRepo.completeErrand(taskId)

                if (result.isSuccess) {
                    Log.d("ErrandViewModel", "Task completed successfully")
                } else {
                    Log.e("ErrandViewModel", "Failed to complete task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error completing task", e)
            }
        }
    }

    /**
     * Convert Firebase Map data to ErrandTask
     */
    private fun mapToErrandTask(firebaseData: Map<String, Any>): ErrandTask? {
        return try {
            val id = firebaseData["id"] as? String ?: ""
            val title = firebaseData["title"] as? String ?: "Untitled"
            val description = firebaseData["description"] as? String ?: ""

            // Handle orderAmount (food/item cost that rider purchases)
            val orderAmount = when (val o = firebaseData["orderAmount"]) {
                is Double -> o
                is Long -> o.toDouble()
                is Number -> o.toDouble()
                else -> null
            }

            // Handle reward as both Double and Long (Firebase may return Long for whole numbers)
            val reward = when (val r = firebaseData["reward"]) {
                is Double -> r
                is Long -> r.toDouble()
                is Number -> r.toDouble()
                else -> 0.0
            }

            // Support both "location" and legacy "pickupLocation"/"deliveryLocation" keys
            val location = firebaseData["location"] as? String
                ?: firebaseData["deliveryLocation"] as? String
                ?: firebaseData["pickupLocation"] as? String
                ?: ""
            val requesterId = firebaseData["requesterId"] as? String ?: ""
            val requesterName = firebaseData["requesterName"] as? String ?: "Unknown"
            val requesterAvatar = firebaseData["requesterAvatar"] as? String ?: ""
            // Support both "timeLimit" and legacy "deadline" keys
            val deadline = firebaseData["timeLimit"] as? String
                ?: firebaseData["deadline"] as? String
                ?: ""
            val timestamp = firebaseData["timestamp"] as? Long ?: System.currentTimeMillis()
            // Get task type (SHOPPING, PICKUP, FOOD_DELIVERY, OTHERS)
            val taskType = firebaseData["type"] as? String ?: "SHOPPING"

            ErrandTask(
                taskId = id,
                title = title,
                description = description,
                orderAmount = orderAmount?.let { String.format("%.2f", it) },
                reward = String.format("%.2f", reward),
                location = location,
                requesterId = requesterId,
                requesterName = requesterName,
                requesterAvatar = requesterAvatar,
                deadline = deadline,
                timestamp = timestamp,
                taskType = taskType
            )
        } catch (e: Exception) {
            Log.w("ErrandViewModel", "Failed to parse errand data: ${e.message}")
            null
        }
    }
}