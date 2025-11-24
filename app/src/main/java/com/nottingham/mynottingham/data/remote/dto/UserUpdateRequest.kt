package com.nottingham.mynottingham.data.remote.dto

// This class is a subset of the backend User entity, used for update requests.
data class UserUpdateRequest(
    val username: String,
    val email: String,
    val fullName: String,
    val role: String, // Note: backend expects 'role', not 'userType'
    val status: String,
    val avatarUrl: String,
    val phone: String?
)
