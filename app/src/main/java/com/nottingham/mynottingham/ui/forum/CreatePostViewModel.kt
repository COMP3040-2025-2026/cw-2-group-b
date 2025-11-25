package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseForumRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for Create Post screen
 * ✅ Migrated to Firebase - no longer uses backend API
 */
class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseForumRepository()
    private val tokenManager = TokenManager(application)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    /**
     * Create a new post
     */
    fun createPost(
        title: String,
        content: String,
        category: String,
        tags: List<String>?,
        isPinned: Boolean = false
    ) {
        // Validation
        if (title.isBlank()) {
            _error.value = "Title is required"
            return
        }

        if (content.isBlank()) {
            _error.value = "Content is required"
            return
        }

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            val userId = tokenManager.getUserId().firstOrNull() ?: ""
            val userName = tokenManager.getFullName().firstOrNull() ?: "User"
            val userAvatar = tokenManager.getAvatar().firstOrNull()

            Log.d("CreatePostViewModel", "Creating post: title=$title, category=$category, tags=$tags")
            Log.d("CreatePostViewModel", "User: userId=$userId, userName=$userName")

            val result = repository.createPost(
                authorId = userId,
                authorName = userName,
                authorAvatar = userAvatar,
                category = category,
                title = title.trim(),
                content = content.trim(),
                imageUrl = null,
                tags = tags,
                isPinned = isPinned
            )

            result.onSuccess {
                Log.d("CreatePostViewModel", "Post created successfully")
                _postCreated.postValue(true)
            }.onFailure { exception ->
                Log.e("CreatePostViewModel", "Failed to create post", exception)
                _error.postValue(exception.message ?: "Failed to create post")
            }

            _loading.postValue(false)
        }
    }

    /**
     * Update an existing post
     */
    fun updatePost(
        postId: String,
        title: String,
        content: String,
        category: String,
        tags: List<String>?,
        isPinned: Boolean = false
    ) {
        // Validation
        if (title.isBlank()) {
            _error.value = "Title is required"
            return
        }

        if (content.isBlank()) {
            _error.value = "Content is required"
            return
        }

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            Log.d("CreatePostViewModel", "Updating post: postId=$postId, title=$title, category=$category")

            val result = repository.updatePost(
                postId = postId,
                title = title.trim(),
                content = content.trim(),
                category = category,
                tags = tags,
                isPinned = isPinned
            )

            result.onSuccess {
                Log.d("CreatePostViewModel", "Post updated successfully")
                _postCreated.postValue(true)
            }.onFailure { exception ->
                Log.e("CreatePostViewModel", "Failed to update post", exception)
                _error.postValue(exception.message ?: "Failed to update post")
            }

            _loading.postValue(false)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
