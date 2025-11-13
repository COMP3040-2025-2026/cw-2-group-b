# Instatt 学生端使用指南

## 功能特性

### 1. 实时状态同步
- 学生端每 **30秒** 自动从后端获取最新课程状态
- 教师 unlock 课程后，学生端会自动显示可签到状态
- 无需手动刷新

### 2. 课程状态显示

#### 🔒 LOCKED (灰色锁)
- **含义**: 教师未开启签到
- **学生操作**: 无法签到，只能等待
- **图标**: 灰色锁

#### ✏️ UNLOCKED (蓝色铅笔)
- **含义**: 教师已开启签到，可以签到
- **学生操作**: 点击铅笔图标即可签到
- **图标**: 蓝色铅笔（可点击）

#### ✅ SIGNED (绿色勾)
- **含义**: 已成功签到
- **学生操作**: 无需操作
- **图标**: 绿色勾

#### ❌ MISSED (红色叉)
- **含义**: 教师开过签到但学生未签
- **学生操作**: 无法补签
- **图标**: 红色叉

### 3. 出勤统计逻辑

**重要**: 只有教师开过签到的课程才计入总课时！

- **已出勤课时** (attendedClasses): 学生成功签到的课程数量
- **总课时** (totalSignedClasses): 教师开过签到的课程总数
- **出勤率** = 已出勤课时 / 总课时

**示例**:
```
如果教师一共 unlock 了 10 次课程：
- 学生签到了 8 次 → 已出勤: 8, 总课时: 10, 出勤率: 80%
- 学生签到了 10 次 → 已出勤: 10, 总课时: 10, 出勤率: 100%
- 学生签到了 5 次 → 已出勤: 5, 总课时: 10, 出勤率: 50%

如果教师只 unlock 了 3 次课程：
- 学生签到了 2 次 → 已出勤: 2, 总课时: 3, 出勤率: 67%
```

### 4. 工作流程

```
1. 学生打开 Instatt → 看到所有课程（默认 LOCKED 状态）
                      ↓
2. 教师在课堂上点击 "Unlock" → 后端数据库更新状态为 UNLOCKED
                      ↓
3. 学生端自动轮询获取更新 (最多30秒延迟)
                      ↓
4. 学生看到蓝色铅笔图标 → 点击签到
                      ↓
5. 后端记录出勤 → 前端图标变绿色勾
                      ↓
6. 20分钟后自动关闭 OR 教师手动 Lock
                      ↓
7. 未签到的学生看到红色叉
```

## API 端点

学生端使用的 API:

### 获取课程列表
```
GET /api/attendance/student/{studentId}/courses?date=2025-11-12
```

**Response 示例**:
```json
{
  "success": true,
  "message": "Student courses retrieved successfully",
  "data": [
    {
      "id": 1,
      "courseCode": "COMP3040",
      "courseName": "Mobile Device Programming",
      "dayOfWeek": "WEDNESDAY",
      "startTime": "09:00:00",
      "endTime": "11:00:00",
      "courseType": "LAB",
      "sessionStatus": "UNLOCKED",
      "hasStudentSigned": false,
      "attendedClasses": 8,
      "totalSignedClasses": 10
    }
  ]
}
```

### 学生签到
```
POST /api/attendance/student/{studentId}/signin

Body:
{
  "courseScheduleId": 1,
  "sessionDate": "2025-11-12"
}
```

## 测试账号

### Student 1
- **Email**: `student1@student.nottingham.edu.my`
- **Password**: `password123`
- **ID**: 1
- **选课**:
  - COMP2001 - Data Structures (Monday 08:00-09:00)
  - COMP3040 - Mobile Device Programming (Monday 09:00-10:00, Wednesday 09:00-11:00)
  - COMP3070 - Symbolic AI (Tuesday 09:00-11:00, Friday 14:00-16:00)

### Student 2
- **Email**: `student2@student.nottingham.edu.my`
- **Password**: `password123`
- **ID**: 2
- **选课**:
  - COMP3041 - Professional Ethics (Monday 14:00-16:00, Thursday 11:00-13:00)
  - COMP4082 - Autonomous Robotics (Tuesday 14:00-16:00, Wednesday 15:00-17:00, Friday 16:00-18:00)

## 技术实现

### 实时轮询机制
```kotlin
// 每30秒自动刷新课程状态
private fun startPolling() {
    handler.postDelayed({
        if (isPolling && _binding != null) {
            loadCourses()  // 从后端API获取最新数据
            handler.postDelayed(this, 30_000)
        }
    }, 30_000)
}
```

### 签到流程
```kotlin
private fun handleSignIn(course: Course) {
    lifecycleScope.launch {
        val result = repository.signIn(studentId, courseScheduleId, today)

        result.onSuccess {
            // 签到成功，立即刷新显示
            loadCourses()
        }
    }
}
```

### 状态图标映射
```kotlin
when (course.signInStatus) {
    SignInStatus.LOCKED -> {
        // 灰色锁，不可点击
        binding.viewStatusLine.setBackgroundColor(Color.GRAY)
        binding.ivAttendanceIcon.setImageResource(R.drawable.ic_sign_locked)
        binding.ivAttendanceIcon.isClickable = false
    }
    SignInStatus.UNLOCKED -> {
        if (course.hasStudentSigned) {
            // 已签到：绿色勾
            binding.ivAttendanceIcon.setImageResource(R.drawable.ic_attendance_check)
        } else {
            // 可签到：蓝色铅笔
            binding.ivAttendanceIcon.setImageResource(R.drawable.ic_sign_pencil)
            binding.ivAttendanceIcon.isClickable = true
        }
    }
    SignInStatus.CLOSED -> {
        if (course.hasStudentSigned) {
            // 已签到：绿色勾
        } else {
            // 缺席：红色叉
            binding.ivAttendanceIcon.setImageResource(R.drawable.ic_attendance_cross)
        }
    }
}
```

## 注意事项

1. **网络连接**: 需要确保设备能连接到后端服务器 (localhost:8080)
2. **轮询延迟**: 最多30秒才能看到教师的 unlock 操作
3. **自动关闭**: 教师 unlock 后20分钟会自动关闭签到
4. **补签**: 签到关闭后无法补签
5. **时区**: 使用系统默认时区

## 常见问题

### Q: 为什么我看不到某些课程？
A: 只显示你选课的课程，检查是否已经 enroll 该课程。

### Q: 为什么点击铅笔没反应？
A: 检查网络连接，或查看 Toast 提示的错误信息。

### Q: 出勤率为什么是 0/0？
A: 因为教师还没有开过任何签到，总课时为0。

### Q: 如何测试实时更新？
A:
1. 用教师账号 unlock 一门课
2. 等待最多30秒
3. 学生端应该自动显示蓝色铅笔

### Q: 签到后多久能看到绿色勾？
A: 立即显示，签到成功后会自动刷新界面。
