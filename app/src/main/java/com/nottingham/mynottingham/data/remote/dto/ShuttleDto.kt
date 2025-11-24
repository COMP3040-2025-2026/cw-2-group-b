package com.nottingham.mynottingham.data.remote.dto

/**
 * Data Transfer Objects for Shuttle
 */

data class ShuttleResponse(
    val route: String,
    val routeName: String,
    val departureLocation: String,
    val arrivalLocation: String,
    val imageUrl: String?
)

data class ShuttleScheduleResponse(
    val route: String,
    val schedules: List<ScheduleItem>
)

data class ScheduleItem(
    val departureTime: String,
    val arrivalTime: String,
    val day: String // "Weekday" or "Weekend"
)
