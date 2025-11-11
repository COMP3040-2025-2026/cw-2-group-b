package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Attendance;
import com.nottingham.mynottingham.backend.entity.Course;
import com.nottingham.mynottingham.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudent(Student student);
    List<Attendance> findByCourse(Course course);
    List<Attendance> findByAttendanceDate(LocalDate date);
    Optional<Attendance> findByStudentAndCourseAndAttendanceDate(Student student, Course course, LocalDate date);
    List<Attendance> findByStudentAndCourse(Student student, Course course);
}
