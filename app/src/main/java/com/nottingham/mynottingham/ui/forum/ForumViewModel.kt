package com.nottingham.mynottingham.ui.forum

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.ForumPost
import com.nottingham.mynottingham.data.repository.FirebaseForumRepository
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.ui.base.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Forum (post list) screen
 * Migrated to Firebase - real-time post updates
 */
class ForumViewModel(application: Application) : BaseViewModel(application) {

    // Replaced with Firebase Repository
    private val repository = FirebaseForumRepository()
    private val tokenManager = TokenManager(application)

    private val _currentCategory = MutableStateFlow<String?>(null)
    private val _currentUserId = MutableStateFlow<String>("")

    init {
        // Get user ID during initialization
        viewModelScope.launch {
            tokenManager.getUserId().collect { uid ->
                _currentUserId.value = uid ?: ""
            }
        }
    }

    // Core change: Combine currentCategory and currentUserId to generate post flow
    // Firebase returns ForumPost model (no longer using Room's ForumPostEntity)
    // Note: Forum posts are public, should be able to load even if userId is empty (userId is only used to check like status)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: Flow<List<ForumPost>> = combine(_currentCategory, _currentUserId) { category, userId ->
        Pair(category, userId)
    }.flatMapLatest { (category, userId) ->
        // Load posts even if userId is empty (public content), userId is only used to check like status
        repository.getPostsFlow(category, userId)
    }

    /**
     * Change: Firebase is real-time, no need to manually call loadPosts
     * This method is now mainly used to handle "refresh" action (Flow will auto-update, but keep interface compatibility)
     */
    fun loadPosts(token: String, category: String? = null, refresh: Boolean = false) {
        // Firebase Flow handles data automatically, can be empty here or reset some UI state
        if (category != _currentCategory.value) {
            filterByCategory(category)
        }
    }

    fun filterByCategory(category: String?) {
        _currentCategory.value = category
    }

    // Keep backward compatibility with Long ID method
    fun likePost(token: String, postId: Long) {
        likePost(postId.toString())
    }

    // New method adapted for Firebase String ID
    fun likePost(postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(postId, _currentUserId.value)
        }
    }

    // Keep backward compatibility with Long ID method
    fun deletePost(token: String, postId: Long) {
         deletePost(postId.toString())
    }

    fun deletePost(postId: String) {
        launchOperation(
            block = { repository.deletePost(postId) },
            onSuccess = { }
        )
    }

    fun cleanOldCache() {
        // Firebase doesn't need manual local cache cleanup, SDK handles it
    }
}
