package com.nottingham.mynottingham.data.model

/**
 * Domain model for Shuttle Bus Route
 */
data class ShuttleRoute(
    val routeId: String,
    val routeName: String,
    val description: String,
    val weekdaySchedule: RouteSchedule?,
    val fridaySchedule: RouteSchedule?,
    val weekendSchedule: RouteSchedule?,
    val specialNote: String? = null,
    val isActive: Boolean = true
)

/**
 * Schedule for a specific route (departure and return times)
 */
data class RouteSchedule(
    val departureFromCampus: List<String>,
    val returnToCampus: List<String>,
    val vehicleType: String = "Bus" // Bus or Van
)

/**
 * Enum for day types
 */
enum class DayType {
    WEEKDAY,    // Mon-Thurs
    FRIDAY,     // Friday
    WEEKEND     // Sat, Sun & Public Holidays
}
