package com.nottingham.mynottingham.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String checkInTime; // ISO datetime format: "2025-11-12T14:30:00"
}
