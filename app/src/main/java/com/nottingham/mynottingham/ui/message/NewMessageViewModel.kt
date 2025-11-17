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

    // Store all contacts for filtering
    private var allContacts: List<ContactSuggestionDto> = emptyList()
    private var currentSearchQuery: String = ""

    /**
     * Load contact suggestions (now loads all contacts)
     */
    fun loadContactSuggestions(token: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.getContactSuggestions(token)
            _loading.value = false

            result.onSuccess { contacts ->
                // Sort contacts alphabetically by name
                allContacts = contacts.sortedBy { it.userName.uppercase() }
                applyFilters()
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to load contacts"
            }
        }
    }

    /**
     * Search contacts by query
     */
    fun searchContacts(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    /**
     * Scroll to letter
     */
    fun scrollToLetter(letter: String) {
        val filteredContacts = getFilteredContacts()
        val index = filteredContacts.indexOfFirst {
            val firstChar = it.userName.firstOrNull()?.uppercaseChar()
            if (letter == "#") {
                firstChar == null || firstChar !in 'A'..'Z'
            } else {
                firstChar?.toString() == letter
            }
        }
        // Index will be used by the Fragment to scroll RecyclerView
        // We return the filtered list which is already in correct order
    }

    /**
     * Apply current filters (search query)
     */
    private fun applyFilters() {
        val filtered = getFilteredContacts()
        _contactSuggestions.value = filtered
    }

    /**
     * Get filtered and sorted contacts
     */
    private fun getFilteredContacts(): List<ContactSuggestionDto> {
        var filtered = allContacts

        // Apply search query
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.userName.contains(currentSearchQuery, ignoreCase = true) ||
                it.program?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        return filtered
    }

    /**
     * Get index of first contact starting with given letter
     */
    fun getIndexForLetter(letter: String): Int {
        val filteredContacts = getFilteredContacts()
        return filteredContacts.indexOfFirst {
            val firstChar = it.userName.firstOrNull()?.uppercaseChar()
            if (letter == "#") {
                firstChar == null || firstChar !in 'A'..'Z'
            } else {
                firstChar?.toString() == letter
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
