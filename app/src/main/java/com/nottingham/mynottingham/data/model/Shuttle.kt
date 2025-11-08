package com.nottingham.mynottingham.data.model

/**
 * Shuttle data model
 */
data class Shuttle(
    val route: String,
    val routeName: String,
    val departureLocation: String,
    val arrivalLocation: String,
    val schedules: List<ShuttleSchedule>
)

data class ShuttleSchedule(
    val departureTime: String,
    val arrivalTime: String,
    val day: String // "Weekday" or "Weekend"
)
