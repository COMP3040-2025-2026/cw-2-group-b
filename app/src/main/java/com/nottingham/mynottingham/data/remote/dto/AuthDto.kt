package com.nottingham.mynottingham.data.remote.dto

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
    val userType: String,
    val studentId: String?,
    val employeeId: String?,
    val phoneNumber: String?,
    val avatarUrl: String?
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
