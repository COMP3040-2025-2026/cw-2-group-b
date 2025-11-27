package com.nottingham.mynottingham.data.model

/**
 * Group member data model
 */
data class GroupMember(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val email: String? = null,
    val role: GroupRole = GroupRole.MEMBER,
    val faculty: String? = null,
    val program: String? = null,
    val year: String? = null,
    val userRole: String? = null  // STUDENT or TEACHER
)

/**
 * Group role enum
 */
enum class GroupRole {
    OWNER,   // 群主 - can do everything
    ADMIN,   // 管理员 - can add/remove members, edit group name
    MEMBER   // 普通成员
}

/**
 * Group info data model
 */
data class GroupInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val ownerId: String,
    val adminIds: List<String> = emptyList(),
    val members: List<GroupMember> = emptyList()
)
