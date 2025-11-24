package com.nottingham.mynottingham.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private String id;

    @JsonProperty("is_group")
    private Boolean isGroup;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("group_avatar")
    private String groupAvatar;

    @JsonProperty("last_message")
    private String lastMessage;

    @JsonProperty("last_message_time")
    private Long lastMessageTime;

    @JsonProperty("last_message_sender_id")
    private String lastMessageSenderId;

    @JsonProperty("unread_count")
    private Integer unreadCount;

    @JsonProperty("is_pinned")
    private Boolean isPinned;

    private List<ParticipantDto> participants;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
