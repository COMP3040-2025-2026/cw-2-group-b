# Instatt API Integration Guide

## Overview
完整的教师-学生签到系统，支持实时状态同步。

## 测试账号

### 教师账号
- **Email**: `teacher1@nottingham.edu.my`
- **Password**: `password123`
- **ID**: 1 (登录后从响应中获取)
- **课程**: 所有5个课程

### 学生账号

**Student 1**:
- **Email**: `student1@student.nottingham.edu.my`
- **Password**: `password123`
- **ID**: 1 (登录后从响应中获取)
- **课程**: 3个课程
  - COMP2001 - Data Structures
  - COMP3040 - Mobile Device Programming
  - COMP3070 - Symbolic Artificial Intelligence

**Student 2**:
- **Email**: `student2@student.nottingham.edu.my`
- **Password**: `password123`
- **ID**: 2 (登录后从响应中获取)
- **课程**: 2个课程
  - COMP3041 - Professional Ethics in Computing
  - COMP4082 - Autonomous Robotic Systems

## API Endpoints

### Base URL
```
http://localhost:8080/api/attendance
```

### 1. 获取教师的课程列表
```
GET /teacher/{teacherId}/courses?date=2025-11-12
```

**Response**:
```json
{
  "success": true,
  "message": "Teacher courses retrieved successfully",
  "data": [
    {
      "id": 1,
      "courseId": 1,
      "courseCode": "COMP2001",
      "courseName": "Data Structures",
      "semester": "25-26",
      "dayOfWeek": "MONDAY",
      "startTime": "08:00:00",
      "endTime": "09:00:00",
      "room": "LT1",
      "building": "Main Building",
      "courseType": "LECTURE",
      "sessionStatus": "LOCKED",
      "hasStudentSigned": null,
      "unlockedAtTimestamp": null
    }
  ]
}
```

### 2. 获取学生的课程列表
```
GET /student/{studentId}/courses?date=2025-11-12
```

**Response**: 同上，但包含 `hasStudentSigned` 字段

### 3. 教师解锁签到（Unlock）
```
POST /teacher/{teacherId}/unlock
```

**Request Body**:
```json
{
  "courseScheduleId": 1,
  "sessionDate": "2025-11-12"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Session unlocked successfully",
  "data": {
    "id": 1,
    "sessionDate": "2025-11-12",
    "status": "UNLOCKED",
    "unlockedAt": "2025-11-12T10:00:00"
  }
}
```

### 4. 教师锁定签到（Lock）
```
POST /teacher/{teacherId}/lock
```

**Request Body**: 同上

### 5. 学生签到
```
POST /student/{studentId}/signin
```

**Request Body**:
```json
{
  "courseScheduleId": 1,
  "sessionDate": "2025-11-12"
}
```

## 课程时间表

### Monday
- 08:00-09:00: COMP2001 Data Structures (Lecture) - LT1
- 09:00-10:00: COMP3040 Mobile Device Programming (Lab) - Lab 2A
- 14:00-16:00: COMP3041 Professional Ethics (Lecture) - LT3

### Tuesday
- 09:00-11:00: COMP3070 Symbolic AI (Computing) - BB80
- 14:00-16:00: COMP4082 Autonomous Robotics (Lecture) - F1A24

### Wednesday
- 09:00-11:00: COMP3040 Mobile Device Programming (Lab) - Lab 2A
- 15:00-17:00: COMP4082 Autonomous Robotics (Lab) - Lab 3B

### Thursday
- 11:00-13:00: COMP3041 Professional Ethics (Tutorial) - LT3

### Friday
- 14:00-16:00: COMP3070 Symbolic AI (Lecture) - LT1
- 16:00-18:00: COMP4082 Autonomous Robotics (Lab) - Lab 3B

## 业务逻辑

### 签到状态流转
1. **LOCKED** (默认) - 灰色锁图标
   - 教师未开启签到
   - 学生无法签到

2. **UNLOCKED** (教师点击 Unlock) - 蓝色铅笔图标
   - 教师开启签到
   - 学生可以点击签到
   - 20分钟后自动变为 CLOSED

3. **CLOSED** (自动或手动锁定)
   - 已签到: 绿色勾
   - 未签到: 红色叉

### 自动锁定
- 系统每1分钟检查一次
- Unlock 20分钟后自动锁定
- 教师也可以手动锁定

## 数据库表结构

### attendance_sessions
签到会话表（核心）
- `id`: 主键
- `course_schedule_id`: 课程时间表ID
- `session_date`: 会话日期
- `status`: LOCKED / UNLOCKED / CLOSED
- `unlocked_at`: 解锁时间
- `locked_at`: 锁定时间
- `unlocked_by_teacher_id`: 解锁的教师
- `auto_lock_minutes`: 自动锁定分钟数(默认20)

### attendances
实际出勤记录表
- `id`: 主键
- `student_id`: 学生ID
- `course_id`: 课程ID
- `attendance_date`: 出勤日期
- `status`: PRESENT / ABSENT / LATE / EXCUSED
- `check_in_time`: 签到时间

### course_schedules
课程时间表
- `id`: 主键
- `course_id`: 课程ID
- `day_of_week`: 星期几
- `start_time`: 开始时间
- `end_time`: 结束时间
- `room`: 教室
- `building`: 建筑物
- `course_type`: 课程类型 (LECTURE/TUTORIAL/COMPUTING/LAB)

### enrollments
学生选课表
- `student_id` + `course_id`: 唯一约束
- `status`: ACTIVE / DROPPED / COMPLETED / FAILED

## Android 集成

### 1. 使用 Repository
```kotlin
val repository = InstattRepository()

// 教师端获取课程
viewModelScope.launch {
    val result = repository.getTeacherCourses(teacherId, "2025-11-12")
    result.onSuccess { courses ->
        // 更新 UI
    }
}

// 教师解锁课程
viewModelScope.launch {
    val result = repository.unlockSession(teacherId, courseScheduleId, "2025-11-12")
    result.onSuccess {
        // 刷新课程列表
    }
}

// 学生签到
viewModelScope.launch {
    val result = repository.signIn(studentId, courseScheduleId, "2025-11-12")
    result.onSuccess {
        // 更新 UI 显示已签到
    }
}
```

### 2. 轮询更新（学生端）
```kotlin
// 每30秒刷新一次课程状态
private fun startPolling() {
    viewModelScope.launch {
        while (isActive) {
            loadCourses()
            delay(30_000) // 30 seconds
        }
    }
}
```

## 测试流程

### 1. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 2. 测试教师端
1. 登录 `teacher1@nottingham.edu.my`
2. 获取今天的课程列表
3. 点击某个课程的 "Unlock" 按钮
4. 验证状态变为 UNLOCKED

### 3. 测试学生端
1. 登录 `student1@student.nottingham.edu.my`
2. 获取今天的课程列表
3. 看到教师已 unlock 的课程显示蓝色铅笔
4. 点击铅笔签到
5. 验证变成绿色勾

### 4. 测试自动锁定
1. 等待20分钟（或修改代码为1分钟测试）
2. 验证 UNLOCKED 状态自动变为 CLOSED
3. 未签到的学生会看到红色叉

## 课程唯一标识
- **格式**: `{courseCode}-{semester}`
- **示例**: `COMP3040-25-26`
- **用途**: 确保同一门课程在不同学期有不同的记录

## 注意事项

1. **时区**: 所有时间使用系统默认时区
2. **日期格式**: ISO 8601 格式 `YYYY-MM-DD`
3. **并发**: 使用数据库事务确保数据一致性
4. **安全**: 验证教师/学生是否有权限操作对应课程
5. **状态同步**: 学生端需要定期轮询或使用 WebSocket 实时更新
