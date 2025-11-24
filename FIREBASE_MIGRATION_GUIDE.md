# 🔄 Firebase Migration Guide - 后端"断奶"路线图

## 📊 当前状态

- ✅ **阶段 1 完成**: 数据已迁移到 Firebase Realtime Database
- ✅ **Firebase Repositories 已创建**: User, Course, Booking, Errand
- 🚧 **阶段 2 进行中**: Android 端仍依赖 Spring Boot API
- ⏳ **阶段 3 目标**: 完全移除后端依赖

---

## ⚠️ 重要警告

**现在绝对不能删除后端代码或 MySQL 数据库！**

虽然数据已经在 Firebase 上，但 Android App 的大部分功能仍然通过 `ApiService` (Retrofit) 调用后端接口。如果现在关闭后端，App 将崩溃。

---

## 🗺️ 完整迁移路线图

### 阶段 1: 数据上云 ✅ (已完成)

通过 `FirebaseDataMigrationService` 已成功将 MySQL 数据迁移到 Firebase:

```
✅ users/ - 用户信息
✅ courses/ - 课程信息
✅ schedules/ - 排课信息
✅ enrollments/ - 选课关系
✅ bookings/ - 场地预订
✅ errands/ - 跑腿任务
✅ forum_posts/ - 论坛帖子
✅ forum_comments/ - 论坛评论
✅ sessions/ - 签到会话
```

### 阶段 2: Android 端"断奶"改造 🚧 (当前阶段)

#### 已创建的 Firebase Repositories:

| Repository | 文件 | 状态 |
|-----------|------|------|
| **FirebaseUserRepository** | `data/repository/FirebaseUserRepository.kt` | ✅ 已创建 |
| **FirebaseCourseRepository** | `data/repository/FirebaseCourseRepository.kt` | ✅ 已创建 |
| **FirebaseBookingRepository** | `data/repository/FirebaseBookingRepository.kt` | ✅ 已创建 |
| **FirebaseErrandRepository** | `data/repository/FirebaseErrandRepository.kt` | ✅ 已创建 |

#### 需要改造的模块（按优先级排序）:

##### 1. **User Profile Module** (最简单，推荐先改)

**现状:**
- `ProfileViewModel` 调用 `ApiService.getUserProfile()`
- 通过 Retrofit 请求后端 `/api/user/profile`

**改造步骤:**

**Step 1:** 修改 `ProfileViewModel.kt`

```kotlin
// 旧代码 (使用 Retrofit)
viewModelScope.launch {
    val response = apiService.getUserProfile(userId)
    // ...
}

// 新代码 (使用 Firebase)
private val firebaseUserRepo = FirebaseUserRepository()

viewModelScope.launch {
    firebaseUserRepo.getUserProfile(userId)
        .collect { user ->
            _userProfile.value = user
        }
}
```

**Step 2:** 更新 `ProfileFragment.kt`

```kotlin
// ViewModel 中的 Flow 会自动更新 UI
viewModel.userProfile.observe(viewLifecycleOwner) { user ->
    binding.tvUserName.text = user.name
    binding.tvEmail.text = user.email
    // ...
}
```

**Step 3:** 测试

1. 关闭后端服务器
2. 打开 App → 进入 Profile 页面
3. 应该能正常显示用户信息（从 Firebase 读取）

**验收标准:** ✅ 关闭后端后，Profile 页面仍能正常显示

---

##### 2. **Course/Schedule Module** (部分已完成)

**现状:**
- `InstattRepository` 已经部分使用 Firebase (签到功能)
- 但课程列表获取仍依赖后端 API

**改造步骤:**

**Step 1:** 修改 `InstattRepository.kt`

```kotlin
// 旧代码
suspend fun getStudentCourses(studentId: Long, date: String): Result<List<Course>> {
    val response = apiService.getStudentCourses(studentId, date)
    // ...
}

// 新代码
private val firebaseCourseRepo = FirebaseCourseRepository()

suspend fun getStudentCourses(studentId: String, date: String): Result<List<Course>> {
    return firebaseCourseRepo.getStudentCourses(studentId, date)
}
```

**Step 2:** 更新 ViewModel 调用

```kotlin
// StudentInstattViewModel.kt
viewModelScope.launch {
    val userId = tokenManager.getUserId().first() ?: return@launch
    val result = repository.getStudentCourses(userId, today)
    // ...
}
```

**验收标准:** ✅ 关闭后端后，课程列表仍能正常显示

---

##### 3. **Authentication Module** (最复杂，最后改)

**现状:**
- `LoginViewModel` 调用后端 `/api/auth/login` 验证密码
- 使用 BCrypt 哈希，App 端很难直接验证

**改造方案 A: 使用 Firebase Authentication (推荐)**

**Step 1:** 添加 Firebase Auth 依赖

```gradle
// app/build.gradle.kts
implementation("com.google.firebase:firebase-auth:22.3.0")
```

**Step 2:** 修改登录逻辑

```kotlin
// LoginViewModel.kt
private val firebaseAuth = FirebaseAuth.getInstance()

fun login(username: String, password: String) {
    viewModelScope.launch {
        try {
            // 1. 从 Firebase 查找用户
            val userId = firebaseUserRepo.findUserIdByUsername(username)
            if (userId == null) {
                _loginResult.value = Result.failure(Exception("User not found"))
                return@launch
            }

            // 2. 使用 Firebase Auth 登录 (需要先迁移用户到 Firebase Auth)
            val email = "${username}@nottingham.edu.my" // 构造邮箱
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // 登录成功
                    tokenManager.saveUserId(userId)
                    _loginResult.value = Result.success(Unit)
                }
                .addOnFailureListener { e ->
                    _loginResult.value = Result.failure(e)
                }
        } catch (e: Exception) {
            _loginResult.value = Result.failure(e)
        }
    }
}
```

**改造方案 B: 临时方案（不安全，仅用于测试）**

将所有用户密码重置为明文 "password123"，直接在 Firebase 中存储：

```
users/student1/password: "password123"
```

然后在 App 端直接比对字符串（**非常不安全，仅用于开发测试**）。

**验收标准:** ✅ 关闭后端后，仍能登录并保持会话

---

##### 4. **Booking Module**

**改造步骤:**

```kotlin
// BookingRepository.kt
private val firebaseBookingRepo = FirebaseBookingRepository()

suspend fun createBooking(facilityName: String, startTime: Long, endTime: Long): Result<String> {
    val booking = mapOf(
        "userId" to currentUserId,
        "userName" to currentUserName,
        "facilityName" to facilityName,
        "facilityType" to "Basketball Court",
        "startTime" to startTime,
        "endTime" to endTime,
        "fee" to 10.0
    )
    return firebaseBookingRepo.createBooking(booking)
}

fun getUserBookings(): Flow<List<Booking>> {
    return firebaseBookingRepo.getUserBookings(currentUserId)
        .map { list -> list.map { mapToBooking(it) } }
}
```

**验收标准:** ✅ 能创建预订、查看预订、取消预订（无需后端）

---

##### 5. **Errand Module**

**改造步骤:**

```kotlin
// ErrandRepository.kt
private val firebaseErrandRepo = FirebaseErrandRepository()

suspend fun createErrand(title: String, description: String, type: String, reward: Double): Result<String> {
    val errand = mapOf(
        "title" to title,
        "description" to description,
        "requesterId" to currentUserId,
        "requesterName" to currentUserName,
        "type" to type,
        "reward" to reward,
        "pickupLocation" to pickupLoc,
        "deliveryLocation" to deliveryLoc
    )
    return firebaseErrandRepo.createErrand(errand)
}

fun getAvailableErrands(): Flow<List<Errand>> {
    return firebaseErrandRepo.getAvailableErrands()
        .map { list -> list.map { mapToErrand(it) } }
}
```

**验收标准:** ✅ 能发布任务、接受任务、完成任务（无需后端）

---

##### 6. **Forum Module** (可选)

**说明:** 你已有 `ForumRepository.kt`，可以参考上述模式创建 `FirebaseForumRepository`。

---

### 阶段 3: 移除后端 🏁 (最终目标)

**前提条件 (全部满足后才能执行):**

- [ ] Profile 模块完全不依赖后端
- [ ] Course/Schedule 模块完全不依赖后端
- [ ] Authentication 模块完全不依赖后端
- [ ] Booking 模块完全不依赖后端
- [ ] Errand 模块完全不依赖后端
- [ ] Forum 模块完全不依赖后端 (如果有)
- [ ] 所有功能经过测试，关闭后端后仍正常运行

**移除步骤:**

**Step 1:** 删除 Retrofit 相关代码

```bash
# 删除以下文件：
rm app/src/main/java/com/nottingham/mynottingham/data/remote/api/ApiService.kt
rm app/src/main/java/com/nottingham/mynottingham/data/remote/RetrofitInstance.kt
rm -rf app/src/main/java/com/nottingham/mynottingham/data/remote/dto/
```

**Step 2:** 移除 Retrofit 依赖

```gradle
// app/build.gradle.kts
// 注释或删除以下行：
// implementation("com.squareup.retrofit2:retrofit:2.9.0")
// implementation("com.squareup.retrofit2:converter-gson:2.9.0")
// implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

**Step 3:** 删除后端项目

```bash
# 在确认 App 完全不依赖后端后，可以删除：
rm -rf backend/
```

**Step 4:** 停止 MySQL 数据库服务

---

## 🧪 测试清单

每完成一个模块的迁移后，使用以下清单测试：

### 测试步骤：

1. ✅ 启动 Android App
2. ✅ **关闭 Spring Boot 后端服务器** (`taskkill /F /IM java.exe`)
3. ✅ 测试该模块的所有功能
4. ✅ 检查 Logcat，确保没有网络错误或 Retrofit 异常
5. ✅ 在 Firebase Console 中验证数据是否正确写入

### 功能测试矩阵：

| 模块 | 功能 | 需要后端 | 迁移后状态 |
|------|------|---------|----------|
| **Profile** | 查看个人信息 | ❌ No | ✅ Firebase |
| **Profile** | 编辑个人信息 | ❌ No | ✅ Firebase |
| **Auth** | 登录 | ❌ No | 🚧 待迁移 |
| **Auth** | 注册 | ❌ No | 🚧 待迁移 |
| **INSTATT** | 查看课程列表 | ❌ No | 🚧 待迁移 |
| **INSTATT** | 签到/解锁 | ❌ No | ✅ Firebase |
| **INSTATT** | 学生名单 | ❌ No | ✅ Firebase (部分) |
| **Booking** | 查看预订 | ❌ No | 🚧 待迁移 |
| **Booking** | 创建预订 | ❌ No | 🚧 待迁移 |
| **Errand** | 查看任务 | ❌ No | 🚧 待迁移 |
| **Errand** | 发布任务 | ❌ No | 🚧 待迁移 |
| **Forum** | 查看帖子 | ❌ No | 🚧 待迁移 |
| **Forum** | 发布帖子 | ❌ No | 🚧 待迁移 |

---

## 📚 参考文档

### Firebase SDK 常用方法：

```kotlin
// 读取一次
val snapshot = ref.get().await()
val value = snapshot.getValue(String::class.java)

// 实时监听
ref.addValueEventListener(object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
        // 数据变化时触发
    }
    override fun onCancelled(error: DatabaseError) {}
})

// 写入数据
ref.setValue(data).await()

// 更新部分字段
ref.updateChildren(mapOf("field" to value)).await()

// 删除数据
ref.removeValue().await()

// 查询
ref.orderByChild("userId").equalTo("student1").get().await()
```

### Flow 转换：

```kotlin
// Firebase Callback → Kotlin Flow
fun getData(): Flow<List<Item>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val items = parseSnapshot(snapshot)
            trySend(items)
        }
        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
}
```

---

## 🎯 下一步行动

### 立即可以开始的任务：

1. **修改 Profile 模块** (最简单，1-2小时)
   - 文件: `ProfileViewModel.kt`, `ProfileFragment.kt`
   - 替换 API 调用为 `FirebaseUserRepository`
   - 测试并验证

2. **修改 Course 列表加载** (中等难度，2-3小时)
   - 文件: `InstattRepository.kt`, ViewModels
   - 替换 `getStudentCourses` / `getTeacherCourses`
   - 测试课程列表显示

3. **规划 Auth 迁移方案** (复杂，需要设计)
   - 决定使用 Firebase Auth 还是临时方案
   - 如果使用 Firebase Auth，需要先迁移所有用户账号

---

## ⚡ 快速开始示例

### 示例 1: 修改 ProfileFragment 使用 Firebase

```kotlin
// ProfileViewModel.kt
class ProfileViewModel : ViewModel() {
    private val firebaseUserRepo = FirebaseUserRepository()
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            firebaseUserRepo.getUserProfile(userId)
                .collect { user ->
                    _userProfile.value = user
                }
        }
    }
}

// ProfileFragment.kt
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    lifecycleScope.launch {
        val userId = tokenManager.getUserId().first() ?: return@launch
        viewModel.loadProfile(userId)
    }

    lifecycleScope.launch {
        viewModel.userProfile.collect { user ->
            user?.let {
                binding.tvName.text = it.name
                binding.tvEmail.text = it.email
                binding.tvStudentId.text = it.studentId
            }
        }
    }
}
```

---

## 🐛 常见问题

### Q1: Firebase 数据结构与 MySQL 不一致怎么办？

**A:** 在 Firebase Repository 中做数据转换：

```kotlin
private fun mapFirebaseToModel(snapshot: DataSnapshot): User {
    // Firebase 字段名可能不同
    val fbFullName = snapshot.child("fullName").getValue(String::class.java)
    val fbStudentId = snapshot.child("studentId").getValue(Long::class.java)

    // 转换为 App 内部模型
    return User(
        name = fbFullName ?: "",
        studentId = fbStudentId?.toString() ?: ""
    )
}
```

### Q2: 如何处理认证 Token？

**A:** 使用 Firebase Auth 后，Token 由 Firebase 自动管理：

```kotlin
val user = FirebaseAuth.getInstance().currentUser
val token = user?.getIdToken(false)?.await()?.token
```

### Q3: 迁移后性能会变差吗？

**A:** 不会！Firebase 的优势：
- ✅ 本地缓存（离线可用）
- ✅ 实时同步（无需轮询）
- ✅ 自动重连（网络恢复后自动同步）
- ✅ 全球 CDN（访问速度更快）

---

## 📞 需要帮助？

如果在迁移过程中遇到问题，请检查：

1. **Firebase Console** - 查看数据是否正确写入
2. **Logcat** - 查看错误日志
3. **Firebase Debug Mode** - 启用详细日志：

```kotlin
FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
```

---

**最后提醒：在完成所有模块迁移并充分测试之前，请保留后端代码和数据库！**

祝迁移顺利！🚀
