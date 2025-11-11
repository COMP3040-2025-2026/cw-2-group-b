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
    val createdAt: Long
)

/**
 * Forum category
 */
enum class ForumCategory(val displayName: String) {
    GENERAL("General Discussion"),
    ACADEMIC("Academic Help"),
    EVENTS("Campus Events"),
    SPORTS("Sports & Recreation"),
    HOUSING("Housing & Accommodation"),
    JOBS("Jobs & Internships"),
    SOCIAL("Social & Meetups"),
    MARKETPLACE("Buy & Sell")
}
