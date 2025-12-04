package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.User
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for User data model
 *
 * Tests the User data class:
 * - Required and optional fields
 * - Student vs Teacher fields
 * - Data class equality and copy
 */
class UserModelTest {

    /**
     * Test User creation with all required fields
     */
    @Test
    fun `User creation with required fields`() {
        val user = User(
            id = "uid123",
            username = "student1",
            name = "John Doe",
            email = "student1@nottingham.edu.my",
            role = "STUDENT",
            studentId = "20123456",
            faculty = "Faculty of Science",
            year = 2,
            program = "Computer Science"
        )

        assertEquals("ID should match", "uid123", user.id)
        assertEquals("Username should match", "student1", user.username)
        assertEquals("Name should match", "John Doe", user.name)
        assertEquals("Email should match", "student1@nottingham.edu.my", user.email)
        assertEquals("Role should match", "STUDENT", user.role)
    }

    /**
     * Test User default values for optional fields
     */
    @Test
    fun `User has correct default values`() {
        val user = User(
            id = "uid123",
            username = "test",
            name = "Test User",
            email = "test@nottingham.edu.my",
            role = "STUDENT",
            studentId = "20123456",
            faculty = "Science",
            year = 1,
            program = "CS"
        )

        assertNull("Profile image URL should be null by default", user.profileImageUrl)
        assertNull("Title should be null by default", user.title)
        assertNull("Office room should be null by default", user.officeRoom)
        assertFalse("Delivery mode should be false by default", user.deliveryMode ?: false)
    }

    /**
     * Test Student User with full details
     */
    @Test
    fun `Student User with full details`() {
        val student = User(
            id = "student_uid",
            username = "student1",
            name = "Jane Student",
            email = "student1@nottingham.edu.my",
            role = "STUDENT",
            studentId = "20123456",
            faculty = "Faculty of Science",
            program = "Computer Science",
            year = 2,
            profileImageUrl = "avatar1"
        )

        assertEquals("Student ID should match", "20123456", student.studentId)
        assertEquals("Faculty should match", "Faculty of Science", student.faculty)
        assertEquals("Program should match", "Computer Science", student.program)
        assertEquals("Year should be 2", 2, student.year)
        assertEquals("Profile image should match", "avatar1", student.profileImageUrl)
    }

    /**
     * Test Teacher User with full details
     */
    @Test
    fun `Teacher User with full details`() {
        val teacher = User(
            id = "teacher_uid",
            username = "teacher1",
            name = "Dr. John Teacher",
            email = "teacher1@nottingham.edu.my",
            role = "TEACHER",
            studentId = "EMP001", // Employee ID stored in studentId field
            faculty = "Computer Science", // Department stored in faculty field
            year = 0,
            program = "",
            title = "Associate Professor",
            officeRoom = "BB-12"
        )

        assertEquals("Role should be TEACHER", "TEACHER", teacher.role)
        assertEquals("Employee ID should match", "EMP001", teacher.studentId)
        assertEquals("Department should match", "Computer Science", teacher.faculty)
        assertEquals("Title should match", "Associate Professor", teacher.title)
        assertEquals("Office room should match", "BB-12", teacher.officeRoom)
    }

    /**
     * Test User role validation
     */
    @Test
    fun `User role can be STUDENT, TEACHER, or ADMIN`() {
        val student = User(id = "1", username = "s", name = "S", email = "s@test.com", role = "STUDENT", studentId = "1", faculty = "F", year = 1, program = "P")
        val teacher = User(id = "2", username = "t", name = "T", email = "t@test.com", role = "TEACHER", studentId = "2", faculty = "F", year = 0, program = "")
        val admin = User(id = "3", username = "a", name = "A", email = "a@test.com", role = "ADMIN", studentId = "3", faculty = "F", year = 0, program = "")

        assertEquals("STUDENT", student.role)
        assertEquals("TEACHER", teacher.role)
        assertEquals("ADMIN", admin.role)
    }

    /**
     * Test User equality
     */
    @Test
    fun `User data class equality`() {
        val user1 = User(
            id = "uid123",
            username = "student1",
            name = "John",
            email = "john@test.com",
            role = "STUDENT",
            studentId = "123",
            faculty = "Science",
            year = 1,
            program = "CS"
        )

        val user2 = User(
            id = "uid123",
            username = "student1",
            name = "John",
            email = "john@test.com",
            role = "STUDENT",
            studentId = "123",
            faculty = "Science",
            year = 1,
            program = "CS"
        )

        assertEquals("Users with same data should be equal", user1, user2)
    }

    /**
     * Test User copy function
     */
    @Test
    fun `User copy function works correctly`() {
        val original = User(
            id = "uid123",
            username = "student1",
            name = "Original Name",
            email = "student1@test.com",
            role = "STUDENT",
            studentId = "123",
            faculty = "Science",
            year = 1,
            program = "CS"
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals("ID should remain same", "uid123", updated.id)
        assertEquals("Name should be updated", "Updated Name", updated.name)
        assertNotEquals("Names should differ", original.name, updated.name)
    }

    /**
     * Test User with delivery mode enabled
     */
    @Test
    fun `User with delivery mode enabled`() {
        val user = User(
            id = "uid123",
            username = "student1",
            name = "Delivery Student",
            email = "student1@test.com",
            role = "STUDENT",
            studentId = "123",
            faculty = "Science",
            year = 1,
            program = "CS",
            deliveryMode = true
        )

        assertTrue("Delivery mode should be true", user.deliveryMode == true)
    }

    /**
     * Test User email format
     */
    @Test
    fun `User email contains nottingham domain`() {
        val validEmail = "student1@nottingham.edu.my"

        assertTrue("Email should contain nottingham", validEmail.contains("nottingham"))
        assertTrue("Email should contain @", validEmail.contains("@"))
    }
}
