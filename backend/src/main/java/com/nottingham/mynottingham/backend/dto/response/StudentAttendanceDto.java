package com.nottingham.mynottingham.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDto {
    private Long studentId;
    private String studentName;
    private String matricNumber;
    private String email;
    private Boolean hasAttended;
    private String attendanceStatus; // PRESENT, ABSENT, LATE, EXCUSED, or null if not marked
    private LocalDateTime checkInTime;
}
