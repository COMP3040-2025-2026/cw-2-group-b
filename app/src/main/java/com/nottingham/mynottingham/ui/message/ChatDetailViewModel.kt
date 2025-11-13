package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.repository.MessageRepository
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for Chat Detail screen
 */
class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository(application)

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
    private var typingJob: Job? = null

    /**
     * Messages for current conversation
     */
    val messages: MutableLiveData<LiveData<List<ChatMessage>>> = MutableLiveData()

    /**
     * Initialize chat for a specific conversation
     */
    fun initializeChat(conversationId: String, userId: String) {
        this.conversationId = conversationId
        this.currentUserId = userId

        // Observe messages from repository
        messages.value = repository.getMessagesFlow(conversationId).asLiveData()
    }

    /**
     * Load messages from API
     */
    fun loadMessages(token: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.syncMessages(token, conversationId)
            _loading.value = false

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to load messages"
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(token: String, content: String) {
        if (content.isBlank() || content.length > Constants.MAX_MESSAGE_LENGTH) {
            _error.value = "Message is invalid"
            return
        }

        _sendingMessage.value = true
        viewModelScope.launch {
            val result = repository.sendMessage(token, conversationId, content)
            _sendingMessage.value = false

            result.onSuccess {
                _messageSent.value = true
                // Mark as read since user is in the chat
                markAsRead(token)
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to send message"
            }
        }
    }

    /**
     * Mark messages as read
     */
    fun markAsRead(token: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(token, conversationId, currentUserId)
        }
    }

    /**
     * Update typing status
     */
    fun updateTyping(token: String, isTyping: Boolean) {
        // Cancel previous typing job
        typingJob?.cancel()

        viewModelScope.launch {
            repository.updateTypingStatus(token, conversationId, isTyping)
        }

        // Auto-stop typing after timeout
        if (isTyping) {
            typingJob = viewModelScope.launch {
                delay(Constants.TYPING_INDICATOR_TIMEOUT_MS)
                repository.updateTypingStatus(token, conversationId, false)
            }
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
}
