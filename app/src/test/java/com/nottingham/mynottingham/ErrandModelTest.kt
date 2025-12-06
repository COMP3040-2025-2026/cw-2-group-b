package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.Errand
import com.nottingham.mynottingham.data.model.ErrandBalance
import com.nottingham.mynottingham.data.model.ErrandStatus
import com.nottingham.mynottingham.data.model.ErrandType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Errand data models
 *
 * Tests the errand system data classes and enums:
 * - Errand data class
 * - ErrandType enum
 * - ErrandStatus enum
 * - ErrandBalance data class
 */
class ErrandModelTest {

    /**
     * Test ErrandType enum has all expected values
     */
    @Test
    fun `ErrandType enum has all expected values`() {
        val types = ErrandType.values()

        assertEquals("Should have 4 errand types", 4, types.size)
        assertTrue("Should contain SHOPPING", types.contains(ErrandType.SHOPPING))
        assertTrue("Should contain PICKUP", types.contains(ErrandType.PICKUP))
        assertTrue("Should contain FOOD_DELIVERY", types.contains(ErrandType.FOOD_DELIVERY))
        assertTrue("Should contain OTHER", types.contains(ErrandType.OTHER))
    }

    /**
     * Test ErrandType display names
     */
    @Test
    fun `ErrandType has correct display names`() {
        assertEquals("Shopping", ErrandType.SHOPPING.displayName)
        assertEquals("Pickup", ErrandType.PICKUP.displayName)
        assertEquals("Food Delivery", ErrandType.FOOD_DELIVERY.displayName)
        assertEquals("Others", ErrandType.OTHER.displayName)
    }

    /**
     * Test ErrandType colors are valid hex colors
     */
    @Test
    fun `ErrandType colors are valid hex format`() {
        ErrandType.values().forEach { type ->
            assertTrue(
                "Color ${type.color} should start with #",
                type.color.startsWith("#")
            )
            assertEquals(
                "Color ${type.color} should be 7 characters",
                7,
                type.color.length
            )
        }
    }

    /**
     * Test ErrandStatus enum has all expected values
     */
    @Test
    fun `ErrandStatus enum has all expected values`() {
        val statuses = ErrandStatus.values()

        assertEquals("Should have 6 errand statuses", 6, statuses.size)
        assertTrue("Should contain PENDING", statuses.contains(ErrandStatus.PENDING))
        assertTrue("Should contain ACCEPTED", statuses.contains(ErrandStatus.ACCEPTED))
        assertTrue("Should contain DELIVERING", statuses.contains(ErrandStatus.DELIVERING))
        assertTrue("Should contain IN_PROGRESS", statuses.contains(ErrandStatus.IN_PROGRESS))
        assertTrue("Should contain COMPLETED", statuses.contains(ErrandStatus.COMPLETED))
        assertTrue("Should contain CANCELLED", statuses.contains(ErrandStatus.CANCELLED))
    }

    /**
     * Test ErrandStatus valueOf
     */
    @Test
    fun `ErrandStatus valueOf returns correct enum`() {
        assertEquals(ErrandStatus.PENDING, ErrandStatus.valueOf("PENDING"))
        assertEquals(ErrandStatus.ACCEPTED, ErrandStatus.valueOf("ACCEPTED"))
        assertEquals(ErrandStatus.DELIVERING, ErrandStatus.valueOf("DELIVERING"))
        assertEquals(ErrandStatus.IN_PROGRESS, ErrandStatus.valueOf("IN_PROGRESS"))
        assertEquals(ErrandStatus.COMPLETED, ErrandStatus.valueOf("COMPLETED"))
        assertEquals(ErrandStatus.CANCELLED, ErrandStatus.valueOf("CANCELLED"))
    }

    /**
     * Test Errand creation with required fields
     */
    @Test
    fun `Errand creation with required fields`() {
        val errand = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John Doe",
            title = "Pick up package",
            description = "Need help picking up a package from BB building",
            type = ErrandType.PICKUP,
            location = "BB Building",
            deadline = "2024-12-15 17:00",
            reward = 10.0,
            status = ErrandStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        assertEquals("errand123", errand.id)
        assertEquals("user456", errand.requesterId)
        assertEquals("John Doe", errand.requesterName)
        assertEquals("Pick up package", errand.title)
        assertEquals(ErrandType.PICKUP, errand.type)
        assertEquals(ErrandStatus.PENDING, errand.status)
        assertEquals(10.0, errand.reward, 0.01)
    }

    /**
     * Test Errand default values
     */
    @Test
    fun `Errand has correct default values`() {
        val errand = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John Doe",
            title = "Test errand",
            description = "Description",
            type = ErrandType.OTHER,
            location = "Campus",
            deadline = "2024-12-15",
            reward = 5.0,
            status = ErrandStatus.PENDING,
            createdAt = 0L,
            updatedAt = 0L
        )

        assertNull("requesterAvatar should be null by default", errand.requesterAvatar)
        assertEquals("requesterRating should be 0.0 by default", 0.0, errand.requesterRating, 0.01)
        assertEquals("requesterReviewCount should be 0 by default", 0, errand.requesterReviewCount)
        assertNull("providerId should be null by default", errand.providerId)
        assertNull("providerName should be null by default", errand.providerName)
        assertNull("additionalNotes should be null by default", errand.additionalNotes)
        assertNull("imageUrl should be null by default", errand.imageUrl)
        assertNull("completedAt should be null by default", errand.completedAt)
    }

    /**
     * Test Errand with provider assigned
     */
    @Test
    fun `Errand with provider assigned`() {
        val errand = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John Doe",
            providerId = "provider789",
            providerName = "Jane Helper",
            title = "Food delivery",
            description = "Get lunch from cafeteria",
            type = ErrandType.FOOD_DELIVERY,
            location = "Cafeteria",
            deadline = "2024-12-15 12:00",
            reward = 8.0,
            status = ErrandStatus.ACCEPTED,
            createdAt = 0L,
            updatedAt = 0L
        )

        assertEquals("provider789", errand.providerId)
        assertEquals("Jane Helper", errand.providerName)
        assertEquals(ErrandStatus.ACCEPTED, errand.status)
    }

    /**
     * Test Errand with all optional fields
     */
    @Test
    fun `Errand with all optional fields`() {
        val errand = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John Doe",
            requesterAvatar = "avatar1",
            requesterRating = 4.5,
            requesterReviewCount = 10,
            providerId = "provider789",
            providerName = "Jane Helper",
            title = "Shopping task",
            description = "Buy items from store",
            type = ErrandType.SHOPPING,
            location = "UniMart",
            deadline = "2024-12-15 18:00",
            reward = 15.0,
            additionalNotes = "Handle with care",
            status = ErrandStatus.COMPLETED,
            imageUrl = "https://example.com/image.jpg",
            createdAt = 1000L,
            updatedAt = 2000L,
            completedAt = 3000L
        )

        assertEquals("avatar1", errand.requesterAvatar)
        assertEquals(4.5, errand.requesterRating, 0.01)
        assertEquals(10, errand.requesterReviewCount)
        assertEquals("Handle with care", errand.additionalNotes)
        assertEquals("https://example.com/image.jpg", errand.imageUrl)
        assertEquals(3000L, errand.completedAt)
    }

    /**
     * Test Errand data class equality
     */
    @Test
    fun `Errand data class equality`() {
        val errand1 = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John",
            title = "Test",
            description = "Desc",
            type = ErrandType.OTHER,
            location = "Loc",
            deadline = "2024-12-15",
            reward = 5.0,
            status = ErrandStatus.PENDING,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val errand2 = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John",
            title = "Test",
            description = "Desc",
            type = ErrandType.OTHER,
            location = "Loc",
            deadline = "2024-12-15",
            reward = 5.0,
            status = ErrandStatus.PENDING,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals("Errands with same data should be equal", errand1, errand2)
    }

    /**
     * Test Errand copy function
     */
    @Test
    fun `Errand copy function works correctly`() {
        val original = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John",
            title = "Original",
            description = "Desc",
            type = ErrandType.PICKUP,
            location = "Loc",
            deadline = "2024-12-15",
            reward = 5.0,
            status = ErrandStatus.PENDING,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val updated = original.copy(status = ErrandStatus.COMPLETED, completedAt = 2000L)

        assertEquals("errand123", updated.id)
        assertEquals(ErrandStatus.COMPLETED, updated.status)
        assertEquals(2000L, updated.completedAt)
        assertEquals(ErrandStatus.PENDING, original.status)
    }

    /**
     * Test ErrandBalance creation
     */
    @Test
    fun `ErrandBalance creation with all fields`() {
        val balance = ErrandBalance(
            userId = "user123",
            balance = 50.0,
            totalEarned = 100.0,
            totalSpent = 50.0
        )

        assertEquals("user123", balance.userId)
        assertEquals(50.0, balance.balance, 0.01)
        assertEquals(100.0, balance.totalEarned, 0.01)
        assertEquals(50.0, balance.totalSpent, 0.01)
    }

    /**
     * Test ErrandBalance with zero values
     */
    @Test
    fun `ErrandBalance with zero values`() {
        val balance = ErrandBalance(
            userId = "newuser",
            balance = 0.0,
            totalEarned = 0.0,
            totalSpent = 0.0
        )

        assertEquals(0.0, balance.balance, 0.01)
        assertEquals(0.0, balance.totalEarned, 0.01)
        assertEquals(0.0, balance.totalSpent, 0.01)
    }

    /**
     * Test ErrandBalance data class equality
     */
    @Test
    fun `ErrandBalance data class equality`() {
        val balance1 = ErrandBalance(
            userId = "user123",
            balance = 50.0,
            totalEarned = 100.0,
            totalSpent = 50.0
        )

        val balance2 = ErrandBalance(
            userId = "user123",
            balance = 50.0,
            totalEarned = 100.0,
            totalSpent = 50.0
        )

        assertEquals("Balances with same data should be equal", balance1, balance2)
    }

    /**
     * Test ErrandBalance copy function
     */
    @Test
    fun `ErrandBalance copy function works correctly`() {
        val original = ErrandBalance(
            userId = "user123",
            balance = 50.0,
            totalEarned = 100.0,
            totalSpent = 50.0
        )

        val updated = original.copy(balance = 75.0, totalEarned = 125.0)

        assertEquals("user123", updated.userId)
        assertEquals(75.0, updated.balance, 0.01)
        assertEquals(125.0, updated.totalEarned, 0.01)
        assertEquals(50.0, updated.totalSpent, 0.01)
    }

    /**
     * Test Errand reward validation
     */
    @Test
    fun `Errand reward is positive number`() {
        val errand = Errand(
            id = "errand123",
            requesterId = "user456",
            requesterName = "John",
            title = "Test",
            description = "Desc",
            type = ErrandType.OTHER,
            location = "Loc",
            deadline = "2024-12-15",
            reward = 10.0,
            status = ErrandStatus.PENDING,
            createdAt = 0L,
            updatedAt = 0L
        )

        assertTrue("Reward should be positive", errand.reward > 0)
    }

    /**
     * Test different ErrandType for different use cases
     */
    @Test
    fun `ErrandType used for different use cases`() {
        val shoppingErrand = Errand(
            id = "1", requesterId = "u1", requesterName = "User",
            title = "Buy groceries", description = "Buy from store",
            type = ErrandType.SHOPPING, location = "Store",
            deadline = "2024-12-15", reward = 5.0, status = ErrandStatus.PENDING,
            createdAt = 0L, updatedAt = 0L
        )

        val pickupErrand = Errand(
            id = "2", requesterId = "u2", requesterName = "User2",
            title = "Pick up parcel", description = "Get from mailroom",
            type = ErrandType.PICKUP, location = "Mailroom",
            deadline = "2024-12-15", reward = 3.0, status = ErrandStatus.PENDING,
            createdAt = 0L, updatedAt = 0L
        )

        val foodErrand = Errand(
            id = "3", requesterId = "u3", requesterName = "User3",
            title = "Lunch delivery", description = "From cafeteria",
            type = ErrandType.FOOD_DELIVERY, location = "Cafeteria",
            deadline = "2024-12-15", reward = 8.0, status = ErrandStatus.PENDING,
            createdAt = 0L, updatedAt = 0L
        )

        assertEquals(ErrandType.SHOPPING, shoppingErrand.type)
        assertEquals(ErrandType.PICKUP, pickupErrand.type)
        assertEquals(ErrandType.FOOD_DELIVERY, foodErrand.type)
    }
}
