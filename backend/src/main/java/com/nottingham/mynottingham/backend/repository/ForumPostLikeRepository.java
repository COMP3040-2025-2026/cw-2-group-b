package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ForumPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ForumPostLike entity
 */
@Repository
public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, Long> {

    /**
     * Find a like by post and user
     */
    @Query("SELECT l FROM ForumPostLike l WHERE l.post.id = :postId AND l.user.id = :userId")
    Optional<ForumPostLike> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * Count likes for a post
     */
    @Query("SELECT COUNT(l) FROM ForumPostLike l WHERE l.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    /**
     * Check if user has liked a post
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ForumPostLike l WHERE l.post.id = :postId AND l.user.id = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * Delete all likes for a post
     */
    @Query("DELETE FROM ForumPostLike l WHERE l.post.id = :postId")
    @Modifying
    void deleteByPostId(@Param("postId") Long postId);
}
