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
 * ✅ Migrated to Firebase - real-time post updates
 */
class ForumViewModel(application: Application) : BaseViewModel(application) {

    // ✅ 替换为 Firebase Repository
    private val repository = FirebaseForumRepository()
    private val tokenManager = TokenManager(application)

    private val _currentCategory = MutableStateFlow<String?>(null)
    private val _currentUserId = MutableStateFlow<String>("")

    init {
        // 初始化时获取用户 ID
        viewModelScope.launch {
            tokenManager.getUserId().collect { uid ->
                _currentUserId.value = uid ?: ""
            }
        }
    }

    // ✅ 核心修改：组合 currentCategory 和 currentUserId 来生成帖子流
    // Firebase 返回 ForumPost 模型（不再使用 Room 的 ForumPostEntity）
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: Flow<List<ForumPost>> = combine(_currentCategory, _currentUserId) { category, userId ->
        Pair(category, userId)
    }.flatMapLatest { (category, userId) ->
        if (userId.isEmpty()) flowOf(emptyList())
        else repository.getPostsFlow(category, userId)
    }

    /**
     * ✅ 修改：Firebase 是实时的，不需要手动 loadPosts
     * 这个方法现在主要用于处理"刷新"动作（虽然 Flow 会自动更新，但保留接口兼容性）
     */
    fun loadPosts(token: String, category: String? = null, refresh: Boolean = false) {
        // Firebase Flow 自动处理数据，这里可以留空，或者重置一些 UI 状态
        if (category != _currentCategory.value) {
            filterByCategory(category)
        }
    }

    fun filterByCategory(category: String?) {
        _currentCategory.value = category
    }

    // ✅ 保留兼容旧代码的 Long ID 方法
    fun likePost(token: String, postId: Long) {
        likePost(postId.toString())
    }

    // 新方法适配 Firebase String ID
    fun likePost(postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(postId, _currentUserId.value)
        }
    }

    // ✅ 保留兼容旧代码的 Long ID 方法
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
        // Firebase 不需要手动清理本地缓存，SDK 会处理
    }
}
