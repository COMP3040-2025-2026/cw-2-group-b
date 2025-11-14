package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(Long studentId);
    List<Student> findByFaculty(String faculty);
    List<Student> findByYearOfStudy(Integer year);
    boolean existsByStudentId(Long studentId);
}
