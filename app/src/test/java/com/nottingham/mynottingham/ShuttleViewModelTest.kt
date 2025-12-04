package com.nottingham.mynottingham

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.ui.shuttle.ShuttleViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ShuttleViewModel
 *
 * Tests the shuttle bus schedule functionality including:
 * - Route loading
 * - Day type selection
 * - Schedule retrieval for different day types
 */
class ShuttleViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ShuttleViewModel

    @Before
    fun setup() {
        viewModel = ShuttleViewModel()
    }

    /**
     * Test that routes are loaded on initialization
     */
    @Test
    fun `routes are loaded on initialization`() {
        val routes = viewModel.routes.value

        assertNotNull("Routes should not be null", routes)
        assertTrue("Routes should not be empty", routes!!.isNotEmpty())
        assertEquals("Should have 8 routes", 8, routes.size)
    }

    /**
     * Test that all expected route IDs are present
     */
    @Test
    fun `all route IDs are present`() {
        val routes = viewModel.routes.value!!
        val routeIds = routes.map { it.routeId }

        assertTrue("Route A should exist", routeIds.contains("A"))
        assertTrue("Route B should exist", routeIds.contains("B"))
        assertTrue("Route C1 should exist", routeIds.contains("C1"))
        assertTrue("Route C2 should exist", routeIds.contains("C2"))
        assertTrue("Route D should exist", routeIds.contains("D"))
        assertTrue("Route E1 should exist", routeIds.contains("E1"))
        assertTrue("Route E2 should exist", routeIds.contains("E2"))
        assertTrue("Route G should exist", routeIds.contains("G"))
    }

    /**
     * Test default day type is WEEKDAY
     */
    @Test
    fun `default day type is WEEKDAY`() {
        val dayType = viewModel.selectedDayType.value

        assertEquals("Default day type should be WEEKDAY", DayType.WEEKDAY, dayType)
    }

    /**
     * Test setting day type to FRIDAY
     */
    @Test
    fun `setDayType changes to FRIDAY`() {
        viewModel.setDayType(DayType.FRIDAY)

        assertEquals("Day type should be FRIDAY", DayType.FRIDAY, viewModel.selectedDayType.value)
    }

    /**
     * Test setting day type to WEEKEND
     */
    @Test
    fun `setDayType changes to WEEKEND`() {
        viewModel.setDayType(DayType.WEEKEND)

        assertEquals("Day type should be WEEKEND", DayType.WEEKEND, viewModel.selectedDayType.value)
    }

    /**
     * Test getting schedule for Route B on weekday
     */
    @Test
    fun `getScheduleForRoute returns weekday schedule for Route B`() {
        val schedule = viewModel.getScheduleForRoute("B", DayType.WEEKDAY)

        assertNotNull("Route B weekday schedule should not be null", schedule)
        assertTrue("Route B should have weekday departures", schedule!!.departureFromCampus.isNotEmpty())
        assertTrue("Route B should have weekday returns", schedule.returnToCampus.isNotEmpty())
    }

    /**
     * Test getting schedule for Route B on weekend
     */
    @Test
    fun `getScheduleForRoute returns weekend schedule for Route B`() {
        val schedule = viewModel.getScheduleForRoute("B", DayType.WEEKEND)

        assertNotNull("Route B weekend schedule should not be null", schedule)
        assertTrue("Route B should have weekend departures", schedule!!.departureFromCampus.isNotEmpty())
    }

    /**
     * Test that Route A has no weekend schedule
     */
    @Test
    fun `Route A has no weekend schedule`() {
        val schedule = viewModel.getScheduleForRoute("A", DayType.WEEKEND)

        assertNull("Route A weekend schedule should be null", schedule)
    }

    /**
     * Test that Route E1 is Friday only
     */
    @Test
    fun `Route E1 has only Friday schedule`() {
        val weekdaySchedule = viewModel.getScheduleForRoute("E1", DayType.WEEKDAY)
        val fridaySchedule = viewModel.getScheduleForRoute("E1", DayType.FRIDAY)
        val weekendSchedule = viewModel.getScheduleForRoute("E1", DayType.WEEKEND)

        assertNull("Route E1 weekday schedule should be null", weekdaySchedule)
        assertNotNull("Route E1 Friday schedule should not be null", fridaySchedule)
        assertNull("Route E1 weekend schedule should be null", weekendSchedule)
    }

    /**
     * Test that Route G is weekend only
     */
    @Test
    fun `Route G has only weekend schedule`() {
        val weekdaySchedule = viewModel.getScheduleForRoute("G", DayType.WEEKDAY)
        val fridaySchedule = viewModel.getScheduleForRoute("G", DayType.FRIDAY)
        val weekendSchedule = viewModel.getScheduleForRoute("G", DayType.WEEKEND)

        assertNull("Route G weekday schedule should be null", weekdaySchedule)
        assertNull("Route G Friday schedule should be null", fridaySchedule)
        assertNotNull("Route G weekend schedule should not be null", weekendSchedule)
    }

    /**
     * Test getting schedule for non-existent route returns null
     */
    @Test
    fun `getScheduleForRoute returns null for non-existent route`() {
        val schedule = viewModel.getScheduleForRoute("X", DayType.WEEKDAY)

        assertNull("Non-existent route should return null schedule", schedule)
    }

    /**
     * Test Route B has special note about MRT
     */
    @Test
    fun `Route B has special note`() {
        val routes = viewModel.routes.value!!
        val routeB = routes.find { it.routeId == "B" }

        assertNotNull("Route B should exist", routeB)
        assertNotNull("Route B should have special note", routeB!!.specialNote)
        assertTrue("Route B note should mention MRT", routeB.specialNote!!.contains("MRT"))
    }

    /**
     * Test all routes have required fields
     */
    @Test
    fun `all routes have required fields`() {
        val routes = viewModel.routes.value!!

        routes.forEach { route ->
            assertNotNull("Route ${route.routeId} should have routeName", route.routeName)
            assertNotNull("Route ${route.routeId} should have description", route.description)
            assertTrue("Route ${route.routeId} should have routeName", route.routeName.isNotEmpty())
            assertTrue("Route ${route.routeId} should have description", route.description.isNotEmpty())
        }
    }
}
