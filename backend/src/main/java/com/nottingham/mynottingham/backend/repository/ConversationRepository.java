package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationUuid(String conversationUuid);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id " +
           "AND c.isGroup = false")
    Optional<Conversation> findOneOnOneConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
