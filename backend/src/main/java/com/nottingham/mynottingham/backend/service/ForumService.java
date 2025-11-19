package com.nottingham.mynottingham.backend.service;

import com.nottingham.mynottingham.backend.dto.ForumDto.*;
import com.nottingham.mynottingham.backend.entity.ForumComment;
import com.nottingham.mynottingham.backend.entity.ForumPost;
import com.nottingham.mynottingham.backend.entity.ForumPostLike;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.ForumCommentRepository;
import com.nottingham.mynottingham.backend.repository.ForumPostLikeRepository;
import com.nottingham.mynottingham.backend.repository.ForumPostRepository;
import com.nottingham.mynottingham.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumCommentRepository commentRepository;
    private final ForumPostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/forum/";

    /**
     * Get paginated posts with optional category filter
     */
    @Transactional(readOnly = true)
    public PagedForumResponse getPosts(Integer page, Integer size, ForumPost.ForumCategory category, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ForumPost> postPage;

        if (category != null) {
            postPage = postRepository.findByCategory(category, pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        List<ForumPostDto> postDtos = postPage.getContent().stream()
                .map(post -> toPostDto(post, currentUserId))
                .collect(Collectors.toList());

        return PagedForumResponse.builder()
                .posts(postDtos)
                .currentPage(postPage.getNumber())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .hasNext(postPage.hasNext())
                .hasPrevious(postPage.hasPrevious())
                .build();
    }

    /**
     * Get post by ID with comments
     */
    @Transactional
    public ForumPostDetailDto getPostById(Long postId, Long currentUserId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Increment views
        post.setViews(post.getViews() + 1);
        postRepository.save(post);

        List<ForumCommentDto> commentDtos = commentRepository.findByPostOrderByCreatedAtDesc(post).stream()
                .map(comment -> toCommentDto(comment, currentUserId))
                .collect(Collectors.toList());

        return ForumPostDetailDto.builder()
                .post(toPostDto(post, currentUserId))
                .comments(commentDtos)
                .build();
    }

    /**
     * Create new post
     */
    @Transactional
    public ForumPostDto createPost(Long userId, CreateForumPostRequest request, MultipartFile image) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ForumPost post = new ForumPost();
        post.setAuthor(author);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);
        post.setLikes(0);
        post.setViews(0);
        post.setIsPinned(false);
        post.setIsLocked(false);

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            post.setImageUrl(imageUrl);
        }

        ForumPost savedPost = postRepository.save(post);
        return toPostDto(savedPost, userId);
    }

    /**
     * Update post
     */
    @Transactional
    public ForumPostDto updatePost(Long postId, Long userId, UpdateForumPostRequest request) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this post");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);

        ForumPost updatedPost = postRepository.save(post);
        return toPostDto(updatedPost, userId);
    }

    /**
     * Delete post
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this post");
        }

        postRepository.delete(post);
    }

    /**
     * Like/unlike post (toggle)
     */
    @Transactional
    public ForumPostDto likePost(Long postId, Long userId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has already liked the post
        Optional<ForumPostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            // Unlike: remove the like
            postLikeRepository.delete(existingLike.get());
            post.setLikes(Math.max(0, post.getLikes() - 1)); // Prevent negative likes
        } else {
            // Like: add a new like
            ForumPostLike like = new ForumPostLike();
            like.setPost(post);
            like.setUser(user);
            postLikeRepository.save(like);
            post.setLikes(post.getLikes() + 1);
        }

        ForumPost savedPost = postRepository.save(post);
        return toPostDto(savedPost, userId);
    }

    /**
     * Create comment
     */
    @Transactional
    public ForumCommentDto createComment(Long postId, Long userId, CreateCommentRequest request) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ForumComment comment = new ForumComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.getContent());
        comment.setLikes(0);

        ForumComment savedComment = commentRepository.save(comment);
        return toCommentDto(savedComment, userId);
    }

    /**
     * Like/unlike comment
     */
    @Transactional
    public ForumCommentDto likeComment(Long commentId, Long userId) {
        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        comment.setLikes(comment.getLikes() + 1);
        ForumComment savedComment = commentRepository.save(comment);

        return toCommentDto(savedComment, userId);
    }

    /**
     * Convert ForumPost entity to DTO
     */
    private ForumPostDto toPostDto(ForumPost post, Long currentUserId) {
        Long commentCount = commentRepository.countByPost(post);
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return ForumPostDto.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorAvatar(post.getAuthor().getAvatarUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .imageUrl(post.getImageUrl())
                .likes(post.getLikes())
                .comments(commentCount.intValue())
                .views(post.getViews())
                .isPinned(post.getIsPinned())
                .isLocked(post.getIsLocked())
                .isLikedByCurrentUser(isLiked)
                .tags(post.getTags() != null && !post.getTags().isEmpty()
                    ? List.of(post.getTags().split(","))
                    : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Convert ForumComment entity to DTO
     */
    private ForumCommentDto toCommentDto(ForumComment comment, Long currentUserId) {
        return ForumCommentDto.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .authorAvatar(comment.getAuthor().getAvatarUrl())
                .content(comment.getContent())
                .likes(comment.getLikes())
                .isLikedByCurrentUser(false) // TODO: Implement like tracking
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * Save uploaded image and return URL
     */
    private String saveImage(MultipartFile file) {
        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/forum/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }
}
