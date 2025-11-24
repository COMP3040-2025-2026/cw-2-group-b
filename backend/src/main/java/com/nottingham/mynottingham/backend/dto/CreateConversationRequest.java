package com.nottingham.mynottingham.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @JsonProperty("participant_ids")
    private List<String> participantIds;

    @JsonProperty("is_group")
    private Boolean isGroup;

    @JsonProperty("group_name")
    private String groupName;
}
