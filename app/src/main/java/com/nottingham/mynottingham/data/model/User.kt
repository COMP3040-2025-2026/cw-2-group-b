package com.nottingham.mynottingham.data.model

/**
 * User data model
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val studentId: String,
    val faculty: String,
    val year: Int,
    val program: String,
    val profileImageUrl: String? = null
)
