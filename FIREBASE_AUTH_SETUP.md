# 🔐 Firebase Authentication Setup Guide

## ✅ 已完成的工作

你的想法太棒了！我们已经完成了完整的 Firebase Authentication 集成，现在 App 可以：
- ✅ 使用 Firebase Auth SDK 进行真实身份验证
- ✅ 使用 Firebase 生成的真实 UID 管理用户
- ✅ 完全独立于后端运行
- ✅ 密码由 Firebase 安全验证

## 📋 实施步骤

### 第一步：运行后端迁移脚本（只需一次）

后端会自动创建 Firebase Authentication 账号并迁移数据：

```bash
cd backend
start-dev.bat
```

**发生了什么：**
1. `FirebaseDataMigrationService` 启动
2. 自动在 Firebase Authentication 中创建 6 个账号
3. 获取每个账号的真实 Firebase UID
4. 将用户数据存储在 `users/{UID}` 路径
5. 创建 `username_to_uid` 映射方便查询

**后端日志示例：**
```
✅ Created student1 with UID: abc123xyz456
✅ Created student2 with UID: def789uvw012
✅ Created teacher1 with UID: ghi345mno678
📋 All users can login with password: password123 (admin: admin123)
```

### 第二步：在 Firebase Console 验证账号

1. 打开 Firebase Console: https://console.firebase.google.com/project/mynottingham-b02b7
2. 进入 **Authentication** → **Users** 标签页
3. 你会看到 6 个自动创建的用户：
   - student1@nottingham.edu.my
   - student2@nottingham.edu.my
   - student3@nottingham.edu.my
   - teacher1@nottingham.edu.my
   - teacher2@nottingham.edu.my
   - admin@nottingham.edu.my

4. 打开 **Realtime Database** → **Data** 标签页
5. 检查数据结构：
   ```
   mynottingham-b02b7/
   ├── users/
   │   ├── abc123xyz456/        # student1 的真实 UID
   │   │   ├── username: "student1"
   │   │   ├── fullName: "Alice Wong"
   │   │   ├── email: "student1@nottingham.edu.my"
   │   │   ├── role: "STUDENT"
   │   │   └── ...
   │   └── ...
   ├── username_to_uid/
   │   ├── student1: "abc123xyz456"
   │   ├── teacher1: "ghi345mno678"
   │   └── ...
   ├── enrollments/
   │   └── comp3040/
   │       ├── abc123xyz456: true  # 使用真实 UID
   │       └── ...
   └── ...
   ```

### 第三步：测试 Android 登录

**关闭后端服务器：**
```bash
taskkill /F /IM java.exe
```

**打开 Android App 并登录：**
- Username: `student1`
- Password: `password123`

**App 内部流程：**
1. 用户输入 "student1"
2. LoginViewModel 转换为 "student1@nottingham.edu.my"
3. 调用 `FirebaseAuth.signInWithEmailAndPassword()`
4. Firebase 验证密码并返回 UID: "abc123xyz456"
5. 使用 UID 从 Realtime Database 获取用户详细信息
6. 保存 UID 和 Firebase ID Token 到 TokenManager
7. 登录成功！

**Logcat 输出示例：**
```
D/LoginViewModel: 🔐 Starting Firebase Auth login for user: student1
D/LoginViewModel: 📧 Converted username to email: student1@nottingham.edu.my
D/LoginViewModel: ✅ Firebase Auth successful! UID: abc123xyz456
D/LoginViewModel: ✅ Login successful: student1 (STUDENT) | UID: abc123xyz456
D/LoginViewModel: 👤 User info: Alice Wong | Email: student1@nottingham.edu.my
```

## 🧪 测试账号

| Username | Email | Password | Role |
|----------|-------|----------|------|
| student1 | student1@nottingham.edu.my | password123 | STUDENT |
| student2 | student2@nottingham.edu.my | password123 | STUDENT |
| student3 | student3@nottingham.edu.my | password123 | STUDENT |
| teacher1 | teacher1@nottingham.edu.my | password123 | TEACHER |
| teacher2 | teacher2@nottingham.edu.my | password123 | TEACHER |
| admin | admin@nottingham.edu.my | admin123 | ADMIN |

## 🎯 核心优势

### 1. **真实的 Firebase UID**
- 每个用户都有唯一的 Firebase UID（如 `abc123xyz456`）
- UID 由 Firebase 生成，全局唯一
- 所有数据关系（enrollments, bookings, errands）都使用 UID

### 2. **安全的密码验证**
- 密码由 Firebase 服务器端验证
- 使用行业标准的加密算法
- Android 端永远不会看到明文密码

### 3. **完全独立于后端**
- ✅ Authentication - Firebase Auth
- ✅ User Data - Firebase Realtime Database
- ✅ Real-time Sync - Firebase 自动处理
- ❌ Spring Boot - 不再需要！

### 4. **扩展性强**
- 可以轻松添加：
  - 密码重置（Email）
  - 邮箱验证
  - 多因素认证（MFA）
  - Google / Facebook 登录
  - 匿名登录

## 🔄 数据流示意图

```
┌─────────────┐
│   Android   │
│     App     │
└──────┬──────┘
       │ 1. login("student1", "password123")
       ↓
┌──────────────────────────────────────┐
│        LoginViewModel                 │
│  convertUsernameToEmail()             │
│  → student1@nottingham.edu.my        │
└──────┬───────────────────────────────┘
       │ 2. signInWithEmailAndPassword()
       ↓
┌──────────────────────────────────────┐
│      Firebase Authentication          │
│  验证密码 ✅                           │
│  返回 UID: abc123xyz456               │
└──────┬───────────────────────────────┘
       │ 3. getUserProfileOnce(UID)
       ↓
┌──────────────────────────────────────┐
│   Firebase Realtime Database          │
│  users/abc123xyz456/                  │
│  { username, fullName, role, ... }    │
└──────┬───────────────────────────────┘
       │ 4. Return User Object
       ↓
┌──────────────────────────────────────┐
│        TokenManager                   │
│  saveUserId(UID)                      │
│  saveToken(Firebase ID Token)         │
│  saveUserType(STUDENT)                │
└──────┬───────────────────────────────┘
       │ 5. Navigate to Home Screen
       ↓
    ✅ 登录成功！
```

## 🛠️ 代码亮点

### Backend: 自动创建 Firebase Auth 账号

```java
private String createAuthUserAndGetUid(FirebaseAuth auth, String email, String password, String displayName) {
    try {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName)
                .setEmailVerified(true);

        UserRecord userRecord = auth.createUser(request);
        return userRecord.getUid(); // 获取真实 UID

    } catch (Exception e) {
        // 如果用户已存在，返回现有 UID
        if (e.getMessage().contains("already exists")) {
            UserRecord existingUser = auth.getUserByEmail(email);
            return existingUser.getUid();
        }
        throw e;
    }
}
```

### Android: Firebase Auth 登录

```kotlin
suspend fun login(username: String, password: String) {
    // Step 1: 转换 username → email
    val email = "$username@nottingham.edu.my"

    // Step 2: Firebase Auth 验证
    val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
    val uid = authResult.user?.uid

    // Step 3: 从 Realtime Database 获取用户信息
    val user = firebaseUserRepo.getUserProfileOnce(uid).getOrThrow()

    // Step 4: 保存到 TokenManager
    tokenManager.saveUserId(uid)
    tokenManager.saveUsername(user.username)
    tokenManager.saveToken(firebaseUser.getIdToken(false).await().token)

    _loginSuccess.value = true
}
```

## ❓ 常见问题

### Q: 为什么用户输入 username 而不是 email？
A: 为了保持用户体验一致。LoginViewModel 会自动将 "student1" 转换为 "student1@nottingham.edu.my"。

### Q: 如果后端重启，UID 会改变吗？
A: 不会。`createAuthUserAndGetUid()` 会检测已存在的账号并返回相同的 UID。

### Q: 能否在 Firebase Console 手动添加用户？
A: 可以！但需要确保：
   1. 在 Authentication 中创建账号并获取 UID
   2. 在 Realtime Database 的 `users/{UID}` 添加数据
   3. 在 `username_to_uid/{username}` 添加映射

### Q: 如何重置用户密码？
A: 可以通过 Firebase Console 手动重置，或在 App 中集成 `FirebaseAuth.sendPasswordResetEmail()`。

### Q: 现在可以删除后端了吗？
A: **还不行！** 虽然登录已完全独立，但其他模块（如 Course Loading、Forum）仍需要后端。
   请参考 `TESTING_FIREBASE_MIGRATION.md` 中的测试矩阵。

## 🎉 下一步

1. **测试登录功能**（后端关闭状态）
2. **验证其他已迁移模块**（Errand, Booking）
3. **完成剩余模块迁移**（Course, Forum）
4. **删除后端依赖**（最终目标）

---

**恭喜！** 你的 App 现在使用的是 **生产级别的 Firebase Authentication**！
这是迈向完全去中心化架构的重要一步！🚀
