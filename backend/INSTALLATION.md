# 后端安装指南

## 方式一：使用 IntelliJ IDEA（最推荐）

### 1. 安装 IntelliJ IDEA
- 下载 Community Edition（免费）: https://www.jetbrains.com/idea/download/
- 或使用 Android Studio（已包含 IntelliJ）

### 2. 打开项目
1. 打开 IntelliJ IDEA
2. 选择 **File > Open**
3. 选择 `backend` 文件夹
4. 点击 **OK**

### 3. 等待依赖下载
- IDEA 会自动下载所有 Maven 依赖
- 右下角显示进度条
- 等待完成（首次可能需要几分钟）

### 4. 运行应用
1. 找到 `MyNottinghamBackendApplication.java` 文件
2. 右键点击文件
3. 选择 **Run 'MyNottinghamBackendApplication'**
4. 或点击类名旁边的绿色三角形运行按钮

### 5. 查看运行结果
- 控制台显示 "MyNottingham Backend API is running!"
- 浏览器访问: http://localhost:8080/api/users

## 方式二：使用 Eclipse

### 1. 安装 Eclipse
- 下载 Eclipse IDE for Java Developers: https://www.eclipse.org/downloads/

### 2. 导入项目
1. 打开 Eclipse
2. 选择 **File > Import**
3. 选择 **Maven > Existing Maven Projects**
4. 点击 **Next**
5. 浏览到 `backend` 文件夹
6. 点击 **Finish**

### 3. 运行应用
1. 在 Package Explorer 中找到 `MyNottinghamBackendApplication.java`
2. 右键点击
3. 选择 **Run As > Spring Boot App**

## 方式三：安装 Maven 命令行工具

### Windows

1. **下载 Maven**
   - 访问: https://maven.apache.org/download.cgi
   - 下载 Binary zip archive (例如: apache-maven-3.9.6-bin.zip)

2. **解压文件**
   - 解压到 `C:\Program Files\Apache\maven`

3. **设置环境变量**
   - 右键点击 **此电脑** > **属性** > **高级系统设置** > **环境变量**
   - 在系统变量中，新建：
     - 变量名: `MAVEN_HOME`
     - 变量值: `C:\Program Files\Apache\maven`
   - 编辑系统变量 `Path`，添加: `%MAVEN_HOME%\bin`

4. **验证安装**
   ```cmd
   mvn --version
   ```

5. **运行应用**
   ```cmd
   cd "D:\Android Studio Project\backend"
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Mac

使用 Homebrew:
```bash
brew install maven
```

验证:
```bash
mvn --version
```

运行:
```bash
cd "Android Studio Project/backend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Linux

Ubuntu/Debian:
```bash
sudo apt update
sudo apt install maven
```

验证:
```bash
mvn --version
```

运行:
```bash
cd "Android Studio Project/backend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 方式四：使用 JAR 文件（需要先构建）

如果有其他人已经构建了 JAR 文件，你可以直接运行：

```bash
java -jar mynottingham-backend-1.0.0.jar
```

或使用开发配置：
```bash
java -jar -Dspring.profiles.active=dev mynottingham-backend-1.0.0.jar
```

## 验证安装成功

无论使用哪种方式，应用启动后：

1. **控制台输出**应该显示：
   ```
   ==============================================
   MyNottingham Backend API is running!
   API Base URL: http://localhost:8080/api
   H2 Console (dev): http://localhost:8080/api/h2-console
   ==============================================
   ```

2. **浏览器测试**:
   - 访问: http://localhost:8080/api/users
   - 应该看到 JSON 格式的用户列表

3. **查看数据库**:
   - 访问: http://localhost:8080/api/h2-console
   - 使用以下配置登录：
     - JDBC URL: `jdbc:h2:mem:mynottingham`
     - Username: `sa`
     - Password: (留空)

## 常见问题

### 端口 8080 被占用

**症状**: 看到 "Port 8080 is already in use"

**解决方法**:
1. 修改 `src/main/resources/application.properties`:
   ```properties
   server.port=8081
   ```
2. 重新启动应用

### 依赖下载失败

**症状**: Maven 依赖下载失败

**解决方法**:
1. 检查网络连接
2. 使用国内Maven镜像：创建 `C:\Users\你的用户名\.m2\settings.xml`:
   ```xml
   <settings>
     <mirrors>
       <mirror>
         <id>aliyun</id>
         <mirrorOf>central</mirrorOf>
         <name>Aliyun Maven</name>
         <url>https://maven.aliyun.com/repository/public</url>
       </mirror>
     </mirrors>
   </settings>
   ```

### Java 版本不兼容

**症状**: "Unsupported class file major version"

**解决方法**:
- 项目需要 Java 17 或更高版本
- 你当前有 Java 21，是兼容的
- 如果仍有问题，在 IDEA 中：
  - File > Project Structure > Project
  - 设置 SDK 为 Java 17 或 21
  - 设置 Language Level 为 17

## 推荐方式

🎯 **强烈推荐使用 IntelliJ IDEA 或 Android Studio**
- 最简单，无需手动配置
- 自动管理依赖
- 内置调试工具
- 代码提示和自动补全

## 下一步

安装成功后，查看：
- **API-TEST-GUIDE.md** - 学习如何测试 API
- **README.md** - 查看完整功能文档
- 开始连接 Android 应用
