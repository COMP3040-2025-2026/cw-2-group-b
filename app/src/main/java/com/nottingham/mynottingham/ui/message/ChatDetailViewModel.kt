package com.nottingham.mynottingham.ui.message

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.launch

/**
 * ViewModel for Chat Detail screen
 * 🔥 Migrated to Firebase - no longer depends on backend API or WebSocket
 */
class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseMessageRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sendingMessage = MutableLiveData<Boolean>()
    val sendingMessage: LiveData<Boolean> = _sendingMessage

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private val _typingStatus = MutableLiveData<String?>()
    val typingStatus: LiveData<String?> = _typingStatus

    private var conversationId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String? = null

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

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

        viewModelScope.launch {
            _loading.postValue(true)
            firebaseRepo.getMessagesFlow(conversationId).collect { messageList ->
                _messages.postValue(messageList)
                _loading.postValue(false)
            }
        }
        markAsRead()
    }

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
                markAsRead()
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to send message"
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    fun updateTyping(isTyping: Boolean) {
        viewModelScope.launch {
            firebaseRepo.updateTypingStatus(conversationId, currentUserId, isTyping)
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            firebaseRepo.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun resetMessageSent() {
        _messageSent.value = false
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }
}
