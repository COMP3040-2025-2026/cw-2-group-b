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
 * 完全使用 Firebase Realtime Database 管理跑腿任务
 * 不再依赖 Spring Boot 后端 API
 *
 * 功能：
 * - 实时加载可用任务 (PENDING 状态)
 * - 创建新任务
 * - 接受任务 (TODO: 需要添加UI)
 * - 完成任务 (TODO: 需要添加UI)
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
     * 实时加载可用任务 (PENDING 状态)
     *
     * 根据配送模式过滤：
     * - 配送模式 OFF (普通用户): 只看自己发布的订单
     * - 配送模式 ON (骑手): 看别人发布的订单 (可接单)
     */
    fun loadTasks() {
        viewModelScope.launch {
            try {
                val currentUserId = tokenManager.getUserId().first() ?: ""
                val isDeliveryMode = tokenManager.getDeliveryMode().first()

                Log.d("ErrandViewModel", "📥 Loading tasks from Firebase... (deliveryMode=$isDeliveryMode, userId=$currentUserId)")

                // 使用 Firebase Flow 实时监听
                firebaseErrandRepo.getAvailableErrands().collect { firebaseErrands ->
                    // 转换 Firebase 数据为 ErrandTask
                    val allTasks = firebaseErrands.mapNotNull { mapToErrandTask(it) }

                    // 根据配送模式过滤任务
                    val filteredTasks = if (isDeliveryMode) {
                        // 骑手模式：显示别人发布的订单（排除自己的）
                        allTasks.filter { it.requesterId != currentUserId }
                    } else {
                        // 普通用户模式：只显示自己发布的订单
                        allTasks.filter { it.requesterId == currentUserId }
                    }

                    Log.d("ErrandViewModel", "✅ Loaded ${filteredTasks.size} tasks (filtered from ${allTasks.size})")
                    _tasks.postValue(filteredTasks)
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "❌ Error loading tasks", e)
                // 发生错误时显示空列表
                _tasks.postValue(emptyList())
            }
        }
    }

    /**
     * 创建新任务
     */
    fun addTask(task: ErrandTask) {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId().first() ?: ""
                val userName = tokenManager.getFullName().first() ?: "Unknown User"
                val userAvatar = tokenManager.getAvatar().first() ?: ""

                Log.d("ErrandViewModel", "📤 Creating new task: ${task.title}")

                val errandData = mapOf<String, Any>(
                    "title" to task.title,
                    "description" to task.description,
                    "requesterId" to userId,
                    "requesterName" to userName,
                    "requesterAvatar" to userAvatar,
                    "type" to "SHOPPING",
                    "reward" to (task.price.toDoubleOrNull() ?: 0.0),
                    "pickupLocation" to task.location,
                    "deliveryLocation" to task.location,
                    "deadline" to (task.deadline ?: "")
                )

                val result = firebaseErrandRepo.createErrand(errandData)

                if (result.isSuccess) {
                    val errandId = result.getOrNull()
                    Log.d("ErrandViewModel", "✅ Task created successfully: $errandId")
                } else {
                    Log.e("ErrandViewModel", "❌ Failed to create task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "❌ Error creating task", e)
            }
        }
    }

    /**
     * 接受任务（供其他用户使用）
     */
    fun acceptTask(taskId: String) {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId().first() ?: ""
                val userName = tokenManager.getFullName().first() ?: "Unknown User"

                Log.d("ErrandViewModel", "📥 Accepting task: $taskId")

                val result = firebaseErrandRepo.acceptErrand(taskId, userId, userName)

                if (result.isSuccess) {
                    Log.d("ErrandViewModel", "✅ Task accepted successfully")
                } else {
                    Log.e("ErrandViewModel", "❌ Failed to accept task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "❌ Error accepting task", e)
            }
        }
    }

    /**
     * 完成任务
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                Log.d("ErrandViewModel", "✅ Completing task: $taskId")

                val result = firebaseErrandRepo.completeErrand(taskId)

                if (result.isSuccess) {
                    Log.d("ErrandViewModel", "✅ Task completed successfully")
                } else {
                    Log.e("ErrandViewModel", "❌ Failed to complete task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "❌ Error completing task", e)
            }
        }
    }

    /**
     * 将 Firebase Map 数据转换为 ErrandTask
     */
    private fun mapToErrandTask(firebaseData: Map<String, Any>): ErrandTask? {
        return try {
            val id = firebaseData["id"] as? String ?: ""
            val title = firebaseData["title"] as? String ?: "Untitled"
            val description = firebaseData["description"] as? String ?: ""
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
                price = String.format("%.2f", reward),
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