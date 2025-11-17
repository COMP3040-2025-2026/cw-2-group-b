package com.nottingham.mynottingham.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageListDto {
    private List<MessageDto> messages;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("total_count")
    private Long totalCount;
}
