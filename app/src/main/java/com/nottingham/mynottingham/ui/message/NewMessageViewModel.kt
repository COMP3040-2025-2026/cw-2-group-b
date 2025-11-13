package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.data.remote.dto.ContactSuggestionDto
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for New Message screen
 */
class NewMessageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository(application)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _contactSuggestions = MutableLiveData<List<ContactSuggestionDto>>()
    val contactSuggestions: LiveData<List<ContactSuggestionDto>> = _contactSuggestions

    private val _conversationCreated = MutableLiveData<Conversation?>()
    val conversationCreated: LiveData<Conversation?> = _conversationCreated

    /**
     * Load contact suggestions
     */
    fun loadContactSuggestions(token: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.getContactSuggestions(token)
            _loading.value = false

            result.onSuccess { contacts ->
                _contactSuggestions.value = contacts
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to load contacts"
            }
        }
    }

    /**
     * Create a new conversation with a user
     */
    fun createOneOnOneConversation(token: String, participantId: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.createConversation(
                token = token,
                participantIds = listOf(participantId),
                isGroup = false
            )
            _loading.value = false

            result.onSuccess { conversation ->
                _conversationCreated.value = conversation
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to create conversation"
            }
        }
    }

    /**
     * Create a group conversation
     */
    fun createGroupConversation(token: String, participantIds: List<String>, groupName: String) {
        if (participantIds.size < 2) {
            _error.value = "Group must have at least 2 participants"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = repository.createConversation(
                token = token,
                participantIds = participantIds,
                isGroup = true,
                groupName = groupName
            )
            _loading.value = false

            result.onSuccess { conversation ->
                _conversationCreated.value = conversation
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to create group"
            }
        }
    }

    /**
     * Reset conversation created status
     */
    fun resetConversationCreated() {
        _conversationCreated.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
