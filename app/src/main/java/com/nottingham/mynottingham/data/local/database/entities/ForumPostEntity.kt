package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Forum post entity for Room database
 * Stores cached forum posts from the backend
 */
@Entity(tableName = "forum_posts")
data class ForumPostEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val title: String,
    val content: String,
    val category: String,
    val imageUrl: String?,
    val likes: Int = 0,
    val comments: Int = 0,
    val views: Int = 0,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isLikedByCurrentUser: Boolean = false,
    val tags: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
