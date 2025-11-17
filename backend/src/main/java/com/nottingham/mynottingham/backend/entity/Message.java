package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnoreProperties({"participants", "messages"})
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageType type = MessageType.TEXT;

    private String attachmentUrl;

    // Delivery status for tracking message lifecycle
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageStatus status = MessageStatus.SENT;

    public enum MessageType {
        TEXT, IMAGE, FILE
    }

    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
}
