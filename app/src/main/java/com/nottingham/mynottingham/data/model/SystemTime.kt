package com.nottingham.mynottingham.data.model

/**
 * System time domain model
 * Represents server's current date and time for use in UI layer
 */
data class SystemTime(
    val currentDate: String,          // "2025-11-13"
    val currentTime: String,          // "14:30:00"
    val currentDateTime: String,      // "2025-11-13T14:30:00"
    val dayOfWeek: DayOfWeek,         // Converted to enum
    val timestamp: Long               // 1699876800000
) {
    companion object {
        /**
         * Convert SystemTimeDto to SystemTime domain model
         */
        fun fromDto(dto: com.nottingham.mynottingham.data.remote.dto.SystemTimeDto): SystemTime {
            // Parse day of week, default to MONDAY if parsing fails
            val dayOfWeek = try {
                DayOfWeek.valueOf(dto.dayOfWeek.uppercase())
            } catch (e: Exception) {
                DayOfWeek.MONDAY
            }

            return SystemTime(
                currentDate = dto.currentDate,
                currentTime = dto.currentTime.substringBefore('.'),  // Remove fractional seconds
                currentDateTime = dto.currentDateTime.substringBefore('.'),  // Remove fractional seconds
                dayOfWeek = dayOfWeek,
                timestamp = dto.timestamp
            )
        }
    }
}
