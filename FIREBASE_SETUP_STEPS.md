# Firebase Realtime Database 配置步骤

## 问题：教师端签到管理一直显示 Loading 转圈

**原因：** Firebase Realtime Database 还没有在 Firebase Console 创建和启用。

---

## 解决方案：立即配置 Firebase

### 步骤 1：登录 Firebase Console

1. 打开浏览器，访问：https://console.firebase.google.com/
2. 使用您的 Google 账号登录
3. 找到项目 **mynottingham-b02b7**（或创建新项目）

---

### 步骤 2：启用 Realtime Database

1. 在 Firebase Console 左侧菜单，点击 **"Build"** → **"Realtime Database"**
2. 点击 **"Create Database"** 按钮
3. 选择数据库位置：
   - 推荐：**asia-southeast1 (Singapore)** - 距离马来西亚最近，延迟最低
4. 选择安全规则模式：
   - **开发环境（临时）：** 选择 **"Start in test mode"**
   - 这会在 30 天内允许读写，方便测试
5. 点击 **"Enable"**

---

### 步骤 3：配置数据库规则（重要）

#### 开发环境规则（临时使用，30 天后失效）

在 Firebase Console → Realtime Database → Rules 标签页，粘贴以下规则：

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

**⚠️ 警告：** 这个规则允许任何人读写数据，仅用于开发测试！

点击 **"Publish"** 按钮发布规则。

---

#### 生产环境规则（推荐，安全）

如果需要更安全的规则，使用以下配置：

```json
{
  "rules": {
    "sessions": {
      "$sessionKey": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

**说明：**
- 允许所有人读写 `sessions` 节点
- 后续可以集成 Firebase Authentication 进行身份验证

---

### 步骤 4：验证配置

1. 在 Firebase Console 的 Realtime Database 页面
2. 您应该看到一个空的数据库，显示：
   ```
   mynottingham-b02b7-default-rtdb: null
   ```
3. 数据库 URL 应该类似：
   ```
   https://mynottingham-b02b7-default-rtdb.firebaseio.com/
   ```

---

### 步骤 5：测试连接

1. 重新运行 Android 应用
2. 教师端：点击某门课程，打开 "Course Management" 弹窗
3. 应该立即看到 "No students signed in yet"（而不是一直转圈）
4. 点击 "Unlock Sign-In" 按钮
5. 在 Firebase Console 查看数据库，应该看到新的 session 数据：

```json
{
  "sessions": {
    "123_2025-11-24": {
      "isLocked": false,
      "isActive": true,
      "startTime": 1732435200000
    }
  }
}
```

---

## 常见问题排查

### Q1: 仍然一直转圈，没有显示学生列表

**检查清单：**

1. ✅ Firebase Realtime Database 已创建
2. ✅ 数据库规则设置为允许读写
3. ✅ `google-services.json` 文件已正确放置在 `app/` 目录
4. ✅ 应用已重新构建（Clean & Rebuild）
5. ✅ 网络连接正常

**调试方法：**

在 Android Studio 的 Logcat 中过滤 `Firebase`，查看是否有连接错误：

```
FirebaseDatabase: Failed to connect to Firebase
```

---

### Q2: Firebase Console 显示 "Permission Denied"

**原因：** 数据库规则太严格，不允许读写。

**解决：** 临时使用开发环境规则（见步骤 3）

---

### Q3: 应用崩溃或报错

**检查 Logcat 错误信息：**

```bash
# 在 Android Studio Logcat 中过滤
DatabaseException: Failed to connect
```

**解决：**
- 确认 `google-services.json` 文件内容正确
- 确认包名匹配：`com.nottingham.mynottingham`
- 重新同步 Gradle：File → Sync Project with Gradle Files

---

## 快速测试命令（可选）

如果您想直接在 Firebase 中插入测试数据，可以在 Firebase Console 的 Data 标签页手动添加：

```json
{
  "sessions": {
    "0_2025-11-24": {
      "isLocked": false,
      "isActive": true,
      "startTime": 1732435200000,
      "students": {
        "1": {
          "studentId": 1,
          "studentName": "Test Student",
          "status": "PRESENT",
          "checkInTime": "2025-11-24T09:30:00Z",
          "timestamp": 1732435800000
        }
      }
    }
  }
}
```

点击 **"+"** 按钮，选择 "Import JSON"，粘贴上述内容。

---

## 下一步

配置完成后：

1. **重启应用**
2. **教师端测试：**
   - 打开某门课程
   - 点击 "Unlock Sign-In"
   - 查看 Firebase Console，应该看到 session 数据
3. **学生端测试：**
   - 学生点击签到按钮
   - 教师端应该**实时看到**学生出现在列表中

---

**作者：** Claude Code
**更新日期：** 2025-11-24
