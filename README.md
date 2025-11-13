# My Nottingham - Campus Life Hub

> An Enhanced Campus Services Platform for University of Nottingham Malaysia

**Course:** COMP3040 Mobile Device Programming Coursework 2 – Application Design, Implementation, Report, and Presentation
**Team:** Group B
**Members:** Junru Wang, Hao Pan, Wenjun Xia

---

## 📱 Project Overview

My Nottingham is a comprehensive mobile application designed to integrate all essential campus services into one unified platform for students at the University of Nottingham Malaysia. The app replaces fragmented systems with a seamless, user-friendly experience.

### Key Features

1.  **User Authentication** – Secure login system with JWT token management
2.  **Campus Shuttle Timeline** – View real-time shuttle schedules and routes
3.  **INSTATT (Attendance System)** – Comprehensive attendance tracking with teacher and student views
4.  **Sports Facility Booking** – Reserve sports facilities with ease
5.  **Campus Errand Service** – Peer-to-peer delivery marketplace
6.  **AI Assistant (Notti)** – Intelligent chatbot for campus queries
7.  **Messaging** – Direct communication with classmates
8.  **Campus Forum** – Community discussions and announcements
9.  **User Profile** – Manage personal information and settings

---

## 🏗️ Architecture
### Design Pattern

- **MVVM (Model–View–ViewModel):** Clean separation of concerns
- **Repository Pattern:** Single source of truth for data
- **Navigation Component:** Type-safe fragment navigation
- **RESTful API:** Backend integration with Spring Boot

### Project Structure
```
MyNottingham/
├── app/src/main/
│   ├── java/com/nottingham/mynottingham/
│   │   ├── data/               # Data layer
│   │   │   ├── local/         # Room database, DAOs, Entities, TokenManager
│   │   │   ├── remote/        # Retrofit API services, DTOs
│   │   │   ├── repository/    # Repository pattern implementation
│   │   │   └── model/         # Domain models
│   │   ├── domain/            # Business logic
│   │   │   ├── usecase/       # Use cases
│   │   │   └── validator/     # Data validators
│   │   ├── ui/                # Presentation layer
│   │   │   ├── auth/          # Authentication module
│   │   │   ├── home/          # Home module
│   │   │   ├── shuttle/       # Shuttle module
│   │   │   ├── booking/       # Booking module
│   │   │   ├── errand/        # Errand module
│   │   │   ├── notti/         # AI Assistant module
│   │   │   ├── message/       # Messaging module
│   │   │   ├── forum/         # Forum module
│   │   │   └── profile/       # Profile module
│   │   └── util/              # Utility classes and extensions
│   └── res/                   # Resources (layouts, strings, etc.)
├── backend/                   # Spring Boot Backend
│   ├── src/main/
│   │   ├── java/com/nottingham/mynottingham/backend/
│   │   │   ├── config/        # Security and Jackson configuration
│   │   │   ├── controller/    # REST Controllers
│   │   │   ├── dto/           # Data Transfer Objects
│   │   │   ├── entity/        # JPA Entities
│   │   │   ├── repository/    # Spring Data Repositories
│   │   │   ├── service/       # Business Logic Services
│   │   │   └── util/          # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql       # Sample data
│   ├── pom.xml               # Maven configuration
│   └── start-dev.bat/.sh     # Quick start scripts
```

---

## 🛠️ Technology Stack
### Android Frontend

- **Language:** Kotlin
- **Min SDK:** Android 11.0 (API 30)
- **Target Device:** Pixel 2 (1080×1920, 420dpi)

#### Libraries

- **AndroidX Core:** Core KTX, AppCompat, ConstraintLayout
- **Jetpack:** Navigation, Lifecycle, Room, DataStore
- **Networking:** Retrofit, OkHttp, Gson
- **UI:** Material Design 3, ViewBinding
- **Image Loading:** Glide
- **Concurrency:** Kotlin Coroutines
- **Testing:** JUnit, Espresso

### Spring Boot Backend

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.1
- **Database:** H2 (file-based for data persistence)
- **ORM:** Hibernate / JPA
- **Security:** Spring Security, BCrypt
- **Build Tool:** Maven 3.9+

#### Key Dependencies

- Spring Web
- Spring Data JPA
- Spring Security
- H2 Database
- Jackson (JSON processing)
- Lombok (code generation)

---

## 🚀 Getting Started
### Prerequisites

- **Android:**
  - Android Studio Hedgehog or newer
  - JDK 17
  - Android SDK 34
  - Gradle 8.1.0+

- **Backend:**
  - JDK 17
  - Maven 3.9+ (or use Android Studio's embedded Maven)

### Installation

#### 1. Clone the repository
```bash
git clone <repository-url>
cd "Android Studio Project"
```

#### 2. Start the Backend Server

**Option A: Using start script (Recommended)**
```bash
# Windows
cd backend
start-dev.bat

# Linux/Mac
cd backend
chmod +x start-dev.sh
./start-dev.sh
```

**Option B: Using Maven directly**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend will start on `http://localhost:8080/api`

#### 3. Run the Android App

- Open project in Android Studio
- Wait for Gradle sync to complete
- Create/Start Pixel 2 emulator (Android 11.0)
- Click Run (Shift + F10)

### Test Accounts

All accounts use the password: `password123`

- **Students:** `student1`, `student2`, `student3`
- **Teachers:** `teacher1`, `teacher2`
- **Admin:** `admin`

---

## 🔐 Authentication System

The app implements a secure authentication flow:

1. **Login Screen** – First screen on app launch
2. **Token Storage** – JWT tokens stored using DataStore
3. **API Integration** – Retrofit with authentication headers
4. **Session Management** – Automatic logout on token expiry

### API Endpoints

- `POST /api/auth/login` – User login
- `POST /api/auth/logout` – User logout
- `GET /api/user/profile` – Get user profile (requires auth)

---

## 📋 Features Implementation Status
### ✅ Completed

- [x] Project architecture setup (MVVM)
- [x] Database configuration (Room)
- [x] Network layer (Retrofit)
- [x] Navigation system
- [x] Home screen with service cards
- [x] Bottom navigation (Home, Message, Forum, Me)
- [x] **User Authentication**
    - [x] Login UI with Material Design 3
    - [x] Spring Boot backend with BCrypt
    - [x] JWT token management
    - [x] DataStore persistence
    - [x] API integration
- [x] **Shuttle Bus feature**
    - [x] Complete route data models
    - [x] 8 route schedules (A, B, C1, C2, D, E1, E2, G)
    - [x] Day type selector (Weekday/Friday/Weekend)
    - [x] Color-coded route badges
    - [x] Modern gradient UI design
- [x] **INSTATT (Attendance System)**
    - [x] Teacher view with course management
    - [x] Student view with attendance tracking
    - [x] Session unlock/lock functionality
    - [x] Student sign-in capability
    - [x] Manual attendance marking (Present/Absent/Late/Excused)
    - [x] Real-time status synchronization
    - [x] Attendance statistics and progress tracking
    - [x] File-based H2 database for data persistence

### 🔄 In Progress

- [ ] Sports Booking system (backend ready)
- [ ] Campus Errand marketplace (backend ready)
- [ ] Notti AI integration
- [ ] Messaging system (backend ready)
- [ ] Forum functionality (backend ready)
- [ ] Profile management

---

## 🧪 Testing
### Android Tests

**Unit Tests:**
```bash
./gradlew test
```

**Instrumented Tests:**
```bash
./gradlew connectedAndroidTest
```

### Backend Tests

```bash
cd backend
mvn test
```

### API Testing

See `backend/API-TEST-GUIDE.md` for detailed API testing instructions.

---

## 📁 Key Files
### Android

- **Application Entry:** MyNottinghamApplication.kt
- **Main Activity:** ui/MainActivity.kt
- **Database:** data/local/database/AppDatabase.kt
- **API Service:** data/remote/api/ApiService.kt
- **Navigation:** res/navigation/nav_graph.xml
- **Constants:** util/Constants.kt

### Authentication Module

- **Login Fragment:** ui/auth/LoginFragment.kt
- **Login ViewModel:** ui/auth/LoginViewModel.kt
- **Token Manager:** data/local/TokenManager.kt
- **Auth DTOs:** data/remote/dto/AuthDto.kt
- **Layout:** res/layout/fragment_login.xml

### Shuttle Bus Module

- **Data Models:** data/model/Shuttle.kt
- **ViewModel:** ui/shuttle/ShuttleViewModel.kt
- **Fragment:** ui/shuttle/ShuttleFragment.kt
- **Adapter:** ui/shuttle/ShuttleRouteAdapter.kt
- **Layouts:**
    - res/layout/fragment_shuttle.xml
    - res/layout/item_shuttle_route.xml

### Backend

- **Application Entry:** MyNottinghamBackendApplication.java
- **Auth Controller:** controller/AuthController.java
- **User Controller:** controller/UserController.java
- **Security Config:** config/SecurityConfig.java
- **Entities:** entity/*.java
- **Repositories:** repository/*.java
- **Services:** service/UserService.java

---

## 🎨 Design Guidelines
### Color Scheme

- **Primary:** #1976D2 (Blue)
- **Accent:** #FF5722 (Deep Orange)
- **Background:** #FAFAFA (Light Gray)

### Typography

- **Headlines:** Bold, 24sp
- **Body:** Regular, 16sp
- **Captions:** Regular, 14sp

---

## 📝 Development Guidelines
### Code Style

- Follow Kotlin coding conventions (Android)
- Follow Java coding conventions (Backend)
- Use meaningful variable and function names
- Add KDoc/JavaDoc comments for public APIs
- Keep functions small and focused

### Git Commit Messages

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Build/tools configuration

**Example:**
```
feat: implement login authentication system
```

---

## 🐛 Known Issues

- None at the moment

---

## 📚 Documentation

- `backend/README.md` - Backend documentation
- `backend/API-TEST-GUIDE.md` - API testing guide
- `backend/INSTALLATION.md` - Backend installation guide
- `backend/QUICK-START.md` - Quick start guide
- `CLAUDE.md` - Claude Code assistant instructions

---

## 📅 Development Roadmap
### Phase 1: Foundation Setup ✅

- Project initialization and architecture configuration
- Database and network layer setup
- Navigation system integration

### Phase 2: Core Features Development 🔄

- ✅ Shuttle timeline implementation
- ✅ Authentication system
- ⏳ Sports booking module
- ⏳ Campus errand marketplace

### Phase 3: Extended Features Development

- Messaging system
- Forum
- AI Assistant integration

### Phase 4: Polish & Testing

- UI/UX improvements
- Bug fixes and performance optimization
- Comprehensive testing

### Phase 5: Documentation & Presentation

- Final README and report completion
- Presentation preparation and submission

---

## 👥 Team Members

- Junru Wang
- Hao Pan
- Wenjun Xia

---

## 📞 API Base URLs

- **Local Development:** `http://10.0.2.2:8080/api` (Android Emulator)
- **Backend Server:** `http://localhost:8080/api`

---

Last Updated: November 2025
