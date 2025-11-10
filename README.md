# My Nottingham - Campus Life Hub

> An Enhanced Campus Services Platform for University of Nottingham Malaysia

**Course:** COMP3040 Mobile Device Programming Coursework 2 – Application Design, Implementation, Report, and Presentation
**Team:** Group B
**Members:** Junru Wang, Hao Pan, Wenjun Xia

---

## 📱 Project Overview

My Nottingham is a comprehensive mobile application designed to integrate all essential campus services into one unified platform for students at the University of Nottingham Malaysia. The app replaces fragmented systems with a seamless, user-friendly experience.

### Key Features

1.  **Campus Shuttle Timeline** – View real-time shuttle schedules and routes
2.  **Sports Facility Booking** – Reserve sports facilities with ease
3.  **Campus Errand Service** – Peer-to-peer delivery marketplace
4.  **AI Assistant (Notti)** – Intelligent chatbot for campus queries
5.  **Messaging** – Direct communication with classmates
6.  **Campus Forum** – Community discussions and announcements
7.  **User Profile** – Manage personal information and settings

---

## 🏗️ Architecture
### Design Pattern

- **MVVM (Model–View–ViewModel):** Clean separation of concerns
- **Repository Pattern:** Single source of truth for data
- **Navigation Component:** Type-safe fragment navigation

### Project Structure
```
MyNottingham/
├── app/src/main/
│   ├── java/com/nottingham/mynottingham/
│   │   ├── data/               # Data layer
│   │   │   ├── local/         # Room database, DAOs, Entities
│   │   │   ├── remote/        # Retrofit API services, DTOs
│   │   │   ├── repository/    # Repository pattern implementation
│   │   │   └── model/         # Domain models
│   │   ├── domain/            # Business logic
│   │   │   ├── usecase/       # Use cases
│   │   │   └── validator/     # Data validators
│   │   ├── ui/                # Presentation layer
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
```

---

## 🛠️ Technology Stack
### Core

- **Language:** Kotlin
- **Min SDK:** Android 11.0 (API 30)
- **Target Device:** Pixel 2 (1080×1920, 420dpi)

### Libraries

- **AndroidX Core:** Core KTX, AppCompat, ConstraintLayout
- **Jetpack:** Navigation, Lifecycle, Room, DataStore
- **Networking:** Retrofit, OkHttp, Gson
- **UI:** Material Design 3, ViewBinding
- **Image Loading:** Glide
- **Concurrency:** Kotlin Coroutines
- **Testing:** JUnit, Espresso

---

## 🚀 Getting Started
### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- Gradle 8.1.0+

### Installation

1.  **Clone the repository**
    ```bash
    git clone <repository-url>
    cd "Android Studio Project"
    ```

2.  **Open in Android Studio**
    - File → Open → Select project directory
    - Wait for Gradle sync to complete

3.  **Run the application**
    - Create/Start Pixel 2 emulator (Android 11.0)
    - Click Run (Shift + F10)

---

## 📋 Features Implementation Status
### ✅ Completed

- [x] Project architecture setup (MVVM)
- [x] Database configuration (Room)
- [x] Network layer (Retrofit)
- [x] Navigation system
- [x] Home screen with service cards
- [x] Bottom navigation (Home, Message, Forum, Me)
- [x] **Shuttle Bus feature**
    - [x] Complete route data models
    - [x] 8 route schedules (A, B, C1, C2, D, E1, E2, G)
    - [x] Day type selector (Weekday/Friday/Weekend)
    - [x] Color-coded route badges
    - [x] Modern gradient UI design

### 🔄 In Progress

- [ ] Sports Booking system
- [ ] Campus Errand marketplace
- [ ] Notti AI integration
- [ ] Messaging system
- [ ] Forum functionality
- [ ] User authentication

---

## 🧪 Testing
### Running Tests

**Unit Tests:**
```bash
./gradlew test
```

**Instrumented Tests:**
```bash
./gradlew connectedAndroidTest
```

---

## 📁 Key Files

- **Application Entry:** MyNottinghamApplication.kt
- **Main Activity:** ui/MainActivity.kt
- **Database:** data/local/database/AppDatabase.kt
- **API Service:** data/remote/api/ApiService.kt
- **Navigation:** res/navigation/nav_graph.xml
- **Constants:** util/Constants.kt

### Shuttle Bus Module

- **Data Models:** data/model/Shuttle.kt
- **ViewModel:** ui/shuttle/ShuttleViewModel.kt
- **Fragment:** ui/shuttle/ShuttleFragment.kt
- **Adapter:** ui/shuttle/ShuttleRouteAdapter.kt
- **Layouts:**
    - res/layout/fragment_shuttle.xml
    - res/layout/item_shuttle_route.xml

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

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
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
feat: implement shuttle timeline feature
```

---

## 🐛 Known Issues

- None at the moment

---

## 📅 Development Roadmap
### Phase 1: Foundation Setup

- Project initialization and architecture configuration
- Database and network layer setup
- Navigation system integration

### Phase 2: Core Features Development

- Shuttle timeline implementation
- Authentication system
- Sports booking module

### Phase 3: Extended Features Development

- Campus errand marketplace
- Messaging system
- Forum

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
Last Updated: November 2025