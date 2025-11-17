package com.nottingham.mynottingham.backend.dto;

import com.nottingham.mynottingham.backend.entity.ForumPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Forum feature
 */
public class ForumDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForumPostDto {
        private Long id;
        private Long authorId;
        private String authorName;
        private String authorAvatar;
        private String title;
        private String content;
        private String category;
        private String imageUrl;
        private Integer likes;
        private Integer comments;
        private Integer views;
        private Boolean isPinned;
        private Boolean isLocked;
        private Boolean isLikedByCurrentUser;
        private List<String> tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForumCommentDto {
        private Long id;
        private Long postId;
        private Long authorId;
        private String authorName;
        private String authorAvatar;
        private String content;
        private Integer likes;
        private Boolean isLikedByCurrentUser;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateForumPostRequest {
        private String title;
        private String content;
        private ForumPost.ForumCategory category;
        private List<String> tags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateForumPostRequest {
        private String title;
        private String content;
        private ForumPost.ForumCategory category;
        private List<String> tags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommentRequest {
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedForumResponse {
        private List<ForumPostDto> posts;
        private Integer currentPage;
        private Integer totalPages;
        private Long totalElements;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForumPostDetailDto {
        private ForumPostDto post;
        private List<ForumCommentDto> comments;
    }
}
