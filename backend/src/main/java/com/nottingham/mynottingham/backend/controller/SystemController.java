package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.response.ApiResponse;
import com.nottingham.mynottingham.backend.dto.response.SystemTimeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * System Controller
 * Provides system-level information such as server time
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    /**
     * Get current server time
     * This endpoint provides the authoritative time for the application
     * to prevent client-side time manipulation
     *
     * @return System time information including date, time, day of week, and timestamp
     */
    @GetMapping("/time")
    public ResponseEntity<ApiResponse<SystemTimeResponse>> getSystemTime() {
        LocalDateTime now = LocalDateTime.now();

        SystemTimeResponse response = SystemTimeResponse.builder()
                .currentDate(now.toLocalDate().toString())              // "2025-11-13"
                .currentTime(now.toLocalTime().toString())              // "14:30:00.123456"
                .currentDateTime(now.toString())                         // "2025-11-13T14:30:00.123456"
                .dayOfWeek(now.getDayOfWeek().name())                   // "WEDNESDAY"
                .timestamp(System.currentTimeMillis())                   // Unix timestamp in milliseconds
                .build();

        return ResponseEntity.ok(ApiResponse.success("System time retrieved successfully", response));
    }
}
