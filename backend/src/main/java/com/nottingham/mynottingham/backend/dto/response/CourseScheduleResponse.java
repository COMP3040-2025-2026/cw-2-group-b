package com.nottingham.mynottingham.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseScheduleResponse {
    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String semester;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String room;
    private String building;
    private String courseType;
    private String sessionStatus; // LOCKED, UNLOCKED, CLOSED
    private Boolean hasStudentSigned;
    private String attendanceStatus;  // PRESENT, ABSENT, LATE, EXCUSED (student's specific status, null if not marked)
    private Long unlockedAtTimestamp;
    private Integer attendedClasses;  // Number of classes student attended
    private Integer totalSignedClasses;  // Total classes where sign-in was opened
}
