# 🧪 Firebase Migration Testing Guide

## 🎯 目标

测试以下模块是否能在 **不依赖后端** 的情况下正常工作。

---

## ✅ 已迁移的模块

| 模块 | ViewModel | Repository | 状态 |
|------|-----------|------------|------|
| **Login** | LoginViewModel | FirebaseUserRepository | ✅ 已迁移 |
| **Errand** | ErrandViewModel | FirebaseErrandRepository | ✅ 已迁移 |
| **Booking** | BookingViewModel | FirebaseBookingRepository | ✅ 已迁移 |
| **Profile** | HomeViewModel | TokenManager (无需改造) | ✅ 已完成 |

---

## 📋 测试步骤

### 前提条件

1. ✅ 后端已运行过一次，执行了 `FirebaseDataMigrationService`
2. ✅ Firebase Realtime Database 中已有测试数据
3. ✅ 确认 `google-services.json` 和 `serviceAccountKey.json` 配置正确

---

### 测试 1: 登录功能 🔐

**步骤：**

1. **关闭后端服务器**
   ```bash
   # Windows
   taskkill /F /IM java.exe

   # 或者检查端口并杀死进程
   netstat -ano | findstr :8080
   taskkill /F /PID <进程ID>
   ```

2. **打开 Android App**
3. **尝试登录**
   - 用户名: `student1`
   - 密码: `password123`

**预期结果：**
- ✅ 登录成功
- ✅ 跳转到主页
- ✅ 显示用户名称 "Hi, Alice Wong"
- ✅ Logcat 显示: `✅ Login successful: student1 (STUDENT)`

**如果失败：**
- ❌ 检查 Logcat 中的错误信息
- ❌ 确认 Firebase 中有 `users/student1` 节点
- ❌ 确认密码是 `password123`

---

### 测试 2: 个人信息显示 👤

**步骤：**

1. 登录后，查看首页的用户信息
2. 检查显示的名称和院系

**预期结果：**
- ✅ 显示正确的用户名
- ✅ 显示正确的院系和年级
- ✅ 角色判断正确（Student/Teacher）

---

### 测试 3: 跑腿任务 (Errand) 📦

**步骤：**

1. **确保后端已关闭**
2. 进入 **Errand** 页面
3. 查看任务列表

**预期结果：**
- ✅ 显示 Firebase 中的测试任务（Pickup Food, Library Return 等）
- ✅ Logcat 显示: `📥 Loading tasks from Firebase...`
- ✅ Logcat 显示: `✅ Loaded 3 tasks from Firebase`

**创建新任务：**

1. 点击 "Add Task" 或 "+" 按钮
2. 填写任务信息
   - Title: "Buy Coffee"
   - Description: "Need 2 cups of coffee"
   - Location: "Cafeteria"
   - Price: "5"
3. 提交

**预期结果：**
- ✅ Logcat 显示: `📤 Creating new task: Buy Coffee`
- ✅ Logcat 显示: `✅ Task created successfully: <firebase-id>`
- ✅ 任务列表自动刷新，显示新任务
- ✅ 打开 Firebase Console，确认 `errands/` 节点有新数据

---

### 测试 4: 场地预订 (Booking) 🏀

**步骤：**

1. **确保后端已关闭**
2. 进入 **Booking** 页面
3. 选择设施（Basketball Court 或 Badminton Court）
4. 选择日期和时间
5. 提交预订

**预期结果：**
- ✅ Logcat 显示: `📤 Creating booking: Basketball Court 1 on 2025-11-25 at 14:00`
- ✅ Logcat 显示: `✅ Booking created successfully: <firebase-id>`
- ✅ 打开 Firebase Console，确认 `bookings/` 节点有新数据

**查看我的预订：**

1. 进入 "My Bookings" 或 "预订记录" 页面
2. 查看预订列表

**预期结果：**
- ✅ Logcat 显示: `📥 Loading bookings for user: student1`
- ✅ 显示之前创建的预订
- ✅ 能够取消预订

---

### 测试 5: 课程列表 (INSTATT) 📚

**步骤：**

1. **确保后端已关闭**
2. 进入 **INSTATT** 页面

**预期结果 (目前)：**
- ⚠️ 可能仍然显示 "Backend offline" 错误
- ⚠️ 因为 INSTATT 的课程列表加载尚未完全迁移

**TODO: 下一步需要迁移 `InstattRepository.getStudentCourses()` 使用 `FirebaseCourseRepository`**

---

## 🐛 常见问题排查

### 问题 1: 登录时显示 "User not found"

**原因：** Firebase 中没有该用户数据

**解决方案：**
1. 确认后端至少运行过一次，触发了 `FirebaseDataMigrationService`
2. 打开 Firebase Console → Realtime Database
3. 检查 `users/` 节点是否有数据
4. 如果没有，重启后端让迁移脚本运行

---

### 问题 2: 登录后 App 崩溃

**原因：** 某些页面仍依赖后端 API

**解决方案：**
1. 查看 Logcat 中的错误栈
2. 如果是网络错误（Retrofit / Connection refused），说明该模块尚未迁移
3. 暂时启动后端，或者等待该模块迁移完成

---

### 问题 3: 跑腿任务列表为空

**原因：** Firebase 中没有测试数据

**解决方案：**
1. 打开 Firebase Console → Realtime Database
2. 检查 `errands/` 节点
3. 如果为空，手动添加测试数据或重启后端触发迁移

---

### 问题 4: Firebase 权限错误

**错误示例：** `Permission denied`

**解决方案：**
1. 打开 Firebase Console → Realtime Database → Rules
2. 确认规则设置为 test mode（开发期间）:
   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```
3. **⚠️ 注意：生产环境必须修改为安全规则！**

---

## 📊 测试矩阵

| 功能 | 后端关闭 | 后端开启 | Firebase 数据 | 状态 |
|------|---------|---------|--------------|------|
| **Login** | ✅ 工作 | ✅ 工作 | users/ | 完成 |
| **Profile** | ✅ 工作 | ✅ 工作 | TokenManager | 完成 |
| **Errand List** | ✅ 工作 | ✅ 工作 | errands/ | 完成 |
| **Errand Create** | ✅ 工作 | ✅ 工作 | errands/ | 完成 |
| **Booking Create** | ✅ 工作 | ✅ 工作 | bookings/ | 完成 |
| **Booking List** | ✅ 工作 | ✅ 工作 | bookings/ | 完成 |
| **Course List** | ❌ 需要后端 | ✅ 工作 | courses/, schedules/ | 🚧 待迁移 |
| **INSTATT Sign-in** | ✅ 工作 | ✅ 工作 | sessions/ | 完成 |
| **Forum** | ❌ 需要后端 | ✅ 工作 | forum_posts/ | 🚧 待迁移 |

---

## 🎯 下一步任务

### 优先级 1: 完成课程列表迁移

**需要修改的文件：**
- `InstattRepository.kt` - `getStudentCourses()` / `getTeacherCourses()`
- `TeacherInstattViewModel.kt` / `StudentInstattViewModel.kt`

**目标：**
- 关闭后端后，课程列表仍能正常显示
- 使用 `FirebaseCourseRepository` 加载课程

---

### 优先级 2: 完成 Forum 模块迁移

**需要：**
1. 创建 `FirebaseForumRepository`
2. 修改 `ForumViewModel` / `ForumDetailViewModel`

---

### 优先级 3: 完成 Authentication 迁移

**终极目标：**
- 集成 Firebase Authentication SDK
- 重置所有用户密码
- 使用 `FirebaseAuth.signInWithEmailAndPassword()`

---

## ✅ 验收标准

**可以删除后端的前提条件：**

- [ ] Login ✅ 完全不依赖后端
- [ ] Profile ✅ 完全不依赖后端
- [ ] Errand ✅ 完全不依赖后端
- [ ] Booking ✅ 完全不依赖后端
- [ ] Course/Schedule 🚧 等待迁移完成
- [ ] INSTATT Sign-in ✅ 完全不依赖后端
- [ ] Forum 🚧 等待迁移完成
- [ ] Message 🚧 可能需要迁移

**最终测试：**
1. 关闭后端服务器
2. 完整使用 App 的所有功能
3. 每个功能都能正常工作
4. 无 Retrofit 错误或网络异常

**只有满足以上所有条件，才能安全删除 `backend/` 文件夹和 MySQL！**

---

## 📞 需要帮助？

如果测试中遇到问题：

1. **查看 Logcat**
   - Android Studio → Logcat
   - 搜索 "Firebase", "Error", "Failed"

2. **检查 Firebase Console**
   - https://console.firebase.google.com/project/mynottingham-b02b7
   - Realtime Database → 查看数据结构

3. **启用 Firebase 调试日志**
   ```kotlin
   // 在 Application 类的 onCreate() 中添加
   FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
   ```

---

**记住：在所有模块迁移并测试通过之前，请保留后端代码！** 🔒
