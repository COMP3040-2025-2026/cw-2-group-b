# 快速启动指南

## 最快启动方式（推荐新手）

### Windows 用户

1. **确保已安装 JDK 17**
   - 打开命令提示符，输入: `java -version`
   - 应该显示 Java 17 或更高版本

2. **双击运行启动脚本**
   ```
   双击: backend\start-dev.bat
   ```

3. **等待启动完成**
   - 看到 "MyNottingham Backend API is running!" 表示成功

4. **测试 API**
   - 浏览器打开: http://localhost:8080/api/users
   - 应该看到用户列表的 JSON 数据

5. **查看数据库（可选）**
   - 浏览器打开: http://localhost:8080/api/h2-console
   - JDBC URL: `jdbc:h2:mem:mynottingham`
   - Username: `sa`
   - Password: (留空)
   - 点击 "Connect"

### Mac/Linux 用户

1. **打开终端，进入 backend 目录**
   ```bash
   cd "Android Studio Project/backend"
   ```

2. **添加执行权限**
   ```bash
   chmod +x start-dev.sh
   ```

3. **运行启动脚本**
   ```bash
   ./start-dev.sh
   ```

4. **测试 API**
   ```bash
   curl http://localhost:8080/api/users
   ```

## 命令行启动方式

### 开发模式（H2 内存数据库）

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产模式（MySQL 数据库）

1. **安装并启动 MySQL**
2. **创建数据库**
   ```sql
   CREATE DATABASE mynottingham;
   ```
3. **修改配置** (backend/src/main/resources/application.properties)
   ```properties
   spring.datasource.username=你的MySQL用户名
   spring.datasource.password=你的MySQL密码
   ```
4. **启动应用**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student1 | password123 | 学生 |
| student2 | password123 | 学生 |
| teacher1 | password123 | 教师 |
| admin | password123 | 管理员 |

## 常用 API 端点

- 所有用户: http://localhost:8080/api/users
- 所有学生: http://localhost:8080/api/users/students
- 所有教师: http://localhost:8080/api/users/teachers
- 单个用户: http://localhost:8080/api/users/1

## 端口占用问题

如果 8080 端口被占用，修改配置文件：

编辑 `application.properties`:
```properties
server.port=8081
```

然后访问 http://localhost:8081/api/users

## 停止服务

- Windows: 在命令提示符窗口按 `Ctrl + C`
- Mac/Linux: 在终端按 `Ctrl + C`

## 遇到问题？

### Maven 命令找不到
- 确保已安装 Maven: https://maven.apache.org/download.cgi
- 或使用项目自带的 Maven Wrapper: `./mvnw` (Mac/Linux) 或 `mvnw.cmd` (Windows)

### 端口 8080 被占用
- 修改 `application.properties` 中的 `server.port`
- 或关闭占用 8080 端口的程序

### Java 版本不对
- 必须使用 JDK 17 或更高版本
- 下载: https://www.oracle.com/java/technologies/downloads/

### 数据库连接失败（生产模式）
- 确保 MySQL 服务正在运行
- 检查用户名和密码是否正确
- 确认数据库 `mynottingham` 已创建

## 下一步

- 阅读完整 API 文档: `README.md`
- 查看 API 测试指南: `API-TEST-GUIDE.md`
- 开始开发 Android 应用，连接此后端

## 重要提示

### Android 应用连接本地后端

Android 模拟器连接本地后端时，不能使用 `localhost`！

**正确配置**:
```kotlin
// 使用电脑的局域网 IP
private const val BASE_URL = "http://192.168.1.100:8080/api/"

// 或使用模拟器特殊地址（代表主机）
private const val BASE_URL = "http://10.0.2.2:8080/api/"
```

**查看电脑 IP 地址**:
- Windows: 命令提示符输入 `ipconfig`
- Mac/Linux: 终端输入 `ifconfig` 或 `ip addr`

## 支持

遇到问题请查看:
- 完整文档: `README.md`
- API 测试: `API-TEST-GUIDE.md`
- GitHub Issues: https://github.com/COMP3040-2025-2026/cw-2-group-b/issues
