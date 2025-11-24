package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.entities.ForumCommentEntity
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.data.remote.dto.CreateCommentRequest
import com.nottingham.mynottingham.data.repository.ForumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ForumDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForumRepository(application)

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _commentSuccess = MutableLiveData<Boolean>()
    val commentSuccess: LiveData<Boolean> = _commentSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    // Fetch local data flows
    fun getPostFlow(postId: Long): Flow<ForumPostEntity?> {
        return repository.getPostByIdFlow(postId)
    }

    fun getCommentsFlow(postId: Long): Flow<List<ForumCommentEntity>> {
        return repository.getCommentsFlow(postId)
    }

    fun loadPostDetail(token: String, postId: Long) {
        if (_loading.value == true) return

        _loading.value = true
        viewModelScope.launch {
            val result = repository.getPostDetail(token, postId)
            result.onFailure { exception ->
                Log.e("ForumDetailViewModel", "Failed to load post detail", exception)
                _error.postValue(exception.message ?: "Failed to load post")
            }
            _loading.postValue(false)
        }
    }

    fun sendComment(token: String, postId: Long, content: String) {
        if (content.isBlank()) return

        _loading.value = true
        viewModelScope.launch {
            val request = CreateCommentRequest(content)
            val result = repository.createComment(token, postId, request)

            result.onSuccess {
                _commentSuccess.postValue(true)
            }.onFailure { exception ->
                _error.postValue(exception.message ?: "Failed to send comment")
            }
            _loading.postValue(false)
        }
    }

    fun likePost(token: String, postId: Long) {
        viewModelScope.launch {
            repository.likePost(token, postId)
        }
    }

    fun likeComment(token: String, commentId: Long) {
        viewModelScope.launch {
            repository.likeComment(token, commentId)
        }
    }

    fun deletePost(token: String, postId: Long) {
        if (_loading.value == true) return

        _loading.value = true
        viewModelScope.launch {
            val result = repository.deletePost(token, postId)
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
