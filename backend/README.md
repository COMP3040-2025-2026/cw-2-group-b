# MyNottingham Backend API

Backend REST API service supporting the MyNottingham campus mobile application.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **Spring Security**
- **MySQL 8.0** (Production)
- **H2 Database** (Development - File-based)
- **Maven**

## Features

- ✅ User Management (Students/Teachers/Admins)
- ✅ Course Management and Enrollment System
- ✅ Attendance System (INSTATT)
- ✅ Sports Facility Booking
- ✅ Campus Errand Marketplace
- ✅ Forum System
- ✅ Messaging System with 7-day retention
- ✅ RESTful API
- ✅ Password Encryption (BCrypt)
- ✅ CORS Support
- ✅ JWT Authentication

## Quick Start

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- MySQL 8.0 (if using production mode)

### Installation and Running

#### Option 1: Using H2 File-based Database (Development Mode - Recommended)

```bash
# Navigate to backend directory
cd backend

# Run with Maven (development mode)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or use the start script
# Windows:
start-dev.bat

# Linux/Mac:
./start-dev.sh
```

H2 Console Access: http://localhost:8080/api/h2-console
- JDBC URL: `jdbc:h2:file:./data/mynottingham`
- Username: `sa`
- Password: (leave empty)

**Note:** Development mode uses file-based H2 database for persistent storage. Data is preserved between restarts.

#### Option 2: Using MySQL Database (Production Mode)

1. **Install MySQL and create database**

```sql
CREATE DATABASE mynottingham CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Configure database connection**

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mynottingham?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

3. **Run the application**

```bash
cd backend
mvn spring-boot:run
```

#### Option 3: Run using JAR file

```bash
# Build JAR
mvn clean package

# Run (development mode)
java -jar -Dspring.profiles.active=dev target/mynottingham-backend-1.0.0.jar

# Or run (production mode)
java -jar target/mynottingham-backend-1.0.0.jar
```

### Verify Installation

After the application starts, access:
- API Base URL: http://localhost:8080/api
- Health Check: http://localhost:8080/api/users

## Test Data

The application automatically loads test data on first startup (from `data.sql`):

### Test Accounts

All accounts use password: `password123`

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| student1 | password123 | STUDENT | Alice Wong - CS Year 3 |
| student2 | password123 | STUDENT | Bob Chen - SE Year 2 |
| student3 | password123 | STUDENT | Charlie Lee - EE Year 4 |
| teacher1 | password123 | TEACHER | Dr. Sarah Johnson |
| teacher2 | password123 | TEACHER | Prof. John Smith |
| admin | password123 | ADMIN | System Administrator |

### Test Data Includes

- 3 student accounts
- 2 teacher accounts
- 1 admin account
- 3 courses (COMP3040, COMP2040, SOFT3010)
- Course schedules
- Enrollment records
- Attendance records
- Facility bookings
- Errand tasks
- Forum posts
- Messages

## API Documentation

### User Management API

#### Get All Users
```http
GET /api/users
```

**Response Example:**
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

#### Get Single User
```http
GET /api/users/{id}
```

#### Get All Students
```http
GET /api/users/students
```

**Response Example:**
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

#### Get All Teachers
```http
GET /api/users/teachers
```

#### Update User
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

#### Delete User
```http
DELETE /api/users/{id}
```

### Authentication API

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "student1",
  "password": "password123"
}
```

#### Logout
```http
POST /api/auth/logout
Authorization: Bearer {token}
```

#### Get User Profile
```http
GET /api/user/profile
Authorization: Bearer {token}
```

### Messaging API

#### Get Conversations
```http
GET /api/message/conversations
Authorization: Bearer {token}
```

#### Send Message
```http
POST /api/message/send
Authorization: Bearer {token}
Content-Type: application/json

{
  "conversationId": "conv-123",
  "content": "Hello!"
}
```

#### Get Messages
```http
GET /api/message/{conversationId}/messages
Authorization: Bearer {token}
```

#### Mark as Read
```http
POST /api/message/{conversationId}/read
Authorization: Bearer {token}
```

#### Update Typing Status
```http
POST /api/message/typing
Authorization: Bearer {token}
Content-Type: application/json

{
  "conversationId": "conv-123",
  "isTyping": true
}
```

#### Search Messages
```http
GET /api/message/search?query=hello
Authorization: Bearer {token}
```

## Data Models

### User
- id: Long
- username: String
- email: String
- fullName: String
- phone: String
- role: STUDENT | TEACHER | ADMIN
- status: ACTIVE | INACTIVE | SUSPENDED

### Student (extends User)
- studentId: String
- faculty: String
- major: String
- yearOfStudy: Integer
- matricNumber: String
- gpa: Double

### Teacher (extends User)
- employeeId: String
- department: String
- title: String
- officeRoom: String
- officeHours: String

### Course
- courseCode: String
- courseName: String
- credits: Integer
- faculty: String
- semester: String
- teacher: Teacher
- capacity: Integer
- enrolled: Integer

### Attendance
- student: Student
- course: Course
- attendanceDate: LocalDate
- status: PRESENT | ABSENT | LATE | EXCUSED
- checkInTime: LocalDateTime

### Booking
- user: User
- facilityName: String
- facilityType: String
- startTime: LocalDateTime
- endTime: LocalDateTime
- status: PENDING | CONFIRMED | CANCELLED | COMPLETED

### Errand
- requester: User
- provider: User
- title: String
- type: SHOPPING | PICKUP | FOOD_DELIVERY | OTHER
- location: String
- reward: Double
- status: PENDING | IN_PROGRESS | COMPLETED | CANCELLED

### Message
- id: String
- conversationId: String
- senderId: String
- senderName: String
- content: String
- timestamp: Long
- isRead: Boolean
- messageType: TEXT | IMAGE | FILE

### Conversation
- id: String
- isGroup: Boolean
- groupName: String (optional)
- lastMessage: String
- lastMessageTime: Long
- unreadCount: Integer
- isPinned: Boolean

## Configuration

### Switch Database Configuration

**Development Mode (H2 File-based):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production Mode (MySQL):**
```bash
mvn spring-boot:run
```

### Change Port

Edit `application.properties`:
```properties
server.port=8080
```

### Disable Sample Data

Edit `application.properties`:
```properties
spring.sql.init.mode=never
```

### Message Retention Settings

Edit `application.properties`:
```properties
# Default is 7 days, configured in Android app
# Messages older than 7 days are automatically deleted on app startup
```

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/nottingham/mynottingham/backend/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST Controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   │   ├── request/      # Request DTOs
│   │   │   │   └── response/     # Response DTOs
│   │   │   ├── entity/           # JPA Entities
│   │   │   ├── repository/       # Spring Data Repositories
│   │   │   ├── service/          # Business Logic Layer
│   │   │   └── MyNottinghamBackendApplication.java
│   │   └── resources/
│   │       ├── application.properties          # Production config
│   │       ├── application-dev.properties      # Development config
│   │       └── data.sql                        # Test data
│   └── test/                      # Test files
├── data/                          # H2 database files (dev mode)
├── pom.xml                        # Maven configuration
├── start-dev.bat                  # Windows start script
├── start-dev.sh                   # Linux/Mac start script
└── README.md                      # This file
```

## Deployment to Server

### 1. Build JAR file

```bash
mvn clean package
```

### 2. Upload to server

```bash
scp target/mynottingham-backend-1.0.0.jar user@server:/path/to/app/
```

### 3. Run on server

```bash
# Run in background
nohup java -jar mynottingham-backend-1.0.0.jar > app.log 2>&1 &

# Or use systemd service
sudo systemctl start mynottingham-backend
```

### 4. Configure Nginx Reverse Proxy (Optional)

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

## Troubleshooting

### 1. Port Already in Use

Change `server.port` in `application.properties`

### 2. MySQL Connection Failed

Check:
- MySQL service is running
- Database username and password are correct
- Firewall settings

### 3. Data Initialization Failed

Check if SQL syntax in `data.sql` is compatible with your database version

### 4. H2 Console Not Accessible

Ensure you're running in development mode:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Cannot Access H2 Database

Verify JDBC URL matches configuration:
- File mode: `jdbc:h2:file:./data/mynottingham`
- Ensure the `data/` directory exists and has write permissions

## Development Guide

### Adding New API Endpoints

1. Create entity class in `entity/`
2. Create Repository interface in `repository/`
3. Implement business logic in `service/`
4. Create REST endpoints in `controller/`

### Running Tests

```bash
mvn test
```

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods small and focused

## Additional Notes

For API testing, use tools like Postman or curl. All API endpoints are documented above with request/response examples.

## License

MIT License

## Contact

- Project Repository: https://github.com/COMP3040-2025-2026/cw-2-group-b
- Issue Tracker: Create a GitHub Issue

## Changelog

### v1.1.0 (2025-11-13)
- Added messaging system with 7-day retention policy
- Implemented message search functionality
- Added typing indicators and online status
- Enhanced authentication with JWT tokens
- Switched to file-based H2 database for development

### v1.0.0 (2024-12-18)
- Initial release
- User management system
- Course and attendance features
- Booking and errand features
- Forum and messaging features
