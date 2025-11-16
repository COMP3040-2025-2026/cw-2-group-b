package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for Message (conversation list) screen
 */
class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository(application)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchQuery = MutableLiveData<String>("")
    private var currentUserId: String = ""

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    // Pinned conversations
    val pinnedConversations: LiveData<List<Conversation>> = conversations.map { list ->
        list.filter { it.isPinned }
    }

    // Regular conversations (not pinned)
    val regularConversations: LiveData<List<Conversation>> = conversations.map { list ->
        list.filter { !it.isPinned }
    }

    /**
     * Set current user ID
     */
    fun setCurrentUserId(userId: String) {
        this.currentUserId = userId
        // Start observing conversations
        observeConversations()
    }

    /**
     * Observe conversations from repository
     */
    private fun observeConversations() {
        viewModelScope.launch {
            repository.getConversationsFlow(currentUserId).collect { conversationList ->
                // Sort by pinned status and last message time
                val sorted = conversationList.sortedWith(
                    compareByDescending<Conversation> { it.isPinned }
                        .thenByDescending { it.lastMessageTime }
                )
                _conversations.postValue(sorted)
            }
        }
    }

    /**
     * Sync conversations from API
     */
    fun syncConversations(token: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.syncConversations(token)
            _loading.value = false

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to sync conversations"
            }
        }
    }

    /**
     * Search conversations
     */
    fun searchConversations(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                // Revert to full list
                observeConversations()
            } else {
                // Search in repository
                repository.searchConversations(query).collect { conversationList ->
                    val sorted = conversationList.sortedWith(
                        compareByDescending<Conversation> { it.isPinned }
                            .thenByDescending { it.lastMessageTime }
                    )
                    _conversations.postValue(sorted)
                }
            }
        }
    }

    /**
     * Toggle pinned status
     */
    fun togglePinned(token: String, conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            val result = repository.updatePinnedStatus(token, conversationId, !isPinned)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to update pinned status"
            }
        }
    }

    /**
     * Mark conversation as read
     */
    fun markAsRead(token: String, conversationId: String, currentUserId: String) {
        viewModelScope.launch {
            val result = repository.markMessagesAsRead(token, conversationId, currentUserId)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to mark as read"
            }
        }
    }

    /**
     * Mark conversation as unread
     */
    fun markAsUnread(conversationId: String) {
        viewModelScope.launch {
            repository.markConversationAsUnread(conversationId)
        }
    }

    /**
     * Delete conversation
     */
    fun deleteConversation(token: String, conversationId: String) {
        viewModelScope.launch {
            val result = repository.deleteConversation(token, conversationId)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to delete conversation"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
