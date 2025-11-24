package com.nottingham.mynottingham.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseUserRepository
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * LoginViewModel - Firebase Migration Edition
 *
 * ⚠️ TEMPORARY AUTHENTICATION SOLUTION
 * This version uses Firebase for user lookup but bypasses password validation
 * since Firebase stores BCrypt hashed passwords that can't be verified client-side.
 *
 * PRODUCTION SOLUTION:
 * - Migrate to Firebase Authentication SDK
 * - Reset all user passwords during migration
 * - Use FirebaseAuth.signInWithEmailAndPassword()
 *
 * CURRENT BEHAVIOR:
 * - User enters username (e.g., "student1")
 * - System checks if user exists in Firebase
 * - Password is validated against hardcoded "password123" (INSECURE - for testing only)
 * - User info is saved to TokenManager
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val firebaseUserRepo = FirebaseUserRepository()
    private val messageRepository = MessageRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    // 临时的测试密码 - 所有用户都使用这个密码
    private val TEST_PASSWORD = "password123"

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // ===== Firebase 登录逻辑 =====

                // Step 1: 验证密码 (临时方案 - 所有用户统一密码)
                if (password != TEST_PASSWORD) {
                    _error.value = "Invalid password. Hint: Try 'password123'"
                    return@launch
                }

                // Step 2: 从 Firebase 查找用户
                val userId = firebaseUserRepo.findUserIdByUsername(username)
                if (userId == null) {
                    _error.value = "User not found: $username"
                    return@launch
                }

                // Step 3: 获取完整用户信息
                val userResult = firebaseUserRepo.getUserProfileOnce(userId)
                if (userResult.isFailure) {
                    _error.value = "Failed to load user profile: ${userResult.exceptionOrNull()?.message}"
                    return@launch
                }

                val user = userResult.getOrNull() ?: run {
                    _error.value = "User data is null"
                    return@launch
                }

                // Step 4: 保存用户信息到 TokenManager
                // 生成一个临时 token (Firebase Auth 会提供真实 token)
                val tempToken = "firebase_token_${System.currentTimeMillis()}"

                tokenManager.saveUserId(userId)
                tokenManager.saveUsername(username)
                tokenManager.saveFullName(user.name)
                tokenManager.saveToken(tempToken)

                // 根据 studentId 判断角色
                val userType = if (user.studentId.isNotEmpty() && user.studentId.toLongOrNull() != null) {
                    "STUDENT"
                } else {
                    "TEACHER"
                }
                tokenManager.saveUserType(userType)

                // 保存额外信息
                if (userType == "STUDENT") {
                    tokenManager.saveFaculty(user.faculty)
                    // 暂时设置为固定值，因为 Firebase 中没有这些字段
                    tokenManager.saveYearOfStudy(user.year.toString())
                } else {
                    tokenManager.saveDepartment(user.faculty) // Teacher 的 faculty 字段存的是 department
                }

                android.util.Log.d("LoginViewModel", "✅ Login successful: $username ($userType)")

                // Step 5: 创建默认对话 (可选)
                try {
                    createDefaultConversations(tempToken, userId)
                } catch (e: Exception) {
                    // 忽略错误，不影响登录
                    android.util.Log.w("LoginViewModel", "Failed to create default conversations: ${e.message}")
                }

                _loginSuccess.value = true

            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "❌ Login error", e)
                _error.value = "Login error: ${e.message}"
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
