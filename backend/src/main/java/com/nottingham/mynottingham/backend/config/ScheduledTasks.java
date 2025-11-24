package com.nottingham.mynottingham.backend.config;

import com.nottingham.mynottingham.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private final AttendanceService attendanceService;

    /**
     * Auto-lock expired sessions every 1 minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void autoLockExpiredSessions() {
        try {
            attendanceService.autoLockExpiredSessions();
        } catch (Exception e) {
            log.error("Error auto-locking sessions: {}", e.getMessage());
        }
    }
}
