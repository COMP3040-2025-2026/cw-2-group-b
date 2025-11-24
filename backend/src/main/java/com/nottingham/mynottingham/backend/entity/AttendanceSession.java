package com.nottingham.mynottingham.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a sign-in session for a course
 * Teachers can unlock/lock these sessions for students to sign in
 */
@Data
@Entity
@Table(name = "attendance_sessions")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_schedule_id", nullable = false)
    @JsonIgnoreProperties({"course"})
    private CourseSchedule courseSchedule;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.LOCKED;

    private LocalDateTime unlockedAt;

    private LocalDateTime lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlocked_by_teacher_id")
    @JsonIgnoreProperties({"courses", "password"})
    private Teacher unlockedBy;

    // Auto-lock after 20 minutes
    @Column(nullable = false)
    private Integer autoLockMinutes = 20;

    public enum SessionStatus {
        LOCKED,    // Not yet available for sign-in
        UNLOCKED,  // Available for sign-in
        CLOSED     // Sign-in period ended
    }

    /**
     * Check if session should auto-lock
     */
    public boolean shouldAutoLock() {
        if (status != SessionStatus.UNLOCKED || unlockedAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(unlockedAt.plusMinutes(autoLockMinutes));
    }
}
