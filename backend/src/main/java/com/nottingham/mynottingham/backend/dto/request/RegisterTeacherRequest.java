package com.nottingham.mynottingham.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterTeacherRequest {

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

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Department is required")
    private String department;

    private String title;

    private String officeRoom;

    private String officeHours;
}
