package com.nottingham.mynottingham.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking user likes on forum comments
 */
@Entity
@Table(name = "forum_comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private ForumComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
