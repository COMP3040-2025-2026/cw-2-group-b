package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Course;
import com.nottingham.mynottingham.backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    List<Course> findByTeacher(Teacher teacher);
    List<Course> findByFaculty(String faculty);
    List<Course> findBySemester(String semester);
}
