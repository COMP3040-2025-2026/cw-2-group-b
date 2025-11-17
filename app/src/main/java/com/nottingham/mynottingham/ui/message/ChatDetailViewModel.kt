package com.nottingham.mynottingham.ui.message

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.repository.MessageRepository
import com.nottingham.mynottingham.data.websocket.WebSocketManager
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for Chat Detail screen
 */
class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository(application)
    private var webSocketManager: WebSocketManager? = null

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
    private var typingJob: Job? = null

    /**
     * Messages for current conversation
     */
    val messages: MutableLiveData<LiveData<List<ChatMessage>>> = MutableLiveData()

    /**
     * Initialize chat for a specific conversation
     */
    fun initializeChat(conversationId: String, userId: String, userName: String = "") {
        this.conversationId = conversationId
        this.currentUserId = userId
        this.currentUserName = userName

        // Observe messages from repository
        messages.value = repository.getMessagesFlow(conversationId).asLiveData()

        // Setup WebSocket
        setupWebSocket(userId)
    }

    /**
     * Setup WebSocket connection and listeners
     */
    private fun setupWebSocket(userId: String) {
        webSocketManager = WebSocketManager.getInstance(userId)
        webSocketManager?.connect()
        webSocketManager?.joinConversation(conversationId)

        // Listen to WebSocket messages
        viewModelScope.launch {
            webSocketManager?.messageFlow?.collect { wsMessage ->
                Log.d(TAG, "WebSocket message received: ${wsMessage.type}")
                handleWebSocketMessage(wsMessage)
            }
        }
    }

    /**
     * Handle incoming WebSocket messages
     */
    private fun handleWebSocketMessage(wsMessage: com.nottingham.mynottingham.data.websocket.WebSocketMessage) {
        when (wsMessage.type) {
            "NEW_MESSAGE" -> {
                // Reload messages when new message arrives
                val data = wsMessage.data
                if (data != null && data["conversationId"] == conversationId) {
                    viewModelScope.launch {
                        // Refresh messages from local database
                        // The message should already be saved from repository sendMessage
                        // or we can trigger a sync here if needed
                        Log.d(TAG, "New message received in conversation")
                    }
                }
            }
            "TYPING" -> {
                val data = wsMessage.data
                if (data != null && data["conversationId"] == conversationId) {
                    val senderId = data["senderId"] as? String
                    if (senderId != currentUserId) {
                        val senderName = data["senderName"] as? String ?: "Someone"
                        _typingStatus.value = "$senderName is typing..."
                    }
                }
            }
            "STOP_TYPING" -> {
                val data = wsMessage.data
                if (data != null && data["conversationId"] == conversationId) {
                    _typingStatus.value = null
                }
            }
            "MESSAGE_READ" -> {
                // Handle message read status
                Log.d(TAG, "Message read notification received")
            }
        }
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

        // Send via WebSocket for real-time updates
        if (isTyping) {
            webSocketManager?.sendTyping(conversationId, currentUserId, currentUserName)
        } else {
            webSocketManager?.sendStopTyping(conversationId, currentUserId)
        }

        // Also update via API
        viewModelScope.launch {
            repository.updateTypingStatus(token, conversationId, isTyping)
        }

        // Auto-stop typing after timeout
        if (isTyping) {
            typingJob = viewModelScope.launch {
                delay(Constants.TYPING_INDICATOR_TIMEOUT_MS)
                webSocketManager?.sendStopTyping(conversationId, currentUserId)
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

    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        // Leave conversation and disconnect WebSocket
        webSocketManager?.leaveConversation(conversationId)
        // Note: Don't destroy WebSocket instance as it might be used by other screens
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }
}
