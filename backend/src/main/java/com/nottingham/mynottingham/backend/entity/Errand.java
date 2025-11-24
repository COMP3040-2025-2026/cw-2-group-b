package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "errands")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Errand extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    @JsonIgnoreProperties({"password"})
    private User provider;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ErrandType type;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 100)
    private String deadline;

    @Column(nullable = false)
    private Double reward = 0.0;

    @Column(length = 500)
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ErrandStatus status = ErrandStatus.PENDING;

    private String imageUrl;

    public enum ErrandType {
        SHOPPING, PICKUP, FOOD_DELIVERY, OTHER
    }

    public enum ErrandStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
