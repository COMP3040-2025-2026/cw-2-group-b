package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.Booking
import com.nottingham.mynottingham.data.model.BookingStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Booking data models
 *
 * Tests the sports booking system data classes and enums:
 * - Booking data class
 * - BookingStatus enum
 */
class BookingModelTest {

    /**
     * Test BookingStatus enum has all expected values
     */
    @Test
    fun `BookingStatus enum has all expected values`() {
        val statuses = BookingStatus.values()

        assertEquals("Should have 3 booking statuses", 3, statuses.size)
        assertTrue("Should contain CONFIRMED", statuses.contains(BookingStatus.CONFIRMED))
        assertTrue("Should contain CANCELLED", statuses.contains(BookingStatus.CANCELLED))
        assertTrue("Should contain COMPLETED", statuses.contains(BookingStatus.COMPLETED))
    }

    /**
     * Test BookingStatus valueOf
     */
    @Test
    fun `BookingStatus valueOf returns correct enum`() {
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.valueOf("CONFIRMED"))
        assertEquals(BookingStatus.CANCELLED, BookingStatus.valueOf("CANCELLED"))
        assertEquals(BookingStatus.COMPLETED, BookingStatus.valueOf("COMPLETED"))
    }

    /**
     * Test Booking creation with required fields
     */
    @Test
    fun `Booking creation with required fields`() {
        val booking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Badminton Court",
            facilityName = "Badminton Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = System.currentTimeMillis()
        )

        assertEquals("booking123", booking.id)
        assertEquals("user456", booking.userId)
        assertEquals("Badminton Court", booking.facilityType)
        assertEquals("Badminton Court 1", booking.facilityName)
        assertEquals("2024-12-15", booking.bookingDate)
        assertEquals("10:00", booking.startTime)
        assertEquals("11:00", booking.endTime)
        assertEquals(BookingStatus.CONFIRMED, booking.status)
    }

    /**
     * Test Booking default values
     */
    @Test
    fun `Booking has correct default values`() {
        val booking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Tennis Court",
            facilityName = "Tennis Court 1",
            bookingDate = "2024-12-15",
            startTime = "14:00",
            endTime = "15:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 0L
        )

        assertNull("notes should be null by default", booking.notes)
    }

    /**
     * Test Booking with notes
     */
    @Test
    fun `Booking with notes`() {
        val booking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "3G Pitch",
            facilityName = "3G Pitch",
            bookingDate = "2024-12-15",
            startTime = "16:00",
            endTime = "18:00",
            status = BookingStatus.CONFIRMED,
            notes = "Team practice session",
            createdAt = 0L
        )

        assertEquals("Team practice session", booking.notes)
    }

    /**
     * Test Booking for different facility types
     */
    @Test
    fun `Booking for different facility types`() {
        val badmintonBooking = Booking(
            id = "1", userId = "u1",
            facilityType = "Badminton Court",
            facilityName = "Badminton Court 1",
            bookingDate = "2024-12-15", startTime = "10:00", endTime = "11:00",
            status = BookingStatus.CONFIRMED, createdAt = 0L
        )

        val tennisBooking = Booking(
            id = "2", userId = "u2",
            facilityType = "Tennis Court",
            facilityName = "Tennis Court 1",
            bookingDate = "2024-12-15", startTime = "11:00", endTime = "12:00",
            status = BookingStatus.CONFIRMED, createdAt = 0L
        )

        val pitchBooking = Booking(
            id = "3", userId = "u3",
            facilityType = "3G Pitch",
            facilityName = "3G Pitch",
            bookingDate = "2024-12-15", startTime = "14:00", endTime = "16:00",
            status = BookingStatus.CONFIRMED, createdAt = 0L
        )

        val squashBooking = Booking(
            id = "4", userId = "u4",
            facilityType = "Squash Court",
            facilityName = "Squash Court 1",
            bookingDate = "2024-12-15", startTime = "15:00", endTime = "16:00",
            status = BookingStatus.CONFIRMED, createdAt = 0L
        )

        assertEquals("Badminton Court", badmintonBooking.facilityType)
        assertEquals("Tennis Court", tennisBooking.facilityType)
        assertEquals("3G Pitch", pitchBooking.facilityType)
        assertEquals("Squash Court", squashBooking.facilityType)
    }

    /**
     * Test Booking with different statuses
     */
    @Test
    fun `Booking with different statuses`() {
        val confirmedBooking = Booking(
            id = "1", userId = "u1",
            facilityType = "Badminton Court", facilityName = "Court 1",
            bookingDate = "2024-12-15", startTime = "10:00", endTime = "11:00",
            status = BookingStatus.CONFIRMED, createdAt = 0L
        )

        val cancelledBooking = Booking(
            id = "2", userId = "u1",
            facilityType = "Badminton Court", facilityName = "Court 1",
            bookingDate = "2024-12-14", startTime = "10:00", endTime = "11:00",
            status = BookingStatus.CANCELLED, createdAt = 0L
        )

        val completedBooking = Booking(
            id = "3", userId = "u1",
            facilityType = "Badminton Court", facilityName = "Court 1",
            bookingDate = "2024-12-13", startTime = "10:00", endTime = "11:00",
            status = BookingStatus.COMPLETED, createdAt = 0L
        )

        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.status)
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.status)
        assertEquals(BookingStatus.COMPLETED, completedBooking.status)
    }

    /**
     * Test Booking data class equality
     */
    @Test
    fun `Booking data class equality`() {
        val booking1 = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Badminton Court",
            facilityName = "Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 1000L
        )

        val booking2 = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Badminton Court",
            facilityName = "Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 1000L
        )

        assertEquals("Bookings with same data should be equal", booking1, booking2)
    }

    /**
     * Test Booking copy function
     */
    @Test
    fun `Booking copy function works correctly`() {
        val original = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Badminton Court",
            facilityName = "Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 1000L
        )

        val cancelled = original.copy(status = BookingStatus.CANCELLED)

        assertEquals("booking123", cancelled.id)
        assertEquals(BookingStatus.CANCELLED, cancelled.status)
        assertEquals(BookingStatus.CONFIRMED, original.status)
    }

    /**
     * Test Booking time slot format
     */
    @Test
    fun `Booking time slot format`() {
        val booking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Tennis Court",
            facilityName = "Tennis Court 1",
            bookingDate = "2024-12-15",
            startTime = "09:00",
            endTime = "10:30",
            status = BookingStatus.CONFIRMED,
            createdAt = 0L
        )

        assertTrue(booking.startTime.contains(":"))
        assertTrue(booking.endTime.contains(":"))
        assertEquals("09:00", booking.startTime)
        assertEquals("10:30", booking.endTime)
    }

    /**
     * Test Booking date format
     */
    @Test
    fun `Booking date format`() {
        val booking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Squash Court",
            facilityName = "Squash Court 1",
            bookingDate = "2024-12-15",
            startTime = "14:00",
            endTime = "15:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 0L
        )

        assertTrue(booking.bookingDate.contains("-"))
        val parts = booking.bookingDate.split("-")
        assertEquals(3, parts.size)
        assertEquals("2024", parts[0])
        assertEquals("12", parts[1])
        assertEquals("15", parts[2])
    }

    /**
     * Test multiple bookings for same user
     */
    @Test
    fun `Multiple bookings for same user`() {
        val booking1 = Booking(
            id = "b1", userId = "user123",
            facilityType = "Badminton Court", facilityName = "Court 1",
            bookingDate = "2024-12-15", startTime = "10:00", endTime = "11:00",
            status = BookingStatus.CONFIRMED, createdAt = 1000L
        )

        val booking2 = Booking(
            id = "b2", userId = "user123",
            facilityType = "Tennis Court", facilityName = "Court 1",
            bookingDate = "2024-12-15", startTime = "14:00", endTime = "15:00",
            status = BookingStatus.CONFIRMED, createdAt = 2000L
        )

        val booking3 = Booking(
            id = "b3", userId = "user123",
            facilityType = "3G Pitch", facilityName = "3G Pitch",
            bookingDate = "2024-12-16", startTime = "16:00", endTime = "18:00",
            status = BookingStatus.CONFIRMED, createdAt = 3000L
        )

        val userBookings = listOf(booking1, booking2, booking3)
        assertEquals(3, userBookings.size)
        assertTrue(userBookings.all { it.userId == "user123" })
    }

    /**
     * Test Booking status transitions
     */
    @Test
    fun `Booking status transitions`() {
        val newBooking = Booking(
            id = "booking123",
            userId = "user456",
            facilityType = "Badminton Court",
            facilityName = "Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 1000L
        )

        // Simulate cancellation
        val cancelledBooking = newBooking.copy(status = BookingStatus.CANCELLED)
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.status)

        // Create a new confirmed booking
        val confirmedBooking = Booking(
            id = "booking456",
            userId = "user456",
            facilityType = "Tennis Court",
            facilityName = "Court 1",
            bookingDate = "2024-12-14",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 2000L
        )

        // Simulate completion
        val completedBooking = confirmedBooking.copy(status = BookingStatus.COMPLETED)
        assertEquals(BookingStatus.COMPLETED, completedBooking.status)
    }

    /**
     * Test Booking createdAt timestamp
     */
    @Test
    fun `Booking createdAt timestamp ordering`() {
        val bookings = listOf(
            Booking("b1", "u1", "Court", "Court 1", "2024-12-15", "10:00", "11:00", BookingStatus.CONFIRMED, createdAt = 1000L),
            Booking("b2", "u1", "Court", "Court 2", "2024-12-15", "11:00", "12:00", BookingStatus.CONFIRMED, createdAt = 2000L),
            Booking("b3", "u1", "Court", "Court 3", "2024-12-15", "12:00", "13:00", BookingStatus.CONFIRMED, createdAt = 3000L)
        )

        val sorted = bookings.sortedByDescending { it.createdAt }

        assertEquals("b3", sorted[0].id)
        assertEquals("b2", sorted[1].id)
        assertEquals("b1", sorted[2].id)
    }

    /**
     * Test Booking facility naming convention
     */
    @Test
    fun `Booking facility naming convention`() {
        val sportsHallBooking = Booking(
            id = "b1", userId = "u1",
            facilityType = "Sports Hall",
            facilityName = "Sports Hall Court 1",
            bookingDate = "2024-12-15",
            startTime = "10:00",
            endTime = "11:00",
            status = BookingStatus.CONFIRMED,
            createdAt = 0L
        )

        assertTrue(sportsHallBooking.facilityName.contains(sportsHallBooking.facilityType))
    }
}
