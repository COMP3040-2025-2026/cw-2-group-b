package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "teachers")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "user_id")
public class Teacher extends User {

    @Column(unique = true, nullable = false, length = 20)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 100)
    private String title; // Professor, Associate Professor, Lecturer, etc.

    @Column(length = 50)
    private String officeRoom;

    @Column(length = 100)
    private String officeHours;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"teacher", "enrollments", "schedules"})
    private List<Course> courses = new ArrayList<>();
}
