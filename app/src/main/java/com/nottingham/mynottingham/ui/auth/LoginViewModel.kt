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

                        // Save token and user info
                        // Remove "Bearer " prefix before saving
                        val pureToken = loginData.token.removePrefix("Bearer ").trim()
                        tokenManager.saveToken(pureToken)
                        tokenManager.saveUserInfo(
                            userId = loginData.user.id.toString(),
                            username = loginData.user.username,
                            userType = loginData.user.userType
                        )

                        // Save full name
                        tokenManager.saveFullName(loginData.user.fullName)

                        // Save email
                        tokenManager.saveEmail(loginData.user.email)

                        // Save phone if available
                        loginData.user.phone?.let { phone ->
                            tokenManager.savePhone(phone)
                        }

                        // Save avatar if available
                        loginData.user.avatarUrl?.let { avatarUrl ->
                            tokenManager.saveAvatar(avatarUrl)
                        }

                        // Save student-specific fields if available
                        loginData.user.studentId?.let { studentId ->
                            tokenManager.saveStudentId(studentId.toString())
                        }

                        loginData.user.faculty?.let { faculty ->
                            tokenManager.saveFaculty(faculty)
                        }

                        loginData.user.major?.let { major ->
                            tokenManager.saveMajor(major)
                        }

                        loginData.user.yearOfStudy?.let { year ->
                            tokenManager.saveYearOfStudy(year.toString())
                        }

                        // Save teacher-specific fields if available
                        loginData.user.employeeId?.let { employeeId ->
                            tokenManager.saveEmployeeId(employeeId)
                        }

                        loginData.user.department?.let { department ->
                            tokenManager.saveDepartment(department)
                        }

                        loginData.user.title?.let { title ->
                            tokenManager.saveTitle(title)
                        }

                        loginData.user.officeRoom?.let { officeRoom ->
                            tokenManager.saveOfficeRoom(officeRoom)
                        }

                        loginData.user.officeHours?.let { officeHours ->
                            tokenManager.saveOfficeHours(officeHours)
                        }

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
