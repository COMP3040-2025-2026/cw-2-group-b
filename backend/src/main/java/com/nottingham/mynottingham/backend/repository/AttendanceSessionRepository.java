package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.AttendanceSession;
import com.nottingham.mynottingham.backend.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByCourseScheduleAndSessionDate(CourseSchedule courseSchedule, LocalDate sessionDate);

    List<AttendanceSession> findBySessionDate(LocalDate sessionDate);

    @Query("SELECT a FROM AttendanceSession a WHERE a.status = 'UNLOCKED'")
    List<AttendanceSession> findAllUnlocked();

    @Query("SELECT a FROM AttendanceSession a WHERE a.courseSchedule.course.teacher.id = :teacherId AND a.sessionDate = :date")
    List<AttendanceSession> findByTeacherIdAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);

    // Count sessions for a course schedule that are not locked (i.e., were opened at some point)
    long countByCourseScheduleAndStatusNot(CourseSchedule courseSchedule, AttendanceSession.SessionStatus status);
}
