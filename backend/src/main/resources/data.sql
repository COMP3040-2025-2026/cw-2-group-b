-- Insert sample users (passwords are BCrypt hashed 'password123')
-- Students
INSERT INTO users (username, password, email, full_name, phone, role, status, created_at, updated_at) VALUES
('student1', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'student1@nottingham.edu.my', 'Alice Wong', '+60123456789', 'STUDENT', 'ACTIVE', NOW(), NOW()),
('student2', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'student2@nottingham.edu.my', 'Bob Chen', '+60123456790', 'STUDENT', 'ACTIVE', NOW(), NOW()),
('student3', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'student3@nottingham.edu.my', 'Charlie Lee', '+60123456791', 'STUDENT', 'ACTIVE', NOW(), NOW());

-- Teachers
INSERT INTO users (username, password, email, full_name, phone, role, status, created_at, updated_at) VALUES
('teacher1', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'teacher1@nottingham.edu.my', 'Dr. Sarah Johnson', '+60198765432', 'TEACHER', 'ACTIVE', NOW(), NOW()),
('teacher2', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'teacher2@nottingham.edu.my', 'Prof. John Smith', '+60198765433', 'TEACHER', 'ACTIVE', NOW(), NOW());

-- Admin
INSERT INTO users (username, password, email, full_name, phone, role, status, created_at, updated_at) VALUES
('admin', '$2a$10$9i02LK5xSVkgXrAouf7gO.cJPu8h2b/Ew.b7Rr0Bo4QgB4o2D.CP6', 'admin@nottingham.edu.my', 'System Admin', '+60199999999', 'ADMIN', 'ACTIVE', NOW(), NOW());

-- Student details
INSERT INTO students (user_id, student_id, faculty, major, year_of_study, matric_number, gpa) VALUES
(1, 20210001, 'Faculty of Computer Science', 'Computer Science', 3, 'M2021001', 3.75),
(2, 20220002, 'Faculty of Computer Science', 'Software Engineering', 2, 'M2022002', 3.50),
(3, 20200003, 'Faculty of Engineering', 'Electrical Engineering', 4, 'M2020003', 3.85);

-- Teacher details
INSERT INTO teachers (user_id, employee_id, department, title, office_room, office_hours) VALUES
(4, 'T001', 'Computer Science Department', 'Associate Professor', 'CS-301', 'Mon/Wed 2-4 PM'),
(5, 'T002', 'Software Engineering Department', 'Professor', 'CS-401', 'Tue/Thu 10 AM-12 PM');

-- Courses
INSERT INTO courses (course_code, course_name, description, credits, faculty, semester, teacher_id, capacity, enrolled, created_at, updated_at) VALUES
('COMP3040', 'Mobile Application Development', 'Introduction to Android and iOS development', 3, 'Faculty of Computer Science', 'Fall 2024', 4, 50, 2, NOW(), NOW()),
('COMP2040', 'Database Systems', 'Relational databases and SQL', 3, 'Faculty of Computer Science', 'Fall 2024', 4, 60, 1, NOW(), NOW()),
('SOFT3010', 'Software Architecture', 'Design patterns and architecture', 3, 'Faculty of Computer Science', 'Fall 2024', 5, 40, 1, NOW(), NOW());

-- Course Schedules
INSERT INTO course_schedules (course_id, day_of_week, start_time, end_time, room, building, course_type, created_at, updated_at) VALUES
(1, 'MONDAY', '09:00:00', '11:00:00', 'Lab A', 'CS Building', 'LAB', NOW(), NOW()),
(1, 'TUESDAY', '14:00:00', '16:00:00', 'Lab B', 'CS Building', 'LAB', NOW(), NOW()),
(1, 'WEDNESDAY', '15:00:00', '17:00:00', 'Lab A', 'CS Building', 'LAB', NOW(), NOW()),
(2, 'TUESDAY', '14:00:00', '16:00:00', 'Room 201', 'CS Building', 'LECTURE', NOW(), NOW()),
(2, 'THURSDAY', '14:00:00', '16:00:00', 'Room 201', 'CS Building', 'LECTURE', NOW(), NOW()),
(3, 'FRIDAY', '10:00:00', '13:00:00', 'Room 305', 'CS Building', 'LECTURE', NOW(), NOW());

-- Enrollments
INSERT INTO enrollments (student_id, course_id, status, grade, letter_grade, created_at, updated_at) VALUES
(1, 1, 'ACTIVE', NULL, NULL, NOW(), NOW()),
(1, 2, 'ACTIVE', NULL, NULL, NOW(), NOW()),
(2, 1, 'ACTIVE', NULL, NULL, NOW(), NOW()),
(3, 3, 'ACTIVE', NULL, NULL, NOW(), NOW());

-- Attendances
INSERT INTO attendances (student_id, course_id, attendance_date, status, check_in_time, remarks, location, created_at, updated_at) VALUES
(1, 1, DATEADD('DAY', -7, CURRENT_DATE), 'PRESENT', DATEADD('DAY', -7, NOW()), 'On time', 'CS Building Lab A', NOW(), NOW()),
(1, 1, DATEADD('DAY', -5, CURRENT_DATE), 'PRESENT', DATEADD('DAY', -5, NOW()), 'On time', 'CS Building Lab A', NOW(), NOW()),
(1, 1, DATEADD('DAY', -2, CURRENT_DATE), 'LATE', DATEADD('DAY', -2, NOW()), '5 minutes late', 'CS Building Lab A', NOW(), NOW()),
(2, 1, DATEADD('DAY', -7, CURRENT_DATE), 'PRESENT', DATEADD('DAY', -7, NOW()), 'On time', 'CS Building Lab A', NOW(), NOW()),
(2, 1, DATEADD('DAY', -5, CURRENT_DATE), 'ABSENT', NULL, 'No show', NULL, NOW(), NOW());

-- Bookings
INSERT INTO bookings (user_id, facility_name, facility_type, start_time, end_time, status, purpose, fee, remarks, created_at, updated_at) VALUES
(1, 'Basketball Court 1', 'Basketball Court', DATEADD('DAY', 1, NOW()), DATEADD('HOUR', 2, DATEADD('DAY', 1, NOW())), 'CONFIRMED', 'Practice session', 10.00, 'Need basketballs', NOW(), NOW()),
(2, 'Badminton Court 2', 'Badminton Court', DATEADD('DAY', 2, NOW()), DATEADD('HOUR', 1, DATEADD('DAY', 2, NOW())), 'PENDING', 'Tournament', 15.00, NULL, NOW(), NOW());

-- Errands
INSERT INTO errands (requester_id, provider_id, title, description, type, location, deadline, reward, additional_notes, status, image_url, created_at, updated_at) VALUES
(1, NULL, 'Pickup Food from Cafeteria', 'Need someone to pickup lunch from cafeteria', 'FOOD_DELIVERY', 'Main Cafeteria', '2024-12-20 12:30:00', 5.00, 'Will pay extra if quick', 'PENDING', NULL, NOW(), NOW()),
(2, NULL, 'Buy Stationery from Bookstore', 'Need notebooks and pens', 'SHOPPING', 'Campus Bookstore', '2024-12-21 17:00:00', 8.00, NULL, 'PENDING', NULL, NOW(), NOW()),
(3, 1, 'Library Book Return', 'Return books to library', 'PICKUP', 'Main Library', '2024-12-19 16:00:00', 3.00, 'Books are at my dorm', 'IN_PROGRESS', NULL, NOW(), NOW());

-- Forum Posts
INSERT INTO forum_posts (author_id, title, content, category, likes, views, tags, is_pinned, is_locked, created_at, updated_at) VALUES
(1, 'Study Group for COMP3040', 'Looking for students interested in forming a study group for Mobile App Development. Let''s meet on Fridays!', 'ACADEMIC', 5, 25, 'study-group,mobile-dev', FALSE, FALSE, NOW(), NOW()),
(4, 'Important: Midterm Exam Schedule', 'The midterm exams will be held from Dec 15-20. Please check your course schedules.', 'ANNOUNCEMENTS', 12, 150, 'exam,important', TRUE, FALSE, NOW(), NOW()),
(2, 'Basketball Tournament Next Week', 'Anyone interested in joining the inter-faculty basketball tournament? Sign up at sports complex!', 'SPORTS', 8, 45, 'basketball,tournament', FALSE, FALSE, NOW(), NOW());

-- Forum Comments
INSERT INTO forum_comments (post_id, author_id, content, likes, created_at, updated_at) VALUES
(1, 2, 'I''m interested! What time on Fridays?', 2, NOW(), NOW()),
(1, 3, 'Count me in too!', 1, NOW(), NOW()),
(3, 1, 'I''ll join! When is the sign up deadline?', 0, NOW(), NOW());

-- Messages
INSERT INTO messages (sender_id, receiver_id, content, is_read, type, attachment_url, created_at, updated_at) VALUES
(1, 2, 'Hey! Did you understand the lecture today?', TRUE, 'TEXT', NULL, DATEADD('HOUR', -2, NOW()), DATEADD('HOUR', -2, NOW())),
(2, 1, 'Yeah, but I''m confused about the MVC pattern. Can we discuss?', TRUE, 'TEXT', NULL, DATEADD('HOUR', -1, NOW()), DATEADD('HOUR', -1, NOW())),
(1, 2, 'Sure! Let''s meet at the library tomorrow.', FALSE, 'TEXT', NULL, DATEADD('MINUTE', -30, NOW()), DATEADD('MINUTE', -30, NOW())),
(4, 1, 'Your assignment submission was excellent! Keep up the good work.', FALSE, 'TEXT', NULL, NOW(), NOW());
