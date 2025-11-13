package com.nottingham.mynottingham.backend.service;

import com.nottingham.mynottingham.backend.dto.response.CourseScheduleResponse;
import com.nottingham.mynottingham.backend.entity.*;
import com.nottingham.mynottingham.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Get teacher's courses for a specific day
     */
    public List<CourseScheduleResponse> getTeacherCoursesForDay(Long teacherId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<CourseSchedule> schedules = scheduleRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek);

        return schedules.stream()
                .map(schedule -> convertToResponse(schedule, date, null))
                .collect(Collectors.toList());
    }

    /**
     * Get student's courses for a specific day
     */
    public List<CourseScheduleResponse> getStudentCoursesForDay(Long studentId, LocalDate date) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<CourseScheduleResponse> responses = new ArrayList<>();

        // Get all enrollments for this student
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            // Get schedules for this course on this day
            List<CourseSchedule> schedules = course.getSchedules().stream()
                    .filter(s -> s.getDayOfWeek() == dayOfWeek)
                    .collect(Collectors.toList());

            for (CourseSchedule schedule : schedules) {
                responses.add(convertToResponse(schedule, date, studentId));
            }
        }

        return responses;
    }

    /**
     * Unlock a session for sign-in
     */
    @Transactional
    public AttendanceSession unlockSession(Long courseScheduleId, LocalDate date, Long teacherId) {
        CourseSchedule schedule = scheduleRepository.findById(courseScheduleId)
                .orElseThrow(() -> new RuntimeException("Course schedule not found"));

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // Check if teacher owns this course
        if (!schedule.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: You don't teach this course");
        }

        // Find or create session
        AttendanceSession session = sessionRepository
                .findByCourseScheduleAndSessionDate(schedule, date)
                .orElse(new AttendanceSession());

        session.setCourseSchedule(schedule);
        session.setSessionDate(date);
        session.setStatus(AttendanceSession.SessionStatus.UNLOCKED);
        session.setUnlockedAt(LocalDateTime.now());
        session.setUnlockedBy(teacher);

        return sessionRepository.save(session);
    }

    /**
     * Lock a session manually
     */
    @Transactional
    public AttendanceSession lockSession(Long courseScheduleId, LocalDate date, Long teacherId) {
        CourseSchedule schedule = scheduleRepository.findById(courseScheduleId)
                .orElseThrow(() -> new RuntimeException("Course schedule not found"));

        // Check if teacher owns this course
        if (!schedule.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: You don't teach this course");
        }

        AttendanceSession session = sessionRepository
                .findByCourseScheduleAndSessionDate(schedule, date)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(AttendanceSession.SessionStatus.LOCKED);
        session.setLockedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    /**
     * Student sign in to a course
     */
    @Transactional
    public Attendance signIn(Long courseScheduleId, LocalDate date, Long studentId) {
        CourseSchedule schedule = scheduleRepository.findById(courseScheduleId)
                .orElseThrow(() -> new RuntimeException("Course schedule not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if student is enrolled
        boolean isEnrolled = enrollmentRepository
                .findByStudent(student).stream()
                .anyMatch(e -> e.getCourse().getId().equals(schedule.getCourse().getId()));

        if (!isEnrolled) {
            throw new RuntimeException("You are not enrolled in this course");
        }

        // Check if session is unlocked
        AttendanceSession session = sessionRepository
                .findByCourseScheduleAndSessionDate(schedule, date)
                .orElseThrow(() -> new RuntimeException("Sign-in not available"));

        if (session.getStatus() != AttendanceSession.SessionStatus.UNLOCKED) {
            throw new RuntimeException("Sign-in is not available. Status: " + session.getStatus());
        }

        // Check if already signed in
        Optional<Attendance> existing = attendanceRepository.findByStudentAndCourseAndAttendanceDate(
                student, schedule.getCourse(), date);

        if (existing.isPresent()) {
            return existing.get(); // Already signed in
        }

        // Create new attendance record
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setCourse(schedule.getCourse());
        attendance.setAttendanceDate(date);
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        attendance.setCheckInTime(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    /**
     * Auto-lock expired sessions
     */
    @Transactional
    public void autoLockExpiredSessions() {
        List<AttendanceSession> unlockedSessions = sessionRepository.findAllUnlocked();

        for (AttendanceSession session : unlockedSessions) {
            if (session.shouldAutoLock()) {
                session.setStatus(AttendanceSession.SessionStatus.CLOSED);
                session.setLockedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }
        }
    }

    /**
     * Convert CourseSchedule to Response DTO
     */
    private CourseScheduleResponse convertToResponse(CourseSchedule schedule, LocalDate date, Long studentId) {
        Course course = schedule.getCourse();

        // Get session status
        Optional<AttendanceSession> sessionOpt = sessionRepository
                .findByCourseScheduleAndSessionDate(schedule, date);

        String sessionStatus = "LOCKED";
        Long unlockedAtTimestamp = null;

        if (sessionOpt.isPresent()) {
            AttendanceSession session = sessionOpt.get();
            sessionStatus = session.getStatus().name();
            if (session.getUnlockedAt() != null) {
                unlockedAtTimestamp = session.getUnlockedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }

            // Check auto-lock
            if (session.shouldAutoLock()) {
                sessionStatus = "CLOSED";
            }
        }

        // Check if student has signed in (if studentId provided)
        Boolean hasStudentSigned = null;
        Integer attendedClasses = null;
        Integer totalSignedClasses = null;

        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null) {
                // Check today's attendance
                Optional<Attendance> todayAttendance = attendanceRepository.findByStudentAndCourseAndAttendanceDate(
                        student, course, date);

                System.out.println("DEBUG - Checking attendance for student: " + studentId +
                                   ", course: " + course.getId() + " (" + course.getCourseCode() + ")" +
                                   ", date: " + date +
                                   ", found: " + todayAttendance.isPresent());

                if (todayAttendance.isPresent()) {
                    System.out.println("DEBUG - Attendance status: " + todayAttendance.get().getStatus());
                }

                hasStudentSigned = todayAttendance.isPresent() &&
                                   todayAttendance.get().getStatus() == Attendance.AttendanceStatus.PRESENT;

                // Calculate attended classes (only count PRESENT status)
                List<Attendance> allAttendances = attendanceRepository.findByStudentAndCourse(student, course);
                attendedClasses = (int) allAttendances.stream()
                        .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                        .count();

                // Calculate total signed classes using a single efficient query
                // Count all unlocked/closed sessions for this course's schedules
                List<CourseSchedule> courseSchedules = schedule.getCourse().getSchedules();
                totalSignedClasses = 0;

                // Get all session IDs for this course's schedules
                for (CourseSchedule cs : courseSchedules) {
                    // Count sessions that were unlocked (not just LOCKED status)
                    long count = sessionRepository.countByCourseScheduleAndStatusNot(
                            cs, AttendanceSession.SessionStatus.LOCKED);
                    totalSignedClasses += (int) count;
                }
            }
        }

        return CourseScheduleResponse.builder()
                .id(schedule.getId())
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .semester(course.getSemester())
                .dayOfWeek(schedule.getDayOfWeek().name())
                .startTime(schedule.getStartTime().toString())
                .endTime(schedule.getEndTime().toString())
                .room(schedule.getRoom())
                .building(schedule.getBuilding())
                .courseType(schedule.getCourseType() != null ? schedule.getCourseType().name() : "LECTURE")
                .sessionStatus(sessionStatus)
                .hasStudentSigned(hasStudentSigned)
                .unlockedAtTimestamp(unlockedAtTimestamp)
                .attendedClasses(attendedClasses)
                .totalSignedClasses(totalSignedClasses)
                .build();
    }

    /**
     * Get list of students enrolled in a course with their attendance status for a specific date
     */
    public List<com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto> getStudentAttendanceList(
            Long courseScheduleId, LocalDate date, Long teacherId) {

        CourseSchedule schedule = scheduleRepository.findById(courseScheduleId)
                .orElseThrow(() -> new RuntimeException("Course schedule not found"));

        // Verify teacher owns this course
        if (!schedule.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: You don't teach this course");
        }

        Course course = schedule.getCourse();

        // Get all enrolled students
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);

        List<com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto> result = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();

            // Check if student has attendance record for this date
            Optional<Attendance> attendance = attendanceRepository.findByStudentAndCourseAndAttendanceDate(
                    student, course, date);

            com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto dto =
                    com.nottingham.mynottingham.backend.dto.response.StudentAttendanceDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getFullName())
                    .matricNumber(student.getMatricNumber())
                    .email(student.getEmail())
                    .hasAttended(attendance.isPresent())
                    .attendanceStatus(attendance.map(a -> a.getStatus().name()).orElse(null))
                    .checkInTime(attendance.map(Attendance::getCheckInTime).orElse(null))
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * Teacher manually marks student attendance
     */
    @Transactional
    public Attendance markAttendanceManually(Long courseScheduleId, LocalDate date, Long studentId,
                                            String status, Long teacherId) {
        CourseSchedule schedule = scheduleRepository.findById(courseScheduleId)
                .orElseThrow(() -> new RuntimeException("Course schedule not found"));

        // Verify teacher owns this course
        if (!schedule.getCourse().getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: You don't teach this course");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = schedule.getCourse();

        // Check if student is enrolled
        boolean isEnrolled = enrollmentRepository.findByStudent(student).stream()
                .anyMatch(e -> e.getCourse().getId().equals(course.getId()));

        if (!isEnrolled) {
            throw new RuntimeException("Student is not enrolled in this course");
        }

        // Find or create attendance record
        Attendance attendance = attendanceRepository.findByStudentAndCourseAndAttendanceDate(
                student, course, date)
                .orElse(new Attendance());

        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setAttendanceDate(date);

        // Parse status
        try {
            attendance.setStatus(Attendance.AttendanceStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid attendance status: " + status);
        }

        if (attendance.getCheckInTime() == null &&
            attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
            attendance.setCheckInTime(java.time.LocalDateTime.now());
        }

        Attendance savedAttendance = attendanceRepository.save(attendance);

        System.out.println("DEBUG - Marked attendance: student=" + studentId +
                           ", course=" + course.getId() + " (" + course.getCourseCode() + ")" +
                           ", date=" + date +
                           ", status=" + savedAttendance.getStatus() +
                           ", id=" + savedAttendance.getId());

        return savedAttendance;
    }
}
