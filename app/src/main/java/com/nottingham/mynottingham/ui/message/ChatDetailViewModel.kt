package com.nottingham.mynottingham.ui.message

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.GroupInfo
import com.nottingham.mynottingham.data.model.GroupMember
import com.nottingham.mynottingham.data.model.GroupRole
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import com.nottingham.mynottingham.data.repository.FirebaseUserRepository
import com.nottingham.mynottingham.data.repository.ImageUploadRepository
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.launch

/**
 * ViewModel for Chat Detail screen
 * 🔥 Migrated to Firebase - no longer depends on backend API or WebSocket
 */
class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseMessageRepository()
    private val userRepo = FirebaseUserRepository()
    private val imageUploadRepo = ImageUploadRepository()

    private val _uploadingImage = MutableLiveData<Boolean>()
    val uploadingImage: LiveData<Boolean> = _uploadingImage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _participantStatus = MutableLiveData<String>()
    val participantStatus: LiveData<String> = _participantStatus

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sendingMessage = MutableLiveData<Boolean>()
    val sendingMessage: LiveData<Boolean> = _sendingMessage

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private val _typingStatus = MutableLiveData<String?>()
    val typingStatus: LiveData<String?> = _typingStatus

    private var conversationId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String? = null

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    // Group info
    private val _groupInfo = MutableLiveData<GroupInfo?>()
    val groupInfo: LiveData<GroupInfo?> = _groupInfo

    private val _groupMembers = MutableLiveData<List<GroupMember>>()
    val groupMembers: LiveData<List<GroupMember>> = _groupMembers

    private val _currentUserRole = MutableLiveData<GroupRole>()
    val currentUserRole: LiveData<GroupRole> = _currentUserRole

    // User profile
    private val _userProfile = MutableLiveData<Map<String, Any?>>()
    val userProfile: LiveData<Map<String, Any?>> = _userProfile

    // Operation results
    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    fun initializeChat(
        conversationId: String,
        userId: String,
        userName: String = "",
        userAvatar: String? = null
    ) {
        this.conversationId = conversationId
        this.currentUserId = userId
        this.currentUserName = userName
        this.currentUserAvatar = userAvatar

        viewModelScope.launch {
            _loading.postValue(true)
            firebaseRepo.getMessagesFlow(conversationId).collect { messageList ->
                _messages.postValue(messageList)
                _loading.postValue(false)
            }
        }
        markAsRead()
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || content.length > Constants.MAX_MESSAGE_LENGTH) {
            _error.value = "Message is invalid"
            return
        }

        _sendingMessage.value = true
        viewModelScope.launch {
            val result = firebaseRepo.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderAvatar = currentUserAvatar,
                message = content
            )
            _sendingMessage.value = false

            result.onSuccess {
                _messageSent.value = true
                markAsRead()
            }

            result.onFailure { e ->
                _error.value = e.message ?: "Failed to send message"
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    fun updateTyping(isTyping: Boolean) {
        viewModelScope.launch {
            firebaseRepo.updateTypingStatus(conversationId, currentUserId, isTyping)
        }
    }

    /**
     * Send an image message
     */
    fun sendImageMessage(imageUri: Uri) {
        _uploadingImage.value = true
        _sendingMessage.value = true

        viewModelScope.launch {
            // Upload image to Firebase Storage
            val uploadResult = imageUploadRepo.uploadImage(
                context = getApplication(),
                imageUri = imageUri,
                folder = ImageUploadRepository.FOLDER_CHAT_IMAGES,
                userId = currentUserId
            )

            uploadResult.onSuccess { imageUrl ->
                // Send message with image
                val result = firebaseRepo.sendMessage(
                    conversationId = conversationId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    message = "",
                    messageType = "IMAGE",
                    imageUrl = imageUrl
                )

                result.onSuccess {
                    _messageSent.value = true
                    markAsRead()
                }

                result.onFailure { e ->
                    _error.value = e.message ?: "Failed to send image"
                    Log.e(TAG, "Failed to send image message", e)
                }
            }

            uploadResult.onFailure { e ->
                _error.value = "Failed to upload image: ${e.message}"
                Log.e(TAG, "Failed to upload image", e)
            }

            _uploadingImage.value = false
            _sendingMessage.value = false
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            firebaseRepo.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun resetMessageSent() {
        _messageSent.value = false
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Observe participant's online status (Telegram-style)
     */
    fun observeParticipantPresence(participantId: String) {
        viewModelScope.launch {
            userRepo.observeUserPresence(participantId).collect { (isOnline, lastSeen) ->
                val status = if (isOnline) {
                    "Active now"
                } else {
                    userRepo.formatLastSeen(lastSeen)
                }
                _participantStatus.postValue(status)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    // ========== User Profile Methods ==========

    /**
     * Load user profile for 1:1 chat
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val result = userRepo.getUserProfileOnce(userId)
                result.onSuccess { user ->
                    _userProfile.value = mapOf(
                        "id" to user.id,
                        "name" to user.name,
                        "email" to user.email,
                        "role" to user.role,
                        "faculty" to user.faculty,
                        "program" to user.program,
                        "year" to user.year.toString(),
                        "avatar" to user.profileImageUrl
                    )
                }
                result.onFailure { e: Throwable ->
                    Log.e(TAG, "Failed to load user profile: ${e.message}")
                    _error.value = "Failed to load user profile"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _error.value = "Error loading user profile"
            }
        }
    }

    // ========== Group Management Methods ==========

    /**
     * Load group info and members
     */
    fun loadGroupInfo() {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.getGroupInfo(conversationId)
                result.onSuccess { info ->
                    val ownerId = info["ownerId"] as? String ?: ""
                    val adminIds = (info["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val participantIds = (info["participantIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    // Determine current user's role
                    val role = when {
                        currentUserId == ownerId -> GroupRole.OWNER
                        adminIds.contains(currentUserId) -> GroupRole.ADMIN
                        else -> GroupRole.MEMBER
                    }
                    _currentUserRole.value = role

                    // Load member details
                    val members = mutableListOf<GroupMember>()
                    participantIds.forEach { memberId ->
                        val userResult = userRepo.getUserProfileOnce(memberId)
                        userResult.onSuccess { user ->
                            val memberRole = when {
                                memberId == ownerId -> GroupRole.OWNER
                                adminIds.contains(memberId) -> GroupRole.ADMIN
                                else -> GroupRole.MEMBER
                            }
                            members.add(
                                GroupMember(
                                    id = user.id,
                                    name = user.name,
                                    avatar = user.profileImageUrl,
                                    email = user.email,
                                    role = memberRole,
                                    faculty = user.faculty,
                                    program = user.program,
                                    year = user.year.toString(),
                                    userRole = user.role
                                )
                            )
                        }
                    }

                    // Sort: owner first, then admins, then members
                    val sortedMembers = members.sortedWith(
                        compareBy(
                            { it.role != GroupRole.OWNER },
                            { it.role != GroupRole.ADMIN },
                            { it.name }
                        )
                    )
                    _groupMembers.value = sortedMembers

                    _groupInfo.value = GroupInfo(
                        id = info["id"] as? String ?: "",
                        name = info["groupName"] as? String ?: "Group",
                        createdAt = info["createdAt"] as? Long ?: 0L,
                        ownerId = ownerId,
                        adminIds = adminIds,
                        members = sortedMembers
                    )
                }
                result.onFailure { e ->
                    Log.e(TAG, "Failed to load group info: ${e.message}")
                    _error.value = "Failed to load group info"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading group info", e)
                _error.value = "Error loading group info"
            }
        }
    }

    /**
     * Update group name
     */
    fun updateGroupName(newName: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.updateGroupName(conversationId, newName)
                result.onSuccess {
                    _operationSuccess.value = "Group name updated"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to update group name: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error updating group name"
            }
        }
    }

    /**
     * Add member to group
     */
    fun addGroupMember(userId: String) {
        viewModelScope.launch {
            try {
                val groupName = _groupInfo.value?.name ?: "Group"
                val result = firebaseRepo.addGroupMember(conversationId, userId, groupName)
                result.onSuccess {
                    _operationSuccess.value = "Member added"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to add member: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error adding member"
            }
        }
    }

    /**
     * Remove member from group
     */
    fun removeGroupMember(userId: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.removeGroupMember(conversationId, userId)
                result.onSuccess {
                    _operationSuccess.value = "Member removed"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to remove member: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error removing member"
            }
        }
    }

    /**
     * Set user as admin
     */
    fun setAsAdmin(userId: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.addAdmin(conversationId, userId)
                result.onSuccess {
                    _operationSuccess.value = "Admin added"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to set admin: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error setting admin"
            }
        }
    }

    /**
     * Remove admin role
     */
    fun removeAdmin(userId: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.removeAdmin(conversationId, userId)
                result.onSuccess {
                    _operationSuccess.value = "Admin removed"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to remove admin: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error removing admin"
            }
        }
    }

    /**
     * Transfer group ownership
     */
    fun transferOwnership(newOwnerId: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.transferOwnership(conversationId, newOwnerId)
                result.onSuccess {
                    _operationSuccess.value = "Ownership transferred"
                    loadGroupInfo()
                }
                result.onFailure { e ->
                    _error.value = "Failed to transfer ownership: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error transferring ownership"
            }
        }
    }

    /**
     * Leave group
     */
    fun leaveGroup() {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.leaveGroup(conversationId, currentUserId)
                result.onSuccess {
                    _operationSuccess.value = "LEFT_GROUP"
                }
                result.onFailure { e ->
                    _error.value = "Failed to leave group: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error leaving group"
            }
        }
    }

    /**
     * Dissolve group (owner only)
     */
    fun dissolveGroup() {
        viewModelScope.launch {
            try {
                val result = firebaseRepo.dissolveGroup(conversationId)
                result.onSuccess {
                    _operationSuccess.value = "GROUP_DISSOLVED"
                }
                result.onFailure { e ->
                    _error.value = "Failed to dissolve group: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error dissolving group"
            }
        }
    }

    fun clearOperationSuccess() {
        _operationSuccess.value = null
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }
}
