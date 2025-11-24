package com.nottingham.mynottingham.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnlockSessionRequest {
    private Long courseScheduleId;
    private String sessionDate; // ISO format: "2025-11-12"
}
