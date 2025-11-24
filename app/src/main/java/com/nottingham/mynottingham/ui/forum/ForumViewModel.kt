package com.nottingham.mynottingham.ui.forum

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.data.repository.ForumRepository
import com.nottingham.mynottingham.ui.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * ViewModel for Forum (post list) screen
 * Extends BaseViewModel for common utilities
 */
class ForumViewModel(application: Application) : BaseViewModel(application) {

    private val repository = ForumRepository(application)

    private val _currentCategory = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: Flow<List<ForumPostEntity>> = _currentCategory.flatMapLatest { category ->
        when (category) {
            "TRENDING" -> repository.getTrendingPostsFlow()
            else -> repository.getPostsFlow(category)
        }
    }

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

        val page = if (refresh) 0 else currentPage

        launchDataLoad(
            block = {
                Log.d("ForumViewModel", "Fetching posts from API: page=$page, category=$category")
                repository.fetchPosts(token, page, 20, category)
            },
            onSuccess = { pagedResponse ->
                Log.d("ForumViewModel", "Successfully fetched ${pagedResponse.posts.size} posts")
                hasMore = pagedResponse.hasNext
                currentPage = if (refresh) 0 else page
            }
        )
    }

    /**
     * Filter posts by category
     */
    fun filterByCategory(category: String?) {
        Log.d("ForumViewModel", "filterByCategory called with: $category")
        _currentCategory.value = category
    }

    /**
     * Like/unlike post
     */
    fun likePost(token: String, postId: Long) {
        launchDataLoad(
            block = {
                Log.d("ForumViewModel", "Liking post: $postId")
                repository.likePost(token, postId)
            },
            onSuccess = {
                Log.d("ForumViewModel", "Post liked successfully")
            }
        )
    }

    /**
     * Delete post
     */
    fun deletePost(token: String, postId: Long) {
        launchOperation(
            block = { repository.deletePost(token, postId) },
            onSuccess = {
                Log.d("ForumViewModel", "Post deleted successfully")
            }
        )
    }

    /**
     * Clean old cached data
     */
    fun cleanOldCache() {
        launchOperation(
            block = {
                repository.cleanOldCache()
                Result.success(Unit)
            },
            onSuccess = { }
        )
    }

}
