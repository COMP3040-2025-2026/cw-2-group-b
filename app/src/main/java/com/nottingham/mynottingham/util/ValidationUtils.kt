package com.nottingham.mynottingham.util

/**
 * Utility class for data validation
 */
object ValidationUtils {

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && email.isValidEmail()
    }

    /**
     * Validate password strength
     * At least 6 characters
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Validate student ID
     * Must be 8 digits
     */
    fun isValidStudentId(studentId: String): Boolean {
        return studentId.isValidStudentId()
    }

    /**
     * Validate phone number (Malaysia format)
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.isValidPhoneNumber()
    }

    /**
     * Validate booking time slot
     * Check if time is in the future
     */
    fun isValidBookingTime(bookingTime: Long): Boolean {
        return bookingTime > System.currentTimeMillis()
    }

    /**
     * Validate delivery fee
     */
    fun isValidDeliveryFee(fee: Double): Boolean {
        return fee >= 0
    }

    /**
     * Validate price
     */
    fun isValidPrice(price: Double): Boolean {
        return price >= 0
    }

    /**
     * Validate text input (not empty)
     */
    fun isValidText(text: String, minLength: Int = 1): Boolean {
        return text.trim().length >= minLength
    }

    /**
     * Validate rating (1-5)
     */
    fun isValidRating(rating: Float): Boolean {
        return rating in 1.0..5.0
    }
}
