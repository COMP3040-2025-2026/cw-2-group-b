package com.nottingham.mynottingham.backend.service;

import com.nottingham.mynottingham.backend.entity.*;
import com.nottingham.mynottingham.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Student createStudent(Student student) {
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setRole(User.UserRole.STUDENT);
        student.setStatus(User.UserStatus.ACTIVE);
        return studentRepository.save(student);
    }

    public Teacher createTeacher(Teacher teacher) {
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        teacher.setRole(User.UserRole.TEACHER);
        teacher.setStatus(User.UserStatus.ACTIVE);
        return teacherRepository.save(teacher);
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    public Optional<Student> getStudentByStudentId(Long studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public Optional<Teacher> getTeacherById(Long id) {
        return teacherRepository.findById(id);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setEmail(updatedUser.getEmail());
                    user.setFullName(updatedUser.getFullName());
                    user.setPhone(updatedUser.getPhone());
                    user.setAvatarUrl(updatedUser.getAvatarUrl());
                    user.setStatus(updatedUser.getStatus());
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
