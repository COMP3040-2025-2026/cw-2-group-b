package com.nottingham.mynottingham.data.remote.dto

/**
 * Data Transfer Objects for Authentication
 */

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val studentId: String,
    val faculty: String,
    val year: Int,
    val program: String,
    val profileImageUrl: String?
)
