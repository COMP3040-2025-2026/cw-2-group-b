package com.nottingham.mynottingham.backend.dto;

import com.nottingham.mynottingham.backend.entity.Student;
import com.nottingham.mynottingham.backend.entity.Teacher;
import com.nottingham.mynottingham.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String role;

    // Student specific fields
    private Long studentId;
    private String faculty;
    private String major;
    private Integer yearOfStudy;

    // Teacher specific fields
    private String employeeId;
    private String department;

    /**
     * Create UserDto from User entity
     */
    public static UserDto fromUser(User user) {
        UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name());

        // Add student-specific fields if user is a student
        if (user instanceof Student) {
            Student student = (Student) user;
            builder.studentId(student.getStudentId())
                   .faculty(student.getFaculty())
                   .major(student.getMajor())
                   .yearOfStudy(student.getYearOfStudy());
        }

        // Add teacher-specific fields if user is a teacher
        if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            builder.employeeId(teacher.getEmployeeId())
                   .department(teacher.getDepartment());
        }

        return builder.build();
    }
}
