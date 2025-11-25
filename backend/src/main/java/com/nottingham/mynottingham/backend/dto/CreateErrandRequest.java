package com.nottingham.mynottingham.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateErrandRequest {
    private String title;
    private String description;
    private String type;
    private String priority;
    private String pickupLocation;
    private String deliveryLocation;
    private Double fee;
    private String imageUrl;
    private String deadline;
}
