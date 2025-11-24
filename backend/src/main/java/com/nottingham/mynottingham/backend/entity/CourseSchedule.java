package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "course_schedules")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourseSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"enrollments", "schedules", "teacher"})
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 100)
    private String room;

    @Column(length = 100)
    private String building;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CourseType courseType = CourseType.LECTURE;

    public enum CourseType {
        LECTURE, TUTORIAL, COMPUTING, LAB
    }
}
