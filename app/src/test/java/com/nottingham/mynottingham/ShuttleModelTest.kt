package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.data.model.RouteSchedule
import com.nottingham.mynottingham.data.model.ShuttleRoute
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Shuttle data models
 *
 * Tests the shuttle bus data classes and enums:
 * - DayType enum values
 * - RouteSchedule data class
 * - ShuttleRoute data class
 */
class ShuttleModelTest {

    /**
     * Test DayType enum has all expected values
     */
    @Test
    fun `DayType enum has all expected values`() {
        val dayTypes = DayType.values()

        assertEquals("Should have 3 day types", 3, dayTypes.size)
        assertTrue("Should contain WEEKDAY", dayTypes.contains(DayType.WEEKDAY))
        assertTrue("Should contain FRIDAY", dayTypes.contains(DayType.FRIDAY))
        assertTrue("Should contain WEEKEND", dayTypes.contains(DayType.WEEKEND))
    }

    /**
     * Test DayType enum valueOf
     */
    @Test
    fun `DayType valueOf returns correct enum`() {
        assertEquals(DayType.WEEKDAY, DayType.valueOf("WEEKDAY"))
        assertEquals(DayType.FRIDAY, DayType.valueOf("FRIDAY"))
        assertEquals(DayType.WEEKEND, DayType.valueOf("WEEKEND"))
    }

    /**
     * Test RouteSchedule creation with default vehicle type
     */
    @Test
    fun `RouteSchedule default vehicleType is Bus`() {
        val schedule = RouteSchedule(
            departureFromCampus = listOf("9:00am", "10:00am"),
            returnToCampus = listOf("8:00am", "9:00am")
        )

        assertEquals("Default vehicle type should be Bus", "Bus", schedule.vehicleType)
    }

    /**
     * Test RouteSchedule with custom vehicle type
     */
    @Test
    fun `RouteSchedule accepts custom vehicleType`() {
        val schedule = RouteSchedule(
            departureFromCampus = listOf("9:00am"),
            returnToCampus = listOf("8:00am"),
            vehicleType = "Van"
        )

        assertEquals("Vehicle type should be Van", "Van", schedule.vehicleType)
    }

    /**
     * Test RouteSchedule with empty lists
     */
    @Test
    fun `RouteSchedule accepts empty departure list`() {
        val schedule = RouteSchedule(
            departureFromCampus = emptyList(),
            returnToCampus = listOf("8:00am")
        )

        assertTrue("Departure list should be empty", schedule.departureFromCampus.isEmpty())
        assertFalse("Return list should not be empty", schedule.returnToCampus.isEmpty())
    }

    /**
     * Test ShuttleRoute creation with required fields
     */
    @Test
    fun `ShuttleRoute creation with required fields`() {
        val route = ShuttleRoute(
            routeId = "A",
            routeName = "Route A",
            description = "UNM to TBS",
            weekdaySchedule = RouteSchedule(
                departureFromCampus = listOf("6:45pm"),
                returnToCampus = listOf("7:45am")
            ),
            fridaySchedule = null,
            weekendSchedule = null
        )

        assertEquals("Route ID should be A", "A", route.routeId)
        assertEquals("Route name should be Route A", "Route A", route.routeName)
        assertNotNull("Weekday schedule should not be null", route.weekdaySchedule)
        assertNull("Friday schedule should be null", route.fridaySchedule)
        assertNull("Weekend schedule should be null", route.weekendSchedule)
    }

    /**
     * Test ShuttleRoute default isActive is true
     */
    @Test
    fun `ShuttleRoute default isActive is true`() {
        val route = ShuttleRoute(
            routeId = "B",
            routeName = "Route B",
            description = "Test route",
            weekdaySchedule = null,
            fridaySchedule = null,
            weekendSchedule = null
        )

        assertTrue("Default isActive should be true", route.isActive)
    }

    /**
     * Test ShuttleRoute with special note
     */
    @Test
    fun `ShuttleRoute with special note`() {
        val route = ShuttleRoute(
            routeId = "E1",
            routeName = "Route E1",
            description = "Mosque route",
            weekdaySchedule = null,
            fridaySchedule = RouteSchedule(
                departureFromCampus = listOf("12:45pm"),
                returnToCampus = listOf("2:00pm")
            ),
            weekendSchedule = null,
            specialNote = "Friday Only"
        )

        assertEquals("Special note should be Friday Only", "Friday Only", route.specialNote)
    }

    /**
     * Test ShuttleRoute equality
     */
    @Test
    fun `ShuttleRoute data class equality`() {
        val schedule = RouteSchedule(
            departureFromCampus = listOf("9:00am"),
            returnToCampus = listOf("8:00am")
        )

        val route1 = ShuttleRoute(
            routeId = "A",
            routeName = "Route A",
            description = "Test",
            weekdaySchedule = schedule,
            fridaySchedule = null,
            weekendSchedule = null
        )

        val route2 = ShuttleRoute(
            routeId = "A",
            routeName = "Route A",
            description = "Test",
            weekdaySchedule = schedule,
            fridaySchedule = null,
            weekendSchedule = null
        )

        assertEquals("Routes with same data should be equal", route1, route2)
    }

    /**
     * Test RouteSchedule equality
     */
    @Test
    fun `RouteSchedule data class equality`() {
        val schedule1 = RouteSchedule(
            departureFromCampus = listOf("9:00am", "10:00am"),
            returnToCampus = listOf("8:00am")
        )

        val schedule2 = RouteSchedule(
            departureFromCampus = listOf("9:00am", "10:00am"),
            returnToCampus = listOf("8:00am")
        )

        assertEquals("Schedules with same data should be equal", schedule1, schedule2)
    }

    /**
     * Test ShuttleRoute copy function
     */
    @Test
    fun `ShuttleRoute copy function works correctly`() {
        val original = ShuttleRoute(
            routeId = "A",
            routeName = "Route A",
            description = "Original",
            weekdaySchedule = null,
            fridaySchedule = null,
            weekendSchedule = null
        )

        val copied = original.copy(description = "Modified")

        assertEquals("Copied route should have same ID", "A", copied.routeId)
        assertEquals("Copied route should have modified description", "Modified", copied.description)
        assertNotEquals("Original and copy should have different descriptions", original.description, copied.description)
    }
}
