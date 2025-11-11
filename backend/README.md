# MyNottingham Backend API

后端 REST API 服务，支持 MyNottingham 校园移动应用。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **Spring Security**
- **MySQL 8.0** (生产环境)
- **H2 Database** (开发环境)
- **Maven**

## 功能特性

- ✅ 用户管理（学生/教师/管理员）
- ✅ 课程管理和选课系统
- ✅ 考勤系统（Instatt）
- ✅ 体育设施预订
- ✅ 校园跑腿任务
- ✅ 论坛系统
- ✅ 消息系统
- ✅ RESTful API
- ✅ 密码加密（BCrypt）
- ✅ CORS 支持

## 快速开始

### 前置要求

- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.0（如果使用生产模式）

### 安装和运行

#### 方式一：使用 H2 内存数据库（开发模式 - 推荐快速测试）

```bash
# 进入 backend 目录
cd backend

# 使用 Maven 运行（开发模式）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

H2 控制台访问: http://localhost:8080/api/h2-console
- JDBC URL: `jdbc:h2:mem:mynottingham`
- Username: `sa`
- Password: (留空)

#### 方式二：使用 MySQL 数据库（生产模式）

1. **安装 MySQL 并创建数据库**

```sql
CREATE DATABASE mynottingham CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **配置数据库连接**

编辑 `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mynottingham?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=你的MySQL用户名
spring.datasource.password=你的MySQL密码
```

3. **运行应用**

```bash
cd backend
mvn spring-boot:run
```

#### 方式三：使用 JAR 文件运行

```bash
# 构建 JAR
mvn clean package

# 运行（开发模式）
java -jar -Dspring.profiles.active=dev target/mynottingham-backend-1.0.0.jar

# 或运行（生产模式）
java -jar target/mynottingham-backend-1.0.0.jar
```

### 验证安装

应用启动后，访问：
- API Base URL: http://localhost:8080/api
- 健康检查: http://localhost:8080/api/users

## 测试数据

应用启动时会自动加载测试数据（来自 `data.sql`）：

### 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| student1 | password123 | STUDENT | Alice Wong - CS Year 3 |
| student2 | password123 | STUDENT | Bob Chen - SE Year 2 |
| student3 | password123 | STUDENT | Charlie Lee - EE Year 4 |
| teacher1 | password123 | TEACHER | Dr. Sarah Johnson |
| teacher2 | password123 | TEACHER | Prof. John Smith |
| admin | password123 | ADMIN | 系统管理员 |

### 测试数据包含

- 3 个学生账号
- 2 个教师账号
- 1 个管理员账号
- 3 门课程（COMP3040, COMP2040, SOFT3010）
- 课程时间表
- 选课记录
- 考勤记录
- 设施预订
- 跑腿任务
- 论坛帖子
- 消息记录

## API 文档

### 用户管理 API

#### 获取所有用户
```http
GET /api/users
```

**响应示例:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "username": "student1",
      "email": "student1@nottingham.edu.my",
      "fullName": "Alice Wong",
      "role": "STUDENT",
      "status": "ACTIVE"
    }
  ]
}
```

#### 获取单个用户
```http
GET /api/users/{id}
```

#### 获取所有学生
```http
GET /api/users/students
```

**响应示例:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "username": "student1",
      "email": "student1@nottingham.edu.my",
      "fullName": "Alice Wong",
      "studentId": "S001",
      "faculty": "Faculty of Computer Science",
      "major": "Computer Science",
      "yearOfStudy": 3,
      "gpa": 3.75
    }
  ]
}
```

#### 获取所有教师
```http
GET /api/users/teachers
```

#### 更新用户信息
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "email": "newemail@nottingham.edu.my",
  "fullName": "Updated Name",
  "phone": "+60123456789",
  "status": "ACTIVE"
}
```

#### 删除用户
```http
DELETE /api/users/{id}
```

### 数据模型

#### User（用户）
- id: Long
- username: String
- email: String
- fullName: String
- phone: String
- role: STUDENT | TEACHER | ADMIN
- status: ACTIVE | INACTIVE | SUSPENDED

#### Student（学生）继承自 User
- studentId: String
- faculty: String
- major: String
- yearOfStudy: Integer
- matricNumber: String
- gpa: Double

#### Teacher（教师）继承自 User
- employeeId: String
- department: String
- title: String
- officeRoom: String
- officeHours: String

#### Course（课程）
- courseCode: String
- courseName: String
- credits: Integer
- faculty: String
- semester: String
- teacher: Teacher
- capacity: Integer
- enrolled: Integer

#### Attendance（考勤）
- student: Student
- course: Course
- attendanceDate: LocalDate
- status: PRESENT | ABSENT | LATE | EXCUSED
- checkInTime: LocalDateTime

#### Booking（预订）
- user: User
- facilityName: String
- facilityType: String
- startTime: LocalDateTime
- endTime: LocalDateTime
- status: PENDING | CONFIRMED | CANCELLED | COMPLETED

#### Errand（跑腿任务）
- requester: User
- provider: User
- title: String
- type: SHOPPING | PICKUP | FOOD_DELIVERY | OTHER
- location: String
- reward: Double
- status: PENDING | IN_PROGRESS | COMPLETED | CANCELLED

## 配置说明

### 切换数据库配置

**开发模式（H2）：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**生产模式（MySQL）：**
```bash
mvn spring-boot:run
```

### 修改端口

编辑 `application.properties`:
```properties
server.port=8080
```

### 禁用示例数据

编辑 `application.properties`:
```properties
spring.sql.init.mode=never
```

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/nottingham/mynottingham/backend/
│   │   │   ├── config/           # 配置类
│   │   │   ├── controller/       # REST Controllers
│   │   │   ├── dto/              # 数据传输对象
│   │   │   │   ├── request/      # 请求 DTO
│   │   │   │   └── response/     # 响应 DTO
│   │   │   ├── entity/           # JPA 实体类
│   │   │   ├── repository/       # Spring Data Repositories
│   │   │   ├── service/          # 业务逻辑层
│   │   │   └── MyNottinghamBackendApplication.java
│   │   └── resources/
│   │       ├── application.properties      # 生产配置
│   │       ├── application-dev.properties  # 开发配置
│   │       └── data.sql                    # 测试数据
│   └── test/                      # 测试文件
├── pom.xml                        # Maven 配置
└── README.md                      # 本文档
```

## 部署到服务器

### 1. 构建 JAR 文件

```bash
mvn clean package
```

### 2. 上传到服务器

```bash
scp target/mynottingham-backend-1.0.0.jar user@server:/path/to/app/
```

### 3. 在服务器上运行

```bash
# 后台运行
nohup java -jar mynottingham-backend-1.0.0.jar > app.log 2>&1 &

# 或使用 systemd service
sudo systemctl start mynottingham-backend
```

### 4. 配置 Nginx 反向代理（可选）

```nginx
server {
    listen 80;
    server_name api.mynottingham.com;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 常见问题

### 1. 端口已被占用

修改 `application.properties` 中的 `server.port`

### 2. MySQL 连接失败

检查：
- MySQL 服务是否运行
- 数据库用户名和密码是否正确
- 防火墙设置

### 3. 数据初始化失败

检查 `data.sql` 中的 SQL 语法是否与您的数据库版本兼容

## 开发指南

### 添加新的 API 端点

1. 在 `entity/` 中创建实体类
2. 在 `repository/` 中创建 Repository 接口
3. 在 `service/` 中实现业务逻辑
4. 在 `controller/` 中创建 REST 端点

### 运行测试

```bash
mvn test
```

## 许可证

MIT License

## 联系方式

- 项目主页: https://github.com/COMP3040-2025-2026/cw-2-group-b
- 问题反馈: 创建 GitHub Issue

## 更新日志

### v1.0.0 (2024-12-18)
- 初始版本发布
- 用户管理系统
- 课程和考勤功能
- 预订和跑腿功能
- 论坛和消息功能
