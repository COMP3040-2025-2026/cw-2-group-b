package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.remote.dto.CreateForumPostRequest
import com.nottingham.mynottingham.data.repository.ForumRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * ViewModel for Create Post screen
 */
class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForumRepository(application)

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
        token: String,
        title: String,
        content: String,
        category: String,
        tags: List<String>?,
        image: MultipartBody.Part? = null
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
            val request = CreateForumPostRequest(
                title = title.trim(),
                content = content.trim(),
                category = category,
                tags = tags?.filter { it.isNotBlank() }
            )

            Log.d("CreatePostViewModel", "Creating post: title=$title, category=$category, tags=$tags, hasImage=${image != null}")
            Log.d("CreatePostViewModel", "Token: ${if (token.isNotEmpty()) "present (${token.length} chars)" else "EMPTY!"}")

            val result = repository.createPost(token, request, image)

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
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
