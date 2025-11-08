package com.nottingham.mynottingham.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for common operations
 */

// ========== Toast Extensions ==========

/**
 * Show a toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Show a toast message from Fragment
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

// ========== String Extensions ==========

/**
 * Validate if string is a valid email
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Validate if string is a valid student ID (8 digits)
 */
fun String.isValidStudentId(): Boolean {
    return this.matches(Regex("^[0-9]{8}$"))
}

/**
 * Validate if string is a valid phone number (Malaysia format)
 */
fun String.isValidPhoneNumber(): Boolean {
    return this.matches(Regex("^(\\+?6?01)[0-9]{8,9}$"))
}

/**
 * Check if string is empty or blank
 */
fun String?.isNullOrEmpty(): Boolean {
    return this == null || this.trim().isEmpty()
}

/**
 * Capitalize first letter of each word
 */
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}

// ========== Date Extensions ==========

/**
 * Format Date to string
 */
fun Date.toFormattedString(pattern: String = Constants.DATE_FORMAT): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}

/**
 * Parse string to Date
 */
fun String.toDate(pattern: String = Constants.DATE_FORMAT): Date? {
    return try {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        formatter.parse(this)
    } catch (e: Exception) {
        null
    }
}

/**
 * Get current timestamp
 */
fun getCurrentTimestamp(): Long {
    return System.currentTimeMillis()
}

/**
 * Check if date is today
 */
fun Date.isToday(): Boolean {
    val today = Calendar.getInstance()
    val dateCalendar = Calendar.getInstance().apply { time = this@isToday }

    return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
}

// ========== View Extensions ==========

/**
 * Show view
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Hide view
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Make view invisible
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Toggle view visibility
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Check if view is visible
 */
fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

// ========== Number Extensions ==========

/**
 * Format Double to currency string (RM)
 */
fun Double.toRinggit(): String {
    return String.format("RM %.2f", this)
}

/**
 * Format Int to currency string (RM)
 */
fun Int.toRinggit(): String {
    return String.format("RM %.2f", this.toDouble())
}

// ========== Collection Extensions ==========

/**
 * Safe get item from list
 */
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}
