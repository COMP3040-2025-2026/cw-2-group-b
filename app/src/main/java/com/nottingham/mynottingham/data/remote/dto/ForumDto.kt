package com.nottingham.mynottingham.data.remote.dto

import java.time.LocalDateTime

/**
 * Data Transfer Objects for Forum feature
 * Maps to backend ForumDto.java
 */

// Response DTOs
data class ForumPostDto(
    val id: Long,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val title: String,
    val content: String,
    val category: String,
    val imageUrl: String?,
    val likes: Int,
    val comments: Int,
    val views: Int,
    val isPinned: Boolean,
    val isLocked: Boolean,
    val isLikedByCurrentUser: Boolean,
    val tags: List<String>?,
    val createdAt: String,  // ISO 8601 format from backend
    val updatedAt: String
)

data class ForumCommentDto(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val content: String,
    val likes: Int,
    val isLikedByCurrentUser: Boolean,
    val createdAt: String  // ISO 8601 format from backend
)

data class PagedForumResponse(
    val posts: List<ForumPostDto>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class ForumPostDetailDto(
    val post: ForumPostDto,
    val comments: List<ForumCommentDto>
)

// Request DTOs
data class CreateForumPostRequest(
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>?
)

data class UpdateForumPostRequest(
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>?
)

data class CreateCommentRequest(
    val content: String
)

// API Response wrappers
data class ForumPostResponse(
    val success: Boolean,
    val message: String,
    val data: ForumPostDto?
)

data class ForumPostDetailResponse(
    val success: Boolean,
    val message: String,
    val data: ForumPostDetailDto?
)

data class PagedForumPostsResponse(
    val success: Boolean,
    val message: String,
    val data: PagedForumResponse?
)

data class ForumCommentResponse(
    val success: Boolean,
    val message: String,
    val data: ForumCommentDto?
)
