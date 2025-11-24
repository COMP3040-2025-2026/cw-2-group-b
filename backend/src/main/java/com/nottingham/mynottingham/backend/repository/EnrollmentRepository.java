package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Course;
import com.nottingham.mynottingham.backend.entity.Enrollment;
import com.nottingham.mynottingham.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(Student student);
    List<Enrollment> findByCourse(Course course);
    Optional<Enrollment> findByStudentAndCourse(Student student, Course course);
    List<Enrollment> findByStudentAndStatus(Student student, Enrollment.EnrollmentStatus status);
}
