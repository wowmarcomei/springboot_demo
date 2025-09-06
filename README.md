# Spring Boot 图书管理系统学习项目

## 项目简介

这是一个基于Spring Boot 3.x开发的图书管理系统，旨在帮助初学者系统性地学习Spring Boot框架的核心功能和最佳实践。项目采用OpenGauss作为数据库，涵盖了从基础配置到高级功能的完整开发流程。

## 功能特性

- **图书管理** - 图书的增删改查操作
- **用户管理** - 用户注册、登录、权限控制
- **借阅管理** - 图书借阅、归还、借阅记录查询
- **数据统计** - 图书统计、借阅数据分析
- **RESTful API** - 完整的REST接口设计
- **Web界面** - 基于Thymeleaf的前端页面

## 技术栈

- **Spring Boot 3.x** - 核心开发框架
- **MyBatis** - 数据持久化框架
- **Spring Security** - 安全认证授权
- **Spring Web** - Web应用开发
- **OpenGauss** - 企业级数据库
- **Thymeleaf** - 服务端模板引擎
- **Maven** - 项目构建工具
- **JUnit 5** - 单元测试框架

## 项目结构

```
spring-boot-library-demo/
├── src/main/java/com/demo/library/
│   ├── LibraryApplication.java          # 主启动类
│   ├── controller/                      # 控制器层
│   │   ├── BookController.java          # 图书控制器
│   │   ├── UserController.java          # 用户控制器
│   │   └── BorrowController.java        # 借阅控制器
│   ├── service/                         # 业务逻辑层
│   │   ├── BookService.java             # 图书服务
│   │   ├── UserService.java             # 用户服务
│   │   └── BorrowService.java           # 借阅服务
│   ├── repository/                      # 数据访问层
│   │   ├── BookRepository.java          # 图书数据仓库
│   │   ├── UserRepository.java          # 用户数据仓库
│   │   └── BorrowRecordRepository.java  # 借阅记录仓库
│   ├── entity/                          # 实体类
│   │   ├── Book.java                    # 图书实体
│   │   ├── User.java                    # 用户实体
│   │   └── BorrowRecord.java            # 借阅记录实体
│   ├── dto/                             # 数据传输对象
│   │   ├── BookDTO.java                 # 图书DTO
│   │   └── UserDTO.java                 # 用户DTO
│   ├── config/                          # 配置类
│   │   ├── DatabaseConfig.java          # 数据库配置
│   │   └── SecurityConfig.java          # 安全配置
│   └── exception/                       # 异常处理
│       └── GlobalExceptionHandler.java  # 全局异常处理器
├── src/main/resources/
│   ├── application.yml                  # 主配置文件
│   ├── application-dev.yml              # 开发环境配置
│   ├── application-prod.yml             # 生产环境配置
│   ├── templates/                       # Thymeleaf模板
│   │   ├── books/                       # 图书相关页面
│   │   ├── users/                       # 用户相关页面
│   │   └── layout/                      # 布局模板
│   └── static/                          # 静态资源
│       ├── css/                         # 样式文件
│       ├── js/                          # JavaScript文件
│       └── images/                      # 图片资源
├── src/test/java/com/demo/library/      # 测试代码
├── docs/                                # 学习文档
├── pom.xml                              # Maven配置文件
└── README.md                            # 项目说明文档
```

## 学习路径

本项目采用循序渐进的学习方式，每个步骤都有详细的文档说明：

### 基础篇
- [Step 1: 环境准备与OpenGauss安装](docs/step1.md)
- [Step 2: Spring Boot项目创建与基础配置](docs/step2.md)
- [Step 3: OpenGauss数据库集成与MyBatis配置](docs/step3.md)
- [Step 4: 实体类设计与数据库表创建](docs/step4.md)

### 数据访问篇
- [Step 5: MyBatis Mapper接口与SQL映射开发](docs/step5.md)
- [Step 6: Service层业务逻辑实现](docs/step6.md)

### Web开发篇
- [Step 7: Controller层与REST API开发](docs/step7.md)
- [Step 8: Thymeleaf模板与前端页面开发](docs/step8.md)

### 高级功能篇
- [Step 9: Spring Security安全框架集成](docs/step9.md)
- [Step 10: 异常处理与日志配置](docs/step10.md)

### 测试与部署篇
- [Step 11: 单元测试与集成测试](docs/step11.md)
- [Step 12: 项目打包与部署](docs/step12.md)

## 快速开始

### 前置要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- OpenGauss 数据库
- IDE (推荐IntelliJ IDEA或Eclipse)

### 运行步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd spring-boot-library-demo
   ```

2. **配置数据库**
   - 安装并启动OpenGauss数据库
   - 创建数据库：`library_db`
   - 修改 `application.yml` 中的数据库连接信息

3. **运行项目**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **访问应用**
   - 浏览器访问：http://localhost:8080
   - API接口：http://localhost:8080/api

## 学习建议

1. **按步骤学习** - 严格按照Step1-Step12的顺序进行学习
2. **动手实践** - 每个步骤都要动手编码，不要只看不做
3. **理解原理** - 不仅要知道怎么做，更要理解为什么这么做
4. **扩展练习** - 完成基础功能后，尝试添加新的功能模块
5. **查阅文档** - 遇到问题时积极查阅Spring Boot官方文档

## 常见问题

**Q: OpenGauss连接失败怎么办？**
A: 检查数据库是否启动，连接参数是否正确，防火墙是否开放相应端口。

**Q: 项目启动失败怎么办？**
A: 查看控制台错误信息，检查依赖是否正确导入，配置文件是否正确。

**Q: 如何调试代码？**
A: 使用IDE的调试功能，设置断点进行调试，或使用日志输出关键信息。

## 参考资料

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA文档](https://spring.io/projects/spring-data-jpa)
- [Spring Security文档](https://spring.io/projects/spring-security)
- [OpenGauss官方文档](https://opengauss.org/zh/)
- [Thymeleaf文档](https://www.thymeleaf.org/)

## 贡献指南

欢迎提交Issue和Pull Request来改进这个学习项目！

## 许可证

本项目采用MIT许可证，详情请见LICENSE文件。

---

**开始你的Spring Boot学习之旅吧！** 🚀