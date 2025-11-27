package com.nottingham.mynottingham.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.messaging.FirebaseMessaging
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseUserRepository
import com.nottingham.mynottingham.data.repository.MessageRepository
import com.nottingham.mynottingham.service.MyFirebaseMessagingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * LoginViewModel - Firebase Authentication Edition
 *
 * ✅ PRODUCTION-READY AUTHENTICATION
 * This version uses Firebase Authentication SDK for secure user authentication.
 *
 * AUTHENTICATION FLOW:
 * 1. User enters username (e.g., "student1") and password
 * 2. Convert username to email format (student1 → student1@nottingham.edu.my)
 * 3. Authenticate with FirebaseAuth.signInWithEmailAndPassword()
 * 4. Retrieve Firebase UID from authenticated user
 * 5. Fetch user profile from Realtime Database using UID
 * 6. Save user info to TokenManager
 *
 * EMAIL FORMAT MAPPING:
 * - Students: {username}@nottingham.edu.my (e.g., student1@nottingham.edu.my)
 * - Teachers: {username}@nottingham.edu.my (e.g., teacher1@nottingham.edu.my)
 * - Admin: admin@nottingham.edu.my
 *
 * DEFAULT PASSWORD: password123 (admin: admin123)
 *
 * BENEFITS:
 * - ✅ Secure password verification (handled by Firebase)
 * - ✅ No backend dependency
 * - ✅ Industry-standard authentication
 * - ✅ Password reset functionality (can be added)
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseUserRepo = FirebaseUserRepository()
    private val messageRepository = MessageRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    /**
     * Login with Firebase Authentication
     *
     * @param username Username (will be converted to email)
     * @param password User's password
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "🔐 Starting Firebase Auth login for user: $username")

                // Step 1: Convert username to email format
                val email = convertUsernameToEmail(username)
                Log.d(TAG, "📧 Converted username to email: $email")

                // Step 2: Authenticate with Firebase Auth
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    _error.value = "Authentication failed: User is null"
                    Log.e(TAG, "❌ Firebase user is null after authentication")
                    return@launch
                }

                val uid = firebaseUser.uid
                Log.d(TAG, "✅ Firebase Auth successful! UID: $uid")

                // Step 3: Fetch user profile from Realtime Database
                val userResult = firebaseUserRepo.getUserProfileOnce(uid)
                if (userResult.isFailure) {
                    _error.value = "Failed to load user profile: ${userResult.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Failed to fetch user profile from database", userResult.exceptionOrNull())
                    return@launch
                }

                val user = userResult.getOrNull() ?: run {
                    _error.value = "User data is null"
                    Log.e(TAG, "❌ User data is null")
                    return@launch
                }

                // Step 4: Save user information to TokenManager
                // Get Firebase ID token (can be used for backend authentication if needed)
                val idToken = firebaseUser.getIdToken(false).await().token ?: ""

                // Determine user type based on role
                val userType = when {
                    user.role == "STUDENT" -> "STUDENT"
                    user.role == "TEACHER" -> "TEACHER"
                    user.role == "ADMIN" -> "ADMIN"
                    else -> "STUDENT" // Default fallback
                }

                // Save user info (uses DataStore)
                tokenManager.saveToken(idToken)
                tokenManager.saveUserInfo(uid, user.username, userType)
                tokenManager.saveFullName(user.name)
                tokenManager.saveEmail(user.email) // ✅ 保存邮箱

                // ✅ 保存完整的用户信息
                if (userType == "STUDENT") {
                    tokenManager.saveStudentId(user.studentId) // 保存学号
                    tokenManager.saveFaculty(user.faculty)     // 保存学院
                    tokenManager.saveMajor(user.program)        // 保存专业 (User.program -> TokenManager.major)
                    tokenManager.saveYearOfStudy(user.year.toString()) // 保存年级
                } else if (userType == "TEACHER") {
                    tokenManager.saveEmployeeId(user.studentId) // 教师的 studentId 字段存的是 Employee ID
                    tokenManager.saveDepartment(user.faculty)   // 教师的 faculty 字段存的是 Department
                    // ✅ 保存教师专属字段
                    user.title?.let { tokenManager.saveTitle(it) }
                    user.officeRoom?.let { tokenManager.saveOfficeRoom(it) }
                }

                // ✅ 保存头像 URL (如果有)
                user.profileImageUrl?.let { tokenManager.saveAvatar(it) }

                // ✅ 保存骑手模式状态 (从 Firebase 同步)
                val deliveryMode = user.deliveryMode ?: false
                tokenManager.setDeliveryMode(deliveryMode)
                Log.d(TAG, "🚴 Delivery mode loaded: $deliveryMode")

                Log.d(TAG, "✅ Login successful: ${user.username} ($userType) | UID: $uid")
                Log.d(TAG, "👤 User info: ${user.name} | Email: ${user.email}")

                // Step 5: Create default conversations (optional)
                try {
                    createDefaultConversations(idToken, uid)
                } catch (e: Exception) {
                    // Ignore errors, not critical
                    Log.w(TAG, "Failed to create default conversations: ${e.message}")
                }

                // Step 6: Setup presence for online status tracking
                firebaseUserRepo.setupPresence(uid)
                Log.d(TAG, "🟢 Presence system initialized for user: $uid")

                // Step 7: Save FCM token for push notifications
                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    MyFirebaseMessagingService.saveTokenToFirebase(fcmToken)
                    Log.d(TAG, "📲 FCM token saved for push notifications")
                }

                _loginSuccess.value = true

            } catch (e: FirebaseAuthInvalidUserException) {
                Log.e(TAG, "❌ User not found", e)
                _error.value = "User not found. Please check your username."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "❌ Invalid password", e)
                _error.value = "Invalid password. Please try again."
            } catch (e: Exception) {
                Log.e(TAG, "❌ Login error", e)
                _error.value = "Login error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Convert username to email format for Firebase Authentication
     *
     * Examples:
     * - student1 → student1@nottingham.edu.my
     * - teacher1 → teacher1@nottingham.edu.my
     * - admin → admin@nottingham.edu.my
     */
    private fun convertUsernameToEmail(username: String): String {
        return "$username@nottingham.edu.my"
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

    /**
     * Check if a user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            // Set offline status before signing out
            val userId = firebaseAuth.currentUser?.uid
            if (!userId.isNullOrEmpty()) {
                firebaseUserRepo.setOffline(userId)
                Log.d(TAG, "🔴 User set to offline: $userId")
            }
            // Remove FCM token to stop receiving notifications
            MyFirebaseMessagingService.removeTokenFromFirebase()
            firebaseAuth.signOut()
            tokenManager.clearToken()
            Log.d(TAG, "🚪 User logged out successfully")
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}
