# Step 2: Spring Boot项目创建与基础配置

## 学习目标

通过本步骤，你将学会：
- 理解Spring Boot的核心概念和优势
- 掌握Spring Boot的自动配置原理
- 学会配置多环境的应用配置文件
- 理解Spring Boot的核心注解
- 创建第一个简单的Controller

## 1. Spring Boot核心概念

### 1.1 什么是Spring Boot

Spring Boot是Spring团队提供的脚手架框架，它的设计目标是：
- **开箱即用**：最小化配置，快速启动项目
- **约定优于配置**：采用合理的默认配置
- **独立运行**：内嵌Web服务器，可独立运行
- **生产就绪**：提供监控、健康检查等生产特性

### 1.2 Spring Boot的优势

相比传统Spring项目：
- **简化配置**：自动配置减少XML配置
- **快速开发**：starter依赖简化依赖管理  
- **内嵌服务器**：无需外部容器部署
- **监控管理**：内置Actuator监控端点

## 2. 项目结构详解

### 2.1 标准Maven项目结构

```
spring-boot-library-demo/
├── src/main/java/              # Java源代码
│   └── com/demo/library/
│       ├── LibraryApplication.java    # 主启动类
│       ├── controller/         # 控制器层
│       ├── service/           # 服务层
│       ├── mapper/            # MyBatis Mapper接口
│       ├── entity/            # 实体类
│       ├── dto/               # 数据传输对象
│       ├── config/            # 配置类
│       └── exception/         # 异常处理
├── src/main/resources/         # 资源文件
│   ├── application.yml         # 主配置文件
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-prod.yml    # 生产环境配置
│   ├── mybatis/               # MyBatis映射文件
│   │   └── mapper/
│   ├── static/                # 静态资源
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
│   └── templates/             # Thymeleaf模板
└── src/test/java/             # 测试代码
```

### 2.2 创建完整目录结构

```bash
# 切换到项目目录
cd spring-boot-library-demo

# 创建mapper目录（将repository改为mapper）
mkdir -p src/main/java/com/demo/library/mapper
mkdir -p src/main/resources/mybatis/mapper

# 创建其他必要目录
mkdir -p src/main/resources/static/{css,js,images}
mkdir -p src/main/resources/templates/{books,users,layout}
```

## 3. Spring Boot核心注解

### 3.1 主启动类注解

更新`LibraryApplication.java`：

```java
package com.demo.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot主启动类
 * 
 * @SpringBootApplication 复合注解，包含：
 * - @Configuration：标记配置类
 * - @EnableAutoConfiguration：启用自动配置
 * - @ComponentScan：组件扫描
 */
@SpringBootApplication
@MapperScan("com.demo.library.mapper")  // 扫描MyBatis Mapper接口
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
        System.out.println("====================================");
        System.out.println("图书管理系统启动成功!");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("====================================");
    }
}
```

### 3.2 常用注解说明

| 注解 | 作用域 | 说明 |
|------|--------|------|
| `@SpringBootApplication` | 类 | 主启动类标记，包含多个注解 |
| `@RestController` | 类 | REST控制器，返回JSON数据 |
| `@Controller` | 类 | MVC控制器，返回视图 |
| `@Service` | 类 | 服务层标记 |
| `@Repository` | 类 | 数据访问层标记 |
| `@Component` | 类 | 通用组件标记 |
| `@Autowired` | 字段/方法 | 依赖注入 |
| `@Value` | 字段 | 注入配置值 |

## 4. 配置文件详解

### 4.1 主配置文件 application.yml

创建`src/main/resources/application.yml`：

```yaml
# Spring Boot应用配置
spring:
  # 应用基本信息
  application:
    name: spring-boot-library-demo
  
  # 环境配置
  profiles:
    active: dev  # 默认激活开发环境
  
  # 开发工具配置
  devtools:
    restart:
      enabled: true           # 启用热重启
      additional-paths: src/main/java
    livereload:
      enabled: true           # 启用LiveReload

# 服务器配置
server:
  port: 8080                  # 端口号
  servlet:
    context-path: /           # 应用根路径
    encoding:
      charset: UTF-8          # 字符编码
      enabled: true
      force: true

# 日志配置
logging:
  level:
    root: INFO                           # 根日志级别
    com.demo.library: DEBUG              # 项目包日志级别
    org.springframework.web: DEBUG       # Spring Web日志
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n"
  file:
    name: logs/library-demo.log          # 日志文件路径

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics     # 开放的端点
  endpoint:
    health:
      show-details: when_authorized      # 健康检查详情
```

### 4.2 开发环境配置 application-dev.yml

创建`src/main/resources/application-dev.yml`：

```yaml
# 开发环境配置
spring:
  # 数据源配置（下一步配置）
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://localhost:5432/library_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
    username: library_user
    password: library@2023
    hikari:
      maximum-pool-size: 10             # 最大连接池大小
      minimum-idle: 5                   # 最小空闲连接数
      connection-timeout: 30000         # 连接超时时间
      idle-timeout: 600000              # 空闲超时时间
  
  # MyBatis配置
  mybatis:
    mapper-locations: classpath:mybatis/mapper/*.xml  # Mapper XML文件路径
    type-aliases-package: com.demo.library.entity     # 实体类包路径
    configuration:
      map-underscore-to-camel-case: true              # 下划线转驼峰
      cache-enabled: true                             # 启用二级缓存
      lazy-loading-enabled: true                      # 启用延迟加载
      multiple-result-sets-enabled: true              # 多结果集支持
      use-column-label: true                          # 使用列标签
      use-generated-keys: true                        # 使用生成的主键
      auto-mapping-behavior: partial                  # 自动映射行为
      default-statement-timeout: 25                   # 默认语句超时时间

# 服务器配置
server:
  port: 8080

# 日志配置
logging:
  level:
    com.demo.library.mapper: DEBUG       # MyBatis Mapper SQL日志
    org.springframework.security: DEBUG  # Spring Security日志
```

### 4.3 生产环境配置 application-prod.yml

创建`src/main/resources/application-prod.yml`：

```yaml
# 生产环境配置
spring:
  # 数据源配置
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://prod-server:5432/library_db?serverTimezone=UTC
    username: ${DB_USERNAME}              # 使用环境变量
    password: ${DB_PASSWORD}              # 使用环境变量
    hikari:
      maximum-pool-size: 20               # 生产环境更大连接池
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000               # 连接最大生存时间
  
  # MyBatis配置
  mybatis:
    mapper-locations: classpath:mybatis/mapper/*.xml
    type-aliases-package: com.demo.library.entity
    configuration:
      map-underscore-to-camel-case: true
      cache-enabled: true
      lazy-loading-enabled: true

# 服务器配置
server:
  port: ${SERVER_PORT:8080}               # 使用环境变量，默认8080

# 日志配置
logging:
  level:
    root: WARN                            # 生产环境减少日志
    com.demo.library: INFO
  file:
    name: /var/log/library-demo/app.log   # 生产环境日志路径

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info              # 生产环境减少暴露端点
  endpoint:
    health:
      show-details: never                 # 不显示健康检查详情
```

## 5. 创建第一个Controller

### 5.1 创建Welcome控制器

创建`src/main/java/com/demo/library/controller/WelcomeController.java`：

```java
package com.demo.library.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 欢迎页面控制器
 * 演示基础的Spring Boot Web功能
 */
@Controller
public class WelcomeController {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * 首页 - 返回Thymeleaf模板
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", applicationName);
        model.addAttribute("message", "欢迎使用图书管理系统！");
        return "index";  // 返回templates/index.html
    }
    
    /**
     * API接口 - 返回JSON数据
     */
    @GetMapping("/api/welcome")
    @ResponseBody
    public WelcomeResponse welcome() {
        return new WelcomeResponse(
            applicationName, 
            "Spring Boot图书管理系统启动成功！",
            System.currentTimeMillis()
        );
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
    
    /**
     * 响应对象
     */
    public static class WelcomeResponse {
        private String applicationName;
        private String message;
        private Long timestamp;
        
        public WelcomeResponse(String applicationName, String message, Long timestamp) {
            this.applicationName = applicationName;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getter methods
        public String getApplicationName() { return applicationName; }
        public String getMessage() { return message; }
        public Long getTimestamp() { return timestamp; }
    }
}
```

### 5.2 创建首页模板

创建`src/main/resources/templates/index.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${appName}">图书管理系统</title>
    <link href="https://cdn.bootcdn.net/ajax/libs/bootstrap/5.1.3/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .welcome-container {
            margin-top: 50px;
            text-align: center;
        }
        .feature-box {
            margin: 20px 0;
            padding: 20px;
            border: 1px solid #ddd;
            border-radius: 5px;
            background-color: #f9f9f9;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="welcome-container">
            <h1 class="display-4" th:text="${message}">欢迎使用图书管理系统！</h1>
            <p class="lead">基于Spring Boot 3.x + MyBatis + OpenGauss开发</p>
            
            <div class="row mt-5">
                <div class="col-md-4">
                    <div class="feature-box">
                        <h3>图书管理</h3>
                        <p>完整的图书增删改查功能</p>
                        <a href="/books" class="btn btn-primary">管理图书</a>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="feature-box">
                        <h3>用户管理</h3>
                        <p>用户注册、登录和权限管理</p>
                        <a href="/users" class="btn btn-success">管理用户</a>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="feature-box">
                        <h3>借阅管理</h3>
                        <p>图书借阅和归还管理</p>
                        <a href="/borrow" class="btn btn-info">借阅管理</a>
                    </div>
                </div>
            </div>
            
            <div class="mt-4">
                <p class="text-muted">
                    应用名称: <span th:text="${appName}"></span><br>
                    当前时间: <span th:text="${#dates.format(#dates.createNow(), 'yyyy-MM-dd HH:mm:ss')}"></span>
                </p>
            </div>
        </div>
    </div>
    
    <script src="https://cdn.bootcdn.net/ajax/libs/bootstrap/5.1.3/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

## 6. 测试应用功能

### 6.1 启动应用

```bash
# 方法1: 使用Maven
mvn spring-boot:run

# 方法2: 使用IDE
# 直接运行LibraryApplication类的main方法

# 方法3: 打包后运行
mvn clean package
java -jar target/spring-boot-library-demo.jar
```

### 6.2 访问测试

**启动成功后，访问以下地址：**

1. **首页**: http://localhost:8080/
   - 应该看到欢迎页面和功能模块

2. **API接口**: http://localhost:8080/api/welcome
   - 应该返回JSON格式的欢迎信息

3. **健康检查**: http://localhost:8080/health
   - 应该返回"OK"

4. **监控端点**: http://localhost:8080/actuator/health
   - 应该返回应用健康状态

### 6.3 切换环境配置

**测试不同环境配置：**

```bash
# 启动开发环境（默认）
mvn spring-boot:run

# 启动生产环境
mvn spring-boot:run -Dspring.profiles.active=prod

# 或者使用环境变量
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

## 7. 自动配置原理

### 7.1 @SpringBootApplication注解解析

```java
@SpringBootApplication = 
    @Configuration +          // 配置类
    @EnableAutoConfiguration+ // 自动配置
    @ComponentScan           // 组件扫描
```

### 7.2 自动配置工作原理

1. **启动时扫描**: Spring Boot启动时扫描`META-INF/spring.factories`
2. **条件判断**: 根据`@Conditional`注解判断是否满足条件
3. **自动配置**: 满足条件则自动配置相应的Bean
4. **用户覆盖**: 用户自定义配置优先于自动配置

### 7.3 查看自动配置

在`application.yml`中添加：
```yaml
# 显示自动配置报告
debug: true
```

或启动时添加参数：
```bash
mvn spring-boot:run -Ddebug=true
```

## 8. 配置优先级

Spring Boot配置的优先级（从高到低）：
1. 命令行参数
2. 系统环境变量
3. `application-{profile}.properties/yml`
4. `application.properties/yml`
5. `@PropertySource`注解的配置
6. 默认配置

## 9. 常见问题与解决

### 9.1 端口被占用

```bash
# 查看端口占用
netstat -an | grep 8080   # Linux/macOS
netstat -an | findstr 8080  # Windows

# 修改端口
server:
  port: 8081
```

### 9.2 热重启不生效

```yaml
spring:
  devtools:
    restart:
      enabled: true
      additional-paths: src/main/java
```

### 9.3 中文乱码问题

```yaml
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

## 10. 总结

通过本步骤学习，你已经掌握：

✅ **Spring Boot核心概念**
- 自动配置原理
- 核心注解使用
- 约定优于配置理念

✅ **项目结构设计**
- 标准Maven项目布局
- 包结构规划
- 资源文件组织

✅ **配置文件管理**
- 多环境配置
- 配置优先级
- 属性注入使用

✅ **Web功能开发**
- Controller创建
- 视图模板使用
- REST API开发

## 下一步

基础配置完成后，我们将在[Step 3](step3.md)中学习OpenGauss数据库集成与MyBatis配置，包括：
- OpenGauss连接配置
- MyBatis框架集成
- 数据源配置优化
- SQL映射基础

---

**提示**：完成本步骤后，确保应用能够正常启动并访问首页，这为后续的数据库集成奠定了基础。