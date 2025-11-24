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
     */
    fun loadTasks() {
        viewModelScope.launch {
            try {
                Log.d("ErrandViewModel", "📥 Loading tasks from Firebase...")

                // 使用 Firebase Flow 实时监听
                firebaseErrandRepo.getAvailableErrands().collect { firebaseErrands ->
                    // 转换 Firebase 数据为 ErrandTask
                    val taskList = firebaseErrands.mapNotNull { mapToErrandTask(it) }

                    Log.d("ErrandViewModel", "✅ Loaded ${taskList.size} tasks from Firebase")
                    _tasks.postValue(taskList)
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

                Log.d("ErrandViewModel", "📤 Creating new task: ${task.title}")

                val errandData = mapOf(
                    "title" to task.title,
                    "description" to task.description,
                    "requesterId" to userId,
                    "requesterName" to userName,
                    "type" to "SHOPPING", // TODO: 从 UI 获取类型
                    "reward" to (task.price.toDoubleOrNull() ?: 0.0),
                    "pickupLocation" to task.location,
                    "deliveryLocation" to task.location, // TODO: 添加独立的 deliveryLocation 字段
                )

                val result = firebaseErrandRepo.createErrand(errandData)

                if (result.isSuccess) {
                    val errandId = result.getOrNull()
                    Log.d("ErrandViewModel", "✅ Task created successfully: $errandId")
                    // Firebase Flow 会自动更新任务列表，不需要手动 reload
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
            val reward = firebaseData["reward"] as? Double ?: 0.0
            val pickupLocation = firebaseData["pickupLocation"] as? String ?: ""
            val requesterId = firebaseData["requesterId"] as? String ?: ""
            val requesterName = firebaseData["requesterName"] as? String ?: "Unknown"
            val timestamp = firebaseData["timestamp"] as? Long ?: System.currentTimeMillis()

            ErrandTask(
                taskId = id,
                title = title,
                description = description,
                price = reward.toString(),
                location = pickupLocation,
                requesterId = requesterId,
                requesterName = requesterName,
                requesterAvatar = "", // TODO: 支持头像
                deadline = "", // TODO: 添加 deadline 字段
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.w("ErrandViewModel", "Failed to parse errand data: ${e.message}")
            null
        }
    }
}