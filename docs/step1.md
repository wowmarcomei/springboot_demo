# Step 1: 环境准备与OpenGauss安装

## 学习目标

通过本步骤，你将学会：
- 配置Spring Boot开发环境
- 安装和配置OpenGauss数据库
- 创建Spring Boot项目的基础结构
- 验证环境配置是否正确

## 前置要求

### 系统要求
- 操作系统：Windows 10+、macOS 10.14+、或Linux发行版
- 内存：至少4GB RAM（推荐8GB+）
- 磁盘空间：至少5GB可用空间

## 1. Java开发环境配置

### 1.1 安装Java JDK 17

**Windows系统：**
1. 访问 [Oracle JDK下载页面](https://www.oracle.com/java/technologies/downloads/)
2. 下载JDK 17 Windows x64版本
3. 运行安装程序，按默认配置安装
4. 配置环境变量：
   ```bash
   JAVA_HOME=C:\Program Files\Java\jdk-17
   PATH=%JAVA_HOME%\bin;%PATH%
   ```

**macOS系统：**
```bash
# 使用Homebrew安装
brew install openjdk@17

# 配置环境变量（添加到~/.zshrc或~/.bash_profile）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH
```

**Linux系统：**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

**验证安装：**
```bash
java -version
javac -version
```

应该看到类似输出：
```
java version "17.0.8" 2023-07-18 LTS
Java(TM) SE Runtime Environment (build 17.0.8+9-LTS-211)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.8+9-LTS-211, mixed mode, sharing)
```

### 1.2 安装Maven

**Windows系统：**
1. 访问 [Maven下载页面](https://maven.apache.org/download.cgi)
2. 下载Binary zip archive
3. 解压到 `C:\Program Files\Maven`
4. 配置环境变量：
   ```bash
   MAVEN_HOME=C:\Program Files\Maven\apache-maven-3.9.5
   PATH=%MAVEN_HOME%\bin;%PATH%
   ```

**macOS系统：**
```bash
# 使用Homebrew安装
brew install maven
```

**Linux系统：**
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo yum install maven
```

**验证安装：**
```bash
mvn -version
```

## 2. IDE安装与配置

### 2.1 推荐IDE

**IntelliJ IDEA**（推荐）
1. 访问 [JetBrains官网](https://www.jetbrains.com/idea/)
2. 下载Community版本（免费）或Ultimate版本
3. 安装并启动
4. 安装必要插件：
   - Spring Boot
   - Maven
   - Database Tools and SQL

**Eclipse STS**
1. 访问 [Spring Tools官网](https://spring.io/tools)
2. 下载Spring Tools 4 for Eclipse
3. 解压并启动

### 2.2 IDE配置

**IntelliJ IDEA配置：**
1. 设置JDK：File → Project Structure → SDKs → 添加JDK 17
2. 设置Maven：File → Settings → Build → Build Tools → Maven
   - Maven home path: 指向Maven安装目录
   - User settings file: 使用默认或自定义settings.xml

## 3. OpenGauss数据库安装

### 3.1 下载OpenGauss

访问 [OpenGauss官网](https://opengauss.org/zh/download/) 下载适合你系统的版本。

### 3.2 Windows系统安装

1. **下载安装包**
   ```bash
   # 下载企业版或极简版
   wget https://opengauss.obs.cn-south-1.myhuaweicloud.com/5.0.0/x86/openGauss-5.0.0-WINDOWS-64bit.zip
   ```

2. **安装步骤**
   - 解压下载的zip文件
   - 运行 `install.bat` 安装脚本
   - 按提示完成安装

3. **配置数据库**
   ```bash
   # 初始化数据库
   gs_initdb -D "C:\openGauss\data" --pwprompt --nodename=opengauss
   
   # 启动数据库服务
   gs_ctl start -D "C:\openGauss\data"
   ```

### 3.3 Linux系统安装

1. **创建用户和目录**
   ```bash
   # 创建omm用户（OpenGauss专用用户）
   sudo useradd -m omm
   sudo passwd omm
   
   # 创建安装目录
   sudo mkdir -p /opt/software/openGauss
   sudo chown omm:omm /opt/software/openGauss
   ```

2. **下载并解压**
   ```bash
   su - omm
   cd /opt/software/openGauss
   
   # 下载OpenGauss
   wget https://opengauss.obs.cn-south-1.myhuaweicloud.com/5.0.0/x86_openEuler/openGauss-5.0.0-openEuler-64bit.tar.bz2
   
   # 解压
   tar -jxf openGauss-5.0.0-openEuler-64bit.tar.bz2
   ```

3. **安装和初始化**
   ```bash
   # 设置环境变量
   export GAUSSHOME=/opt/software/openGauss
   export PATH=$GAUSSHOME/bin:$PATH
   export LD_LIBRARY_PATH=$GAUSSHOME/lib:$LD_LIBRARY_PATH
   
   # 初始化数据库
   gs_initdb -D /opt/software/openGauss/data --pwprompt --nodename=opengauss
   
   # 启动数据库
   gs_ctl start -D /opt/software/openGauss/data
   ```

### 3.4 macOS系统安装（Docker方式）

由于OpenGauss暂不直接支持macOS，推荐使用Docker：

1. **安装Docker**
   ```bash
   # 安装Docker Desktop for Mac
   brew install --cask docker
   ```

2. **拉取OpenGauss镜像**
   ```bash
   docker pull enmotech/opengauss:5.0.0
   ```

3. **启动OpenGauss容器**
   ```bash
   docker run --name opengauss \
     -p 5432:5432 \
     -e GS_PASSWORD=Enmo@123 \
     -d enmotech/opengauss:5.0.0
   ```

### 3.5 验证OpenGauss安装

**连接测试：**
```bash
# 使用gsql客户端连接
gsql -d postgres -p 5432 -U gaussdb -W

# Docker环境连接
docker exec -it opengauss gsql -d postgres -U gaussdb
```

**创建项目数据库：**
```sql
-- 创建库管理系统数据库
CREATE DATABASE library_db;

-- 创建应用用户
CREATE USER library_user WITH PASSWORD 'library@2023';

-- 授权
GRANT ALL PRIVILEGES ON DATABASE library_db TO library_user;

-- 验证数据库创建
\l
```

## 4. 项目结构创建

### 4.1 使用Spring Initializr创建项目

**方法一：网页版**
1. 访问 https://start.spring.io/
2. 配置项目参数：
   - Project: Maven
   - Language: Java
   - Spring Boot: 3.2.0
   - Group: com.demo
   - Artifact: spring-boot-library-demo
   - Name: Spring Boot Library Demo
   - Package name: com.demo.library
   - Packaging: Jar
   - Java: 17

3. 添加依赖：
   - Spring Web
   - Spring Data JPA
   - Spring Security
   - Thymeleaf
   - Spring Boot DevTools

4. 点击"Generate"下载项目

**方法二：IDE创建**
- IntelliJ IDEA: File → New → Project → Spring Initializr
- Eclipse STS: File → New → Spring Starter Project

### 4.2 项目目录结构验证

创建完成后，项目结构应该如下：
```
spring-boot-library-demo/
├── src/
│   ├── main/
│   │   ├── java/com/demo/library/
│   │   │   └── LibraryApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/com/demo/library/
│           └── LibraryApplicationTests.java
├── pom.xml
└── README.md
```

## 5. 基础配置文件

### 5.1 创建application.yml

删除默认的`application.properties`，创建`application.yml`：

```yaml
# 应用基础配置
server:
  port: 8080
  servlet:
    context-path: /

spring:
  # 应用名称
  application:
    name: spring-boot-library-demo
  
  # 开发工具配置
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
  
  # 数据源配置（暂时注释，Step 3中配置）
  # datasource:
  #   driver-class-name: org.opengauss.Driver
  #   url: jdbc:opengauss://localhost:5432/library_db
  #   username: library_user
  #   password: library@2023
  
  # JPA配置（暂时注释，Step 3中配置）
  # jpa:
  #   hibernate:
  #     ddl-auto: update
  #   show-sql: true
  #   properties:
  #     hibernate:
  #       dialect: org.hibernate.dialect.PostgreSQLDialect
  
  # Thymeleaf配置
  thymeleaf:
    cache: false
    encoding: UTF-8
    mode: HTML

# 日志配置
logging:
  level:
    com.demo.library: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 6. 创建主启动类

编辑`src/main/java/com/demo/library/LibraryApplication.java`：

```java
package com.demo.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
```

## 7. 验证环境配置

### 7.1 编译项目

```bash
cd spring-boot-library-demo
mvn clean compile
```

### 7.2 运行项目

```bash
mvn spring-boot:run
```

或者在IDE中直接运行`LibraryApplication`类。

### 7.3 验证启动

1. **查看控制台输出**，应该看到类似信息：
   ```
   Started LibraryApplication in 2.843 seconds (JVM running for 3.875)
   ```

2. **访问应用**：
   打开浏览器访问 http://localhost:8080
   
   由于还未配置具体页面，可能会看到错误页面，这是正常的。

### 7.4 验证依赖

运行测试确保依赖正确：
```bash
mvn test
```

## 8. 常见问题解决

### 8.1 Java版本问题
```bash
# 错误：java版本不匹配
# 解决：确认JAVA_HOME指向JDK 17
echo $JAVA_HOME  # Linux/macOS
echo %JAVA_HOME% # Windows
```

### 8.2 Maven依赖下载失败
```bash
# 清理并重新下载依赖
mvn clean install -U
```

### 8.3 OpenGauss连接问题
```bash
# 检查服务状态
gs_ctl status -D /path/to/data

# 检查端口占用
netstat -an | grep 5432
```

### 8.4 IDE识别问题
- 刷新Maven项目
- 重新导入项目
- 检查JDK配置

## 9. 总结

通过本步骤，我们完成了：

✅ **环境配置**
- JDK 17安装配置
- Maven构建工具安装
- IDE开发环境配置

✅ **数据库准备**
- OpenGauss数据库安装
- 数据库连接验证
- 项目数据库创建

✅ **项目创建**
- Spring Boot项目初始化
- 基础目录结构创建
- 主要配置文件设置

✅ **环境验证**
- 项目编译测试
- 应用启动验证

## 下一步

环境准备完成后，我们将在[Step 2](step2.md)中学习Spring Boot的基础配置，包括：
- 应用配置详解
- 多环境配置
- 自动配置原理
- 核心注解使用

---

**提示**：如果在安装过程中遇到问题，建议：
1. 查看官方文档
2. 检查系统兼容性
3. 确认防火墙设置
4. 查看错误日志定位问题