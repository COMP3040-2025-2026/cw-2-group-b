package com.nottingham.mynottingham.ui.errand

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.Errand
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
                        val taskList = errands.map { it.toErrandTask() }
                        Log.d("ErrandViewModel", "Loaded ${taskList.size} tasks from backend")
                        _tasks.postValue(taskList)
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
                val userId = tokenManager.getUserId().first() ?: ""
                val token = "Bearer $userId"

                // Convert ErrandTask to CreateErrandRequest
                val request = CreateErrandRequest(
                    title = task.title,
                    description = task.description,
                    type = "SHOPPING", // Default type, could be enhanced
                    priority = "HIGH",
                    pickupLocation = task.location,
                    deliveryLocation = task.location,
                    fee = task.price.toDoubleOrNull() ?: 0.0,
                    imageUrl = null
                )

                val response = RetrofitInstance.apiService.createErrand(token, request)
                if (response.isSuccessful) {
                    Log.d("ErrandViewModel", "Task created successfully")
                    // Reload tasks to get updated list from server
                    loadTasks()
                } else {
                    Log.e("ErrandViewModel", "Failed to create task: ${response.errorBody()?.string()}")
                    // Fallback: add to local list only
                    val currentTasks = _tasks.value?.toMutableList() ?: mutableListOf()
                    currentTasks.add(task)
                    _tasks.value = currentTasks
                }
            } catch (e: Exception) {
                Log.e("ErrandViewModel", "Error creating task", e)
                // Fallback: add to local list only
                val currentTasks = _tasks.value?.toMutableList() ?: mutableListOf()
                currentTasks.add(task)
                _tasks.value = currentTasks
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
            requesterAvatar = "", // ErrandResponse does not have avatar
            deadline = this.deadline ?: "",
            timestamp = this.createdAt
        )
    }
}
