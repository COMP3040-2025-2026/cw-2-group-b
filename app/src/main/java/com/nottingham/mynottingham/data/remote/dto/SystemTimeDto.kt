package com.nottingham.mynottingham.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * System time data transfer object
 * Receives server's current date and time information from API
 */
data class SystemTimeDto(
    @SerializedName("currentDate")
    val currentDate: String,          // "2025-11-13"

    @SerializedName("currentTime")
    val currentTime: String,          // "14:30:00.123456"

    @SerializedName("currentDateTime")
    val currentDateTime: String,      // "2025-11-13T14:30:00.123456"

    @SerializedName("dayOfWeek")
    val dayOfWeek: String,            // "WEDNESDAY"

    @SerializedName("timestamp")
    val timestamp: Long               // 1699876800000
)
