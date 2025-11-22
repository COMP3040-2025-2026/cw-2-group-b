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

    fun getTrendingPostsFlow(): Flow<List<ForumPostEntity>> {
        return forumDao.getTrendingPosts()
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
        return networkBoundResource(
            apiCall = { apiService.getForumPosts("Bearer $token", page, size, category) },
            saveFetchResult = { data ->
                val entities = data.posts.map { it.toEntity() }
                forumDao.insertPosts(entities)
            }
        )
    }

    /**
     * Get post by ID with comments
     */
    suspend fun getPostDetail(token: String, postId: Long): Result<ForumPostDetailDto> {
        return networkBoundResource(
            apiCall = { apiService.getForumPostById("Bearer $token", postId) },
            saveFetchResult = { data ->
                forumDao.insertPost(data.post.toEntity())
                val commentEntities = data.comments.map { it.toEntity() }
                forumDao.insertComments(commentEntities)
            }
        )
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
     * (Special handling due to multipart request with logging)
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

                if (response.isSuccessful && response.body()?.success == true) {
                    val post = response.body()?.data!!
                    // Cache new post using extension function
                    forumDao.insertPost(post.toEntity())
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
        return networkBoundResource(
            apiCall = { apiService.updateForumPost("Bearer $token", postId, request) },
            saveFetchResult = { post ->
                forumDao.insertPost(post.toEntity())
            }
        )
    }

    /**
     * Delete post
     */
    suspend fun deletePost(token: String, postId: Long): Result<Unit> {
        return networkBoundResourceNoData(
            apiCall = { apiService.deleteForumPost("Bearer $token", postId) },
            onSuccess = {
                forumDao.deletePostById(postId)
                forumDao.deleteCommentsByPostId(postId)
            }
        )
    }

    /**
     * Like/unlike post
     */
    suspend fun likePost(token: String, postId: Long): Result<ForumPostDto> {
        return networkBoundResource(
            apiCall = { apiService.likeForumPost("Bearer $token", postId) },
            saveFetchResult = { post ->
                forumDao.updatePostLikeStatus(postId, post.likes, post.isLikedByCurrentUser)
            }
        )
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
        return networkBoundResource(
            apiCall = { apiService.createForumComment("Bearer $token", postId, request) },
            saveFetchResult = { comment ->
                forumDao.insertComment(comment.toEntity())
            }
        )
    }

    /**
     * Like/unlike comment
     */
    suspend fun likeComment(token: String, commentId: Long): Result<ForumCommentDto> {
        return networkBoundResource(
            apiCall = { apiService.likeForumComment("Bearer $token", commentId) },
            saveFetchResult = { comment ->
                forumDao.updateCommentLikeStatus(commentId, comment.likes, comment.isLikedByCurrentUser)
            }
        )
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

}
