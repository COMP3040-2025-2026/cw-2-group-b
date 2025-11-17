package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Conversation;
import com.nottingham.mynottingham.backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find messages by conversation with pagination (most recent first)
    Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);

    // Find all messages in a conversation ordered by creation time
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    // Get total unread count for a conversation and user (messages sent by others)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    // Find latest message in conversation
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestMessage(@Param("conversationId") Long conversationId);

    // Delete old messages (for cleanup)
    @Modifying
    @Query("DELETE FROM Message m WHERE m.createdAt < :cutoffDate")
    void deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Mark all messages as read for a specific conversation and receiver
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    void markAllAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    // Search messages by content in a conversation
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchInConversation(@Param("conversationId") Long conversationId, @Param("searchTerm") String searchTerm);
}
