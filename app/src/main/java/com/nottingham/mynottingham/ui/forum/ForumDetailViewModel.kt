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
 * ✅ Migrated to Firebase - real-time post and comments
 */
class ForumDetailViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ 替换为 Firebase Repository
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

    // ✅ 保留兼容旧代码的 Long ID 方法
    fun getPostFlow(postId: Long): Flow<ForumPost?> {
        return _post
    }

    fun getCommentsFlow(postId: Long): Flow<List<ForumComment>> {
        return _comments
    }

    // ✅ 修改：使用 String ID
    fun loadPostDetail(token: String, postId: Long) {
        loadPostDetail(postId.toString())
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _loading.value = true
            // 1. 监听帖子详情
            repository.getPostDetailFlow(postId, currentUserId).collect {
                _post.value = it
                _loading.value = false
            }
        }

        viewModelScope.launch {
            // 2. 监听评论
            repository.getCommentsFlow(postId, currentUserId).collect {
                _comments.value = it
            }
        }

        // 3. 增加浏览量 (一次性)
        viewModelScope.launch {
             repository.incrementViews(postId)
        }
    }

    // ✅ 保留兼容旧代码的 Long ID 方法
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

    // ✅ 保留兼容旧代码的方法
    fun likePost(token: String, postId: Long) {
        likePost(postId.toString())
    }

    fun likePost(postId: String) {
        viewModelScope.launch { repository.toggleLikePost(postId, currentUserId) }
    }

    // ✅ 保留兼容旧代码的方法
    fun likeComment(token: String, commentId: Long) {
        likeComment(commentId.toString())
    }

    fun likeComment(commentId: String) {
        // Firebase 需要同时知道 postId 和 commentId，但旧接口没有 postId
        // 为了兼容，我们需要从当前帖子获取 postId
        val postId = _post.value?.id ?: return
        viewModelScope.launch {
            repository.toggleLikeComment(commentId, postId, currentUserId)
        }
    }

    // ✅ 保留兼容旧代码的方法
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
