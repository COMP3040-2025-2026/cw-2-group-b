package com.nottingham.mynottingham.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.LoginRequest
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val apiService = RetrofitInstance.apiService
    private val messageRepository = MessageRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val request = LoginRequest(username, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    if (apiResponse.success && apiResponse.data != null) {
                        val loginData = apiResponse.data

                        // Save all user info in one transaction using new helper function
                        val pureToken = loginData.token.removePrefix("Bearer ").trim()
                        tokenManager.saveFullUser(loginData.user, pureToken)

                        // Create default conversations (teachers/students)
                        createDefaultConversations(loginData.token, loginData.user.id.toString())

                        _loginSuccess.value = true
                    } else {
                        _error.value = apiResponse.message ?: "Login failed"
                    }
                } else {
                    _error.value = "Network error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create default conversations with teachers (for students) or students (for teachers)
     */
    private fun createDefaultConversations(token: String, userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.createDefaultConversations(token, userId)
                // Silently create conversations, don't show error to user
            } catch (e: Exception) {
                // Ignore errors in creating default conversations
                // User can still manually create conversations
            }
        }
    }
}
