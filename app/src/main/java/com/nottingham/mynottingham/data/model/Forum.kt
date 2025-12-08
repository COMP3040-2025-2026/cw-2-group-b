package com.nottingham.mynottingham.data.model

/**
 * Forum post data model
 */
data class ForumPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val category: String,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val tags: List<String>? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val views: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Forum comment data model
 */
data class ForumComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val createdAt: Long
)

/**
 * Forum category - matches backend ForumPost.ForumCategory enum
 */
enum class ForumCategory(val displayName: String) {
    ACADEMIC("Academic"),
    EVENTS("Events"),
    SPORTS("Sports"),
    SOCIAL("Social"),
    GENERAL("General"),
    ANNOUNCEMENTS("Announcements"),
    QUESTIONS("Questions"),
    CAREER("Career"),
    FOOD("Food & Dining")
}
