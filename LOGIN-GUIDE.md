# 登录功能使用指南

## 概述
应用已成功集成登录功能，首次打开应用会显示登录界面。

## 测试账号

以下是后端数据库中的测试账号（密码均为 `password123`）：

### 学生账号
- **用户名**: `student1` | **密码**: `password123`
  - 姓名: Alice Wong
  - 专业: Computer Science
  - GPA: 3.75

- **用户名**: `student2` | **密码**: `password123`
  - 姓名: Bob Chen
  - 专业: Software Engineering
  - GPA: 3.5

- **用户名**: `student3` | **密码**: `password123`
  - 姓名: Charlie Lee
  - 专业: Electrical Engineering
  - GPA: 3.85

### 教师账号
- **用户名**: `teacher1` | **密码**: `password123`
  - 姓名: Dr. Sarah Johnson
  - 职称: Associate Professor
  - 系别: Computer Science Department

- **用户名**: `teacher2` | **密码**: `password123`
  - 姓名: Prof. John Smith
  - 职称: Professor
  - 系别: Software Engineering Department

### 管理员账号
- **用户名**: `admin` | **密码**: `password123`
  - 姓名: System Admin

## 技术实现

### 前端（Android）
1. **LoginFragment** (`app/src/main/java/com/nottingham/mynottingham/ui/auth/LoginFragment.kt`)
   - 登录界面UI
   - 输入验证
   - 错误处理

2. **LoginViewModel** (`app/src/main/java/com/nottingham/mynottingham/ui/auth/LoginViewModel.kt`)
   - 处理登录业务逻辑
   - 调用后端API
   - 管理登录状态

3. **TokenManager** (`app/src/main/java/com/nottingham/mynottingham/data/local/TokenManager.kt`)
   - 使用DataStore存储JWT token
   - 存储用户信息（ID、用户名、用户类型）

### 后端（Spring Boot）
1. **AuthController** (`backend/src/main/java/.../controller/AuthController.java`)
   - `/api/auth/login` - 登录端点
   - `/api/auth/logout` - 登出端点

2. **DTOs**
   - `LoginRequest` - 登录请求（用户名、密码）
   - `LoginResponse` - 登录响应（token、用户信息）
   - `ApiResponse<T>` - 统一API响应格式

## 网络配置

### 后端服务器
- **本地运行地址**: `http://localhost:8080/api/`
- **H2 数据库控制台**: `http://localhost:8080/api/h2-console/`

### Android 应用配置
- **模拟器访问地址**: `http://10.0.2.2:8080/api/`
  - 在Android模拟器中，`10.0.2.2` 指向宿主机的 `localhost`
- **配置文件**: `app/src/main/java/com/nottingham/mynottingham/util/Constants.kt`

## 启动步骤

### 1. 启动后端服务器
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. 运行Android应用
1. 启动Android Studio
2. 打开项目
3. 选择 Pixel 2 模拟器（API 30）
4. 点击Run

### 3. 测试登录
1. 应用启动后会显示登录界面
2. 输入任一测试账号的用户名和密码
3. 点击"Login"按钮
4. 登录成功后会跳转到主页面

## 功能特性

✅ **已实现**
- 登录界面UI（Material Design 3）
- 用户名和密码验证
- 后端API集成
- Token持久化存储
- 登录状态管理
- 错误处理和显示
- 加载状态指示
- 自动隐藏底部导航栏

⚠️ **待实现**
- API请求拦截器（自动添加token到请求头）
- Token过期处理
- 记住我功能
- 密码找回
- 首次登录引导

## 数据流

```
用户输入 → LoginFragment → LoginViewModel → Retrofit API Service → 后端 AuthController
                                                                            ↓
Token + 用户信息 ← TokenManager ← LoginViewModel ← API Response ← 验证密码（BCrypt）
```

## 安全性

- 所有密码使用BCrypt加密存储
- HTTPS支持（生产环境推荐）
- Token-based认证
- CORS配置（当前允许所有来源，生产环境需限制）

## 故障排除

### 登录失败
1. 确保后端服务器正在运行（http://localhost:8080/api/）
2. 检查网络连接
3. 确认使用正确的测试账号
4. 查看Logcat错误日志

### 网络连接问题
- 确保 `AndroidManifest.xml` 中有 `INTERNET` 权限
- 确认 `usesCleartextTraffic="true"` 已启用
- 检查防火墙设置

### 后端数据库问题
- 访问 H2 控制台: http://localhost:8080/api/h2-console/
- JDBC URL: `jdbc:h2:mem:mynottingham`
- 用户名: `sa`
- 密码: （留空）

## 下一步计划

1. 为各个功能模块集成后端API
2. 添加API请求拦截器自动附加token
3. 实现刷新token机制
4. 添加自动登录功能
5. 优化错误处理
