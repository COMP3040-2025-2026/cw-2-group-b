package com.nottingham.mynottingham.ui.errand

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.CreateErrandRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ErrandViewModel(application: Application) : AndroidViewModel(application) {

    private val _tasks = MutableLiveData<List<ErrandTask>>()
    val tasks: LiveData<List<ErrandTask>> = _tasks
    private val tokenManager = TokenManager(application)

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            try {
                Log.d("ErrandViewModel", "Loading tasks...")
                val response = RetrofitInstance.apiService.getAvailableErrands()
                if (response.isSuccessful) {
                    response.body()?.let { errands ->
                        // 1. 过滤：只显示 PENDING (待接单) 的任务
                        val filteredErrands = errands.filter { it.status == "PENDING" }

                        // 2. 转换
                        val taskList = filteredErrands.map { it.toErrandTask() }

                        // 3. 排序：按时间倒序 (最新的在最上面)
                        val sortedList = taskList.sortedByDescending { it.timestamp }

                        Log.d("ErrandViewModel", "Loaded ${sortedList.size} tasks from backend")
                        _tasks.postValue(sortedList)
                    }
                } else {
                    Log.e("ErrandViewModel", "Failed to load tasks: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error loading tasks", e)
            }
        }
    }

    fun addTask(task: ErrandTask) {
        viewModelScope.launch {
            try {
                // [关键修复] 必须使用 getToken() 获取 JWT，不能用 getUserId()
                val jwtToken = tokenManager.getToken().first() ?: ""
                val token = "Bearer $jwtToken"

                val request = CreateErrandRequest(
                    title = task.title,
                    description = task.description,
                    type = "SHOPPING",
                    priority = "HIGH",
                    pickupLocation = task.location,
                    deliveryLocation = task.location,
                    fee = task.price.toDoubleOrNull() ?: 0.0,
                    imageUrl = null,
                    deadline = task.deadline
                )

                val response = RetrofitInstance.apiService.createErrand(token, request)
                if (response.isSuccessful) {
                    Log.d("ErrandViewModel", "Task created successfully on server")
                    // 成功后重新从服务器加载，确保数据一致且排序正确
                    loadTasks()
                } else {
                    Log.e("ErrandViewModel", "Failed to create task: ${response.code()} ${response.errorBody()?.string()}")
                    // 如果需要 Fallback 可以在这里处理，但建议依赖服务器刷新
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error creating task", e)
            }
        }
    }

    private fun com.nottingham.mynottingham.data.remote.dto.ErrandResponse.toErrandTask(): ErrandTask {
        return ErrandTask(
            taskId = this.id,
            title = this.title,
            description = this.description,
            price = this.fee.toString(),
            location = this.location,
            requesterId = this.requesterId,
            requesterName = this.requesterName,
            requesterAvatar = "",
            deadline = this.deadline ?: "",
            timestamp = this.createdAt,
        )
    }
}