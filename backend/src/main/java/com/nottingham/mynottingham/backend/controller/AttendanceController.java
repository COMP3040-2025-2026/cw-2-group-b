package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.request.SignInRequest;
import com.nottingham.mynottingham.backend.dto.request.UnlockSessionRequest;
import com.nottingham.mynottingham.backend.dto.response.ApiResponse;
import com.nottingham.mynottingham.backend.dto.response.CourseScheduleResponse;
import com.nottingham.mynottingham.backend.entity.Attendance;
import com.nottingham.mynottingham.backend.entity.AttendanceSession;
import com.nottingham.mynottingham.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Get teacher's courses for today or a specific date
     */
    @GetMapping("/teacher/{teacherId}/courses")
    public ResponseEntity<ApiResponse<List<CourseScheduleResponse>>> getTeacherCourses(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String date) {

        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<CourseScheduleResponse> courses = attendanceService.getTeacherCoursesForDay(teacherId, targetDate);

        return ResponseEntity.ok(ApiResponse.success("Teacher courses retrieved successfully", courses));
    }

    /**
     * Get student's courses for today or a specific date
     */
    @GetMapping("/student/{studentId}/courses")
    public ResponseEntity<ApiResponse<List<CourseScheduleResponse>>> getStudentCourses(
            @PathVariable Long studentId,
            @RequestParam(required = false) String date) {

        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<CourseScheduleResponse> courses = attendanceService.getStudentCoursesForDay(studentId, targetDate);

        return ResponseEntity.ok(ApiResponse.success("Student courses retrieved successfully", courses));
    }

    /**
     * Unlock a session for sign-in (Teacher)
     */
    @PostMapping("/teacher/{teacherId}/unlock")
    public ResponseEntity<ApiResponse<AttendanceSession>> unlockSession(
            @PathVariable Long teacherId,
            @RequestBody UnlockSessionRequest request) {

        try {
            LocalDate date = LocalDate.parse(request.getSessionDate());
            AttendanceSession session = attendanceService.unlockSession(
                    request.getCourseScheduleId(), date, teacherId);

            return ResponseEntity.ok(ApiResponse.success("Session unlocked successfully", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Lock a session manually (Teacher)
     */
    @PostMapping("/teacher/{teacherId}/lock")
    public ResponseEntity<ApiResponse<AttendanceSession>> lockSession(
            @PathVariable Long teacherId,
            @RequestBody UnlockSessionRequest request) {

        try {
            LocalDate date = LocalDate.parse(request.getSessionDate());
            AttendanceSession session = attendanceService.lockSession(
                    request.getCourseScheduleId(), date, teacherId);

            return ResponseEntity.ok(ApiResponse.success("Session locked successfully", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Student sign in to a course
     */
    @PostMapping("/student/{studentId}/signin")
    public ResponseEntity<ApiResponse<Attendance>> signIn(
            @PathVariable Long studentId,
            @RequestBody SignInRequest request) {

        try {
            LocalDate date = LocalDate.parse(request.getSessionDate());
            Attendance attendance = attendanceService.signIn(
                    request.getCourseScheduleId(), date, studentId);

            return ResponseEntity.ok(ApiResponse.success("Signed in successfully", attendance));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Trigger auto-lock for expired sessions (can be called by a scheduled task)
     */
    @PostMapping("/auto-lock")
    public ResponseEntity<ApiResponse<String>> autoLock() {
        attendanceService.autoLockExpiredSessions();
        return ResponseEntity.ok(ApiResponse.success("Auto-lock completed", "Success"));
    }

    /**
     * Get list of students enrolled in a course with their attendance status
     */
    @GetMapping("/teacher/{teacherId}/course/{courseScheduleId}/students")
    public ResponseEntity<ApiResponse<List<com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto>>> getStudentAttendanceList(
            @PathVariable Long teacherId,
            @PathVariable Long courseScheduleId,
            @RequestParam(required = false) String date) {

        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            List<com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto> students =
                    attendanceService.getStudentAttendanceList(courseScheduleId, targetDate, teacherId);

            return ResponseEntity.ok(ApiResponse.success("Student attendance list retrieved successfully", students));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Teacher manually marks student attendance
     */
    @PostMapping("/teacher/{teacherId}/mark")
    public ResponseEntity<ApiResponse<Attendance>> markAttendance(
            @PathVariable Long teacherId,
            @RequestBody com.nottingham.mynottingham.backend.dto.request.MarkAttendanceRequest request) {

        try {
            LocalDate date = LocalDate.parse(request.getSessionDate());
            Attendance attendance = attendanceService.markAttendanceManually(
                    request.getCourseScheduleId(),
                    date,
                    request.getStudentId(),
                    request.getStatus(),
                    teacherId);

            return ResponseEntity.ok(ApiResponse.success("Attendance marked successfully", attendance));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
