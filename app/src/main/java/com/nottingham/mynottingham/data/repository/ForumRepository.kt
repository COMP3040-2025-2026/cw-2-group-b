package com.nottingham.mynottingham.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.local.database.entities.ForumCommentEntity
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for Forum operations
 * Coordinates between local database (Room) and remote API
 */
class ForumRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val forumDao = database.forumDao()
    private val apiService = RetrofitInstance.apiService
    private val gson = Gson()

    // ========== Post Operations ==========

    /**
     * Get all posts from local database as Flow
     */
    fun getPostsFlow(category: String? = null): Flow<List<ForumPostEntity>> {
        return if (category != null) {
            forumDao.getPostsByCategory(category)
        } else {
            forumDao.getAllPosts()
        }
    }

    /**
     * Fetch posts from API and cache in local database
     */
    suspend fun fetchPosts(
        token: String,
        page: Int = 0,
        size: Int = 20,
        category: String? = null
    ): Result<PagedForumResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForumPosts("Bearer $token", page, size, category)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data!!

                    // Cache posts to local database
                    val entities = data.posts.map { dtoToPostEntity(it) }
                    forumDao.insertPosts(entities)

                    Result.success(data)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to fetch posts"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get post by ID with comments
     */
    suspend fun getPostDetail(token: String, postId: Long): Result<ForumPostDetailDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForumPostById("Bearer $token", postId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data!!

                    // Cache post and comments
                    forumDao.insertPost(dtoToPostEntity(data.post))
                    val commentEntities = data.comments.map { dtoToCommentEntity(it) }
                    forumDao.insertComments(commentEntities)

                    Result.success(data)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to fetch post"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get post by ID as Flow from local database
     */
    fun getPostByIdFlow(postId: Long): Flow<ForumPostEntity?> {
        return forumDao.getPostByIdFlow(postId)
    }

    /**
     * Get comments for a post from local database
     */
    fun getCommentsFlow(postId: Long): Flow<List<ForumCommentEntity>> {
        return forumDao.getCommentsByPostId(postId)
    }

    /**
     * Create new post
     */
    suspend fun createPost(
        token: String,
        request: CreateForumPostRequest,
        image: okhttp3.MultipartBody.Part? = null
    ): Result<ForumPostDto> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert request to JSON RequestBody
                val requestJson = gson.toJson(request)
                Log.d("ForumRepository", "Request JSON: $requestJson")
                val requestBody = requestJson.toRequestBody("application/json".toMediaTypeOrNull())

                Log.d("ForumRepository", "Calling API with token: Bearer ${token.take(20)}...")
                val response = apiService.createForumPost("Bearer $token", requestBody, image)

                Log.d("ForumRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                Log.d("ForumRepository", "Response body: ${response.body()}")
                Log.d("ForumRepository", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val post = response.body()?.data!!

                    // Cache new post
                    forumDao.insertPost(dtoToPostEntity(post))

                    Result.success(post)
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to create post"
                    Log.e("ForumRepository", "Failed to create post: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ForumRepository", "Exception creating post", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update existing post
     */
    suspend fun updatePost(
        token: String,
        postId: Long,
        request: UpdateForumPostRequest
    ): Result<ForumPostDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateForumPost("Bearer $token", postId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val post = response.body()?.data!!

                    // Update cache
                    forumDao.insertPost(dtoToPostEntity(post))

                    Result.success(post)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to update post"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete post
     */
    suspend fun deletePost(token: String, postId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteForumPost("Bearer $token", postId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Remove from cache
                    forumDao.deletePostById(postId)
                    forumDao.deleteCommentsByPostId(postId)

                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to delete post"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Like/unlike post
     */
    suspend fun likePost(token: String, postId: Long): Result<ForumPostDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.likeForumPost("Bearer $token", postId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val post = response.body()?.data!!

                    // Update cache
                    forumDao.updatePostLikeStatus(postId, post.likes, post.isLikedByCurrentUser)

                    Result.success(post)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to like post"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========== Comment Operations ==========

    /**
     * Create comment on post
     */
    suspend fun createComment(
        token: String,
        postId: Long,
        request: CreateCommentRequest
    ): Result<ForumCommentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createForumComment("Bearer $token", postId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val comment = response.body()?.data!!

                    // Cache new comment
                    forumDao.insertComment(dtoToCommentEntity(comment))

                    Result.success(comment)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to create comment"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Like/unlike comment
     */
    suspend fun likeComment(token: String, commentId: Long): Result<ForumCommentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.likeForumComment("Bearer $token", commentId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val comment = response.body()?.data!!

                    // Update cache
                    forumDao.updateCommentLikeStatus(commentId, comment.likes, comment.isLikedByCurrentUser)

                    Result.success(comment)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to like comment"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clean old cached data (7 days)
     */
    suspend fun cleanOldCache() {
        withContext(Dispatchers.IO) {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            forumDao.deleteOldPosts(sevenDaysAgo)
            forumDao.deleteOldComments(sevenDaysAgo)
        }
    }

    // ========== Helper Functions ==========

    /**
     * Convert DTO to Entity
     */
    private fun dtoToPostEntity(dto: ForumPostDto): ForumPostEntity {
        return ForumPostEntity(
            id = dto.id,
            authorId = dto.authorId,
            authorName = dto.authorName,
            authorAvatar = dto.authorAvatar,
            title = dto.title,
            content = dto.content,
            category = dto.category,
            imageUrl = dto.imageUrl,
            likes = dto.likes,
            comments = dto.comments,
            views = dto.views,
            isPinned = dto.isPinned,
            isLocked = dto.isLocked,
            isLikedByCurrentUser = dto.isLikedByCurrentUser,
            tags = dto.tags?.joinToString(","),
            createdAt = parseTimestamp(dto.createdAt),
            updatedAt = parseTimestamp(dto.updatedAt)
        )
    }

    private fun dtoToCommentEntity(dto: ForumCommentDto): ForumCommentEntity {
        return ForumCommentEntity(
            id = dto.id,
            postId = dto.postId,
            authorId = dto.authorId,
            authorName = dto.authorName,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            likes = dto.likes,
            isLikedByCurrentUser = dto.isLikedByCurrentUser,
            createdAt = parseTimestamp(dto.createdAt)
        )
    }

    /**
     * Parse ISO 8601 timestamp string to millis
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            ZonedDateTime.parse(timestamp).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
