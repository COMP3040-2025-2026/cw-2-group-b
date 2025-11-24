package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
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

    // 🔥 使用 Firebase Repository 替代传统的 HTTP Repository
    private val firebaseRepo = FirebaseMessageRepository()

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
     * 🔥 Firebase 实时监听 - 无需手动同步
     */
    private fun observeConversations() {
        viewModelScope.launch {
            firebaseRepo.getConversationsFlow(currentUserId).collect { conversationList ->
                // Firebase 已经处理了排序，但我们保持一致性
                val sorted = conversationList.sortedWith(
                    compareByDescending<Conversation> { it.isPinned }
                        .thenByDescending { it.lastMessageTime }
                )
                _conversations.postValue(sorted)
                _loading.postValue(false) // 数据加载完成
            }
        }
    }

    /**
     * 🔥 已移除 syncConversations() 方法
     * Firebase 实时监听自动同步，无需手动调用
     * 保留此注释以提醒：如果 Fragment 中有调用 syncConversations，需要移除
     */

    /**
     * Search conversations (client-side filtering)
     * 🔥 Firebase 版本 - 在本地过滤现有数据
     */
    fun searchConversations(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                // Revert to full list
                observeConversations()
            } else {
                // 在现有对话列表中搜索
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
     * 🔥 不再需要 token 参数
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
     * 🔥 不再需要 token 参数
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
     * Delete conversation
     * 🔥 不再需要 token 参数
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
     * 🔥 新增方法 - 搜索用户以创建对话
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
     * 🔥 新增方法 - 创建新对话
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
