package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Forum comment entity for Room database
 * Stores cached forum comments from the backend
 */
@Entity(tableName = "forum_comments")
data class ForumCommentEntity(
    @PrimaryKey
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val content: String,
    val likes: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
