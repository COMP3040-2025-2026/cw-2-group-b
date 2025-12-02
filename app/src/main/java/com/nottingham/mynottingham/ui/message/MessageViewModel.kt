package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for Message (conversation list) screen
 * 🔥 Migrated to Firebase - no longer depends on backend API
 */
class MessageViewModel(application: Application) : AndroidViewModel(application) {

    // Using Firebase Repository instead of traditional HTTP Repository
    private val firebaseRepo = FirebaseMessageRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

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
     * Firebase real-time listening - no manual sync required
     */
    private fun observeConversations() {
        viewModelScope.launch {
            firebaseRepo.getConversationsFlow(currentUserId).collect { conversationList ->
                // Firebase already handles sorting, but we maintain consistency
                val sorted = conversationList.sortedWith(
                    compareByDescending<Conversation> { it.isPinned }
                        .thenByDescending { it.lastMessageTime }
                )
                _conversations.postValue(sorted)
                _loading.postValue(false) // Data loading complete
            }
        }
    }

    /**
     * syncConversations() method has been removed
     * Firebase real-time listening auto-syncs, no manual call needed
     * Keep this comment as reminder: if Fragment calls syncConversations, remove it
     */

    /**
     * Search conversations (client-side filtering)
     * Firebase version - filter existing data locally
     */
    fun searchConversations(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                // Revert to full list
                observeConversations()
            } else {
                // Search within existing conversation list
                val currentList = _conversations.value ?: emptyList()
                val filtered = currentList.filter { conversation ->
                    conversation.participantName.contains(query, ignoreCase = true) ||
                    conversation.lastMessage.contains(query, ignoreCase = true)
                }
                _conversations.postValue(filtered)
            }
        }
    }

    /**
     * Toggle pinned status
     * No longer requires token parameter
     */
    fun togglePinned(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            val result = firebaseRepo.togglePinConversation(currentUserId, conversationId, !isPinned)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to update pinned status"
            }
        }
    }

    /**
     * Mark conversation as read
     * No longer requires token parameter
     */
    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            val result = firebaseRepo.markMessagesAsRead(conversationId, currentUserId)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to mark as read"
            }
        }
    }

    /**
     * Mark conversation as unread
     * Placeholder method - future implementation
     */
    fun markAsUnread(conversationId: String) {
        // TODO: Implement mark as unread functionality in repository
        // For now, this is a placeholder to satisfy the fragment call
    }

    /**
     * Delete conversation
     * No longer requires token parameter
     * Returns Result indicating success or failure
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        val result = firebaseRepo.deleteConversation(currentUserId, conversationId)
        result.onFailure { e ->
            _error.value = e.message ?: "Failed to delete conversation"
        }
        return result
    }

    /**
     * Search users for creating new conversation
     * New method - search users to create conversations
     */
    fun searchUsers(query: String): LiveData<List<Map<String, String>>> {
        val result = MutableLiveData<List<Map<String, String>>>()
        viewModelScope.launch {
            firebaseRepo.searchUsers(query).collect { users ->
                result.postValue(users)
            }
        }
        return result
    }

    /**
     * Create new conversation
     * New method - create new conversations
     */
    suspend fun createConversation(
        participantIds: List<String>,
        currentUserName: String,
        isGroup: Boolean = false,
        groupName: String? = null
    ): Result<String> {
        return firebaseRepo.createConversation(
            participantIds = participantIds,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            isGroup = isGroup,
            groupName = groupName
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
