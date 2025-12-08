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
 * Reads and manages forum data directly from Firebase Realtime Database.
 * No longer depends on Spring Boot backend API.
 *
 * Firebase data structure:
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
    // Note: Database uses forum_posts and forum_comments as actual paths
    private val postsRef: DatabaseReference = database.getReference("forum_posts")
    private val postLikesRef: DatabaseReference = database.getReference("forum_post_likes")
    private val postCommentsRef: DatabaseReference = database.getReference("forum_comments")
    private val commentLikesRef: DatabaseReference = database.getReference("forum_comment_likes")
    private val postViewsRef: DatabaseReference = database.getReference("forum_post_views")

    /**
     * Get all posts (real-time listener)
     * @param category Category filter (optional)
     * @return Flow<List<ForumPost>> Post list flow
     *
     * Fix: Dynamically fetch author avatar from users table
     */
    fun getPostsFlow(category: String? = null, currentUserId: String): Flow<List<ForumPost>> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // Use coroutine for async like status and avatar queries
                launch {
                    val posts = mutableListOf<ForumPost>()

                    snapshot.children.forEach { child ->
                        try {
                            val postId = child.key ?: return@forEach
                            val authorId = child.child("authorId").getValue(String::class.java) ?: ""
                            val authorName = child.child("authorName").getValue(String::class.java) ?: "Unknown"
                            // Prefer reading avatar from post, fallback to users table
                            var authorAvatar = child.child("authorAvatar").getValue(String::class.java)
                            val postCategory = child.child("category").getValue(String::class.java) ?: "GENERAL"
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val content = child.child("content").getValue(String::class.java) ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                            // Read tags array
                            val tags = child.child("tags").children.mapNotNull {
                                it.getValue(String::class.java)
                            }.takeIf { it.isNotEmpty() }
                            // Read pinned status
                            val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                            val pinnedAt = child.child("pinnedAt").getValue(Long::class.java)
                            val likes = child.child("likes").getValue(Int::class.java) ?: 0
                            val comments = child.child("comments").getValue(Int::class.java) ?: 0
                            val views = child.child("views").getValue(Int::class.java) ?: 0
                            val createdAt = child.child("createdAt").getValue(Long::class.java)
                                ?: child.child("timestamp").getValue(Long::class.java) ?: 0L
                            val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L

                            // If post has no avatar field, fetch from users table
                            if (authorAvatar == null && authorId.isNotEmpty()) {
                                try {
                                    val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                    authorAvatar = userSnapshot.getValue(String::class.java)
                                } catch (e: Exception) {
                                    android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                                }
                            }

                            // Check if current user liked (async query)
                            val isLiked = if (currentUserId.isNotEmpty()) {
                                isPostLiked(postId, currentUserId)
                            } else {
                                false
                            }

                            // Category filter (TRENDING is special - include all posts)
                            val matchesCategory = category == null ||
                                category == "TRENDING" ||
                                postCategory == category

                            if (matchesCategory) {
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

                    // Sort based on category type
                    val sortedPosts = if (category == "TRENDING") {
                        // Trending algorithm: score = (likes*5 + comments*3 + views) / (1 + daysSinceCreated * 0.3)
                        val now = System.currentTimeMillis()
                        posts.sortedByDescending { post ->
                            val daysSinceCreated = (now - post.createdAt) / (1000.0 * 60 * 60 * 24)
                            val engagementScore = post.likes * 5.0 + post.comments * 3.0 + post.views
                            // Time decay factor: newer posts get higher scores
                            engagementScore / (1.0 + daysSinceCreated * 0.3)
                        }
                    } else {
                        // Default: pinned posts first (by pinned time desc), then normal posts (by created time desc)
                        posts.sortedWith(
                            compareByDescending<ForumPost> { it.isPinned }
                                .thenByDescending { if (it.isPinned) it.pinnedAt ?: 0L else it.createdAt }
                        )
                    }
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
     * Get single post details (real-time listener)
     * @param postId Post ID
     * @param currentUserId Current user ID
     * @return Flow<ForumPost?> Post details flow
     *
     * Fix: Dynamically fetch author avatar from users table
     */
    fun getPostDetailFlow(postId: String, currentUserId: String): Flow<ForumPost?> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                // Use coroutine for async like status and avatar queries
                launch {
                    try {
                        val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = snapshot.child("authorName").getValue(String::class.java) ?: "Unknown"
                        var authorAvatar = snapshot.child("authorAvatar").getValue(String::class.java)
                        val category = snapshot.child("category").getValue(String::class.java) ?: "GENERAL"
                        val title = snapshot.child("title").getValue(String::class.java) ?: ""
                        val content = snapshot.child("content").getValue(String::class.java) ?: ""
                        val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                        // Read tags array
                        val tags = snapshot.child("tags").children.mapNotNull {
                            it.getValue(String::class.java)
                        }.takeIf { it.isNotEmpty() }
                        // Read pinned status
                        val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false
                        val pinnedAt = snapshot.child("pinnedAt").getValue(Long::class.java)
                        val likes = snapshot.child("likes").getValue(Int::class.java) ?: 0
                        val comments = snapshot.child("comments").getValue(Int::class.java) ?: 0
                        val views = snapshot.child("views").getValue(Int::class.java) ?: 0
                        val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                            ?: snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L

                        // If post has no avatar field, fetch from users table
                        if (authorAvatar == null && authorId.isNotEmpty()) {
                            try {
                                val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                authorAvatar = userSnapshot.getValue(String::class.java)
                            } catch (e: Exception) {
                                android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                            }
                        }

                        // Check if current user liked (async query)
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
     * Get post's comment list (real-time listener)
     * @param postId Post ID
     * @param currentUserId Current user ID
     * @return Flow<List<ForumComment>> Comment list flow
     *
     * Fix: Dynamically fetch author avatar from users table
     */
    fun getCommentsFlow(postId: String, currentUserId: String): Flow<List<ForumComment>> = callbackFlow {
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                // Use coroutine for async like status and avatar queries
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
                            val isPinned = child.child("isPinned").getValue(Boolean::class.java) ?: false
                            val pinnedAt = child.child("pinnedAt").getValue(Long::class.java)
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L

                            // If comment has no avatar field, fetch from users table
                            if (authorAvatar == null && authorId.isNotEmpty()) {
                                try {
                                    val userSnapshot = usersRef.child(authorId).child("profileImageUrl").get().await()
                                    authorAvatar = userSnapshot.getValue(String::class.java)
                                } catch (e: Exception) {
                                    android.util.Log.w("FirebaseForumRepo", "Failed to fetch avatar for $authorId")
                                }
                            }

                            // Check if current user liked this comment (async query)
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
                                    isPinned = isPinned,
                                    pinnedAt = pinnedAt,
                                    createdAt = createdAt
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseForumRepo", "Error parsing comment: ${e.message}")
                        }
                    }

                    // Sort: pinned comments first (by pinned time desc), then by creation time ascending
                    val sortedComments = comments.sortedWith(
                        compareByDescending<ForumComment> { it.isPinned }
                            .thenByDescending { if (it.isPinned) it.pinnedAt ?: 0L else 0L }
                            .thenBy { it.createdAt }
                    )
                    trySend(sortedComments)
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
     * Create new post
     * @param authorId Author ID
     * @param authorName Author name
     * @param authorAvatar Author avatar
     * @param category Category
     * @param title Title
     * @param content Content
     * @param imageUrl Image URL (optional)
     * @return Result<String> Post ID or error
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
     * Update post
     * @param postId Post ID
     * @param title New title
     * @param content New content
     * @return Result<Unit> Success or error
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
                    // Set pinned time (if no previous pinned time, set to current time)
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
     * Delete post
     * @param postId Post ID
     * @return Result<Unit> Success or error
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Delete post
            postsRef.child(postId).removeValue().await()

            // Delete post's like records
            postLikesRef.child(postId).removeValue().await()

            // Delete post's comments
            postCommentsRef.child(postId).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error deleting post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Like/unlike post
     * @param postId Post ID
     * @param userId User ID
     * @return Result<Boolean> Whether liked
     */
    suspend fun toggleLikePost(postId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = postLikesRef.child(postId).child(userId)
            val snapshot = likeRef.get().await()

            if (snapshot.exists()) {
                // Unlike
                likeRef.removeValue().await()

                // Decrease like count
                val postRef = postsRef.child(postId).child("likes")
                val currentLikes = postRef.get().await().getValue(Int::class.java) ?: 0
                postRef.setValue(maxOf(0, currentLikes - 1)).await()

                Result.success(false)
            } else {
                // Like
                likeRef.setValue(true).await()

                // Increase like count
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
     * Create comment
     * @param postId Post ID
     * @param authorId Author ID
     * @param authorName Author name
     * @param authorAvatar Author avatar
     * @param content Comment content
     * @return Result<String> Comment ID or error
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

            // Increase post's comment count
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
     * Like/unlike comment
     * @param commentId Comment ID
     * @param postId Post ID
     * @param userId User ID
     * @return Result<Boolean> Whether liked
     */
    suspend fun toggleLikeComment(commentId: String, postId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = commentLikesRef.child(commentId).child(userId)
            val snapshot = likeRef.get().await()

            if (snapshot.exists()) {
                // Unlike
                likeRef.removeValue().await()

                // Decrease like count
                val commentRef = postCommentsRef.child(postId).child(commentId).child("likes")
                val currentLikes = commentRef.get().await().getValue(Int::class.java) ?: 0
                commentRef.setValue(maxOf(0, currentLikes - 1)).await()

                Result.success(false)
            } else {
                // Like
                likeRef.setValue(true).await()

                // Increase like count
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
     * Delete comment
     * @param postId Post ID
     * @param commentId Comment ID
     * @return Result<Unit> Success or error
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            // Delete comment
            postCommentsRef.child(postId).child(commentId).removeValue().await()

            // Delete comment's like records
            commentLikesRef.child(commentId).removeValue().await()

            // Decrease post's comment count
            val postCommentsCountRef = postsRef.child(postId).child("comments")
            val currentCount = postCommentsCountRef.get().await().getValue(Int::class.java) ?: 0
            postCommentsCountRef.setValue(maxOf(0, currentCount - 1)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error deleting comment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Pin/unpin comment (only post author can do this)
     * @param postId Post ID
     * @param commentId Comment ID
     * @param isPinned Whether to pin
     * @return Result<Boolean> New pinned status
     */
    suspend fun togglePinComment(postId: String, commentId: String): Result<Boolean> {
        return try {
            val commentRef = postCommentsRef.child(postId).child(commentId)
            val currentPinned = commentRef.child("isPinned").get().await().getValue(Boolean::class.java) ?: false

            val updates = mutableMapOf<String, Any?>(
                "isPinned" to !currentPinned
            )

            if (!currentPinned) {
                // Pinning: set pinnedAt timestamp
                updates["pinnedAt"] = System.currentTimeMillis()
            } else {
                // Unpinning: remove pinnedAt
                updates["pinnedAt"] = null
            }

            commentRef.updateChildren(updates).await()
            Result.success(!currentPinned)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseForumRepo", "Error toggling pin comment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Increment post view count (counted once per user)
     * @param postId Post ID
     * @param userId User ID
     */
    suspend fun incrementViews(postId: String, userId: String): Result<Unit> {
        if (userId.isEmpty()) {
            return Result.success(Unit) // Logged-out users don't count towards views
        }

        return try {
            val userViewRef = postViewsRef.child(postId).child(userId)
            val hasViewed = userViewRef.get().await().exists()

            // Only increment on first visit
            if (!hasViewed) {
                // Mark user as having viewed
                userViewRef.setValue(true).await()

                // Increment view count
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
     * Check if user liked a post
     * @param postId Post ID
     * @param userId User ID
     * @return Boolean Whether liked
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
     * Check if user liked a comment
     * @param commentId Comment ID
     * @param userId User ID
     * @return Boolean Whether liked
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
