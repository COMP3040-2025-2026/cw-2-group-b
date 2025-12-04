package com.nottingham.mynottingham

import org.junit.Assert.*
import org.junit.Test

/**
 * Basic unit tests for the My Nottingham application.
 *
 * These tests verify fundamental assumptions and serve as
 * a quick sanity check for the testing framework.
 */
class ExampleUnitTest {

    /**
     * Basic arithmetic test - verifies testing framework works
     */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    /**
     * Test string concatenation
     */
    @Test
    fun stringConcatenation_works() {
        val result = "My" + "Nottingham"
        assertEquals("MyNottingham", result)
    }

    /**
     * Test list operations
     */
    @Test
    fun listOperations_work() {
        val list = listOf(1, 2, 3, 4, 5)

        assertEquals(5, list.size)
        assertEquals(1, list.first())
        assertEquals(5, list.last())
        assertTrue(list.contains(3))
    }

    /**
     * Test null safety
     */
    @Test
    fun nullSafety_works() {
        val nullableString: String? = null
        val nonNullString: String? = "Hello"

        assertNull(nullableString)
        assertNotNull(nonNullString)
        assertEquals("Hello", nonNullString)
    }

    /**
     * Test email validation regex pattern
     */
    @Test
    fun emailPattern_validates() {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@(.+)$")

        assertTrue(emailPattern.matches("student1@nottingham.edu.my"))
        assertTrue(emailPattern.matches("teacher1@nottingham.edu.my"))
        assertFalse(emailPattern.matches("invalid-email"))
        assertFalse(emailPattern.matches("@nottingham.edu.my"))
    }

    /**
     * Test Kotlin data class comparison
     */
    @Test
    fun dataClass_equalityWorks() {
        data class SimpleUser(val id: String, val name: String)

        val user1 = SimpleUser("1", "John")
        val user2 = SimpleUser("1", "John")
        val user3 = SimpleUser("2", "Jane")

        assertEquals(user1, user2)
        assertNotEquals(user1, user3)
    }

    /**
     * Test map operations
     */
    @Test
    fun mapOperations_work() {
        val map = mapOf(
            "student" to "STUDENT",
            "teacher" to "TEACHER",
            "admin" to "ADMIN"
        )

        assertEquals(3, map.size)
        assertEquals("STUDENT", map["student"])
        assertTrue(map.containsKey("teacher"))
        assertNull(map["unknown"])
    }

    /**
     * Test when expression (Kotlin switch)
     */
    @Test
    fun whenExpression_works() {
        fun getRoleDescription(role: String): String = when (role) {
            "STUDENT" -> "Student User"
            "TEACHER" -> "Teacher User"
            "ADMIN" -> "Administrator"
            else -> "Unknown"
        }

        assertEquals("Student User", getRoleDescription("STUDENT"))
        assertEquals("Teacher User", getRoleDescription("TEACHER"))
        assertEquals("Administrator", getRoleDescription("ADMIN"))
        assertEquals("Unknown", getRoleDescription("OTHER"))
    }
}
