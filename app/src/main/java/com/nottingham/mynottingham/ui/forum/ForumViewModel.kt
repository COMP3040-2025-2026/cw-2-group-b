package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.data.repository.ForumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Forum (post list) screen
 */
class ForumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForumRepository(application)

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentCategory = MutableStateFlow<String?>(null)

    // Expose posts as Flow from repository
    val posts: Flow<List<ForumPostEntity>> = repository.getPostsFlow(null)

    private var currentPage = 0
    private var hasMore = true

    /**
     * Load posts from API and observe from database
     */
    fun loadPosts(token: String, category: String? = null, refresh: Boolean = false) {
        Log.d("ForumViewModel", "loadPosts called with category: $category, refresh: $refresh")

        if (_loading.value == true) {
            Log.d("ForumViewModel", "Already loading, skipping")
            return
        }

        _loading.value = true
        _error.value = null

        val page = if (refresh) 0 else currentPage

        viewModelScope.launch {
            Log.d("ForumViewModel", "Fetching posts from API: page=$page, category=$category")
            val result = repository.fetchPosts(
                token = token,
                page = page,
                size = 20,
                category = category
            )

            result.onSuccess { pagedResponse ->
                Log.d("ForumViewModel", "Successfully fetched ${pagedResponse.posts.size} posts")
                hasMore = pagedResponse.hasNext
                currentPage = if (refresh) 0 else page
            }.onFailure { exception ->
                Log.e("ForumViewModel", "Failed to fetch posts", exception)
                _error.postValue(exception.message ?: "Failed to load posts")
            }

            _loading.postValue(false)
        }
    }

    /**
     * Filter posts by category
     */
    fun filterByCategory(category: String?) {
        Log.d("ForumViewModel", "filterByCategory called with: $category")
        _currentCategory.value = category

        // Update posts flow to observe from filtered category
        viewModelScope.launch {
            repository.getPostsFlow(category).collect { postList ->
                Log.d("ForumViewModel", "Received ${postList.size} posts for category: $category")
            }
        }
    }

    /**
     * Like/unlike post
     */
    fun likePost(token: String, postId: Long) {
        viewModelScope.launch {
            Log.d("ForumViewModel", "Liking post: $postId")
            val result = repository.likePost(token, postId)
            result.onSuccess {
                Log.d("ForumViewModel", "Post liked successfully")
            }.onFailure { exception ->
                Log.e("ForumViewModel", "Failed to like post", exception)
                _error.postValue(exception.message ?: "Failed to like post")
            }
        }
    }

    /**
     * Delete post
     */
    fun deletePost(token: String, postId: Long) {
        viewModelScope.launch {
            val result = repository.deletePost(token, postId)
            result.onSuccess {
                Log.d("ForumViewModel", "Post deleted successfully")
            }.onFailure { exception ->
                Log.e("ForumViewModel", "Failed to delete post", exception)
                _error.postValue(exception.message ?: "Failed to delete post")
            }
        }
    }

    /**
     * Clean old cached data
     */
    fun cleanOldCache() {
        viewModelScope.launch {
            repository.cleanOldCache()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
