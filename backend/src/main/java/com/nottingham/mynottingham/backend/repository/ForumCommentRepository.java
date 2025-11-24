package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ForumComment;
import com.nottingham.mynottingham.backend.entity.ForumPost;
import com.nottingham.mynottingham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ForumComment entity
 */
@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    /**
     * Find all comments for a specific post
     */
    List<ForumComment> findByPostOrderByCreatedAtDesc(ForumPost post);

    /**
     * Find comments by author
     */
    List<ForumComment> findByAuthorOrderByCreatedAtDesc(User author);

    /**
     * Count comments for a post
     */
    Long countByPost(ForumPost post);

    /**
     * Delete all comments for a specific post
     */
    @Query("DELETE FROM ForumComment c WHERE c.post.id = :postId")
    @Modifying
    void deleteByPostId(@Param("postId") Long postId);
}
