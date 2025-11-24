package com.nottingham.mynottingham.data.model

/**
 * User data model
 *
 * Represents a user in the My Nottingham system.
 * Can be a Student, Teacher, or Admin.
 */
data class User(
    val id: String,              // Firebase UID
    val username: String = "",   // Username (e.g., "student1", "teacher1")
    val name: String,            // Full name (e.g., "Alice Wong")
    val email: String,           // Email address
    val role: String = "STUDENT", // User role: "STUDENT", "TEACHER", or "ADMIN"
    val studentId: String,       // Student matriculation number or Teacher employee ID
    val faculty: String,         // Faculty/Department
    val year: Int,               // Year of study (for students)
    val program: String,         // Program/Course name
    val profileImageUrl: String? = null // Profile picture URL (Firebase Storage)
)
