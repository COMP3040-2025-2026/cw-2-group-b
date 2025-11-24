package com.nottingham.mynottingham.backend.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Data Migration Service
 *
 * 在应用启动时将核心数据从 MySQL 迁移到 Firebase Realtime Database
 *
 * 数据结构设计 (NoSQL):
 * - users/{userId}: 用户信息 (学生/教师)
 * - courses/{courseId}: 课程基本信息
 * - schedules/{scheduleId}: 课程排课信息
 * - enrollments/{courseId}/{studentId}: 课程注册关系 (Course -> Students)
 * - student_courses/{studentId}/{courseId}: 学生选课关系 (Student -> Courses)
 *
 * 优点：
 * 1. 实时同步：所有客户端实时看到数据变化
 * 2. 离线支持：Firebase 自动缓存数据
 * 3. 扁平化设计：避免复杂 JOIN，提升读取性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseDataMigrationService implements CommandLineRunner {

    private final FirebaseDatabase firebaseDatabase;

    // 迁移开关 - 设置为 false 可跳过迁移（避免覆盖已有数据）
    private static final boolean RUN_MIGRATION = true;

    @Override
    public void run(String... args) throws Exception {
        if (!RUN_MIGRATION) {
            log.info("⏸️ Firebase migration is disabled (RUN_MIGRATION = false)");
            return;
        }

        try {
            log.info("🚀 Starting Firebase Data Migration...");

            DatabaseReference ref = firebaseDatabase.getReference();

            // 迁移用户数据
            migrateUsers(ref);

            // 迁移课程和排课数据
            migrateCoursesAndSchedules(ref);

            log.info("✅ Firebase Data Migration Completed Successfully!");
            log.info("📊 Check Firebase Console: https://console.firebase.google.com");

        } catch (Exception e) {
            log.error("❌ Firebase migration failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 迁移用户数据 (教师 + 学生)
     */
    private void migrateUsers(DatabaseReference ref) {
        log.info("📤 Migrating users...");

        Map<String, Object> users = new HashMap<>();

        // --- 教师数据 ---
        Map<String, Object> teacher1 = new HashMap<>();
        teacher1.put("username", "teacher1");
        teacher1.put("fullName", "Dr. John Smith");
        teacher1.put("email", "teacher1@nottingham.edu.my");
        teacher1.put("role", "TEACHER");
        teacher1.put("employeeId", "T001");
        teacher1.put("department", "Computer Science");
        users.put("teacher1", teacher1);

        Map<String, Object> teacher2 = new HashMap<>();
        teacher2.put("username", "teacher2");
        teacher2.put("fullName", "Dr. Sarah Johnson");
        teacher2.put("email", "teacher2@nottingham.edu.my");
        teacher2.put("role", "TEACHER");
        teacher2.put("employeeId", "T002");
        teacher2.put("department", "Computer Science");
        users.put("teacher2", teacher2);

        // --- 学生数据 ---
        Map<String, Object> student1 = new HashMap<>();
        student1.put("username", "student1");
        student1.put("fullName", "Alice Wong");
        student1.put("email", "student1@student.nottingham.edu.my");
        student1.put("role", "STUDENT");
        student1.put("studentId", 1L);
        student1.put("matricNumber", "20123456");
        student1.put("faculty", "Faculty of Science and Engineering");
        users.put("student1", student1);

        Map<String, Object> student2 = new HashMap<>();
        student2.put("username", "student2");
        student2.put("fullName", "Bob Chen");
        student2.put("email", "student2@student.nottingham.edu.my");
        student2.put("role", "STUDENT");
        student2.put("studentId", 2L);
        student2.put("matricNumber", "20123457");
        student2.put("faculty", "Faculty of Science and Engineering");
        users.put("student2", student2);

        Map<String, Object> student3 = new HashMap<>();
        student3.put("username", "student3");
        student3.put("fullName", "Charlie Tan");
        student3.put("email", "student3@student.nottingham.edu.my");
        student3.put("role", "STUDENT");
        student3.put("studentId", 3L);
        student3.put("matricNumber", "20123458");
        student3.put("faculty", "Faculty of Science and Engineering");
        users.put("student3", student3);

        // 写入 Firebase
        ref.child("users").updateChildrenAsync(users);
        log.info("✅ Migrated {} users", users.size());
    }

    /**
     * 迁移课程、排课和选课关系
     */
    private void migrateCoursesAndSchedules(DatabaseReference ref) {
        log.info("📤 Migrating courses, schedules and enrollments...");

        Map<String, Object> courses = new HashMap<>();
        Map<String, Object> schedules = new HashMap<>();
        Map<String, Object> enrollments = new HashMap<>(); // courseId -> {studentId: true}
        Map<String, Object> studentCourses = new HashMap<>(); // studentId -> {courseId: true}

        // ==================== COMP3040 - Mobile Device Programming ====================
        String comp3040Id = "comp3040";

        Map<String, Object> comp3040 = new HashMap<>();
        comp3040.put("code", "COMP3040");
        comp3040.put("name", "Mobile Device Programming");
        comp3040.put("description", "Introduction to Android and iOS development");
        comp3040.put("teacherId", "teacher1");
        comp3040.put("credits", 3);
        comp3040.put("semester", "25-26");
        courses.put(comp3040Id, comp3040);

        // COMP3040 排课
        addSchedule(schedules, comp3040Id, "1", "MONDAY", "09:00", "10:00", "Lab 2A", "LAB");
        addSchedule(schedules, comp3040Id, "2", "WEDNESDAY", "09:00", "11:00", "Lab 2A", "LAB");

        // COMP3040 选课学生: student1, student2, student3
        linkEnrollment(enrollments, studentCourses, comp3040Id, "student1");
        linkEnrollment(enrollments, studentCourses, comp3040Id, "student2");
        linkEnrollment(enrollments, studentCourses, comp3040Id, "student3");

        // ==================== COMP2001 - Data Structures ====================
        String comp2001Id = "comp2001";

        Map<String, Object> comp2001 = new HashMap<>();
        comp2001.put("code", "COMP2001");
        comp2001.put("name", "Data Structures");
        comp2001.put("description", "Fundamental data structures and algorithms");
        comp2001.put("teacherId", "teacher1");
        comp2001.put("credits", 4);
        comp2001.put("semester", "25-26");
        courses.put(comp2001Id, comp2001);

        // COMP2001 排课
        addSchedule(schedules, comp2001Id, "1", "MONDAY", "08:00", "09:00", "LT1", "LECTURE");
        addSchedule(schedules, comp2001Id, "2", "THURSDAY", "10:00", "12:00", "LT1", "LECTURE");

        // COMP2001 选课学生: student1, student2
        linkEnrollment(enrollments, studentCourses, comp2001Id, "student1");
        linkEnrollment(enrollments, studentCourses, comp2001Id, "student2");

        // ==================== COMP3041 - Professional Ethics ====================
        String comp3041Id = "comp3041";

        Map<String, Object> comp3041 = new HashMap<>();
        comp3041.put("code", "COMP3041");
        comp3041.put("name", "Professional Ethics in Computing");
        comp3041.put("description", "Ethical issues in computer science");
        comp3041.put("teacherId", "teacher2");
        comp3041.put("credits", 2);
        comp3041.put("semester", "25-26");
        courses.put(comp3041Id, comp3041);

        // COMP3041 排课
        addSchedule(schedules, comp3041Id, "1", "MONDAY", "14:00", "16:00", "LT3", "LECTURE");

        // COMP3041 选课学生: student1
        linkEnrollment(enrollments, studentCourses, comp3041Id, "student1");

        // ==================== COMP2004 - Computer Systems ====================
        String comp2004Id = "comp2004";

        Map<String, Object> comp2004 = new HashMap<>();
        comp2004.put("code", "COMP2004");
        comp2004.put("name", "Computer Systems");
        comp2004.put("description", "Computer architecture and operating systems");
        comp2004.put("teacherId", "teacher2");
        comp2004.put("credits", 4);
        comp2004.put("semester", "25-26");
        courses.put(comp2004Id, comp2004);

        // COMP2004 排课
        addSchedule(schedules, comp2004Id, "1", "TUESDAY", "14:00", "16:00", "LT2", "LECTURE");
        addSchedule(schedules, comp2004Id, "2", "THURSDAY", "14:00", "16:00", "Lab 3B", "LAB");

        // COMP2004 选课学生: student2, student3
        linkEnrollment(enrollments, studentCourses, comp2004Id, "student2");
        linkEnrollment(enrollments, studentCourses, comp2004Id, "student3");

        // ==================== 写入 Firebase ====================
        ref.child("courses").updateChildrenAsync(courses);
        ref.child("schedules").updateChildrenAsync(schedules);
        ref.child("enrollments").updateChildrenAsync(enrollments);
        ref.child("student_courses").updateChildrenAsync(studentCourses);

        log.info("✅ Migrated {} courses", courses.size());
        log.info("✅ Migrated {} schedules", schedules.size());
        log.info("✅ Migrated enrollments and student courses");
    }

    /**
     * 添加排课信息
     *
     * @param schedulesMap  排课集合
     * @param courseId      课程 ID
     * @param scheduleNum   排课序号 (同一课程可能有多个时间段)
     * @param dayOfWeek     星期几
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param room          教室
     * @param type          类型 (LECTURE/LAB/TUTORIAL)
     */
    private void addSchedule(Map<String, Object> schedulesMap, String courseId, String scheduleNum,
                             String dayOfWeek, String startTime, String endTime, String room, String type) {
        // 生成唯一的 schedule ID: courseId_scheduleNum (例如：comp3040_1, comp3040_2)
        String scheduleId = courseId + "_" + scheduleNum;

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("courseId", courseId);
        schedule.put("dayOfWeek", dayOfWeek);
        schedule.put("startTime", startTime);
        schedule.put("endTime", endTime);
        schedule.put("room", room);
        schedule.put("type", type);

        schedulesMap.put(scheduleId, schedule);
    }

    /**
     * 建立选课关系 (双向关联)
     *
     * @param enrollments     enrollments/{courseId}/{studentId} = true
     * @param studentCourses  student_courses/{studentId}/{courseId} = true
     * @param courseId        课程 ID
     * @param studentId       学生 ID
     */
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
