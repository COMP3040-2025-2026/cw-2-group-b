package com.nottingham.mynottingham.ui.errand

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Errand
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

class ErrandViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<ErrandTask>>()
    val tasks: LiveData<List<ErrandTask>> = _tasks

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.getAvailableErrands()
                if (response.isSuccessful) {
                    response.body()?.let { errands ->
                        _tasks.value = errands.map { it.toErrandTask() }
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
        val currentTasks = _tasks.value?.toMutableList() ?: mutableListOf()
        currentTasks.add(task)
        _tasks.value = currentTasks
    }

    private fun com.nottingham.mynottingham.data.remote.dto.ErrandResponse.toErrandTask(): ErrandTask {
        return ErrandTask(
            taskId = this.id,
            title = this.title,
            description = this.description,
            price = this.fee.toString(),
            location = this.deliveryLocation,
            requesterId = this.requesterId,
            requesterName = this.requesterName,
            requesterAvatar = "", // ErrandResponse does not have avatar
            deadline = "", // ErrandResponse does not have deadline
            timestamp = this.createdAt
        )
    }
}
