package com.nottingham.mynottingham.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String id; // Changed to String to match Android

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("sender_id")
    private String senderId; // Changed to String to match Android

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_avatar")
    private String senderAvatar;

    private String content;

    private Long timestamp; // Unix timestamp in milliseconds

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("message_type")
    private String messageType; // TEXT, IMAGE, FILE

    @JsonProperty("created_at")
    private Long createdAt; // Unix timestamp in milliseconds

    // Fields that are not sent to Android but used internally
    private String attachmentUrl;
    private String status; // SENT, DELIVERED, READ
    private LocalDateTime updatedAt;
}
