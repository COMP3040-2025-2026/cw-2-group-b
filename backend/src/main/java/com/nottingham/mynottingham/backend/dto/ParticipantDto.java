package com.nottingham.mynottingham.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_avatar")
    private String userAvatar;

    @JsonProperty("is_online")
    private Boolean isOnline;

    @JsonProperty("is_typing")
    private Boolean isTyping;

    @JsonProperty("last_seen_at")
    private Long lastSeenAt;
}
