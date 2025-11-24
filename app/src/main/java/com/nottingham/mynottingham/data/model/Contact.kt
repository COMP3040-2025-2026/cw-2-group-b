package com.nottingham.mynottingham.data.model

/**
 * Contact data model for messaging
 */
data class Contact(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val program: String? = null,
    val year: Int? = null,
    val isOnline: Boolean = false
)
