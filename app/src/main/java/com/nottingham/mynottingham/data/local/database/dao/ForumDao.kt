package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.ForumCommentEntity
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Forum operations
 * Supports posts, comments, likes, and category filtering
 */
@Dao
interface ForumDao {

    // ========== Post Operations ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: ForumPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<ForumPostEntity>)

    @Update
    suspend fun updatePost(post: ForumPostEntity)

    @Query("SELECT * FROM forum_posts ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPosts(): Flow<List<ForumPostEntity>>

    @Query("SELECT * FROM forum_posts WHERE category = :category ORDER BY isPinned DESC, createdAt DESC")
    fun getPostsByCategory(category: String): Flow<List<ForumPostEntity>>

    @Query("SELECT * FROM forum_posts WHERE id = :postId")
    suspend fun getPostById(postId: Long): ForumPostEntity?

    @Query("SELECT * FROM forum_posts WHERE id = :postId")
    fun getPostByIdFlow(postId: Long): Flow<ForumPostEntity?>

    @Query("UPDATE forum_posts SET likes = :likes, isLikedByCurrentUser = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: Long, likes: Int, isLiked: Boolean)

    @Query("UPDATE forum_posts SET views = :views WHERE id = :postId")
    suspend fun updatePostViews(postId: Long, views: Int)

    @Query("DELETE FROM forum_posts WHERE id = :postId")
    suspend fun deletePostById(postId: Long)

    @Query("DELETE FROM forum_posts WHERE cachedAt < :timestamp")
    suspend fun deleteOldPosts(timestamp: Long)

    @Query("DELETE FROM forum_posts")
    suspend fun deleteAllPosts()

    // ========== Comment Operations ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ForumCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<ForumCommentEntity>)

    @Update
    suspend fun updateComment(comment: ForumCommentEntity)

    @Query("SELECT * FROM forum_comments WHERE postId = :postId ORDER BY createdAt DESC")
    fun getCommentsByPostId(postId: Long): Flow<List<ForumCommentEntity>>

    @Query("SELECT * FROM forum_comments WHERE id = :commentId")
    suspend fun getCommentById(commentId: Long): ForumCommentEntity?

    @Query("UPDATE forum_comments SET likes = :likes, isLikedByCurrentUser = :isLiked WHERE id = :commentId")
    suspend fun updateCommentLikeStatus(commentId: Long, likes: Int, isLiked: Boolean)

    @Query("DELETE FROM forum_comments WHERE postId = :postId")
    suspend fun deleteCommentsByPostId(postId: Long)

    @Query("DELETE FROM forum_comments WHERE cachedAt < :timestamp")
    suspend fun deleteOldComments(timestamp: Long)

    @Query("DELETE FROM forum_comments")
    suspend fun deleteAllComments()
}
