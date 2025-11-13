package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmployeeId(String employeeId);
    Optional<Teacher> findByEmail(String email);
    List<Teacher> findByDepartment(String department);
    boolean existsByEmployeeId(String employeeId);
}
