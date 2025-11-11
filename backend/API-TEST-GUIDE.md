# MyNottingham Backend API 测试指南

## 快速测试 API

使用以下方法测试 API 端点：

### 使用浏览器测试 GET 请求

1. 启动后端服务
2. 在浏览器中访问以下 URL：

```
http://localhost:8080/api/users
http://localhost:8080/api/users/students
http://localhost:8080/api/users/teachers
http://localhost:8080/api/users/1
```

### 使用 curl 测试

#### 获取所有用户
```bash
curl http://localhost:8080/api/users
```

#### 获取所有学生
```bash
curl http://localhost:8080/api/users/students
```

#### 获取所有教师
```bash
curl http://localhost:8080/api/users/teachers
```

#### 获取单个学生详情
```bash
curl http://localhost:8080/api/users/students/1
```

#### 更新用户信息
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"newemail@nottingham.edu.my\",\"fullName\":\"Updated Name\",\"phone\":\"+60123456789\",\"status\":\"ACTIVE\"}"
```

### 使用 Postman 测试

1. **安装 Postman**: https://www.postman.com/downloads/

2. **导入以下请求**:

#### 获取所有用户
- Method: GET
- URL: `http://localhost:8080/api/users`

#### 获取所有学生
- Method: GET
- URL: `http://localhost:8080/api/users/students`

#### 更新用户
- Method: PUT
- URL: `http://localhost:8080/api/users/1`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "updated@nottingham.edu.my",
  "fullName": "Updated Full Name",
  "phone": "+60199999999",
  "status": "ACTIVE"
}
```

### 使用 PowerShell 测试（Windows）

#### 获取所有用户
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
```

#### 获取所有学生
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users/students" -Method GET
```

#### 更新用户
```powershell
$body = @{
    email = "updated@nottingham.edu.my"
    fullName = "Updated Name"
    phone = "+60123456789"
    status = "ACTIVE"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/users/1" -Method PUT -Body $body -ContentType "application/json"
```

## 测试数据参考

### 学生账号

| ID | 学生ID | 用户名 | 姓名 | 院系 | 专业 | 年级 |
|----|--------|--------|------|------|------|------|
| 1 | S001 | student1 | Alice Wong | Computer Science | Computer Science | 3 |
| 2 | S002 | student2 | Bob Chen | Computer Science | Software Engineering | 2 |
| 3 | S003 | student3 | Charlie Lee | Engineering | Electrical Engineering | 4 |

### 教师账号

| ID | 员工ID | 用户名 | 姓名 | 部门 | 职称 | 办公室 |
|----|--------|--------|------|------|------|--------|
| 4 | T001 | teacher1 | Dr. Sarah Johnson | Computer Science | Associate Professor | CS-301 |
| 5 | T002 | teacher2 | Prof. John Smith | Software Engineering | Professor | CS-401 |

### 课程信息

| 课程代码 | 课程名称 | 学分 | 教师 | 容量 | 已选 |
|----------|----------|------|------|------|------|
| COMP3040 | Mobile Application Development | 3 | Dr. Sarah Johnson | 50 | 2 |
| COMP2040 | Database Systems | 3 | Dr. Sarah Johnson | 60 | 1 |
| SOFT3010 | Software Architecture | 3 | Prof. John Smith | 40 | 1 |

## API 响应格式

所有 API 响应都遵循统一格式：

### 成功响应
```json
{
  "success": true,
  "message": "Success",
  "data": {
    // 实际数据
  }
}
```

### 错误响应
```json
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

## 常见测试场景

### 1. 查看所有学生的课程
```bash
# 先获取所有学生
curl http://localhost:8080/api/users/students

# 查看特定学生详情（包含选课信息）
curl http://localhost:8080/api/users/students/1
```

### 2. 更改用户角色状态
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d "{\"status\":\"INACTIVE\"}"
```

### 3. 查看教师的课程
```bash
curl http://localhost:8080/api/users/teachers/4
```

## 数据库直接查询（开发模式）

如果使用 H2 数据库（开发模式），可以通过 H2 Console 直接查询数据库：

1. 访问: http://localhost:8080/api/h2-console
2. 配置连接:
   - JDBC URL: `jdbc:h2:mem:mynottingham`
   - User Name: `sa`
   - Password: (留空)
3. 点击 "Connect"

### 常用 SQL 查询

```sql
-- 查看所有用户
SELECT * FROM users;

-- 查看所有学生
SELECT u.*, s.* FROM users u
JOIN students s ON u.id = s.user_id;

-- 查看所有课程
SELECT * FROM courses;

-- 查看选课情况
SELECT
  u.full_name as student_name,
  c.course_code,
  c.course_name,
  e.status
FROM enrollments e
JOIN students s ON e.student_id = s.user_id
JOIN users u ON s.user_id = u.id
JOIN courses c ON e.course_id = c.id;

-- 查看考勤记录
SELECT
  u.full_name as student_name,
  c.course_code,
  a.attendance_date,
  a.status
FROM attendances a
JOIN students s ON a.student_id = s.user_id
JOIN users u ON s.user_id = u.id
JOIN courses c ON a.course_id = c.id
ORDER BY a.attendance_date DESC;
```

## 调试技巧

### 1. 查看日志
后端控制台会显示所有 SQL 查询和请求日志

### 2. 修改日志级别
编辑 `application.properties`:
```properties
logging.level.com.nottingham.mynottingham.backend=DEBUG
```

### 3. 重新加载测试数据
重启应用即可重新加载 `data.sql` 中的测试数据

## Android 应用集成

在 Android 应用的 `ApiService.kt` 中配置：

```kotlin
object RetrofitInstance {
    // 开发环境 - 使用本地 IP（不要用 localhost）
    private const val BASE_URL = "http://192.168.1.100:8080/api/"

    // 生产环境
    // private const val BASE_URL = "https://api.mynottingham.com/api/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

**注意**: Android 模拟器连接本地后端时：
- 不要使用 `localhost` 或 `127.0.0.1`
- 使用电脑的局域网 IP 地址（如 `192.168.1.100`）
- 或使用 Android 模拟器的特殊地址 `10.0.2.2`（代表主机的 localhost）
