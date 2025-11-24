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
    val id: Long = 0,
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    @SerializedName("role")  // Map backend's "role" field to "userType"
    val userType: String = "",
    val studentId: Long? = null,
    val employeeId: String? = null,
    val phone: String? = null,
    val avatarUrl: String?,
    // Student specific fields
    val faculty: String? = null,
    val major: String? = null,
    val yearOfStudy: Int? = null,
    // Teacher specific fields
    val department: String? = null,
    val title: String? = null,
    val officeRoom: String? = null,
    val officeHours: String? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
