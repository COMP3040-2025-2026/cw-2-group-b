package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.AttendanceRecord
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.CourseType
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.data.model.StudentAttendance
import com.nottingham.mynottingham.data.model.TodayClassStatus
import com.nottingham.mynottingham.data.model.UserRole
import com.nottingham.mynottingham.data.model.WeeklySchedule
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for Attendance/Instatt data models
 *
 * Tests the attendance system data classes and enums:
 * - Course data class
 * - DayOfWeek enum
 * - AttendanceRecord data class
 * - AttendanceStatus enum
 * - WeeklySchedule data class
 * - CourseType enum
 * - TodayClassStatus enum
 * - SignInStatus enum
 * - UserRole enum
 * - StudentAttendance data class
 */
class AttendanceModelTest {

    /**
     * Test DayOfWeek enum has all expected values
     */
    @Test
    fun `DayOfWeek enum has all expected values`() {
        val days = DayOfWeek.values()

        assertEquals("Should have 7 days", 7, days.size)
        assertTrue("Should contain MONDAY", days.contains(DayOfWeek.MONDAY))
        assertTrue("Should contain TUESDAY", days.contains(DayOfWeek.TUESDAY))
        assertTrue("Should contain WEDNESDAY", days.contains(DayOfWeek.WEDNESDAY))
        assertTrue("Should contain THURSDAY", days.contains(DayOfWeek.THURSDAY))
        assertTrue("Should contain FRIDAY", days.contains(DayOfWeek.FRIDAY))
        assertTrue("Should contain SATURDAY", days.contains(DayOfWeek.SATURDAY))
        assertTrue("Should contain SUNDAY", days.contains(DayOfWeek.SUNDAY))
    }

    /**
     * Test DayOfWeek display names
     */
    @Test
    fun `DayOfWeek has correct display names`() {
        assertEquals("Monday", DayOfWeek.MONDAY.displayName)
        assertEquals("Tuesday", DayOfWeek.TUESDAY.displayName)
        assertEquals("Wednesday", DayOfWeek.WEDNESDAY.displayName)
        assertEquals("Thursday", DayOfWeek.THURSDAY.displayName)
        assertEquals("Friday", DayOfWeek.FRIDAY.displayName)
        assertEquals("Saturday", DayOfWeek.SATURDAY.displayName)
        assertEquals("Sunday", DayOfWeek.SUNDAY.displayName)
    }

    /**
     * Test CourseType enum has all expected values
     */
    @Test
    fun `CourseType enum has all expected values`() {
        val types = CourseType.values()

        assertEquals("Should have 4 course types", 4, types.size)
        assertTrue("Should contain LECTURE", types.contains(CourseType.LECTURE))
        assertTrue("Should contain TUTORIAL", types.contains(CourseType.TUTORIAL))
        assertTrue("Should contain COMPUTING", types.contains(CourseType.COMPUTING))
        assertTrue("Should contain LAB", types.contains(CourseType.LAB))
    }

    /**
     * Test CourseType display names
     */
    @Test
    fun `CourseType has correct display names`() {
        assertEquals("lecture", CourseType.LECTURE.displayName)
        assertEquals("tutorial", CourseType.TUTORIAL.displayName)
        assertEquals("computing", CourseType.COMPUTING.displayName)
        assertEquals("lab", CourseType.LAB.displayName)
    }

    /**
     * Test AttendanceStatus enum has all expected values
     */
    @Test
    fun `AttendanceStatus enum has all expected values`() {
        val statuses = AttendanceStatus.values()

        assertEquals("Should have 4 attendance statuses", 4, statuses.size)
        assertTrue("Should contain PRESENT", statuses.contains(AttendanceStatus.PRESENT))
        assertTrue("Should contain ABSENT", statuses.contains(AttendanceStatus.ABSENT))
        assertTrue("Should contain LATE", statuses.contains(AttendanceStatus.LATE))
        assertTrue("Should contain EXCUSED", statuses.contains(AttendanceStatus.EXCUSED))
    }

    /**
     * Test TodayClassStatus enum has all expected values
     */
    @Test
    fun `TodayClassStatus enum has all expected values`() {
        val statuses = TodayClassStatus.values()

        assertEquals("Should have 4 today class statuses", 4, statuses.size)
        assertTrue("Should contain UPCOMING", statuses.contains(TodayClassStatus.UPCOMING))
        assertTrue("Should contain IN_PROGRESS", statuses.contains(TodayClassStatus.IN_PROGRESS))
        assertTrue("Should contain ATTENDED", statuses.contains(TodayClassStatus.ATTENDED))
        assertTrue("Should contain MISSED", statuses.contains(TodayClassStatus.MISSED))
    }

    /**
     * Test SignInStatus enum has all expected values
     */
    @Test
    fun `SignInStatus enum has all expected values`() {
        val statuses = SignInStatus.values()

        assertEquals("Should have 4 sign-in statuses", 4, statuses.size)
        assertTrue("Should contain LOCKED", statuses.contains(SignInStatus.LOCKED))
        assertTrue("Should contain UNLOCKED", statuses.contains(SignInStatus.UNLOCKED))
        assertTrue("Should contain SIGNED", statuses.contains(SignInStatus.SIGNED))
        assertTrue("Should contain CLOSED", statuses.contains(SignInStatus.CLOSED))
    }

    /**
     * Test UserRole enum has all expected values
     */
    @Test
    fun `UserRole enum has all expected values`() {
        val roles = UserRole.values()

        assertEquals("Should have 2 user roles", 2, roles.size)
        assertTrue("Should contain STUDENT", roles.contains(UserRole.STUDENT))
        assertTrue("Should contain TEACHER", roles.contains(UserRole.TEACHER))
    }

    /**
     * Test Course creation with required fields
     */
    @Test
    fun `Course creation with required fields`() {
        val course = Course(
            id = "course123",
            courseName = "Data Structures",
            courseCode = "COMP2003",
            semester = "25-26",
            attendedClasses = 10,
            totalClasses = 12,
            dayOfWeek = DayOfWeek.MONDAY
        )

        assertEquals("course123", course.id)
        assertEquals("Data Structures", course.courseName)
        assertEquals("COMP2003", course.courseCode)
        assertEquals("25-26", course.semester)
        assertEquals(10, course.attendedClasses)
        assertEquals(12, course.totalClasses)
        assertEquals(DayOfWeek.MONDAY, course.dayOfWeek)
    }

    /**
     * Test Course default values
     */
    @Test
    fun `Course has correct default values`() {
        val course = Course(
            id = "course123",
            courseName = "Algorithms",
            courseCode = "COMP2004",
            semester = "25-26",
            attendedClasses = 0,
            totalClasses = 0,
            dayOfWeek = DayOfWeek.TUESDAY
        )

        assertNull("startTime should be null by default", course.startTime)
        assertNull("endTime should be null by default", course.endTime)
        assertNull("location should be null by default", course.location)
        assertEquals("courseType should be LECTURE by default", CourseType.LECTURE, course.courseType)
        assertNull("todayStatus should be null by default", course.todayStatus)
        assertEquals("signInStatus should be LOCKED by default", SignInStatus.LOCKED, course.signInStatus)
        assertNull("signInUnlockedAt should be null by default", course.signInUnlockedAt)
        assertFalse("hasStudentSigned should be false by default", course.hasStudentSigned)
    }

    /**
     * Test Course with all optional fields
     */
    @Test
    fun `Course with all optional fields`() {
        val course = Course(
            id = "course123",
            courseName = "Database Systems",
            courseCode = "COMP3001",
            semester = "25-26",
            attendedClasses = 8,
            totalClasses = 10,
            dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = "09:00",
            endTime = "11:00",
            location = "BB-LT1",
            courseType = CourseType.LECTURE,
            todayStatus = TodayClassStatus.IN_PROGRESS,
            signInStatus = SignInStatus.UNLOCKED,
            signInUnlockedAt = 1000L,
            hasStudentSigned = true
        )

        assertEquals("09:00", course.startTime)
        assertEquals("11:00", course.endTime)
        assertEquals("BB-LT1", course.location)
        assertEquals(CourseType.LECTURE, course.courseType)
        assertEquals(TodayClassStatus.IN_PROGRESS, course.todayStatus)
        assertEquals(SignInStatus.UNLOCKED, course.signInStatus)
        assertEquals(1000L, course.signInUnlockedAt)
        assertTrue(course.hasStudentSigned)
    }

    /**
     * Test Course with different course types
     */
    @Test
    fun `Course with different course types`() {
        val lecture = Course(
            id = "1", courseName = "DSA Lecture", courseCode = "COMP2003",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.MONDAY, courseType = CourseType.LECTURE
        )

        val tutorial = Course(
            id = "2", courseName = "DSA Tutorial", courseCode = "COMP2003",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.TUESDAY, courseType = CourseType.TUTORIAL
        )

        val computing = Course(
            id = "3", courseName = "DSA Computing", courseCode = "COMP2003",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.WEDNESDAY, courseType = CourseType.COMPUTING
        )

        val lab = Course(
            id = "4", courseName = "DSA Lab", courseCode = "COMP2003",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.THURSDAY, courseType = CourseType.LAB
        )

        assertEquals(CourseType.LECTURE, lecture.courseType)
        assertEquals(CourseType.TUTORIAL, tutorial.courseType)
        assertEquals(CourseType.COMPUTING, computing.courseType)
        assertEquals(CourseType.LAB, lab.courseType)
    }

    /**
     * Test Course attendance percentage calculation
     */
    @Test
    fun `Course attendance percentage calculation`() {
        val course = Course(
            id = "course123",
            courseName = "Test Course",
            courseCode = "TEST101",
            semester = "25-26",
            attendedClasses = 8,
            totalClasses = 10,
            dayOfWeek = DayOfWeek.MONDAY
        )

        val percentage = if (course.totalClasses > 0) {
            (course.attendedClasses.toDouble() / course.totalClasses * 100).toInt()
        } else {
            0
        }

        assertEquals(80, percentage)
    }

    /**
     * Test Course data class equality
     */
    @Test
    fun `Course data class equality`() {
        val course1 = Course(
            id = "course123",
            courseName = "Test",
            courseCode = "TEST101",
            semester = "25-26",
            attendedClasses = 5,
            totalClasses = 10,
            dayOfWeek = DayOfWeek.MONDAY
        )

        val course2 = Course(
            id = "course123",
            courseName = "Test",
            courseCode = "TEST101",
            semester = "25-26",
            attendedClasses = 5,
            totalClasses = 10,
            dayOfWeek = DayOfWeek.MONDAY
        )

        assertEquals("Courses with same data should be equal", course1, course2)
    }

    /**
     * Test Course copy function
     */
    @Test
    fun `Course copy function works correctly`() {
        val original = Course(
            id = "course123",
            courseName = "Test",
            courseCode = "TEST101",
            semester = "25-26",
            attendedClasses = 5,
            totalClasses = 10,
            dayOfWeek = DayOfWeek.MONDAY,
            signInStatus = SignInStatus.LOCKED
        )

        val updated = original.copy(
            attendedClasses = 6,
            signInStatus = SignInStatus.SIGNED,
            hasStudentSigned = true
        )

        assertEquals("course123", updated.id)
        assertEquals(6, updated.attendedClasses)
        assertEquals(SignInStatus.SIGNED, updated.signInStatus)
        assertTrue(updated.hasStudentSigned)
        assertEquals(5, original.attendedClasses)
    }

    /**
     * Test AttendanceRecord creation
     */
    @Test
    fun `AttendanceRecord creation`() {
        val date = Date()
        val record = AttendanceRecord(
            id = "record123",
            courseId = "course456",
            date = date,
            status = AttendanceStatus.PRESENT,
            markedAt = date
        )

        assertEquals("record123", record.id)
        assertEquals("course456", record.courseId)
        assertEquals(date, record.date)
        assertEquals(AttendanceStatus.PRESENT, record.status)
        assertEquals(date, record.markedAt)
    }

    /**
     * Test AttendanceRecord with different statuses
     */
    @Test
    fun `AttendanceRecord with different statuses`() {
        val date = Date()

        val presentRecord = AttendanceRecord("r1", "c1", date, AttendanceStatus.PRESENT, date)
        val absentRecord = AttendanceRecord("r2", "c1", date, AttendanceStatus.ABSENT, null)
        val lateRecord = AttendanceRecord("r3", "c1", date, AttendanceStatus.LATE, date)
        val excusedRecord = AttendanceRecord("r4", "c1", date, AttendanceStatus.EXCUSED, null)

        assertEquals(AttendanceStatus.PRESENT, presentRecord.status)
        assertEquals(AttendanceStatus.ABSENT, absentRecord.status)
        assertEquals(AttendanceStatus.LATE, lateRecord.status)
        assertEquals(AttendanceStatus.EXCUSED, excusedRecord.status)
    }

    /**
     * Test AttendanceRecord with null markedAt
     */
    @Test
    fun `AttendanceRecord with null markedAt`() {
        val record = AttendanceRecord(
            id = "record123",
            courseId = "course456",
            date = Date(),
            status = AttendanceStatus.ABSENT,
            markedAt = null
        )

        assertNull(record.markedAt)
    }

    /**
     * Test WeeklySchedule creation
     */
    @Test
    fun `WeeklySchedule creation`() {
        val courses = listOf(
            Course("c1", "Course 1", "C001", "25-26", 0, 0, DayOfWeek.MONDAY),
            Course("c2", "Course 2", "C002", "25-26", 0, 0, DayOfWeek.MONDAY)
        )

        val schedule = WeeklySchedule(
            dayOfWeek = DayOfWeek.MONDAY,
            courses = courses
        )

        assertEquals(DayOfWeek.MONDAY, schedule.dayOfWeek)
        assertEquals(2, schedule.courses.size)
    }

    /**
     * Test WeeklySchedule with empty courses
     */
    @Test
    fun `WeeklySchedule with empty courses`() {
        val schedule = WeeklySchedule(
            dayOfWeek = DayOfWeek.SATURDAY,
            courses = emptyList()
        )

        assertEquals(DayOfWeek.SATURDAY, schedule.dayOfWeek)
        assertTrue(schedule.courses.isEmpty())
    }

    /**
     * Test StudentAttendance creation
     */
    @Test
    fun `StudentAttendance creation`() {
        val student = StudentAttendance(
            studentId = "student123",
            studentName = "John Doe",
            matricNumber = "20123456",
            email = "john@nottingham.edu.my",
            hasAttended = true,
            attendanceStatus = AttendanceStatus.PRESENT,
            checkInTime = "2024-12-15T10:05:00"
        )

        assertEquals("student123", student.studentId)
        assertEquals("John Doe", student.studentName)
        assertEquals("20123456", student.matricNumber)
        assertEquals("john@nottingham.edu.my", student.email)
        assertTrue(student.hasAttended)
        assertEquals(AttendanceStatus.PRESENT, student.attendanceStatus)
        assertEquals("2024-12-15T10:05:00", student.checkInTime)
    }

    /**
     * Test StudentAttendance with nullable fields
     */
    @Test
    fun `StudentAttendance with nullable fields`() {
        val student = StudentAttendance(
            studentId = "student123",
            studentName = "Jane Doe",
            matricNumber = null,
            email = null,
            hasAttended = false,
            attendanceStatus = null,
            checkInTime = null
        )

        assertNull(student.matricNumber)
        assertNull(student.email)
        assertFalse(student.hasAttended)
        assertNull(student.attendanceStatus)
        assertNull(student.checkInTime)
    }

    /**
     * Test StudentAttendance data class equality
     */
    @Test
    fun `StudentAttendance data class equality`() {
        val student1 = StudentAttendance(
            studentId = "s1",
            studentName = "John",
            matricNumber = "123",
            email = "john@test.com",
            hasAttended = true,
            attendanceStatus = AttendanceStatus.PRESENT,
            checkInTime = "10:00"
        )

        val student2 = StudentAttendance(
            studentId = "s1",
            studentName = "John",
            matricNumber = "123",
            email = "john@test.com",
            hasAttended = true,
            attendanceStatus = AttendanceStatus.PRESENT,
            checkInTime = "10:00"
        )

        assertEquals("Students with same data should be equal", student1, student2)
    }

    /**
     * Test Course signIn workflow
     */
    @Test
    fun `Course signIn workflow`() {
        // Initial state - locked
        var course = Course(
            id = "c1", courseName = "Test", courseCode = "T001",
            semester = "25-26", attendedClasses = 0, totalClasses = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            signInStatus = SignInStatus.LOCKED,
            hasStudentSigned = false
        )

        assertEquals(SignInStatus.LOCKED, course.signInStatus)
        assertFalse(course.hasStudentSigned)

        // Teacher unlocks
        course = course.copy(signInStatus = SignInStatus.UNLOCKED, signInUnlockedAt = 1000L)
        assertEquals(SignInStatus.UNLOCKED, course.signInStatus)
        assertNotNull(course.signInUnlockedAt)

        // Student signs in
        course = course.copy(signInStatus = SignInStatus.SIGNED, hasStudentSigned = true)
        assertEquals(SignInStatus.SIGNED, course.signInStatus)
        assertTrue(course.hasStudentSigned)
    }

    /**
     * Test TodayClassStatus visual indicators
     */
    @Test
    fun `TodayClassStatus visual indicators`() {
        val upcomingCourse = Course(
            id = "c1", courseName = "Course 1", courseCode = "C001",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.MONDAY, todayStatus = TodayClassStatus.UPCOMING
        )

        val inProgressCourse = Course(
            id = "c2", courseName = "Course 2", courseCode = "C002",
            semester = "25-26", attendedClasses = 0, totalClasses = 0,
            dayOfWeek = DayOfWeek.MONDAY, todayStatus = TodayClassStatus.IN_PROGRESS
        )

        val attendedCourse = Course(
            id = "c3", courseName = "Course 3", courseCode = "C003",
            semester = "25-26", attendedClasses = 1, totalClasses = 1,
            dayOfWeek = DayOfWeek.MONDAY, todayStatus = TodayClassStatus.ATTENDED
        )

        val missedCourse = Course(
            id = "c4", courseName = "Course 4", courseCode = "C004",
            semester = "25-26", attendedClasses = 0, totalClasses = 1,
            dayOfWeek = DayOfWeek.MONDAY, todayStatus = TodayClassStatus.MISSED
        )

        assertEquals(TodayClassStatus.UPCOMING, upcomingCourse.todayStatus)
        assertEquals(TodayClassStatus.IN_PROGRESS, inProgressCourse.todayStatus)
        assertEquals(TodayClassStatus.ATTENDED, attendedCourse.todayStatus)
        assertEquals(TodayClassStatus.MISSED, missedCourse.todayStatus)
    }
}
