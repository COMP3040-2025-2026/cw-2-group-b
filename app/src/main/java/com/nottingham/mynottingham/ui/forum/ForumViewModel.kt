package com.nottingham.mynottingham.ui.forum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.entities.ForumCommentEntity
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.data.remote.dto.*
import com.nottingham.mynottingham.data.repository.ForumRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for Forum (post list) screen
 */
class ForumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForumRepository(application)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _posts = MutableLiveData<List<ForumPostEntity>>()
    val posts: LiveData<List<ForumPostEntity>> = _posts

    private val _selectedCategory = MutableLiveData<String?>(null)
    val selectedCategory: LiveData<String?> = _selectedCategory

    private var currentPage = 0
    private var hasMore = true

    /**
     * Set category filter and reload posts
     */
    fun setCategory(category: String?) {
        _selectedCategory.value = category
        currentPage = 0
        hasMore = true
        observePosts(category)
    }

    /**
     * Observe posts from local database
     */
    fun observePosts(category: String? = _selectedCategory.value) {
        viewModelScope.launch {
            repository.getPostsFlow(category).collect { postList ->
                _posts.postValue(postList)
            }
        }
    }

    /**
     * Fetch posts from API (with pagination)
     */
    fun fetchPosts(token: String, loadMore: Boolean = false) {
        if (_loading.value == true || (!hasMore && loadMore)) return

        _loading.value = true
        _error.value = null

        val page = if (loadMore) currentPage + 1 else 0

        viewModelScope.launch {
            val result = repository.fetchPosts(
                token = token,
                page = page,
                size = 20,
                category = _selectedCategory.value
            )

            result.onSuccess { pagedResponse ->
                hasMore = pagedResponse.hasNext
                if (!loadMore) {
                    currentPage = 0
                } else {
                    currentPage = page
                }
            }.onFailure { exception ->
                _error.postValue(exception.message)
            }

            _loading.postValue(false)
        }
    }

    /**
     * Refresh posts
     */
    fun refreshPosts(token: String) {
        currentPage = 0
        hasMore = true
        fetchPosts(token, loadMore = false)
    }

    /**
     * Like/unlike post
     */
    fun likePost(token: String, postId: Long) {
        viewModelScope.launch {
            val result = repository.likePost(token, postId)
            result.onFailure { exception ->
                _error.postValue(exception.message)
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
                // Post will be removed automatically via Flow observation
            }.onFailure { exception ->
                _error.postValue(exception.message)
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
