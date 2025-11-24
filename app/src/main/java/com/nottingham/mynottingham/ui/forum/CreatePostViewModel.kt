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
import okhttp3.MultipartBody

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
     * ⚠️ 注意：image 参数保留但暂不支持（需要先配置 Firebase Storage）
     */
    fun createPost(
        token: String, // 参数保留但不再使用
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
            val userId = tokenManager.getUserId().firstOrNull() ?: ""
            val userName = tokenManager.getFullName().firstOrNull() ?: "User"
            val userAvatar = tokenManager.getAvatar().firstOrNull()

            Log.d("CreatePostViewModel", "Creating post: title=$title, category=$category, tags=$tags")
            Log.d("CreatePostViewModel", "User: userId=$userId, userName=$userName")

            // ⚠️ TODO: 如果有 image，需要先上传到 Firebase Storage 获取 URL
            // 暂时只支持纯文本帖子
            val imageUrl: String? = null

            if (image != null) {
                Log.w("CreatePostViewModel", "Image upload not yet implemented. Image will be ignored.")
            }

            val result = repository.createPost(
                authorId = userId,
                authorName = userName,
                authorAvatar = userAvatar,
                category = category,
                title = title.trim(),
                content = content.trim(),
                imageUrl = imageUrl
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
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
