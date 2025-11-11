# Maven 安装指南 - Windows

## 方式一：手动安装（推荐）

### 步骤 1: 下载 Maven

1. 访问 Maven 官网: https://maven.apache.org/download.cgi
2. 找到 **Binary zip archive**
3. 下载 `apache-maven-3.9.6-bin.zip` （或最新版本）

### 步骤 2: 解压文件

1. 解压下载的 zip 文件
2. 将解压后的文件夹移动到: `C:\Program Files\Apache\maven`

   最终路径应该是：
   ```
   C:\Program Files\Apache\maven\bin\mvn.cmd
   C:\Program Files\Apache\maven\conf\
   C:\Program Files\Apache\maven\lib\
   ```

### 步骤 3: 配置环境变量

#### 3.1 打开环境变量设置

**方法 A（快速）:**
1. 按 `Win + R`
2. 输入 `sysdm.cpl`
3. 回车

**方法 B:**
1. 右键点击 **此电脑** 或 **我的电脑**
2. 选择 **属性**
3. 点击 **高级系统设置**
4. 点击 **环境变量**

#### 3.2 创建 MAVEN_HOME 变量

1. 在 **系统变量** 区域，点击 **新建**
2. 输入：
   - **变量名**: `MAVEN_HOME`
   - **变量值**: `C:\Program Files\Apache\maven`
3. 点击 **确定**

#### 3.3 添加到 PATH

1. 在 **系统变量** 中找到 `Path` 变量
2. 选中后点击 **编辑**
3. 点击 **新建**
4. 输入: `%MAVEN_HOME%\bin`
5. 点击 **确定**
6. 在所有窗口都点击 **确定** 保存

### 步骤 4: 验证安装

1. **关闭所有已打开的命令提示符窗口**（重要！）
2. 打开**新的**命令提示符
3. 运行:
   ```cmd
   mvn --version
   ```

4. 应该看到类似输出:
   ```
   Apache Maven 3.9.6
   Maven home: C:\Program Files\Apache\maven
   Java version: 21.0.8, vendor: Oracle Corporation
   ```

如果看到这个输出，恭喜！Maven 安装成功！🎉

---

## 方式二：使用 Chocolatey（自动安装）

如果你安装了 Chocolatey 包管理器，可以快速安装：

### 1. 以管理员身份打开 PowerShell

### 2. 运行命令
```powershell
choco install maven
```

### 3. 验证
```cmd
mvn --version
```

---

## 方式三：使用 Scoop（自动安装）

如果你使用 Scoop 包管理器：

```powershell
scoop install maven
```

---

## 常见问题

### 问题 1: "mvn 不是内部或外部命令"

**原因**: PATH 环境变量未正确配置

**解决方法**:
1. 确认 Maven 安装路径正确
2. 检查 `MAVEN_HOME` 变量值
3. 确认 PATH 中包含 `%MAVEN_HOME%\bin`
4. **关闭并重新打开命令提示符**

### 问题 2: 环境变量设置后仍然不工作

**解决方法**:
1. 确保关闭了所有旧的命令提示符窗口
2. 打开新的命令提示符
3. 如果还不行，重启电脑

### 问题 3: "JAVA_HOME not defined correctly"

**解决方法**:
需要设置 JAVA_HOME 环境变量：

1. 找到 Java 安装路径（你的是 `D:\Android Studio\jbr`）
2. 添加系统变量：
   - 变量名: `JAVA_HOME`
   - 变量值: `D:\Android Studio\jbr`
3. 重启命令提示符

---

## 快速验证清单

安装完成后，验证以下内容：

```cmd
# 检查 Maven 版本
mvn --version

# 检查 Java 版本
java -version

# 检查环境变量
echo %MAVEN_HOME%
echo %JAVA_HOME%
```

预期输出：
- Maven 版本信息
- Java 21.0.8
- `C:\Program Files\Apache\maven`
- `D:\Android Studio\jbr`

---

## 下一步：运行后端

Maven 安装成功后，在 **backend** 目录运行：

### 开发模式（H2 内存数据库）
```cmd
cd "D:\Android Studio Project\backend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产模式（MySQL 数据库）
```cmd
cd "D:\Android Studio Project\backend"
mvn spring-boot:run
```

### 构建 JAR 文件
```cmd
cd "D:\Android Studio Project\backend"
mvn clean package
```

---

## 帮助脚本

我已经为你创建了帮助脚本：

**启动脚本**:
- `start-dev.bat` - 开发模式启动（H2数据库）
- `start-prod.bat` - 生产模式启动（MySQL数据库）
- `build.bat` - 构建项目

**直接双击这些 .bat 文件即可运行！**

---

## 需要帮助？

如果遇到问题：
1. 检查上面的常见问题
2. 确保 Java 已正确安装（已确认 ✅）
3. 确保以管理员权限设置环境变量
4. 重启命令提示符或电脑

安装成功后，回来告诉我，我们继续测试后端！
