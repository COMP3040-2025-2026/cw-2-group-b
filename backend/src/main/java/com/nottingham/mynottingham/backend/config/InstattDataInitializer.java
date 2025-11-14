package com.nottingham.mynottingham.backend.config;

import com.nottingham.mynottingham.backend.entity.*;
import com.nottingham.mynottingham.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InstattDataInitializer {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initInstattData() {
        return args -> {
            // Check if data already initialized
            if (teacherRepository.findByEmail("teacher1@nottingham.edu.my").isPresent()) {
                log.info("Instatt data already initialized");
                return;
            }

            log.info("Initializing Instatt test data...");

            // Create Teacher1
            Teacher teacher1 = new Teacher();
            teacher1.setEmail("teacher1@nottingham.edu.my");
            teacher1.setPassword(passwordEncoder.encode("password123"));
            teacher1.setUsername("teacher1");
            teacher1.setFullName("Dr. John Smith");
            teacher1.setRole(User.UserRole.TEACHER);
            teacher1.setEmployeeId("T001");
            teacher1.setDepartment("Computer Science");
            teacher1.setOfficeRoom("CS Building Room 301");
            teacher1 = teacherRepository.save(teacher1);

            // Create Students
            Student student1 = new Student();
            student1.setEmail("student1@student.nottingham.edu.my");
            student1.setPassword(passwordEncoder.encode("password123"));
            student1.setUsername("student1");
            student1.setFullName("Alice Johnson");
            student1.setRole(User.UserRole.STUDENT);
            student1.setStudentId(20210001L);
            student1.setFaculty("Faculty of Science");
            student1.setMajor("Computer Science");
            student1.setYearOfStudy(3);
            student1 = studentRepository.save(student1);

            Student student2 = new Student();
            student2.setEmail("student2@student.nottingham.edu.my");
            student2.setPassword(passwordEncoder.encode("password123"));
            student2.setUsername("student2");
            student2.setFullName("Bob Williams");
            student2.setRole(User.UserRole.STUDENT);
            student2.setStudentId(20220002L);
            student2.setFaculty("Faculty of Science");
            student2.setMajor("Computer Science");
            student2.setYearOfStudy(3);
            student2 = studentRepository.save(student2);

            // Create 5 different courses for teacher1
            List<Course> courses = new ArrayList<>();

            // Course 1: Data Structures
            Course course1 = createCourse("COMP2001", "Data Structures",
                    "Introduction to data structures and algorithms", 3,
                    "Computer Science", "25-26", teacher1);
            courses.add(course1);

            // Course 2: Mobile Device Programming
            Course course2 = createCourse("COMP3040", "Mobile Device Programming",
                    "Android and iOS development", 3,
                    "Computer Science", "25-26", teacher1);
            courses.add(course2);

            // Course 3: Professional Ethics in Computing
            Course course3 = createCourse("COMP3041", "Professional Ethics in Computing",
                    "Ethics and professionalism in computing", 2,
                    "Computer Science", "25-26", teacher1);
            courses.add(course3);

            // Course 4: Symbolic Artificial Intelligence
            Course course4 = createCourse("COMP3070", "Symbolic Artificial Intelligence",
                    "Knowledge representation and reasoning", 3,
                    "Computer Science", "25-26", teacher1);
            courses.add(course4);

            // Course 5: Autonomous Robotic Systems
            Course course5 = createCourse("COMP4082", "Autonomous Robotic Systems",
                    "Robotics and autonomous systems", 3,
                    "Computer Science", "25-26", teacher1);
            courses.add(course5);

            // Create schedules for each course
            createScheduleForCourse(course1, DayOfWeek.MONDAY, "08:00", "09:00", "LT1", "Main Building", CourseSchedule.CourseType.LECTURE);

            createScheduleForCourse(course2, DayOfWeek.MONDAY, "09:00", "10:00", "Lab 2A", "CS Building", CourseSchedule.CourseType.LAB);
            createScheduleForCourse(course2, DayOfWeek.WEDNESDAY, "09:00", "11:00", "Lab 2A", "CS Building", CourseSchedule.CourseType.LAB);

            createScheduleForCourse(course3, DayOfWeek.MONDAY, "14:00", "16:00", "LT3", "Main Building", CourseSchedule.CourseType.LECTURE);
            createScheduleForCourse(course3, DayOfWeek.THURSDAY, "11:00", "13:00", "LT3", "Main Building", CourseSchedule.CourseType.TUTORIAL);

            createScheduleForCourse(course4, DayOfWeek.TUESDAY, "09:00", "11:00", "BB80", "BB Building", CourseSchedule.CourseType.COMPUTING);
            createScheduleForCourse(course4, DayOfWeek.FRIDAY, "14:00", "16:00", "LT1", "Main Building", CourseSchedule.CourseType.LECTURE);

            createScheduleForCourse(course5, DayOfWeek.TUESDAY, "14:00", "16:00", "F1A24", "Faculty Building", CourseSchedule.CourseType.LECTURE);
            createScheduleForCourse(course5, DayOfWeek.WEDNESDAY, "15:00", "17:00", "Lab 3B", "CS Building", CourseSchedule.CourseType.LAB);
            createScheduleForCourse(course5, DayOfWeek.FRIDAY, "16:00", "18:00", "Lab 3B", "CS Building", CourseSchedule.CourseType.LAB);

            // Enroll students
            // student1: 3 courses (course1, course2, course4)
            enrollStudent(student1, course1);
            enrollStudent(student1, course2);
            enrollStudent(student1, course4);

            // student2: 2 courses (course3, course5)
            enrollStudent(student2, course3);
            enrollStudent(student2, course5);

            log.info("Instatt test data initialized successfully!");
            log.info("Teacher: teacher1@nottingham.edu.my / password123");
            log.info("Student 1: student1@student.nottingham.edu.my / password123 (3 courses)");
            log.info("Student 2: student2@student.nottingham.edu.my / password123 (2 courses)");
        };
    }

    private Course createCourse(String code, String name, String description,
                                int credits, String faculty, String semester, Teacher teacher) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setDescription(description);
        course.setCredits(credits);
        course.setFaculty(faculty);
        course.setSemester(semester);
        course.setTeacher(teacher);
        course.setCapacity(50);
        course.setEnrolled(0);
        return courseRepository.save(course);
    }

    private void createScheduleForCourse(Course course, DayOfWeek day, String startTime,
                                        String endTime, String room, String building,
                                        CourseSchedule.CourseType type) {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourse(course);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.parse(startTime));
        schedule.setEndTime(LocalTime.parse(endTime));
        schedule.setRoom(room);
        schedule.setBuilding(building);
        schedule.setCourseType(type);
        scheduleRepository.save(schedule);
    }

    private void enrollStudent(Student student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);

        // Update course enrolled count
        course.setEnrolled(course.getEnrolled() + 1);
        courseRepository.save(course);
    }
}
