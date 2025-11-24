package com.nottingham.mynottingham.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Contact
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for New Group creation screen
 */
class NewGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository(application)
    private val apiService = RetrofitInstance.apiService

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _groupCreated = MutableLiveData<String?>()
    val groupCreated: LiveData<String?> = _groupCreated

    /**
     * Load contact suggestions
     */
    fun loadContacts(token: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getContactSuggestions("Bearer $token")
                if (response.isSuccessful) {
                    val contactSuggestions = response.body()?.data
                    if (contactSuggestions != null) {
                        // Convert DTOs to Contact models
                        val contactList = contactSuggestions.map { dto ->
                            Contact(
                                id = dto.userId,
                                name = dto.userName,
                                avatar = dto.userAvatar,
                                program = dto.program,
                                year = dto.year,
                                isOnline = dto.isOnline
                            )
                        }
                        _contacts.value = contactList
                    } else {
                        _error.value = "No contacts found"
                    }
                } else {
                    _error.value = "Failed to load contacts: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load contacts"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Create a group conversation
     */
    fun createGroup(
        token: String,
        groupName: String,
        selectedContacts: Set<Contact>
    ) {
        if (groupName.isBlank()) {
            _error.value = "Please enter a group name"
            return
        }

        if (selectedContacts.size < 2) {
            _error.value = "Please select at least 2 participants"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val participantIds = selectedContacts.map { it.id }
            val result = repository.createConversation(
                token = token,
                participantIds = participantIds,
                isGroup = true,
                groupName = groupName
            )

            _loading.value = false

            result.onSuccess { conversation ->
                _groupCreated.value = conversation.id
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to create group"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset group created status
     */
    fun resetGroupCreated() {
        _groupCreated.value = null
    }
}
