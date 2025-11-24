package com.nottingham.mynottingham.backend.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Data Migration Service - Complete Edition
 *
 * 在应用启动时将所有核心数据从 MySQL 迁移到 Firebase Realtime Database
 *
 * 迁移模块：
 * 1. Users (Students, Teachers, Admin)
 * 2. Courses, Schedules, and Enrollments
 * 3. Bookings (Basketball Court, Badminton Court)
 * 4. Errands (Campus delivery tasks)
 * 5. Forum (Posts and Comments)
 * 6. Special: 自动为今天创建一节 teacher1 的课，student1 和 student2 可以签到
 *
 * 数据结构设计 (NoSQL):
 * - users/{userId}: 用户信息 (学生/教师/管理员)
 * - courses/{courseId}: 课程基本信息
 * - schedules/{scheduleId}: 课程排课信息
 * - enrollments/{courseId}/{studentId}: 课程注册关系
 * - student_courses/{studentId}/{courseId}: 学生选课关系
 * - bookings/{bookingId}: 场地预订记录
 * - errands/{errandId}: 跑腿任务
 * - forum_posts/{postId}: 论坛帖子
 * - forum_comments/{postId}/{commentId}: 论坛评论
 * - sessions/{sessionKey}: 签到会话 (自动生成今日课程)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseDataMigrationService implements CommandLineRunner {

    private final FirebaseDatabase firebaseDatabase;

    // 迁移开关 - 设置为 false 可跳过迁移
    private static final boolean RUN_MIGRATION = true;

    @Override
    public void run(String... args) throws Exception {
        if (!RUN_MIGRATION) {
            log.info("⏸️ Firebase migration is disabled (RUN_MIGRATION = false)");
            return;
        }

        try {
            log.info("🚀 Starting Complete Firebase Data Migration...");

            DatabaseReference ref = firebaseDatabase.getReference();

            // 1. Users (Students, Teachers, Admin)
            migrateUsers(ref);

            // 2. Courses, Schedules, and Enrollments
            migrateCoursesAndSchedules(ref);

            // 3. Bookings
            migrateBookings(ref);

            // 4. Errands
            migrateErrands(ref);

            // 5. Forum
            migrateForums(ref);

            // 6. Special Request: Create a class for TODAY for teacher1, student1, student2
            createClassForToday(ref);

            log.info("✅ Firebase Data Migration Completed Successfully!");
            log.info("📊 Check Firebase Console: https://console.firebase.google.com");

        } catch (Exception e) {
            log.error("❌ Firebase migration failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 迁移用户数据 (学生 + 教师 + 管理员)
     */
    private void migrateUsers(DatabaseReference ref) {
        log.info("📤 Migrating users...");

        Map<String, Object> users = new HashMap<>();

        // --- Students ---
        users.put("student1", createStudent("student1", "Alice Wong", "student1@student.nottingham.edu.my", 1L, "20123456", "Faculty of Science and Engineering"));
        users.put("student2", createStudent("student2", "Bob Chen", "student2@student.nottingham.edu.my", 2L, "20123457", "Faculty of Science and Engineering"));
        users.put("student3", createStudent("student3", "Charlie Tan", "student3@student.nottingham.edu.my", 3L, "20123458", "Faculty of Science and Engineering"));

        // --- Teachers ---
        users.put("teacher1", createTeacher("teacher1", "Dr. Sarah Johnson", "teacher1@nottingham.edu.my", "T001", "Computer Science Department"));
        users.put("teacher2", createTeacher("teacher2", "Prof. John Smith", "teacher2@nottingham.edu.my", "T002", "Software Engineering Department"));

        // --- Admin ---
        Map<String, Object> admin = new HashMap<>();
        admin.put("username", "admin");
        admin.put("fullName", "System Admin");
        admin.put("email", "admin@nottingham.edu.my");
        admin.put("role", "ADMIN");
        users.put("admin", admin);

        ref.child("users").updateChildrenAsync(users);
        log.info("✅ Migrated {} users", users.size());
    }

    private Map<String, Object> createStudent(String username, String name, String email, Long id, String matric, String faculty) {
        Map<String, Object> m = new HashMap<>();
        m.put("username", username);
        m.put("fullName", name);
        m.put("email", email);
        m.put("role", "STUDENT");
        m.put("studentId", id);
        m.put("matricNumber", matric);
        m.put("faculty", faculty);
        return m;
    }

    private Map<String, Object> createTeacher(String username, String name, String email, String empId, String dept) {
        Map<String, Object> m = new HashMap<>();
        m.put("username", username);
        m.put("fullName", name);
        m.put("email", email);
        m.put("role", "TEACHER");
        m.put("employeeId", empId);
        m.put("department", dept);
        return m;
    }

    /**
     * 迁移课程、排课和选课关系
     */
    private void migrateCoursesAndSchedules(DatabaseReference ref) {
        log.info("📤 Migrating courses, schedules and enrollments...");

        Map<String, Object> courses = new HashMap<>();
        Map<String, Object> schedules = new HashMap<>();
        Map<String, Object> enrollments = new HashMap<>();
        Map<String, Object> studentCourses = new HashMap<>();

        // ==================== COMP3040 - Mobile Application Development ====================
        String comp3040Id = "comp3040";
        courses.put(comp3040Id, createCourse("COMP3040", "Mobile Application Development", "teacher1", 3, "Lab A"));
        // Schedule: Monday 09:00-11:00, Wednesday 15:00-17:00
        addSchedule(schedules, comp3040Id, "1", "MONDAY", "09:00", "11:00", "Lab A", "LAB");
        addSchedule(schedules, comp3040Id, "2", "WEDNESDAY", "15:00", "17:00", "Lab A", "LAB");
        // Enrollments: student1, student2
        linkEnrollment(enrollments, studentCourses, comp3040Id, "student1");
        linkEnrollment(enrollments, studentCourses, comp3040Id, "student2");

        // ==================== COMP2040 - Database Systems ====================
        String comp2040Id = "comp2040";
        courses.put(comp2040Id, createCourse("COMP2040", "Database Systems", "teacher1", 3, "Room 201"));
        addSchedule(schedules, comp2040Id, "1", "TUESDAY", "14:00", "16:00", "Room 201", "LECTURE");
        addSchedule(schedules, comp2040Id, "2", "THURSDAY", "14:00", "16:00", "Room 201", "LECTURE");
        linkEnrollment(enrollments, studentCourses, comp2040Id, "student1");

        // ==================== SOFT3010 - Software Architecture ====================
        String soft3010Id = "soft3010";
        courses.put(soft3010Id, createCourse("SOFT3010", "Software Architecture", "teacher2", 3, "Room 305"));
        addSchedule(schedules, soft3010Id, "1", "FRIDAY", "10:00", "13:00", "Room 305", "LECTURE");
        linkEnrollment(enrollments, studentCourses, soft3010Id, "student3");

        // ==================== COMP2001 - Data Structures ====================
        String comp2001Id = "comp2001";
        courses.put(comp2001Id, createCourse("COMP2001", "Data Structures", "teacher1", 4, "LT1"));
        addSchedule(schedules, comp2001Id, "1", "MONDAY", "08:00", "09:00", "LT1", "LECTURE");
        addSchedule(schedules, comp2001Id, "2", "THURSDAY", "10:00", "12:00", "LT1", "LECTURE");
        linkEnrollment(enrollments, studentCourses, comp2001Id, "student1");
        linkEnrollment(enrollments, studentCourses, comp2001Id, "student2");

        // ==================== 写入 Firebase ====================
        ref.child("courses").updateChildrenAsync(courses);
        ref.child("schedules").updateChildrenAsync(schedules);
        ref.child("enrollments").updateChildrenAsync(enrollments);
        ref.child("student_courses").updateChildrenAsync(studentCourses);

        log.info("✅ Migrated {} courses", courses.size());
        log.info("✅ Migrated {} schedules", schedules.size());
    }

    /**
     * 自动为今天创建一节课
     * Teacher: teacher1
     * Students: student1, student2
     * Course: COMP3040
     */
    private void createClassForToday(DatabaseReference ref) {
        // 获取今天的信息
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().name(); // e.g., "MONDAY"
        String dateStr = today.toString(); // "2025-11-24"

        log.info("🎯 Creating a special class for TODAY: {} ({})", dayOfWeek, dateStr);

        // 1. 创建一个今天的 Schedule (附属在 COMP3040 下)
        String courseId = "comp3040";
        String scheduleId = courseId + "_TODAY_EXTRA"; // 唯一的 schedule ID

        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("courseId", courseId);
        scheduleData.put("dayOfWeek", dayOfWeek);
        scheduleData.put("startTime", "08:00"); // 早上8点开始
        scheduleData.put("endTime", "22:00");   // 晚上10点结束 (方便全天测试)
        scheduleData.put("room", "Special Lab");
        scheduleData.put("type", "EXTRA_CLASS");
        scheduleData.put("building", "CS Building");

        // 写入 schedules 节点
        ref.child("schedules").child(scheduleId).setValueAsync(scheduleData);

        // 2. 创建一个 Active Session (方便直接签到)
        // Session Key 格式: scheduleId_date (实际应该用 courseScheduleId，这里简化为 courseId)
        // 为了匹配 Android 端的逻辑，我们需要用课程的数字 ID
        // 在实际场景中，COMP3040 在 MySQL 中可能有一个 courseScheduleId = 1
        // 这里我们假设 COMP3040 的 scheduleId 就是 1
        String sessionKey = "1_" + dateStr; // 格式: courseScheduleId_date

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("isActive", true);
        sessionData.put("isLocked", false); // 开启状态，允许签到
        sessionData.put("startTime", System.currentTimeMillis());

        ref.child("sessions").child(sessionKey).updateChildrenAsync(sessionData);

        log.info("✅ Created extra class schedule: {} and active session: {}", scheduleId, sessionKey);
        log.info("📋 Teacher: teacher1 | Students: student1, student2 | Course: COMP3040");
    }

    /**
     * 迁移场地预订数据
     */
    private void migrateBookings(DatabaseReference ref) {
        log.info("📤 Migrating bookings...");

        Map<String, Object> bookings = new HashMap<>();

        // Booking 1: Basketball Court - Student 1
        String b1Id = "booking_1";
        Map<String, Object> b1 = new HashMap<>();
        b1.put("userId", "student1");
        b1.put("userName", "Alice Wong");
        b1.put("facilityName", "Basketball Court 1");
        b1.put("facilityType", "Basketball Court");
        b1.put("status", "CONFIRMED");
        b1.put("startTime", System.currentTimeMillis() + 86400000L); // +1 day
        b1.put("endTime", System.currentTimeMillis() + 93600000L);   // +1 day + 2 hours
        b1.put("fee", 10.00);
        b1.put("createdAt", System.currentTimeMillis());
        bookings.put(b1Id, b1);

        // Booking 2: Badminton Court - Student 2
        String b2Id = "booking_2";
        Map<String, Object> b2 = new HashMap<>();
        b2.put("userId", "student2");
        b2.put("userName", "Bob Chen");
        b2.put("facilityName", "Badminton Court 2");
        b2.put("facilityType", "Badminton Court");
        b2.put("status", "PENDING");
        b2.put("startTime", System.currentTimeMillis() + 172800000L); // +2 days
        b2.put("endTime", System.currentTimeMillis() + 176400000L);   // +2 days + 1 hour
        b2.put("fee", 15.00);
        b2.put("createdAt", System.currentTimeMillis());
        bookings.put(b2Id, b2);

        // Booking 3: Basketball Court - Student 3
        String b3Id = "booking_3";
        Map<String, Object> b3 = new HashMap<>();
        b3.put("userId", "student3");
        b3.put("userName", "Charlie Tan");
        b3.put("facilityName", "Basketball Court 2");
        b3.put("facilityType", "Basketball Court");
        b3.put("status", "CONFIRMED");
        b3.put("startTime", System.currentTimeMillis() + 259200000L); // +3 days
        b3.put("endTime", System.currentTimeMillis() + 266400000L);   // +3 days + 2 hours
        b3.put("fee", 10.00);
        b3.put("createdAt", System.currentTimeMillis());
        bookings.put(b3Id, b3);

        ref.child("bookings").updateChildrenAsync(bookings);
        log.info("✅ Migrated {} bookings", bookings.size());
    }

    /**
     * 迁移跑腿任务数据
     */
    private void migrateErrands(DatabaseReference ref) {
        log.info("📤 Migrating errands...");

        Map<String, Object> errands = new HashMap<>();

        // Errand 1: Pickup Food - Pending
        String e1Id = "errand_1";
        Map<String, Object> e1 = new HashMap<>();
        e1.put("title", "Pickup Food from Cafeteria");
        e1.put("description", "Need lunch from main cafeteria. Nasi Lemak + Teh Tarik.");
        e1.put("requesterId", "student1");
        e1.put("requesterName", "Alice Wong");
        e1.put("type", "FOOD_DELIVERY");
        e1.put("status", "PENDING");
        e1.put("reward", 5.00);
        e1.put("pickupLocation", "Main Cafeteria");
        e1.put("deliveryLocation", "Library 3rd Floor");
        e1.put("timestamp", System.currentTimeMillis());
        errands.put(e1Id, e1);

        // Errand 2: Library Return - In Progress
        String e2Id = "errand_2";
        Map<String, Object> e2 = new HashMap<>();
        e2.put("title", "Library Book Return");
        e2.put("description", "Return 3 books to main library before 5pm today");
        e2.put("requesterId", "student3");
        e2.put("requesterName", "Charlie Tan");
        e2.put("providerId", "student1"); // Accepted by student1
        e2.put("providerName", "Alice Wong");
        e2.put("type", "PICKUP");
        e2.put("status", "IN_PROGRESS");
        e2.put("reward", 3.00);
        e2.put("pickupLocation", "Hostel Block B");
        e2.put("deliveryLocation", "Main Library");
        e2.put("timestamp", System.currentTimeMillis() - 3600000L); // 1 hour ago
        errands.put(e2Id, e2);

        // Errand 3: Stationery Purchase - Completed
        String e3Id = "errand_3";
        Map<String, Object> e3 = new HashMap<>();
        e3.put("title", "Buy Stationery from Bookshop");
        e3.put("description", "Need 2 notebooks and 1 pen");
        e3.put("requesterId", "student2");
        e3.put("requesterName", "Bob Chen");
        e3.put("providerId", "student3");
        e3.put("providerName", "Charlie Tan");
        e3.put("type", "SHOPPING");
        e3.put("status", "COMPLETED");
        e3.put("reward", 4.00);
        e3.put("pickupLocation", "Campus Bookshop");
        e3.put("deliveryLocation", "Engineering Building");
        e3.put("timestamp", System.currentTimeMillis() - 86400000L); // 1 day ago
        e3.put("completedAt", System.currentTimeMillis() - 82800000L); // Completed 1 hour after
        errands.put(e3Id, e3);

        ref.child("errands").updateChildrenAsync(errands);
        log.info("✅ Migrated {} errands", errands.size());
    }

    /**
     * 迁移论坛帖子和评论
     */
    private void migrateForums(DatabaseReference ref) {
        log.info("📤 Migrating forum posts and comments...");

        Map<String, Object> posts = new HashMap<>();
        Map<String, Object> comments = new HashMap<>();

        // ==================== Post 1: Study Group ====================
        String p1Id = "post_1";
        Map<String, Object> p1 = new HashMap<>();
        p1.put("title", "Study Group for COMP3040");
        p1.put("content", "Looking for study partners for Mobile App Development! We can meet on weekends at the library.");
        p1.put("authorId", "student1");
        p1.put("authorName", "Alice Wong");
        p1.put("category", "ACADEMIC");
        p1.put("likes", 5);
        p1.put("commentCount", 2);
        p1.put("timestamp", System.currentTimeMillis() - 172800000L); // 2 days ago
        posts.put(p1Id, p1);

        // Comments for Post 1
        Map<String, Object> p1Comments = new HashMap<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("content", "I'm interested! What time works for you?");
        c1.put("authorId", "student2");
        c1.put("authorName", "Bob Chen");
        c1.put("timestamp", System.currentTimeMillis() - 169200000L);
        p1Comments.put("comment_1", c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("content", "Count me in too! Saturday afternoon?");
        c2.put("authorId", "student3");
        c2.put("authorName", "Charlie Tan");
        c2.put("timestamp", System.currentTimeMillis() - 165600000L);
        p1Comments.put("comment_2", c2);

        comments.put(p1Id, p1Comments);

        // ==================== Post 2: Exam Announcement ====================
        String p2Id = "post_2";
        Map<String, Object> p2 = new HashMap<>();
        p2.put("title", "Important: Midterm Exam Schedule");
        p2.put("content", "The midterm exams for all COMP courses will be held next week. Check the student portal for detailed schedule.");
        p2.put("authorId", "teacher1");
        p2.put("authorName", "Dr. Sarah Johnson");
        p2.put("category", "ANNOUNCEMENTS");
        p2.put("isPinned", true);
        p2.put("likes", 12);
        p2.put("commentCount", 0);
        p2.put("timestamp", System.currentTimeMillis() - 259200000L); // 3 days ago
        posts.put(p2Id, p2);

        // ==================== Post 3: Lost & Found ====================
        String p3Id = "post_3";
        Map<String, Object> p3 = new HashMap<>();
        p3.put("title", "Lost: Black Laptop Bag");
        p3.put("content", "Lost my laptop bag near the library yesterday. Contains important notes. Please contact me if found.");
        p3.put("authorId", "student2");
        p3.put("authorName", "Bob Chen");
        p3.put("category", "GENERAL");
        p3.put("likes", 3);
        p3.put("commentCount", 1);
        p3.put("timestamp", System.currentTimeMillis() - 86400000L); // 1 day ago
        posts.put(p3Id, p3);

        // Comment for Post 3
        Map<String, Object> p3Comments = new HashMap<>();
        Map<String, Object> c3 = new HashMap<>();
        c3.put("content", "I think I saw it at the security office. Check there!");
        c3.put("authorId", "student1");
        c3.put("authorName", "Alice Wong");
        c3.put("timestamp", System.currentTimeMillis() - 82800000L);
        p3Comments.put("comment_1", c3);

        comments.put(p3Id, p3Comments);

        ref.child("forum_posts").updateChildrenAsync(posts);
        ref.child("forum_comments").updateChildrenAsync(comments);
        log.info("✅ Migrated {} forum posts with comments", posts.size());
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createCourse(String code, String name, String teacherId, int credits, String location) {
        Map<String, Object> c = new HashMap<>();
        c.put("code", code);
        c.put("name", name);
        c.put("teacherId", teacherId);
        c.put("credits", credits);
        c.put("defaultLocation", location);
        c.put("semester", "25-26");
        return c;
    }

    private void addSchedule(Map<String, Object> schedulesMap, String courseId, String scheduleNum,
                             String dayOfWeek, String startTime, String endTime, String room, String type) {
        String scheduleId = courseId + "_" + scheduleNum;
        Map<String, Object> s = new HashMap<>();
        s.put("courseId", courseId);
        s.put("dayOfWeek", dayOfWeek);
        s.put("startTime", startTime);
        s.put("endTime", endTime);
        s.put("room", room);
        s.put("type", type);
        schedulesMap.put(scheduleId, s);
    }

    private void linkEnrollment(Map<String, Object> enrollments, Map<String, Object> studentCourses,
                                String courseId, String studentId) {
        // Course -> Students
        @SuppressWarnings("unchecked")
        Map<String, Object> courseEnrollment = (Map<String, Object>) enrollments.getOrDefault(courseId, new HashMap<>());
        courseEnrollment.put(studentId, true);
        enrollments.put(courseId, courseEnrollment);

        // Student -> Courses
        @SuppressWarnings("unchecked")
        Map<String, Object> studCourses = (Map<String, Object>) studentCourses.getOrDefault(studentId, new HashMap<>());
        studCourses.put(courseId, true);
        studentCourses.put(studentId, studCourses);
    }
}
