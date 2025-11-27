package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseForumRepository
import com.nottingham.mynottingham.data.repository.ImageUploadRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for Create Post screen
 * ✅ Migrated to Firebase - no longer uses backend API
 */
class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseForumRepository()
    private val tokenManager = TokenManager(application)
    private val imageUploadRepo = ImageUploadRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _uploadingImage = MutableLiveData<Boolean>()
    val uploadingImage: LiveData<Boolean> = _uploadingImage

    /**
     * Set selected image URI
     */
    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * Clear selected image
     */
    fun clearSelectedImage() {
        _selectedImageUri.value = null
    }

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

            // Upload image if selected
            var imageUrl: String? = null
            val selectedImage = _selectedImageUri.value
            if (selectedImage != null) {
                _uploadingImage.postValue(true)
                val uploadResult = imageUploadRepo.uploadImage(
                    context = getApplication(),
                    imageUri = selectedImage,
                    folder = ImageUploadRepository.FOLDER_FORUM_IMAGES,
                    userId = userId
                )
                uploadResult.onSuccess { url ->
                    imageUrl = url
                    Log.d("CreatePostViewModel", "Image uploaded: $url")
                }.onFailure { e ->
                    Log.e("CreatePostViewModel", "Failed to upload image", e)
                    _error.postValue("Failed to upload image: ${e.message}")
                    _loading.postValue(false)
                    _uploadingImage.postValue(false)
                    return@launch
                }
                _uploadingImage.postValue(false)
            }

            val result = repository.createPost(
                authorId = userId,
                authorName = userName,
                authorAvatar = userAvatar,
                category = category,
                title = title.trim(),
                content = content.trim(),
                imageUrl = imageUrl,
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
