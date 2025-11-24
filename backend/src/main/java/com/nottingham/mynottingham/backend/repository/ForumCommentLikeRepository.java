package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ForumCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ForumCommentLike entity
 */
@Repository
public interface ForumCommentLikeRepository extends JpaRepository<ForumCommentLike, Long> {

    /**
     * Find a like by comment and user
     */
    @Query("SELECT l FROM ForumCommentLike l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    Optional<ForumCommentLike> findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * Count likes for a comment
     */
    @Query("SELECT COUNT(l) FROM ForumCommentLike l WHERE l.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    /**
     * Check if user has liked a comment
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ForumCommentLike l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * Delete all likes for comments of a specific post
     */
    @Query("DELETE FROM ForumCommentLike l WHERE l.comment.post.id = :postId")
    @Modifying
    void deleteByPostId(@Param("postId") Long postId);
}
