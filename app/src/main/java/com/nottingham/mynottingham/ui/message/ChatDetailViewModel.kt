package com.nottingham.mynottingham.ui.message

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for Chat Detail screen
 * 🔥 Migrated to Firebase - no longer depends on backend API or WebSocket
 */
class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    // 🔥 使用 Firebase Repository 替代传统的 HTTP Repository
    private val firebaseRepo = FirebaseMessageRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sendingMessage = MutableLiveData<Boolean>()
    val sendingMessage: LiveData<Boolean> = _sendingMessage

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private var conversationId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String? = null

    /**
     * Messages for current conversation
     * 🔥 Firebase 实时监听 - 自动更新
     */
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    /**
     * Initialize chat for a specific conversation
     * 🔥 Firebase 实时监听 - 自动接收新消息
     */
    fun initializeChat(
        conversationId: String,
        userId: String,
        userName: String = "",
        userAvatar: String? = null
    ) {
        this.conversationId = conversationId
        this.currentUserId = userId
        this.currentUserName = userName
        this.currentUserAvatar = userAvatar

        // 🔥 实时监听消息 - Firebase 自动推送更新
        viewModelScope.launch {
            _loading.postValue(true)
            firebaseRepo.getMessagesFlow(conversationId).collect { messageList ->
                _messages.postValue(messageList)
                _loading.postValue(false)
            }
        }

        // 自动标记为已读
        markAsRead()
    }

    /**
     * 🔥 已移除 WebSocket 相关代码
     * Firebase ValueEventListener 提供了相同的实时功能
     */

    /**
     * Send a message
     * 🔥 不再需要 token 参数
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || content.length > Constants.MAX_MESSAGE_LENGTH) {
            _error.value = "Message is invalid"
            return
        }

        _sendingMessage.value = true
        viewModelScope.launch {
            val result = firebaseRepo.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderAvatar = currentUserAvatar,
                message = content
            )
            _sendingMessage.value = false

            result.onSuccess {
                _messageSent.value = true
                // Mark as read since user is in the chat
                markAsRead()
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to send message"
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    /**
     * Mark messages as read
     * 🔥 不再需要 token 参数
     */
    fun markAsRead() {
        viewModelScope.launch {
            firebaseRepo.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    /**
     * Reset message sent status
     */
    fun resetMessageSent() {
        _messageSent.value = false
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Cleanup when ViewModel is destroyed
     * 🔥 不再需要清理 WebSocket 连接
     */
    override fun onCleared() {
        super.onCleared()
        // Firebase listeners are automatically cleaned up when Flow collection is cancelled
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }
}
