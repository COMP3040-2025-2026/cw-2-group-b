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
@Table(name = "students")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "user_id")
public class Student extends User {

    @Column(unique = true, nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 100)
    private String faculty;

    @Column(nullable = false, length = 100)
    private String major;

    @Column(nullable = false)
    private Integer yearOfStudy;

    @Column(length = 20)
    private String matricNumber;

    private Double gpa;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"student", "course"})
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("student")
    private List<Attendance> attendances = new ArrayList<>();
}
