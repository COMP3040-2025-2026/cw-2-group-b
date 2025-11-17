package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.ApiResponse;
import com.nottingham.mynottingham.backend.dto.ForumDto.*;
import com.nottingham.mynottingham.backend.entity.ForumPost;
import com.nottingham.mynottingham.backend.service.ForumService;
import com.nottingham.mynottingham.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final JwtUtil jwtUtil;

    /**
     * Get paginated forum posts
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PagedForumResponse>> getPosts(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) ForumPost.ForumCategory category
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            PagedForumResponse response = forumService.getPosts(page, size, category, userId);
            return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve posts: " + e.getMessage()));
        }
    }

    /**
     * Get post by ID with comments
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<ForumPostDetailDto>> getPostById(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumPostDetailDto detail = forumService.getPostById(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", detail));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Post not found: " + e.getMessage()));
        }
    }

    /**
     * Create new post
     */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<ForumPostDto>> createPost(
            @RequestHeader("Authorization") String token,
            @RequestPart("post") CreateForumPostRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumPostDto post = forumService.createPost(userId, request, image);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Post created successfully", post));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create post: " + e.getMessage()));
        }
    }

    /**
     * Update post
     */
    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<ForumPostDto>> updatePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody UpdateForumPostRequest request
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumPostDto post = forumService.updatePost(id, userId, request);
            return ResponseEntity.ok(ApiResponse.success("Post updated successfully", post));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update post: " + e.getMessage()));
        }
    }

    /**
     * Delete post
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            forumService.deletePost(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to delete post: " + e.getMessage()));
        }
    }

    /**
     * Like/unlike post
     */
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<ForumPostDto>> likePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumPostDto post = forumService.likePost(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Post liked successfully", post));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to like post: " + e.getMessage()));
        }
    }

    /**
     * Create comment on post
     */
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<ForumCommentDto>> createComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody CreateCommentRequest request
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumCommentDto comment = forumService.createComment(id, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Comment created successfully", comment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create comment: " + e.getMessage()));
        }
    }

    /**
     * Like/unlike comment
     */
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<ApiResponse<ForumCommentDto>> likeComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id
    ) {
        try {
            Long userId = getUserIdFromToken(token);
            ForumCommentDto comment = forumService.likeComment(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Comment liked successfully", comment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to like comment: " + e.getMessage()));
        }
    }

    /**
     * Extract user ID from JWT token
     */
    private Long getUserIdFromToken(String token) {
        String cleanToken = token.replace("Bearer ", "").trim();
        return jwtUtil.getUserIdFromToken(cleanToken);
    }
}
