package com.nottingham.mynottingham.data.repository

import com.google.firebase.database.*
import com.nottingham.mynottingham.data.model.ForumComment
import com.nottingham.mynottingham.data.model.ForumPost
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase Forum Repository
 *
 * 直接从 Firebase Realtime Database 读取和管理论坛数据
 * 不再依赖 Spring Boot 后端 API
 *
 * Firebase 数据结构：
 * posts/{postId}/
 *   - authorId: string
 *   - authorName: string
 *   - authorAvatar: string (optional)
 *   - category: string
 *   - title: string
 *   - content: string
 *   - imageUrl: string (optional)
 *   - likes: number
 *   - comments: number
 *   - views: number
 *   - createdAt: timestamp
 *   - updatedAt: timestamp
 *
 * post_likes/{postId}/{userId}: true
 *
 * post_comments/{postId}/{commentId}/
 *   - authorId: string
 *   - authorName: string
 *   - authorAvatar: string (optional)
 *   - content: string
 *   - likes: number
 *   - createdAt: timestamp
 *
 * comment_likes/{commentId}/{userId}: true
 */
class FirebaseForumRepository {

    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    // 修正路径：数据库中实际使用 forum_posts 和 forum_comments
    private val postsRef: DatabaseReference = database.getReference("forum_posts")
    private val postLikesRef: DatabaseReference = database.getReference("forum_post_likes")
    private val postCommentsRef: DatabaseReference = database.getReference("forum_comments")
    private val commentLikesRef: DatabaseReference = database.getReference("forum_comment_likes")
    private val postViewsRef: DatabaseReference = database.getReference("forum_post_views")

    /**
     * 获取所有帖子（实时监听）
     * @param category 分类筛选（可选）
     * @return Flow<List<ForumPost>> 帖子列表流
     *
     * 🔴 修复：从 users 表动态获取作者头像
     */
    fun getPostsFlow(category: String? = null, currentUserId: String): Flow<List<ForumPost>> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // 使用协程来异步查询点赞状态和头像
                launch {
                    val posts = mutableListOf<ForumPost>()

                    snapshot.children.forEach { child ->
                        try {
                            val postId = child.key ?: return@forEach
                            val authorId = child.child("authorId").getValue(String::class.java) ?: ""
                            val authorName = child.child("authorName").getValue(String::class.java) ?: "Unknown"
                            // 优先从帖子读取头像，如果没有则从 users 表获取
                            var authorAvatar = child.child("authorAvatar").getValue(String::class.java)
                            val postCategory = child.child("category").getValue(String::class.java) ?: "GENERAL"
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val content = child.child("content").getValue(String::class.java) ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                            // 读取 tags 数组
                            val tags = child.child("tags").children.mapNotNull {
                                it.getValue(String::class.java)
                            }.takeIf { it.isNotEmpty() }
                            // 读取置顶状态
                            val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                            val pinnedAt = child.child("pinnedAt").getValue(Long::class.java)
                            val likes = child.child("likes").getValue(Int::class.java) ?: 0
                            val comments = child.child("comments").getValue(Int::class.java) ?: 0
                            val views = child.child("views").getValue(Int::class.java) ?: 0
                            val createdAt = child.child("createdAt").getValue(Long::class.java)
                                ?: child.child("timestamp").getValue(Long::class.java) ?: 0L
                            val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L

                            // 🔴 如果帖子没有头像字段，从 users 表获取
                            if (authorAvatar == null && authorId.isNotEmpty()) {
                                try {
                                    val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                    authorAvatar = userSnapshot.getValue(String::class.java)
                                } catch (e: Exception) {
                                    android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                                }
                            }

                            // 检查当前用户是否点赞（异步查询）
                            val isLiked = if (currentUserId.isNotEmpty()) {
                                isPostLiked(postId, currentUserId)
                            } else {
                                false
                            }

                            // 分类过滤
                            if (category == null || postCategory == category) {
                                posts.add(
                                    ForumPost(
                                        id = postId,
                                        authorId = authorId,
                                        authorName = authorName,
                                        authorAvatar = authorAvatar,
                                        category = postCategory,
                                        title = title,
                                        content = content,
                                        imageUrl = imageUrl,
                                        tags = tags,
                                        isPinned = isPinned,
                                        pinnedAt = pinnedAt,
                                        likes = likes,
                                        comments = comments,
                                        views = views,
                                        isLiked = isLiked,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseForumRepo", "Error parsing post: ${e.message}")
                        }
                    }

                    // 排序：置顶帖子优先（按置顶时间倒序），然后是普通帖子（按创建时间倒序）
                    val sortedPosts = posts.sortedWith(
                        compareByDescending<ForumPost> { it.isPinned }
                            .thenByDescending { if (it.isPinned) it.pinnedAt ?: 0L else it.createdAt }
                    )
                    trySend(sortedPosts)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseForumRepo", "Error listening to posts: ${error.message}")
                close(error.toException())
            }
        }

        postsRef.addValueEventListener(listener)

        awaitClose {
            postsRef.removeEventListener(listener)
        }
    }

    /**
     * 获取单个帖子详情（实时监听）
     * @param postId 帖子ID
     * @param currentUserId 当前用户ID
     * @return Flow<ForumPost?> 帖子详情流
     *
     * 🔴 修复：从 users 表动态获取作者头像
     */
    fun getPostDetailFlow(postId: String, currentUserId: String): Flow<ForumPost?> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                // 使用协程来异步查询点赞状态和头像
                launch {
                    try {
                        val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = snapshot.child("authorName").getValue(String::class.java) ?: "Unknown"
                        var authorAvatar = snapshot.child("authorAvatar").getValue(String::class.java)
                        val category = snapshot.child("category").getValue(String::class.java) ?: "GENERAL"
                        val title = snapshot.child("title").getValue(String::class.java) ?: ""
                        val content = snapshot.child("content").getValue(String::class.java) ?: ""
                        val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                        // 读取 tags 数组
                        val tags = snapshot.child("tags").children.mapNotNull {
                            it.getValue(String::class.java)
                        }.takeIf { it.isNotEmpty() }
                        // 读取置顶状态
                        val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false
                        val pinnedAt = snapshot.child("pinnedAt").getValue(Long::class.java)
                        val likes = snapshot.child("likes").getValue(Int::class.java) ?: 0
                        val comments = snapshot.child("comments").getValue(Int::class.java) ?: 0
                        val views = snapshot.child("views").getValue(Int::class.java) ?: 0
                        val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                            ?: snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L

                        // 🔴 如果帖子没有头像字段，从 users 表获取
                        if (authorAvatar == null && authorId.isNotEmpty()) {
                            try {
                                val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                authorAvatar = userSnapshot.getValue(String::class.java)
                            } catch (e: Exception) {
                                android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                            }
                        }

                        // 检查当前用户是否点赞（异步查询）
                        val isLiked = if (currentUserId.isNotEmpty()) {
                            isPostLiked(postId, currentUserId)
                        } else {
                            false
                        }

                        trySend(
                            ForumPost(
                                id = postId,
                                authorId = authorId,
                                authorName = authorName,
                                authorAvatar = authorAvatar,
                                category = category,
                                title = title,
                                content = content,
                                imageUrl = imageUrl,
                                tags = tags,
                                isPinned = isPinned,
                                pinnedAt = pinnedAt,
                                likes = likes,
                                comments = comments,
                                views = views,
                                isLiked = isLiked,
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseForumRepo", "Error parsing post detail: ${e.message}")
                        trySend(null)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseForumRepo", "Error listening to post detail: ${error.message}")
                close(error.toException())
            }
        }

        postsRef.child(postId).addValueEventListener(listener)

        awaitClose {
            postsRef.child(postId).removeEventListener(listener)
        }
    }

    /**
     * 获取帖子的评论列表（实时监听）
     * @param postId 帖子ID
     * @param currentUserId 当前用户ID
     * @return Flow<List<ForumComment>> 评论列表流
     *
     * 🔴 修复：从 users 表动态获取作者头像
     */
    fun getCommentsFlow(postId: String, currentUserId: String): Flow<List<ForumComment>> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // 使用协程来异步查询点赞状态和头像
                launch {
                    val comments = mutableListOf<ForumComment>()

                    snapshot.children.forEach { child ->
                        try {
                            val commentId = child.key ?: return@forEach
                            val authorId = child.child("authorId").getValue(String::class.java) ?: ""
                            val authorName = child.child("authorName").getValue(String::class.java) ?: "Unknown"
                            var authorAvatar = child.child("authorAvatar").getValue(String::class.java)
                            val content = child.child("content").getValue(String::class.java) ?: ""
                            val likes = child.child("likes").getValue(Int::class.java) ?: 0
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L

                            // 🔴 如果评论没有头像字段，从 users 表获取
                            if (authorAvatar == null && authorId.isNotEmpty()) {
                                try {
                                    val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                    authorAvatar = userSnapshot.getValue(String::class.java)
                                } catch (e: Exception) {
                                    android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                                }
                            }

                            // 检查当前用户是否点赞了该评论（异步查询）
                            val isLiked = if (currentUserId.isNotEmpty()) {
                                isCommentLiked(commentId, currentUserId)
                            } else {
                                false
                            }

                            comments.add(
                                ForumComment(
                                    id = commentId,
                                    postId = postId,
                                    authorId = authorId,
                                    authorName = authorName,
                                    authorAvatar = authorAvatar,
                                    content = content,
                                    likes = likes,
                                    isLiked = isLiked,
                                    createdAt = createdAt
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseForumRepo", "Error parsing comment: ${e.message}")
                        }
                    }

                    // 按时间正序排列（旧的在前）
                    comments.sortBy { it.createdAt }
                    trySend(comments)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseForumRepo", "Error listening to comments: ${error.message}")
                close(error.toException())
            }
        }

        postCommentsRef.child(postId).addValueEventListener(listener)

        awaitClose {
            postCommentsRef.child(postId).removeEventListener(listener)
        }
    }

    /**
     * 创建新帖子
     * @param authorId 作者ID
     * @param authorName 作者名称
     * @param authorAvatar 作者头像
     * @param category 分类
     * @param title 标题
     * @param content 内容
     * @param imageUrl 图片URL（可选）
     * @return Result<String> 帖子ID或错误
     */
    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorAvatar: String?,
        category: String,
        title: String,
        content: String,
        imageUrl: String? = null,
        tags: List<String>? = null,
        isPinned: Boolean = false
    ): Result<String> {
        return try {
            val newPostRef = postsRef.push()
            val postId = newPostRef.key ?: throw Exception("Failed to generate post ID")
            val timestamp = System.currentTimeMillis()

            val postData = mutableMapOf<String, Any>(
                "authorId" to authorId,
                "authorName" to authorName,
                "category" to category,
                "title" to title,
                "content" to content,
                "likes" to 0,
                "comments" to 0,
                "views" to 0,
                "createdAt" to timestamp,
                "updatedAt" to timestamp,
                "isPinned" to isPinned
            )

            if (authorAvatar != null) {
                postData["authorAvatar"] = authorAvatar
            }

            if (imageUrl != null) {
                postData["imageUrl"] = imageUrl
            }

            if (!tags.isNullOrEmpty()) {
                postData["tags"] = tags
            }

            if (isPinned) {
                postData["pinnedAt"] = timestamp
            }

            newPostRef.setValue(postData).await()

            Result.success(postId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error creating post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 更新帖子
     * @param postId 帖子ID
     * @param title 新标题
     * @param content 新内容
     * @return Result<Unit> 成功或错误
     */
    suspend fun updatePost(
        postId: String,
        title: String,
        content: String,
        category: String? = null,
        tags: List<String>? = null,
        isPinned: Boolean? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "title" to title,
                "content" to content,
                "updatedAt" to System.currentTimeMillis()
            )

            if (category != null) {
                updates["category"] = category
            }

            if (tags != null) {
                updates["tags"] = tags
            }

            if (isPinned != null) {
                updates["isPinned"] = isPinned
                if (isPinned) {
                    // 设置置顶时间（如果之前没有置顶时间，则设置为当前时间）
                    updates["pinnedAt"] = System.currentTimeMillis()
                }
            }

            postsRef.child(postId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error updating post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 删除帖子
     * @param postId 帖子ID
     * @return Result<Unit> 成功或错误
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // 删除帖子
            postsRef.child(postId).removeValue().await()

            // 删除帖子的点赞记录
            postLikesRef.child(postId).removeValue().await()

            // 删除帖子的评论
            postCommentsRef.child(postId).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error deleting post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 点赞/取消点赞帖子
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return Result<Boolean> 是否已点赞
     */
    suspend fun toggleLikePost(postId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = postLikesRef.child(postId).child(userId)
            val snapshot = likeRef.get().await()

            if (snapshot.exists()) {
                // 取消点赞
                likeRef.removeValue().await()

                // 减少点赞数
                val postRef = postsRef.child(postId).child("likes")
                val currentLikes = postRef.get().await().getValue(Int::class.java) ?: 0
                postRef.setValue(maxOf(0, currentLikes - 1)).await()

                Result.success(false)
            } else {
                // 点赞
                likeRef.setValue(true).await()

                // 增加点赞数
                val postRef = postsRef.child(postId).child("likes")
                val currentLikes = postRef.get().await().getValue(Int::class.java) ?: 0
                postRef.setValue(currentLikes + 1).await()

                Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error toggling like: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 创建评论
     * @param postId 帖子ID
     * @param authorId 作者ID
     * @param authorName 作者名称
     * @param authorAvatar 作者头像
     * @param content 评论内容
     * @return Result<String> 评论ID或错误
     */
    suspend fun createComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatar: String?,
        content: String
    ): Result<String> {
        return try {
            val newCommentRef = postCommentsRef.child(postId).push()
            val commentId = newCommentRef.key ?: throw Exception("Failed to generate comment ID")
            val timestamp = System.currentTimeMillis()

            val commentData = mutableMapOf<String, Any>(
                "authorId" to authorId,
                "authorName" to authorName,
                "content" to content,
                "likes" to 0,
                "createdAt" to timestamp
            )

            if (authorAvatar != null) {
                commentData["authorAvatar"] = authorAvatar
            }

            newCommentRef.setValue(commentData).await()

            // 增加帖子的评论计数
            val postCommentsCountRef = postsRef.child(postId).child("comments")
            val currentCount = postCommentsCountRef.get().await().getValue(Int::class.java) ?: 0
            postCommentsCountRef.setValue(currentCount + 1).await()

            Result.success(commentId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error creating comment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 点赞/取消点赞评论
     * @param commentId 评论ID
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return Result<Boolean> 是否已点赞
     */
    suspend fun toggleLikeComment(commentId: String, postId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = commentLikesRef.child(commentId).child(userId)
            val snapshot = likeRef.get().await()

            if (snapshot.exists()) {
                // 取消点赞
                likeRef.removeValue().await()

                // 减少点赞数
                val commentRef = postCommentsRef.child(postId).child(commentId).child("likes")
                val currentLikes = commentRef.get().await().getValue(Int::class.java) ?: 0
                commentRef.setValue(maxOf(0, currentLikes - 1)).await()

                Result.success(false)
            } else {
                // 点赞
                likeRef.setValue(true).await()

                // 增加点赞数
                val commentRef = postCommentsRef.child(postId).child(commentId).child("likes")
                val currentLikes = commentRef.get().await().getValue(Int::class.java) ?: 0
                commentRef.setValue(currentLikes + 1).await()

                Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error toggling comment like: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 增加帖子浏览量（每个用户只计算一次）
     * @param postId 帖子ID
     * @param userId 用户ID
     */
    suspend fun incrementViews(postId: String, userId: String): Result<Unit> {
        if (userId.isEmpty()) {
            return Result.success(Unit) // 未登录用户不计算浏览量
        }

        return try {
            val userViewRef = postViewsRef.child(postId).child(userId)
            val hasViewed = userViewRef.get().await().exists()

            // 只有首次访问才增加浏览量
            if (!hasViewed) {
                // 标记该用户已访问
                userViewRef.setValue(true).await()

                // 增加浏览量计数
                val viewsRef = postsRef.child(postId).child("views")
                val currentViews = viewsRef.get().await().getValue(Int::class.java) ?: 0
                viewsRef.setValue(currentViews + 1).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error incrementing views: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 检查用户是否点赞了帖子
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return Boolean 是否点赞
     */
    suspend fun isPostLiked(postId: String, userId: String): Boolean {
        return try {
            postLikesRef.child(postId).child(userId).get().await().exists()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error checking post like: ${e.message}")
            false
        }
    }

    /**
     * 检查用户是否点赞了评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return Boolean 是否点赞
     */
    suspend fun isCommentLiked(commentId: String, userId: String): Boolean {
        return try {
            commentLikesRef.child(commentId).child(userId).get().await().exists()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error checking comment like: ${e.message}")
            false
        }
    }
}
