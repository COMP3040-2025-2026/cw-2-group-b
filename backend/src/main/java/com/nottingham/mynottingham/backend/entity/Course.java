package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "courses")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Course extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String courseCode;

    @Column(nullable = false, length = 200)
    private String courseName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false, length = 100)
    private String faculty;

    @Column(length = 50)
    private String semester; // e.g., "Fall 2024", "Spring 2025"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @JsonIgnoreProperties({"courses", "password"})
    private Teacher teacher;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"course", "student"})
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("course")
    private List<CourseSchedule> schedules = new ArrayList<>();

    @Column(nullable = false)
    private Integer capacity = 50;

    private Integer enrolled = 0;
}
