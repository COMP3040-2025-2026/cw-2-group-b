package com.nottingham.mynottingham.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for Authentication
 */

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    @SerializedName("role")  // Map backend's "role" field to "userType"
    val userType: String,
    val studentId: Long?,
    val employeeId: String?,
    val phone: String?,
    val avatarUrl: String?,
    // Student specific fields
    val faculty: String?,
    val major: String?,
    val yearOfStudy: Int?,
    // Teacher specific fields
    val department: String?,
    val title: String?,
    val officeRoom: String?,
    val officeHours: String?
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
