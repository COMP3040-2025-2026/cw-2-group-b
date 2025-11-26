package com.nottingham.mynottingham.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseMessageRepository()
    
    // 用于通知 UI 更新未读数量的 LiveData
    private val _unreadMessageCount = MutableLiveData<Int>()
    val unreadMessageCount: LiveData<Int> = _unreadMessageCount

    /**
     * 开始监听指定用户的未读消息数量
     */
    fun startListeningToUnreadCount(userId: String) {
        viewModelScope.launch {
            repository.getTotalUnreadCountFlow(userId).collect { count ->
                _unreadMessageCount.postValue(count)
            }
        }
    }
}
