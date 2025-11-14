package com.nottingham.mynottingham.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterStudentRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String password;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    @NotNull(message = "Student ID is required")
    @Min(value = 10000000, message = "Student ID must be 8 digits")
    @Max(value = 99999999, message = "Student ID must be 8 digits")
    private Long studentId;

    @NotBlank(message = "Faculty is required")
    private String faculty;

    @NotBlank(message = "Major is required")
    private String major;

    @NotNull(message = "Year of study is required")
    @Min(1)
    @Max(4)
    private Integer yearOfStudy;

    private String matricNumber;
}
