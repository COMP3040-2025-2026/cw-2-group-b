package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Message;
import com.nottingham.mynottingham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySender(User sender);
    List<Message> findByReceiver(User receiver);
    List<Message> findByReceiverAndIsReadFalse(User receiver);

    @Query("SELECT m FROM Message m WHERE (m.sender = ?1 AND m.receiver = ?2) OR (m.sender = ?2 AND m.receiver = ?1) ORDER BY m.createdAt")
    List<Message> findConversation(User user1, User user2);
}
