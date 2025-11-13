package com.nottingham.mynottingham.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System time response DTO
 * Returns server's current date and time information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemTimeResponse {
    /**
     * Current date in ISO format (yyyy-MM-dd)
     * Example: "2025-11-13"
     */
    private String currentDate;

    /**
     * Current time in ISO format (HH:mm:ss)
     * Example: "14:30:00"
     */
    private String currentTime;

    /**
     * Current date-time in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     * Example: "2025-11-13T14:30:00"
     */
    private String currentDateTime;

    /**
     * Current day of week
     * Example: "WEDNESDAY"
     */
    private String dayOfWeek;

    /**
     * Current timestamp in milliseconds
     * Example: 1699876800000
     */
    private Long timestamp;
}
