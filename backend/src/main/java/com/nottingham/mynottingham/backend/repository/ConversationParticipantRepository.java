package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversationId(Long conversationId);

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<ConversationParticipant> findByConversationIdAndUserId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    List<ConversationParticipant> findByUserId(Long userId);

    void deleteByConversationId(Long conversationId);
}
