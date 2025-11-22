package com.nottingham.mynottingham.backend.dto;

import com.nottingham.mynottingham.backend.entity.Errand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrandDto {
    private String id;
    private String requesterId;
    private String requesterName;
    private String providerId;
    private String providerName;
    private String title;
    private String description;
    private String type;
    private String location;
    private String deadline;
    private Double fee;
    private String status;
    private String imageUrl;
    private Long createdAt;
    private Long updatedAt;

    public static ErrandDto fromEntity(Errand errand) {
        ErrandDto dto = new ErrandDto();
        dto.setId(errand.getId().toString());
        dto.setRequesterId(errand.getRequester().getId().toString());
        dto.setRequesterName(errand.getRequester().getUsername());

        if (errand.getProvider() != null) {
            dto.setProviderId(errand.getProvider().getId().toString());
            dto.setProviderName(errand.getProvider().getUsername());
        }

        dto.setTitle(errand.getTitle());
        dto.setDescription(errand.getDescription());
        dto.setType(errand.getType().name());
        dto.setLocation(errand.getLocation());
        dto.setDeadline(errand.getDeadline());
        dto.setFee(errand.getReward());
        dto.setStatus(errand.getStatus().name());
        dto.setImageUrl(errand.getImageUrl());

        if (errand.getCreatedAt() != null) {
            dto.setCreatedAt(errand.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (errand.getUpdatedAt() != null) {
            dto.setUpdatedAt(errand.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        return dto;
    }
}
