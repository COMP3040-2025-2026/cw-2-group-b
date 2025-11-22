package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ForumPostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostViewRepository extends JpaRepository<ForumPostView, Long> {

    /**
     * Check if a user has already viewed a specific post
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
