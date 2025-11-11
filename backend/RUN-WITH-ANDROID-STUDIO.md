# 使用 Android Studio 运行后端（最简单方法）

## 为什么使用 Android Studio？

✅ 你已经安装了 Android Studio
✅ 无需额外安装 Maven
✅ 自动下载所有依赖
✅ 图形界面操作，简单直观
✅ 内置调试功能

## 详细步骤（带截图说明）

### 步骤 1: 打开 Backend 项目

1. **打开 Android Studio**

2. **导入项目**
   - 点击 **File** > **Open** （或启动界面的 **Open**）
   - 导航到: `D:\Android Studio Project\backend`
   - 选择 `backend` 文件夹
   - 点击 **OK**

3. **选择项目类型**
   - 如果弹出对话框询问项目类型
   - 选择 **Import project from external model**
   - 选择 **Maven**
   - 点击 **Create**

### 步骤 2: 等待依赖下载

1. **自动下载**
   - Android Studio 会自动开始下载 Maven 依赖
   - 右下角会显示进度条
   - 状态栏显示 "Downloading... " 或 "Indexing..."

2. **耐心等待**
   - 首次下载可能需要 5-10 分钟（取决于网络速度）
   - 可以在底部的 **Build** 标签页查看进度
   - 等待直到所有任务完成

3. **确认完成**
   - 右下角不再显示进度条
   - 状态栏显示 "Gradle sync finished"

### 步骤 3: 配置运行配置（首次需要）

#### 方式A: 自动配置（推荐）

1. **打开主类文件**
   - 在左侧 **Project** 面板，展开：
     ```
     backend
     └── src
         └── main
             └── java
                 └── com.nottingham.mynottingham.backend
                     └── MyNottinghamBackendApplication
     ```

2. **运行按钮**
   - 你会在类名旁边看到一个绿色的三角形 ▶️ 按钮
   - 点击这个按钮
   - 选择 **Run 'MyNottinghamBackendApplication'**

#### 方式B: 手动配置

如果没有看到绿色三角形：

1. **添加配置**
   - 点击顶部工具栏的 **Add Configuration...** （或 **Edit Configurations...**）

2. **创建新配置**
   - 点击左上角的 **+** 号
   - 选择 **Application**

3. **配置参数**
   - Name: `Backend Dev Mode`
   - Main class: 点击 **...** 按钮搜索 `MyNottinghamBackendApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Working directory: 选择 `backend` 文件夹
   - Module: 选择 `mynottingham-backend`

4. **应用并运行**
   - 点击 **Apply**
   - 点击 **OK**
   - 点击顶部工具栏的绿色运行按钮 ▶️

### 步骤 4: 查看运行结果

1. **控制台输出**
   - 底部的 **Run** 标签页会显示输出
   - 看到以下内容表示成功：
     ```
     ==============================================
     MyNottingham Backend API is running!
     API Base URL: http://localhost:8080/api
     H2 Console (dev): http://localhost:8080/api/h2-console
     ==============================================
     ```

2. **测试 API**
   - 打开浏览器
   - 访问: http://localhost:8080/api/users
   - 应该看到 JSON 格式的用户数据

3. **查看数据库**
   - 浏览器访问: http://localhost:8080/api/h2-console
   - 登录信息：
     - JDBC URL: `jdbc:h2:mem:mynottingham`
     - User Name: `sa`
     - Password: (留空)
   - 点击 **Connect**
   - 可以执行 SQL 查询查看数据

### 步骤 5: 停止服务

- 点击控制台左侧的红色方块按钮 ⏹️
- 或按快捷键 `Ctrl + F2` (Windows/Linux) 或 `Cmd + F2` (Mac)

## 切换到生产模式（MySQL）

### 1. 修改配置

编辑 Run Configuration：
- 顶部工具栏点击配置名称旁的下拉箭头
- 选择 **Edit Configurations...**
- 修改 **VM options**:
  - 开发模式: `-Dspring.profiles.active=dev`
  - 生产模式: 删除这个参数（或不填）

### 2. 配置 MySQL

1. 确保 MySQL 已安装并运行
2. 创建数据库:
   ```sql
   CREATE DATABASE mynottingham;
   ```
3. 编辑 `src/main/resources/application.properties`
4. 修改数据库连接信息

## 常见问题

### 1. "Cannot resolve symbol"

**问题**: 代码中有红色下划线，提示找不到类

**解决**:
- 点击 **File** > **Invalidate Caches / Restart**
- 选择 **Invalidate and Restart**
- 等待 Android Studio 重新索引

### 2. 端口 8080 被占用

**问题**: 启动时报错 "Address already in use"

**解决方法1 - 修改端口**:
- 编辑 `application.properties`
- 添加: `server.port=8081`
- 重新运行

**解决方法2 - 关闭占用的程序**:
- 打开命令提示符
- 查找占用端口的进程:
  ```cmd
  netstat -ano | findstr :8080
  ```
- 结束该进程:
  ```cmd
  taskkill /PID [进程ID] /F
  ```

### 3. 依赖下载失败

**问题**: Gradle sync 失败，无法下载依赖

**解决**:
1. **检查网络连接**
2. **使用国内镜像**:
   - 在 `pom.xml` 中添加阿里云镜像（已配置）
3. **重试同步**:
   - 点击 **File** > **Sync Project with Gradle Files**
4. **清除缓存**:
   - 点击 **File** > **Invalidate Caches / Restart**

### 4. Java 版本问题

**问题**: "Unsupported class file major version"

**解决**:
- 点击 **File** > **Project Structure**
- 在 **Project** 标签:
  - SDK: 选择 JDK 17 或更高
  - Language level: 选择 17
- 点击 **OK**

### 5. 找不到主类

**问题**: 运行时提示找不到 MyNottinghamBackendApplication

**解决**:
1. 确认文件路径正确
2. 右键点击 `backend` 文件夹
3. 选择 **Reload from Disk**
4. 或者 **File** > **Invalidate Caches / Restart**

## 调试技巧

### 1. 设置断点

- 在代码行号旁边点击，添加红色断点
- 使用 Debug 模式运行（绿色虫子图标 🐛）
- 程序会在断点处暂停，可以查看变量值

### 2. 查看日志

- 底部 **Run** 标签页显示所有日志
- 可以过滤日志级别（INFO, DEBUG, ERROR）
- SQL 查询会实时显示

### 3. 热重载

- 修改代码后，使用 **Ctrl + F9** 重新构建
- Spring Boot DevTools 会自动重启应用

## 优势总结

使用 Android Studio 运行后端的优势：

✅ **无需额外安装**: 不需要单独安装 Maven
✅ **自动管理依赖**: 自动下载所有需要的库
✅ **图形化界面**: 直观的操作界面
✅ **强大的调试**: 断点、变量查看、步进调试
✅ **代码提示**: 自动补全和错误检测
✅ **集成开发**: 前端和后端在一个 IDE 中

## 下一步

后端成功运行后：

1. **测试 API**: 查看 `API-TEST-GUIDE.md`
2. **连接 Android 应用**: 修改 Android 应用的 BASE_URL
3. **开发新功能**: 参考 `README.md` 了解架构
4. **查看数据**: 使用 H2 Console 浏览数据库

## 需要帮助？

- 查看完整文档: `README.md`
- 安装问题: `INSTALLATION.md`
- API 测试: `API-TEST-GUIDE.md`
- GitHub Issues: https://github.com/COMP3040-2025-2026/cw-2-group-b/issues
