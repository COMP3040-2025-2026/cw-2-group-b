package com.nottingham.mynottingham.util

/**
 * Application-wide constants
 */
object Constants {
    // API Configuration
    // Use 10.0.2.2 to access host machine's localhost from Android emulator
    const val BASE_URL = "http://10.0.2.2:8080/api/"
    const val TIMEOUT_SECONDS = 30L

    // Database Configuration
    const val DATABASE_NAME = "my_nottingham_db"
    const val DATABASE_VERSION = 2

    // SharedPreferences / DataStore
    const val PREFS_NAME = "my_nottingham_prefs"
    const val KEY_USER_TOKEN = "user_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_DELIVERY_MODE = "delivery_mode"
    const val KEY_ERRAND_NOTIFICATIONS = "errand_notifications"

    // Image Configuration
    const val MAX_IMAGE_SIZE_MB = 5
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB

    // Shuttle Routes
    val SHUTTLE_ROUTES = listOf("A", "B", "C", "D", "G")

    // Sports Facilities
    object Facilities {
        const val FACILITY_3G_PITCH = "3G Pitch"
        const val FACILITY_BADMINTON_COURT_1 = "Badminton Court 1"
        const val FACILITY_BADMINTON_COURT_2 = "Badminton Court 2"
        const val FACILITY_TENNIS_COURT_1 = "Tennis Court 1"
        const val FACILITY_TENNIS_COURT_2 = "Tennis Court 2"
        const val FACILITY_SPORTS_HALL_1 = "Sports Hall Court 1"
        const val FACILITY_SPORTS_HALL_2 = "Sports Hall Court 2"
        const val FACILITY_SQUASH_COURT_1 = "Squash Court 1"
        const val FACILITY_SQUASH_COURT_2 = "Squash Court 2"
    }

    // Forum Categories
    object ForumCategories {
        const val CATEGORY_STUDY = "Study"
        const val CATEGORY_EVENTS = "Events"
        const val CATEGORY_CAREER = "Career"
        const val CATEGORY_QUESTIONS = "Questions"
        const val CATEGORY_GENERAL = "General"
        const val CATEGORY_FOOD = "Food"
    }

    // Errand Types
    object ErrandTypes {
        const val TYPE_FOOD_DELIVERY = "Food Delivery"
        const val TYPE_PACKAGE_PICKUP = "Package Pickup"
        const val TYPE_DOCUMENT_DELIVERY = "Document Delivery"
        const val TYPE_SHOPPING = "Shopping"
        const val TYPE_OTHER = "Other"
    }

    // Errand Priority
    object ErrandPriority {
        const val PRIORITY_STANDARD = "Standard" // 1-2 hours
        const val PRIORITY_EXPRESS = "Express" // 30 minutes
    }

    // Delivery Fee
    const val DELIVERY_FEE_STANDARD = 2.0
    const val DELIVERY_FEE_EXPRESS = 5.0

    // Time Formats
    const val TIME_FORMAT_24H = "HH:mm"
    const val TIME_FORMAT_12H = "hh:mm a"
    const val DATE_FORMAT = "dd/MM/yyyy"
    const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"

    // Notification Channels
    const val CHANNEL_ID_GENERAL = "general_notifications"
    const val CHANNEL_ID_BOOKING = "booking_notifications"
    const val CHANNEL_ID_ERRAND = "errand_notifications"
    const val CHANNEL_ID_MESSAGE = "message_notifications"

    // Message Configuration
    const val MESSAGE_RETENTION_DAYS = 7
    const val MESSAGE_RETENTION_MILLIS = MESSAGE_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    const val MAX_MESSAGE_LENGTH = 500
    const val TYPING_INDICATOR_TIMEOUT_MS = 3000L
    const val MESSAGE_PAGE_SIZE = 50

    // Message Types
    object MessageTypes {
        const val TEXT = "TEXT"
        const val IMAGE = "IMAGE"
        const val FILE = "FILE"
    }

    // Request Codes
    const val REQUEST_CODE_PICK_IMAGE = 1001
    const val REQUEST_CODE_CAMERA = 1002
    const val REQUEST_CODE_LOCATION = 1003
}
