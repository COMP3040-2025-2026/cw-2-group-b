# My Nottingham - Campus Life Hub

> A Comprehensive Campus Services Platform for University of Nottingham Malaysia

**Course:** COMP3040 Mobile Device Programming Coursework 2 - Application Design, Implementation, Report, and Presentation
**Team:** Group B
**Members:** Junru Wang, Hao Pan, Wenjun Xia

---

## Project Overview

My Nottingham is a comprehensive mobile application designed to integrate all essential campus services into one unified platform for students and staff at the University of Nottingham Malaysia. The app replaces fragmented systems with a seamless, user-friendly experience powered by Firebase.

### Key Features

| Feature | Description | Status |
|---------|-------------|--------|
| **User Authentication** | Secure Firebase Authentication with role-based access | Completed |
| **Campus Shuttle Timeline** | Real-time shuttle schedules for 8 routes (A, B, C1, C2, D, E1, E2, G) | Completed |
| **INSTATT Attendance** | Comprehensive attendance tracking for teachers and students | Completed |
| **Sports Facility Booking** | Reserve sports facilities with real-time availability | Completed |
| **Campus Errand Service** | Peer-to-peer delivery and errand marketplace | Completed |
| **AI Assistant (Notti)** | Intelligent chatbot powered by Firebase AI (Gemini) | Completed |
| **Messaging System** | Real-time chat with online status, typing indicators, group chat | Completed |
| **Campus Forum** | Community discussions with posts, comments, and likes | Completed |
| **User Profile** | Dynamic profile management for students and teachers | Completed |

---

## Architecture

### Design Pattern: MVVM + Repository

The application follows a clean architecture with strict separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Fragments  │  │  ViewModels │  │     Adapters        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Domain Layer                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Use Cases / Business Logic                 ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Repositories│  │   Models    │  │   Data Sources      │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│         │                                    │               │
│         ▼                                    ▼               │
│  ┌─────────────┐                    ┌─────────────────────┐ │
│  │    Room     │                    │  Firebase Realtime  │ │
│  │  Database   │                    │      Database       │ │
│  └─────────────┘                    └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
MyNottingham/
├── app/src/main/
│   ├── java/com/nottingham/mynottingham/
│   │   ├── data/                    # Data Layer
│   │   │   ├── local/              # Room database, DAOs, Entities, TokenManager
│   │   │   ├── firebase/           # Firebase-specific managers
│   │   │   ├── repository/         # Repository pattern implementations
│   │   │   │   ├── FirebaseUserRepository.kt
│   │   │   │   ├── FirebaseMessageRepository.kt
│   │   │   │   ├── FirebaseForumRepository.kt
│   │   │   │   ├── FirebaseErrandRepository.kt
│   │   │   │   ├── FirebaseBookingRepository.kt
│   │   │   │   ├── FirebaseCourseRepository.kt
│   │   │   │   └── InstattRepository.kt
│   │   │   └── model/              # Domain models (16 files)
│   │   ├── domain/                 # Business logic layer
│   │   ├── ui/                     # Presentation layer (12 feature modules)
│   │   │   ├── auth/               # Authentication (Login)
│   │   │   ├── home/               # Home dashboard
│   │   │   ├── shuttle/            # Shuttle bus timeline
│   │   │   ├── instatt/            # Attendance system
│   │   │   ├── booking/            # Sports facility booking
│   │   │   ├── errand/             # Campus errand marketplace
│   │   │   ├── message/            # Messaging system
│   │   │   ├── forum/              # Community forum
│   │   │   ├── notti/              # AI Assistant
│   │   │   ├── profile/            # User profile
│   │   │   ├── base/               # Base ViewModel
│   │   │   └── common/             # Shared UI components
│   │   ├── service/                # Firebase Messaging Service
│   │   └── util/                   # Utilities and Constants
│   └── res/                        # Resources
│       ├── layout/                 # 57+ XML layouts
│       ├── drawable/               # 140+ vector graphics
│       ├── navigation/             # Navigation graph
│       └── values/                 # Strings, colors, themes
├── app/src/test/                   # Unit tests
└── app/src/androidTest/            # Instrumented tests
```

---

## Technology Stack

### Android Frontend

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Kotlin | 2.0.21 |
| **Min SDK** | Android 11.0 | API 30 |
| **Target SDK** | Android 14 | API 34 |
| **Build Tool** | Gradle | 8.13.1 |

### Core Libraries

| Library | Purpose | Version |
|---------|---------|---------|
| **AndroidX Core KTX** | Kotlin extensions | 1.13.1 |
| **Material Design 3** | UI components | 1.13.0 |
| **Navigation Component** | Fragment navigation | 2.8.5 |
| **Room** | Local database | 2.6.1 |
| **DataStore** | Preferences storage | 1.1.1 |
| **Lifecycle** | ViewModel, LiveData | 2.8.7 |
| **Coroutines** | Async programming | 1.9.0 |
| **Glide** | Image loading | 4.16.0 |

### Firebase Services

| Service | Purpose |
|---------|---------|
| **Firebase Authentication** | User authentication |
| **Firebase Realtime Database** | Real-time data sync |
| **Firebase Cloud Messaging** | Push notifications |
| **Firebase AI (Gemini)** | AI Assistant (Notti) |
| **Firebase Analytics** | Usage analytics |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Gradle 8.1.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd "Android Studio Project"
   ```

2. **Open in Android Studio**
   - Open the project folder in Android Studio
   - Wait for Gradle sync to complete

3. **Run the application**
   - Create/Start Pixel 2 emulator (Android 11.0, 1080x1920, 420dpi)
   - Click Run (Shift + F10)

### Test Accounts

All accounts use the password: `password123`

| Role | Username | Email |
|------|----------|-------|
| Student | student1 | student1@nottingham.edu.my |
| Student | student2 | student2@nottingham.edu.my |
| Student | student3 | student3@nottingham.edu.my |
| Teacher | teacher1 | teacher1@nottingham.edu.my |
| Teacher | teacher2 | teacher2@nottingham.edu.my |

---

## Firebase Configuration

### Database URL
```
https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app
```

### Database Structure

```
Firebase Realtime Database
├── users/{uid}/                    # User profiles
│   ├── username, fullName, email, role
│   ├── profileImageUrl, studentId/employeeId
│   └── faculty/department, deliveryMode
├── user_conversations/{userId}/    # Conversation list per user
├── conversations/{conversationId}/ # Conversation data & messages
├── forum_posts/{postId}/           # Forum posts
├── forum_comments/{postId}/        # Post comments
├── enrollments/{courseId}/         # Course enrollments
├── sessions/{scheduleId}_{date}/   # Attendance sessions
├── errands/{errandId}/             # Errand listings
└── bookings/{bookingId}/           # Sports facility bookings
```

---

## Features Detail

### 1. User Authentication
- Firebase Authentication with email/password
- Role-based access (Student, Teacher, Admin)
- Secure token management with DataStore
- Online/offline presence tracking
- FCM token registration for push notifications

### 2. Campus Shuttle Timeline
- 8 shuttle routes with complete schedules
- Day type selector (Weekday/Friday/Weekend)
- Color-coded route badges
- Special notes and vehicle type indicators

### 3. INSTATT Attendance System
- **Teacher View**: Unlock/lock sessions, view attendance list, manual marking
- **Student View**: View enrolled courses, sign in when session unlocked
- Real-time attendance status synchronization
- Statistics and progress tracking

### 4. Messaging System
- One-on-one and group chat support
- Real-time message synchronization
- Online status indicators
- Typing indicators with 3-second timeout
- Pinned conversations with visual indicator
- Message search with alphabetical index (A-Z)
- 7-day message retention policy
- Privacy protection (local data cleared on logout)

### 5. Campus Forum
- Create posts with categories
- Comments and nested discussions
- Like system for posts and comments
- View count tracking (unique per user)
- Author avatar fetched from user profiles

### 6. Sports Facility Booking
- Browse available facilities
- Date and time slot selection
- Real-time availability checking
- Booking confirmation and history

### 7. Campus Errand Service
- Post errands with item details
- Browse available errands
- Shopping cart functionality
- Checkout and order tracking

### 8. AI Assistant (Notti)
- Powered by Firebase AI (Gemini)
- Campus-related query assistance
- Natural language conversation

### 9. User Profile
- Dynamic fields based on user type
- Student: Faculty, Major, Year of Study
- Teacher: Title, Department, Office Room
- Avatar selection and update
- Notification settings

---

## Documentation

### Project Report

The CW2 report is available in LaTeX format:

```
docs/report/
├── main.tex                        # LaTeX source file
├── Architecture Diagram.png
├── Class Diagram.png
├── Firebase Database Structure.png
├── Login Sequence.png
├── Messaging Sequence.png
└── MVVM Pattern.png
```

### UML Diagrams

PlantUML source files and rendered diagrams:

```
docs/uml/
├── *.puml                          # PlantUML source files
├── *.svg                           # Vector graphics (scalable)
└── *.png                           # Raster graphics
```

| Diagram | Description |
|---------|-------------|
| Architecture Diagram | Three-layer system architecture |
| MVVM Pattern | MVVM design pattern implementation |
| Class Diagram | Core class relationships |
| Firebase Database Structure | Database schema design |
| Login Sequence | User authentication flow |
| Messaging Sequence | Real-time messaging flow |

---

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Files

**Unit Tests** (`app/src/test/`):
- `ShuttleViewModelTest.kt` - ViewModel initialization, route loading, day type selection
- `ShuttleModelTest.kt` - Route model data integrity
- `UserModelTest.kt` - User model validation
- `ConstantsTest.kt` - Application constants verification

**Instrumented Tests** (`app/src/androidTest/`):
- `NavigationTest.kt` - Navigation host setup, fragment navigation
- `MainActivityTest.kt` - Main activity lifecycle
- `LoginFragmentTest.kt` - Login UI components

---

## Build Commands

### Debug Build
```bash
./gradlew.bat assembleDebug --console=plain
```

### Clean Build
```bash
./gradlew.bat clean assembleDebug --console=plain
```

### APK Location
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Code Statistics

| Metric | Count |
|--------|-------|
| Kotlin Source Files | 145 |
| ViewModels | 17 |
| Fragments | 39 |
| Adapters | 23 |
| Room Entities | 8 |
| DAOs | 7 |
| Firebase Repositories | 9 |
| Layout Files | 89 |
| Drawable Resources | 101 |
| Unit Tests | 5 |
| Instrumented Tests | 4 |

---

## Design Guidelines

### Color Scheme

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | #1976D2 | Main actions, headers |
| Accent | #FF5722 | Highlights, FAB |
| Background | #FAFAFA | App background |
| Surface | #FFFFFF | Cards, dialogs |

### Typography

| Style | Size | Weight |
|-------|------|--------|
| Headline | 24sp | Bold |
| Body | 16sp | Regular |
| Caption | 14sp | Regular |

---

## Git Workflow

### Commit Message Format

```
<type>: <description>

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation
- refactor: Code refactoring
- test: Adding tests
- chore: Build/tools configuration
```

### Example
```
feat: implement real-time message synchronization
```

---

## Known Limitations

- WebSocket integration prepared but using Firebase Realtime listeners for real-time sync
- Image attachments UI prepared, file upload through Firebase Storage
- Push notifications require Firebase Cloud Messaging configuration

---

## Team Members

| Name | Role |
|------|------|
| Junru Wang | Developer |
| Hao Pan | Developer |
| Wenjun Xia | Developer |

---

## License

This project is developed for educational purposes as part of COMP3040 Mobile Device Programming coursework at the University of Nottingham Malaysia.

---

Last Updated: December 2025
