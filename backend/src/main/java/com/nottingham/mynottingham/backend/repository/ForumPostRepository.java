package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ForumPost;
import com.nottingham.mynottingham.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findByAuthor(User author);
    List<ForumPost> findByCategory(ForumPost.ForumCategory category);
    Page<ForumPost> findByCategory(ForumPost.ForumCategory category, Pageable pageable);
    List<ForumPost> findByOrderByCreatedAtDesc();
    List<ForumPost> findByIsPinnedTrueOrderByCreatedAtDesc();
}
