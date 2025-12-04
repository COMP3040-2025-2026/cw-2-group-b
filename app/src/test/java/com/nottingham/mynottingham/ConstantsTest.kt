package com.nottingham.mynottingham

import com.nottingham.mynottingham.util.Constants
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Constants
 *
 * Tests the application constants:
 * - Firebase configuration
 * - Database configuration
 * - Message configuration
 * - Delivery fees
 * - Time formats
 */
class ConstantsTest {

    /**
     * Test Firebase Database URL is valid
     */
    @Test
    fun `Firebase database URL is valid`() {
        val url = Constants.FIREBASE_DATABASE_URL

        assertTrue("URL should start with https", url.startsWith("https://"))
        assertTrue("URL should contain firebase", url.contains("firebase"))
        assertTrue("URL should contain asia-southeast1", url.contains("asia-southeast1"))
    }

    /**
     * Test database name is not empty
     */
    @Test
    fun `Database name is valid`() {
        val dbName = Constants.DATABASE_NAME

        assertNotNull("Database name should not be null", dbName)
        assertTrue("Database name should not be empty", dbName.isNotEmpty())
        assertEquals("my_nottingham_db", dbName)
    }

    /**
     * Test database version is positive
     */
    @Test
    fun `Database version is positive`() {
        val version = Constants.DATABASE_VERSION

        assertTrue("Database version should be positive", version > 0)
    }

    /**
     * Test message retention days is reasonable
     */
    @Test
    fun `Message retention days is 7`() {
        val days = Constants.MESSAGE_RETENTION_DAYS

        assertEquals("Message retention should be 7 days", 7, days)
    }

    /**
     * Test message retention millis is calculated correctly
     */
    @Test
    fun `Message retention millis is calculated correctly`() {
        val millis = Constants.MESSAGE_RETENTION_MILLIS
        val expectedMillis = 7L * 24 * 60 * 60 * 1000

        assertEquals("Message retention millis should match 7 days", expectedMillis, millis)
    }

    /**
     * Test max message length
     */
    @Test
    fun `Max message length is 500`() {
        assertEquals("Max message length should be 500", 500, Constants.MAX_MESSAGE_LENGTH)
    }

    /**
     * Test typing indicator timeout
     */
    @Test
    fun `Typing indicator timeout is 3 seconds`() {
        assertEquals("Typing timeout should be 3000ms", 3000L, Constants.TYPING_INDICATOR_TIMEOUT_MS)
    }

    /**
     * Test message page size
     */
    @Test
    fun `Message page size is 50`() {
        assertEquals("Message page size should be 50", 50, Constants.MESSAGE_PAGE_SIZE)
    }

    /**
     * Test delivery fees
     */
    @Test
    fun `Delivery fees are correct`() {
        assertEquals("Standard delivery fee should be 2.0", 2.0, Constants.DELIVERY_FEE_STANDARD, 0.01)
        assertEquals("Express delivery fee should be 5.0", 5.0, Constants.DELIVERY_FEE_EXPRESS, 0.01)
    }

    /**
     * Test time formats are valid
     */
    @Test
    fun `Time formats are valid strings`() {
        assertEquals("HH:mm", Constants.TIME_FORMAT_24H)
        assertEquals("hh:mm a", Constants.TIME_FORMAT_12H)
        assertEquals("dd/MM/yyyy", Constants.DATE_FORMAT)
        assertEquals("dd/MM/yyyy HH:mm", Constants.DATE_TIME_FORMAT)
    }

    /**
     * Test notification channel IDs
     */
    @Test
    fun `Notification channel IDs are valid`() {
        assertNotNull(Constants.CHANNEL_ID_GENERAL)
        assertNotNull(Constants.CHANNEL_ID_BOOKING)
        assertNotNull(Constants.CHANNEL_ID_ERRAND)
        assertNotNull(Constants.CHANNEL_ID_MESSAGE)

        assertTrue(Constants.CHANNEL_ID_GENERAL.isNotEmpty())
        assertTrue(Constants.CHANNEL_ID_BOOKING.isNotEmpty())
        assertTrue(Constants.CHANNEL_ID_ERRAND.isNotEmpty())
        assertTrue(Constants.CHANNEL_ID_MESSAGE.isNotEmpty())
    }

    /**
     * Test image configuration
     */
    @Test
    fun `Image configuration is valid`() {
        assertEquals("Max image size should be 5MB", 5, Constants.MAX_IMAGE_SIZE_MB)
        assertEquals("Cache size should be 10MB", 10 * 1024 * 1024L, Constants.CACHE_SIZE)
    }

    /**
     * Test shuttle routes list
     */
    @Test
    fun `Shuttle routes list is not empty`() {
        val routes = Constants.SHUTTLE_ROUTES

        assertNotNull("Shuttle routes should not be null", routes)
        assertTrue("Shuttle routes should not be empty", routes.isNotEmpty())
    }

    /**
     * Test request codes are unique
     */
    @Test
    fun `Request codes are unique`() {
        val codes = listOf(
            Constants.REQUEST_CODE_PICK_IMAGE,
            Constants.REQUEST_CODE_CAMERA,
            Constants.REQUEST_CODE_LOCATION
        )

        assertEquals("All request codes should be unique", codes.size, codes.distinct().size)
    }

    /**
     * Test Facilities nested object
     */
    @Test
    fun `Facilities constants are valid`() {
        assertEquals("3G Pitch", Constants.Facilities.FACILITY_3G_PITCH)
        assertEquals("Badminton Court 1", Constants.Facilities.FACILITY_BADMINTON_COURT_1)
        assertEquals("Tennis Court 1", Constants.Facilities.FACILITY_TENNIS_COURT_1)
        assertEquals("Sports Hall Court 1", Constants.Facilities.FACILITY_SPORTS_HALL_1)
        assertEquals("Squash Court 1", Constants.Facilities.FACILITY_SQUASH_COURT_1)
    }

    /**
     * Test Forum Categories
     */
    @Test
    fun `Forum categories are valid`() {
        assertEquals("Study", Constants.ForumCategories.CATEGORY_STUDY)
        assertEquals("Events", Constants.ForumCategories.CATEGORY_EVENTS)
        assertEquals("Career", Constants.ForumCategories.CATEGORY_CAREER)
        assertEquals("Questions", Constants.ForumCategories.CATEGORY_QUESTIONS)
        assertEquals("General", Constants.ForumCategories.CATEGORY_GENERAL)
        assertEquals("Food", Constants.ForumCategories.CATEGORY_FOOD)
    }

    /**
     * Test Errand Types
     */
    @Test
    fun `Errand types are valid`() {
        assertEquals("Food Delivery", Constants.ErrandTypes.TYPE_FOOD_DELIVERY)
        assertEquals("Package Pickup", Constants.ErrandTypes.TYPE_PACKAGE_PICKUP)
        assertEquals("Document Delivery", Constants.ErrandTypes.TYPE_DOCUMENT_DELIVERY)
        assertEquals("Shopping", Constants.ErrandTypes.TYPE_SHOPPING)
        assertEquals("Other", Constants.ErrandTypes.TYPE_OTHER)
    }

    /**
     * Test Errand Priority
     */
    @Test
    fun `Errand priority values are valid`() {
        assertEquals("Standard", Constants.ErrandPriority.PRIORITY_STANDARD)
        assertEquals("Express", Constants.ErrandPriority.PRIORITY_EXPRESS)
    }

    /**
     * Test Message Types
     */
    @Test
    fun `Message types are valid`() {
        assertEquals("TEXT", Constants.MessageTypes.TEXT)
        assertEquals("IMAGE", Constants.MessageTypes.IMAGE)
        assertEquals("FILE", Constants.MessageTypes.FILE)
    }

    /**
     * Test preferences keys are unique and valid
     */
    @Test
    fun `Preferences keys are valid`() {
        val keys = listOf(
            Constants.PREFS_NAME,
            Constants.KEY_USER_TOKEN,
            Constants.KEY_USER_ID,
            Constants.KEY_IS_LOGGED_IN,
            Constants.KEY_DELIVERY_MODE,
            Constants.KEY_ERRAND_NOTIFICATIONS
        )

        keys.forEach { key ->
            assertNotNull("Key should not be null: $key", key)
            assertTrue("Key should not be empty: $key", key.isNotEmpty())
        }

        // Verify uniqueness
        assertEquals("All keys should be unique", keys.size, keys.distinct().size)
    }
}
