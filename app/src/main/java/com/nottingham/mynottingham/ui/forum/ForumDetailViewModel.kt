package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.ForumComment
import com.nottingham.mynottingham.data.model.ForumPost
import com.nottingham.mynottingham.data.repository.FirebaseForumRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Forum Detail screen
 * Migrated to Firebase - real-time post and comments
 */
class ForumDetailViewModel(application: Application) : AndroidViewModel(application) {

    // Replaced with Firebase Repository
    private val repository = FirebaseForumRepository()
    private val tokenManager = TokenManager(application)
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String? = null

    // UI State
    private val _post = MutableStateFlow<ForumPost?>(null)
    val post: Flow<ForumPost?> = _post

    private val _comments = MutableStateFlow<List<ForumComment>>(emptyList())
    val comments: Flow<List<ForumComment>> = _comments

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _commentSuccess = MutableLiveData<Boolean>()
    val commentSuccess: LiveData<Boolean> = _commentSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    init {
        viewModelScope.launch {
            currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            currentUserName = tokenManager.getFullName().firstOrNull() ?: "User"
            currentUserAvatar = tokenManager.getAvatar().firstOrNull()
        }
    }

    // Keep backward compatibility with Long ID method
    fun getPostFlow(postId: Long): Flow<ForumPost?> {
        return _post
    }

    fun getCommentsFlow(postId: Long): Flow<List<ForumComment>> {
        return _comments
    }

    // Change: Use String ID
    fun loadPostDetail(token: String, postId: Long) {
        loadPostDetail(postId.toString())
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _loading.value = true

            // Ensure currentUserId is retrieved (wait for init to complete or re-fetch)
            if (currentUserId.isEmpty()) {
                currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            }

            // 1. Listen to post details
            repository.getPostDetailFlow(postId, currentUserId).collect {
                _post.value = it
                _loading.value = false
            }
        }

        viewModelScope.launch {
            // Ensure currentUserId is retrieved
            if (currentUserId.isEmpty()) {
                currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            }

            // 2. Listen to comments
            repository.getCommentsFlow(postId, currentUserId).collect {
                _comments.value = it
            }
        }

        // 3. Increment view count (counted only once per user)
        viewModelScope.launch {
            // Ensure currentUserId is retrieved
            if (currentUserId.isEmpty()) {
                currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            }

            repository.incrementViews(postId, currentUserId)
        }
    }

    // Keep backward compatibility with Long ID method
    fun sendComment(token: String, postId: Long, content: String) {
        sendComment(postId.toString(), content)
    }

    fun sendComment(postId: String, content: String) {
        if (content.isBlank()) return

        _loading.value = true
        viewModelScope.launch {
            val result = repository.createComment(
                postId = postId,
                authorId = currentUserId,
                authorName = currentUserName,
                authorAvatar = currentUserAvatar,
                content = content
            )

            result.onSuccess {
                _commentSuccess.postValue(true)
            }.onFailure { exception ->
                _error.postValue(exception.message ?: "Failed to send comment")
            }
            _loading.postValue(false)
        }
    }

    // Keep backward compatibility with method
    fun likePost(token: String, postId: Long) {
        likePost(postId.toString())
    }

    fun likePost(postId: String) {
        viewModelScope.launch { repository.toggleLikePost(postId, currentUserId) }
    }

    // Keep backward compatibility with method
    fun likeComment(token: String, commentId: Long) {
        likeComment(commentId.toString())
    }

    fun likeComment(commentId: String) {
        // Firebase needs to know both postId and commentId, but old interface doesn't have postId
        // For compatibility, we need to get postId from current post
        val postId = _post.value?.id ?: return
        viewModelScope.launch {
            repository.toggleLikeComment(commentId, postId, currentUserId)
        }
    }

    // Keep backward compatibility with method
    fun deletePost(token: String, postId: Long) {
        deletePost(postId.toString())
    }

    fun deletePost(postId: String) {
        if (_loading.value == true) return

        _loading.value = true
        viewModelScope.launch {
            val result = repository.deletePost(postId)
            result.onSuccess {
                _deleteSuccess.postValue(true)
            }.onFailure { exception ->
                Log.e("ForumDetailViewModel", "Failed to delete post", exception)
                _error.postValue(exception.message ?: "Failed to delete post")
            }
            _loading.postValue(false)
        }
    }

    fun clearCommentSuccess() {
        _commentSuccess.value = false
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
