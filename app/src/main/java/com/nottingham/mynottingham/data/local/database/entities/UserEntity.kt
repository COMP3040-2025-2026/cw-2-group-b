package com.nottingham.mynottingham.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity for Room database
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val studentId: String,
    val faculty: String,
    val year: Int,
    val program: String,
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
