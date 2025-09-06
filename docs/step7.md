# Step 7: Controller层与REST API开发

## 学习目标

通过本步骤，你将学会：
- RESTful API设计原则和最佳实践
- 创建Controller层处理HTTP请求
- 实现完整的CRUD REST接口
- 参数验证和错误处理
- API文档自动生成
- 跨域配置和安全考虑

## 前置要求

确保已完成：
- Step 1: 环境准备与OpenGauss安装
- Step 2: Spring Boot基础配置
- Step 3: MyBatis与OpenGauss集成
- Step 4: 实体类设计与数据库表创建
- Step 5: MyBatis Mapper接口与SQL映射开发
- Step 6: Service层业务逻辑实现

## 1. RESTful API设计原则

### 1.1 REST核心概念

**资源（Resource）：** API的核心概念，每个URL代表一个资源
**表现层（Representation）：** 资源的具体表现形式（JSON、XML等）
**状态转移（State Transfer）：** 通过HTTP方法实现资源状态的改变
**无状态（Stateless）：** 每个请求都包含处理该请求所需的所有信息

### 1.2 HTTP方法语义

- **GET：** 获取资源，幂等操作
- **POST：** 创建新资源，非幂等操作
- **PUT：** 更新整个资源，幂等操作
- **PATCH：** 部分更新资源，非幂等操作
- **DELETE：** 删除资源，幂等操作

### 1.3 URL设计规范

```
// 好的设计
GET    /api/users           获取用户列表
GET    /api/users/123       获取ID为123的用户
POST   /api/users           创建新用户
PUT    /api/users/123       更新ID为123的用户
DELETE /api/users/123       删除ID为123的用户

// 避免的设计
GET    /api/getUsers
POST   /api/createUser
PUT    /api/updateUser/123
DELETE /api/deleteUser/123
```

## 2. API基础配置

### 2.1 添加Swagger依赖

在`pom.xml`中添加Swagger文档依赖：

```xml
<!-- Swagger API文档 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- 参数验证 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 2.2 创建Swagger配置

创建`src/main/java/com/demo/library/config/SwaggerConfig.java`：

```java
package com.demo.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger配置类
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot图书管理系统API")
                        .description("基于Spring Boot 3.x + MyBatis + OpenGauss的图书管理系统REST API")
                        .version("1.0.0")
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")
                                .url("https://example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("开发环境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("生产环境")
                ));
    }
}
```

### 2.3 配置跨域支持

创建`src/main/java/com/demo/library/config/CorsConfig.java`：

```java
package com.demo.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 跨域配置类
 */
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 允许的头信息
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 是否允许携带认证信息
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
```

## 3. 用户管理API

### 3.1 创建用户Controller

创建`src/main/java/com/demo/library/controller/UserController.java`：

```java
package com.demo.library.controller;

import com.demo.library.common.Result;
import com.demo.library.dto.*;
import com.demo.library.service.UserService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户管理Controller
 */
@Tag(name = "用户管理", description = "用户相关的增删改查接口")
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Operation(summary = "根据ID查询用户", description = "根据用户ID查询用户详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        
        logger.debug("查询用户详情，ID: {}", id);
        UserDTO user = userService.getById(id);
        return Result.success("查询成功", user);
    }
    
    @Operation(summary = "查询所有用户", description = "获取系统中所有用户的列表")
    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        logger.debug("查询所有用户");
        List<UserDTO> users = userService.getAllUsers();
        return Result.success("查询成功", users);
    }
    
    @Operation(summary = "条件查询用户", description = "根据条件查询用户列表")
    @GetMapping("/search")
    public Result<List<UserDTO>> searchUsers(
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String fullName,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setUsername(username);
        queryDTO.setEmail(email);
        queryDTO.setFullName(fullName);
        queryDTO.setRole(role);
        queryDTO.setStatus(status);
        queryDTO.setKeyword(keyword);
        
        logger.debug("条件查询用户，查询条件: {}", queryDTO);
        List<UserDTO> users = userService.getUsersByCondition(queryDTO);
        return Result.success("查询成功", users);
    }
    
    @Operation(summary = "分页查询用户", description = "分页获取用户列表")
    @GetMapping("/page")
    public Result<PageInfo<UserDTO>> getUsersByPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String fullName,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setUsername(username);
        queryDTO.setEmail(email);
        queryDTO.setFullName(fullName);
        queryDTO.setRole(role);
        queryDTO.setStatus(status);
        queryDTO.setKeyword(keyword);
        
        logger.debug("分页查询用户，页码: {}, 每页大小: {}, 查询条件: {}", pageNum, pageSize, queryDTO);
        PageInfo<UserDTO> pageInfo = userService.getUsersByPage(pageNum, pageSize, queryDTO);
        return Result.success("查询成功", pageInfo);
    }
    
    @Operation(summary = "创建用户", description = "创建新的用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
    })
    @PostMapping
    public Result<UserDTO> createUser(
            @Parameter(description = "用户创建信息", required = true)
            @RequestBody @Valid UserCreateDTO createDTO) {
        
        logger.info("创建用户，用户名: {}", createDTO.getUsername());
        UserDTO user = userService.createUser(createDTO);
        return Result.success("用户创建成功", user);
    }
    
    @Operation(summary = "更新用户信息", description = "根据ID更新用户信息")
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "用户更新信息", required = true)
            @RequestBody @Valid UserUpdateDTO updateDTO) {
        
        logger.info("更新用户信息，用户ID: {}", id);
        UserDTO user = userService.updateUser(id, updateDTO);
        return Result.success("用户信息更新成功", user);
    }
    
    @Operation(summary = "更新用户状态", description = "更新用户的状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "新状态", required = true)
            @RequestParam @NotNull String status) {
        
        logger.info("更新用户状态，用户ID: {}, 新状态: {}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success("用户状态更新成功");
    }
    
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        
        logger.info("删除用户，用户ID: {}", id);
        userService.deleteUser(id);
        return Result.success("用户删除成功");
    }
    
    @Operation(summary = "批量删除用户", description = "根据ID列表批量删除用户")
    @DeleteMapping
    public Result<Void> deleteUsers(
            @Parameter(description = "用户ID列表", required = true)
            @RequestBody List<@NotNull @Min(1) Long> ids) {
        
        logger.info("批量删除用户，用户ID列表: {}", ids);
        userService.deleteUsers(ids);
        return Result.success("用户批量删除成功");
    }
    
    @Operation(summary = "检查用户名是否存在", description = "检查指定用户名是否已被使用")
    @GetMapping("/exists/username")
    public Result<Boolean> checkUsernameExists(
            @Parameter(description = "用户名", required = true)
            @RequestParam @NotNull String username) {
        
        boolean exists = userService.existsByUsername(username);
        return Result.success("检查完成", exists);
    }
    
    @Operation(summary = "检查邮箱是否存在", description = "检查指定邮箱是否已被使用")
    @GetMapping("/exists/email")
    public Result<Boolean> checkEmailExists(
            @Parameter(description = "邮箱地址", required = true)
            @RequestParam @NotNull String email) {
        
        boolean exists = userService.existsByEmail(email);
        return Result.success("检查完成", exists);
    }
    
    @Operation(summary = "用户登录", description = "用户登录验证")
    @PostMapping("/login")
    public Result<UserDTO> login(
            @Parameter(description = "登录信息", required = true)
            @RequestBody @Valid UserLoginDTO loginDTO) {
        
        logger.info("用户登录，用户名: {}", loginDTO.getUsername());
        UserDTO user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success("登录成功", user);
    }
    
    @Operation(summary = "修改密码", description = "用户修改密码")
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "密码修改信息", required = true)
            @RequestBody @Valid PasswordChangeDTO passwordChangeDTO) {
        
        logger.info("用户修改密码，用户ID: {}", id);
        userService.changePassword(id, passwordChangeDTO.getOldPassword(), passwordChangeDTO.getNewPassword());
        return Result.success("密码修改成功");
    }
    
    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "新密码", required = true)
            @RequestParam @NotNull String newPassword) {
        
        logger.info("重置用户密码，用户ID: {}", id);
        userService.resetPassword(id, newPassword);
        return Result.success("密码重置成功");
    }
    
    @Operation(summary = "查询活跃用户", description = "查询指定天数内有登录的活跃用户")
    @GetMapping("/active")
    public Result<List<UserDTO>> getActiveUsers(
            @Parameter(description = "天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) Integer days) {
        
        logger.debug("查询活跃用户，天数: {}", days);
        List<UserDTO> users = userService.getActiveUsers(days);
        return Result.success("查询成功", users);
    }
    
    @Operation(summary = "根据角色查询用户", description = "查询指定角色的用户列表")
    @GetMapping("/role/{role}")
    public Result<List<UserDTO>> getUsersByRole(
            @Parameter(description = "用户角色", required = true)
            @PathVariable @NotNull String role) {
        
        logger.debug("根据角色查询用户，角色: {}", role);
        List<UserDTO> users = userService.getUsersByRole(role);
        return Result.success("查询成功", users);
    }
}
```

### 3.2 创建登录和密码修改DTO

创建`src/main/java/com/demo/library/dto/UserLoginDTO.java`：

```java
package com.demo.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录数据传输对象
 */
@Schema(description = "用户登录信息")
public class UserLoginDTO {
    
    @Schema(description = "用户名", example = "admin", required = true)
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Schema(description = "密码", example = "password123", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;
    
    // 构造器
    public UserLoginDTO() {}
    
    public UserLoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getter和Setter方法
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String toString() {
        return "UserLoginDTO{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
```

创建`src/main/java/com/demo/library/dto/PasswordChangeDTO.java`：

```java
package com.demo.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 密码修改数据传输对象
 */
@Schema(description = "密码修改信息")
public class PasswordChangeDTO {
    
    @Schema(description = "原密码", required = true)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    
    @Schema(description = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度必须在6-100个字符之间")
    private String newPassword;
    
    // 构造器
    public PasswordChangeDTO() {}
    
    public PasswordChangeDTO(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
    
    // Getter和Setter方法
    public String getOldPassword() {
        return oldPassword;
    }
    
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    @Override
    public String toString() {
        return "PasswordChangeDTO{" +
                "oldPassword='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                '}';
    }
}
```

## 4. 图书管理API

### 4.1 创建图书Controller

创建`src/main/java/com/demo/library/controller/BookController.java`：

```java
package com.demo.library.controller;

import com.demo.library.common.Result;
import com.demo.library.dto.*;
import com.demo.library.service.BookService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 图书管理Controller
 */
@Tag(name = "图书管理", description = "图书相关的增删改查接口")
@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    
    @Autowired
    private BookService bookService;
    
    @Operation(summary = "根据ID查询图书", description = "根据图书ID查询图书详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "图书不存在"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/{id}")
    public Result<BookDTO> getBookById(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        
        logger.debug("查询图书详情，ID: {}", id);
        BookDTO book = bookService.getById(id);
        return Result.success("查询成功", book);
    }
    
    @Operation(summary = "根据ISBN查询图书", description = "根据ISBN号查询图书信息")
    @GetMapping("/isbn/{isbn}")
    public Result<BookDTO> getBookByIsbn(
            @Parameter(description = "ISBN号", required = true)
            @PathVariable @NotNull String isbn) {
        
        logger.debug("根据ISBN查询图书，ISBN: {}", isbn);
        BookDTO book = bookService.getByIsbn(isbn);
        return Result.success("查询成功", book);
    }
    
    @Operation(summary = "查询所有图书", description = "获取系统中所有图书的列表")
    @GetMapping
    public Result<List<BookDTO>> getAllBooks() {
        logger.debug("查询所有图书");
        List<BookDTO> books = bookService.getAllBooks();
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "条件查询图书", description = "根据条件查询图书列表")
    @GetMapping("/search")
    public Result<List<BookDTO>> searchBooks(
            @Parameter(description = "书名") @RequestParam(required = false) String title,
            @Parameter(description = "作者") @RequestParam(required = false) String author,
            @Parameter(description = "出版社") @RequestParam(required = false) String publisher,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "语言") @RequestParam(required = false) String language,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal priceMin,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal priceMax,
            @Parameter(description = "仅显示可借阅") @RequestParam(required = false) Boolean availableOnly,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setTitle(title);
        queryDTO.setAuthor(author);
        queryDTO.setPublisher(publisher);
        queryDTO.setCategory(category);
        queryDTO.setLanguage(language);
        queryDTO.setStatus(status);
        queryDTO.setPriceMin(priceMin);
        queryDTO.setPriceMax(priceMax);
        queryDTO.setAvailableOnly(availableOnly);
        queryDTO.setKeyword(keyword);
        
        logger.debug("条件查询图书，查询条件: {}", queryDTO);
        List<BookDTO> books = bookService.getBooksByCondition(queryDTO);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "分页查询图书", description = "分页获取图书列表")
    @GetMapping("/page")
    public Result<PageInfo<BookDTO>> getBooksByPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @Parameter(description = "书名") @RequestParam(required = false) String title,
            @Parameter(description = "作者") @RequestParam(required = false) String author,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "仅显示可借阅") @RequestParam(required = false) Boolean availableOnly,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setTitle(title);
        queryDTO.setAuthor(author);
        queryDTO.setCategory(category);
        queryDTO.setStatus(status);
        queryDTO.setAvailableOnly(availableOnly);
        queryDTO.setKeyword(keyword);
        
        logger.debug("分页查询图书，页码: {}, 每页大小: {}, 查询条件: {}", pageNum, pageSize, queryDTO);
        PageInfo<BookDTO> pageInfo = bookService.getBooksByPage(pageNum, pageSize, queryDTO);
        return Result.success("查询成功", pageInfo);
    }
    
    @Operation(summary = "创建图书", description = "添加新的图书到系统")
    @PostMapping
    public Result<BookDTO> createBook(
            @Parameter(description = "图书创建信息", required = true)
            @RequestBody @Valid BookCreateDTO createDTO) {
        
        logger.info("创建图书，书名: {}", createDTO.getTitle());
        BookDTO book = bookService.createBook(createDTO);
        return Result.success("图书创建成功", book);
    }
    
    @Operation(summary = "更新图书信息", description = "根据ID更新图书信息")
    @PutMapping("/{id}")
    public Result<BookDTO> updateBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "图书更新信息", required = true)
            @RequestBody @Valid BookUpdateDTO updateDTO) {
        
        logger.info("更新图书信息，图书ID: {}", id);
        BookDTO book = bookService.updateBook(id, updateDTO);
        return Result.success("图书信息更新成功", book);
    }
    
    @Operation(summary = "更新图书状态", description = "更新图书的状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateBookStatus(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "新状态", required = true)
            @RequestParam @NotNull String status) {
        
        logger.info("更新图书状态，图书ID: {}, 新状态: {}", id, status);
        bookService.updateBookStatus(id, status);
        return Result.success("图书状态更新成功");
    }
    
    @Operation(summary = "更新图书库存", description = "更新图书的总册数和可借册数")
    @PatchMapping("/{id}/copies")
    public Result<Void> updateBookCopies(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "总册数") @RequestParam(required = false) @Min(0) Integer totalCopies,
            @Parameter(description = "可借册数") @RequestParam(required = false) @Min(0) Integer availableCopies) {
        
        logger.info("更新图书库存，图书ID: {}, 总册数: {}, 可借册数: {}", id, totalCopies, availableCopies);
        bookService.updateBookCopies(id, totalCopies, availableCopies);
        return Result.success("图书库存更新成功");
    }
    
    @Operation(summary = "删除图书", description = "根据ID删除图书")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBook(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        
        logger.info("删除图书，图书ID: {}", id);
        bookService.deleteBook(id);
        return Result.success("图书删除成功");
    }
    
    @Operation(summary = "批量删除图书", description = "根据ID列表批量删除图书")
    @DeleteMapping
    public Result<Void> deleteBooks(
            @Parameter(description = "图书ID列表", required = true)
            @RequestBody List<@NotNull @Min(1) Long> ids) {
        
        logger.info("批量删除图书，图书ID列表: {}", ids);
        bookService.deleteBooks(ids);
        return Result.success("图书批量删除成功");
    }
    
    @Operation(summary = "检查ISBN是否存在", description = "检查指定ISBN是否已被使用")
    @GetMapping("/exists/isbn")
    public Result<Boolean> checkIsbnExists(
            @Parameter(description = "ISBN号", required = true)
            @RequestParam @NotNull String isbn) {
        
        boolean exists = bookService.existsByIsbn(isbn);
        return Result.success("检查完成", exists);
    }
    
    @Operation(summary = "根据分类查询图书", description = "查询指定分类的图书列表")
    @GetMapping("/category/{category}")
    public Result<List<BookDTO>> getBooksByCategory(
            @Parameter(description = "图书分类", required = true)
            @PathVariable @NotNull String category) {
        
        logger.debug("根据分类查询图书，分类: {}", category);
        List<BookDTO> books = bookService.getBooksByCategory(category);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "根据作者查询图书", description = "查询指定作者的图书列表")
    @GetMapping("/author/{author}")
    public Result<List<BookDTO>> getBooksByAuthor(
            @Parameter(description = "作者姓名", required = true)
            @PathVariable @NotNull String author) {
        
        logger.debug("根据作者查询图书，作者: {}", author);
        List<BookDTO> books = bookService.getBooksByAuthor(author);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "查询可借阅图书", description = "查询当前可借阅的图书列表")
    @GetMapping("/available")
    public Result<List<BookDTO>> getAvailableBooks() {
        logger.debug("查询可借阅图书");
        List<BookDTO> books = bookService.getAvailableBooks();
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "查询热门图书", description = "根据借阅次数查询热门图书")
    @GetMapping("/popular")
    public Result<List<BookDTO>> getPopularBooks(
            @Parameter(description = "返回数量限制", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        
        logger.debug("查询热门图书，限制数量: {}", limit);
        List<BookDTO> books = bookService.getPopularBooks(limit);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "查询新上架图书", description = "查询最近新上架的图书")
    @GetMapping("/new")
    public Result<List<BookDTO>> getNewBooks(
            @Parameter(description = "天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) Integer days,
            @Parameter(description = "返回数量限制", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        
        logger.debug("查询新上架图书，天数: {}, 限制数量: {}", days, limit);
        List<BookDTO> books = bookService.getNewBooks(days, limit);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "按价格区间查询图书", description = "查询指定价格区间的图书")
    @GetMapping("/price-range")
    public Result<List<BookDTO>> getBooksByPriceRange(
            @Parameter(description = "最低价格", required = true)
            @RequestParam @NotNull @Min(0) BigDecimal minPrice,
            @Parameter(description = "最高价格", required = true)
            @RequestParam @NotNull @Min(0) BigDecimal maxPrice) {
        
        logger.debug("按价格区间查询图书，价格区间: {} - {}", minPrice, maxPrice);
        List<BookDTO> books = bookService.getBooksByPriceRange(minPrice, maxPrice);
        return Result.success("查询成功", books);
    }
    
    @Operation(summary = "全文搜索图书", description = "在图书标题、作者、出版社、描述中搜索关键词")
    @GetMapping("/fulltext-search")
    public Result<List<BookDTO>> searchBooksFulltext(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam @NotNull String keyword) {
        
        logger.debug("全文搜索图书，关键词: {}", keyword);
        List<BookDTO> books = bookService.searchBooks(keyword);
        return Result.success("搜索完成", books);
    }
    
    @Operation(summary = "获取所有分类", description = "获取系统中所有图书分类")
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        logger.debug("获取所有图书分类");
        List<String> categories = bookService.getAllCategories();
        return Result.success("查询成功", categories);
    }
    
    @Operation(summary = "获取分类统计", description = "获取各分类的图书数量统计")
    @GetMapping("/categories/stats")
    public Result<List<BookCategoryStats>> getCategoryStats() {
        logger.debug("获取分类统计");
        List<BookCategoryStats> stats = bookService.getCategoryStats();
        return Result.success("查询成功", stats);
    }
}
```

### 4.2 创建图书相关DTO

需要创建`BookCreateDTO`和`BookUpdateDTO`（在前面的Step中可能已经创建，这里完整展示）：

创建`src/main/java/com/demo/library/dto/BookCreateDTO.java`：

```java
package com.demo.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书创建数据传输对象
 */
@Schema(description = "图书创建信息")
public class BookCreateDTO {
    
    @Schema(description = "ISBN号", example = "978-0-123456-78-9")
    private String isbn;
    
    @Schema(description = "书名", example = "Java编程思想", required = true)
    @NotBlank(message = "书名不能为空")
    private String title;
    
    @Schema(description = "作者", example = "Bruce Eckel", required = true)
    @NotBlank(message = "作者不能为空")
    private String author;
    
    @Schema(description = "出版社", example = "机械工业出版社")
    private String publisher;
    
    @Schema(description = "出版日期", example = "2023-01-01")
    private LocalDate publishDate;
    
    @Schema(description = "分类", example = "计算机技术")
    private String category;
    
    @Schema(description = "图书描述")
    private String description;
    
    @Schema(description = "封面图片URL")
    private String coverImageUrl;
    
    @Schema(description = "价格", example = "89.99")
    @DecimalMin(value = "0.0", message = "价格不能小于0")
    private BigDecimal price;
    
    @Schema(description = "总册数", example = "10")
    @Min(value = 1, message = "总册数不能小于1")
    private Integer totalCopies = 1;
    
    @Schema(description = "可借册数", example = "10")
    @Min(value = 0, message = "可借册数不能小于0")
    private Integer availableCopies;
    
    @Schema(description = "语言", example = "zh-CN")
    private String language = "zh-CN";
    
    @Schema(description = "页数", example = "500")
    @Min(value = 1, message = "页数不能小于1")
    private Integer pageCount;
    
    @Schema(description = "创建人ID")
    private Long createdBy;
    
    // 构造器
    public BookCreateDTO() {
        this.availableCopies = this.totalCopies;
    }
    
    // Getter和Setter方法（省略具体实现）
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { 
        this.totalCopies = totalCopies;
        // 自动设置可借册数
        if (this.availableCopies == null || this.availableCopies > totalCopies) {
            this.availableCopies = totalCopies;
        }
    }
    
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
```

## 5. 借阅管理API

### 5.1 创建借阅Controller

创建`src/main/java/com/demo/library/controller/BorrowController.java`：

```java
package com.demo.library.controller;

import com.demo.library.common.Result;
import com.demo.library.dto.*;
import com.demo.library.service.BorrowService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 借阅管理Controller
 */
@Tag(name = "借阅管理", description = "图书借阅相关的接口")
@RestController
@RequestMapping("/api/borrows")
@Validated
public class BorrowController {
    
    private static final Logger logger = LoggerFactory.getLogger(BorrowController.class);
    
    @Autowired
    private BorrowService borrowService;
    
    @Operation(summary = "借阅图书", description = "用户借阅图书")
    @PostMapping
    public Result<BorrowRecordDTO> borrowBook(
            @Parameter(description = "借阅信息", required = true)
            @RequestBody @Valid BorrowRequestDTO borrowRequestDTO) {
        
        logger.info("用户借阅图书，用户ID: {}, 图书ID: {}", 
                    borrowRequestDTO.getUserId(), borrowRequestDTO.getBookId());
        
        BorrowRecordDTO record = borrowService.borrowBook(
                borrowRequestDTO.getUserId(), 
                borrowRequestDTO.getBookId(), 
                borrowRequestDTO.getBorrowNotes());
        
        return Result.success("借阅成功", record);
    }
    
    @Operation(summary = "归还图书", description = "用户归还图书")
    @PutMapping("/{recordId}/return")
    public Result<BorrowRecordDTO> returnBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @NotNull @Min(1) Long recordId,
            @Parameter(description = "归还备注")
            @RequestParam(required = false) String returnNotes) {
        
        logger.info("归还图书，记录ID: {}", recordId);
        BorrowRecordDTO record = borrowService.returnBook(recordId, returnNotes);
        return Result.success("归还成功", record);
    }
    
    @Operation(summary = "续借图书", description = "用户续借图书")
    @PutMapping("/{recordId}/renew")
    public Result<BorrowRecordDTO> renewBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @NotNull @Min(1) Long recordId,
            @Parameter(description = "续借天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) Integer days) {
        
        logger.info("续借图书，记录ID: {}, 续借天数: {}", recordId, days);
        BorrowRecordDTO record = borrowService.renewBook(recordId, days);
        return Result.success("续借成功", record);
    }
    
    @Operation(summary = "查询借阅记录详情", description = "根据ID查询借阅记录详情")
    @GetMapping("/{id}")
    public Result<BorrowRecordDetailDTO> getBorrowRecordDetail(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        
        logger.debug("查询借阅记录详情，ID: {}", id);
        BorrowRecordDetailDTO detail = borrowService.getDetailById(id);
        return Result.success("查询成功", detail);
    }
    
    @Operation(summary = "查询用户当前借阅", description = "查询用户当前正在借阅的图书")
    @GetMapping("/user/{userId}/current")
    public Result<List<BorrowRecordDetailDTO>> getCurrentBorrowsByUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long userId) {
        
        logger.debug("查询用户当前借阅，用户ID: {}", userId);
        List<BorrowRecordDetailDTO> records = borrowService.getCurrentBorrowsByUser(userId);
        return Result.success("查询成功", records);
    }
    
    @Operation(summary = "查询用户借阅历史", description = "查询用户的借阅历史记录")
    @GetMapping("/user/{userId}/history")
    public Result<List<BorrowRecordDetailDTO>> getBorrowHistoryByUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long userId) {
        
        logger.debug("查询用户借阅历史，用户ID: {}", userId);
        List<BorrowRecordDetailDTO> records = borrowService.getBorrowHistoryByUser(userId);
        return Result.success("查询成功", records);
    }
    
    @Operation(summary = "分页查询借阅记录", description = "分页查询系统中的借阅记录")
    @GetMapping("/page")
    public Result<PageInfo<BorrowRecordDetailDTO>> getBorrowRecordsByPage(
            @Parameter(description = "页码", example = "1") 
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "每页大小", example = "10") 
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @Parameter(description = "用户ID") 
            @RequestParam(required = false) Long userId,
            @Parameter(description = "图书ID") 
            @RequestParam(required = false) Long bookId,
            @Parameter(description = "状态") 
            @RequestParam(required = false) String status,
            @Parameter(description = "用户关键词") 
            @RequestParam(required = false) String userKeyword,
            @Parameter(description = "图书关键词") 
            @RequestParam(required = false) String bookKeyword,
            @Parameter(description = "是否逾期") 
            @RequestParam(required = false) Boolean overdue) {
        
        BorrowRecordQueryDTO queryDTO = new BorrowRecordQueryDTO();
        queryDTO.setUserId(userId);
        queryDTO.setBookId(bookId);
        queryDTO.setStatus(status);
        queryDTO.setUserKeyword(userKeyword);
        queryDTO.setBookKeyword(bookKeyword);
        queryDTO.setOverdue(overdue);
        
        logger.debug("分页查询借阅记录，页码: {}, 每页大小: {}, 查询条件: {}", pageNum, pageSize, queryDTO);
        PageInfo<BorrowRecordDetailDTO> pageInfo = borrowService.getBorrowRecordsByPage(pageNum, pageSize, queryDTO);
        return Result.success("查询成功", pageInfo);
    }
    
    @Operation(summary = "查询逾期记录", description = "查询所有逾期未归还的借阅记录")
    @GetMapping("/overdue")
    public Result<List<BorrowRecordDetailDTO>> getOverdueRecords() {
        logger.debug("查询逾期记录");
        List<BorrowRecordDetailDTO> records = borrowService.getOverdueRecords();
        return Result.success("查询成功", records);
    }
    
    @Operation(summary = "查询即将到期记录", description = "查询即将到期的借阅记录")
    @GetMapping("/soon-expire")
    public Result<List<BorrowRecordDetailDTO>> getSoonExpireRecords(
            @Parameter(description = "天数", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) Integer days) {
        
        logger.debug("查询即将到期记录，天数: {}", days);
        List<BorrowRecordDetailDTO> records = borrowService.getSoonExpireRecords(days);
        return Result.success("查询成功", records);
    }
    
    @Operation(summary = "检查是否可以借阅", description = "检查用户是否可以借阅指定图书")
    @GetMapping("/can-borrow")
    public Result<Boolean> canBorrowBook(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotNull @Min(1) Long userId,
            @Parameter(description = "图书ID", required = true)
            @RequestParam @NotNull @Min(1) Long bookId) {
        
        boolean canBorrow = borrowService.canBorrowBook(userId, bookId);
        return Result.success("检查完成", canBorrow);
    }
    
    @Operation(summary = "获取图书借阅统计", description = "获取热门图书的借阅统计")
    @GetMapping("/stats/books")
    public Result<List<BookBorrowStats>> getBookBorrowStats(
            @Parameter(description = "返回数量限制", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        
        logger.debug("获取图书借阅统计，限制数量: {}", limit);
        List<BookBorrowStats> stats = borrowService.getBookBorrowStats(limit);
        return Result.success("查询成功", stats);
    }
    
    @Operation(summary = "获取月度借阅统计", description = "获取指定年份的月度借阅统计")
    @GetMapping("/stats/monthly")
    public Result<List<MonthlyBorrowStats>> getMonthlyBorrowStats(
            @Parameter(description = "年份", example = "2023")
            @RequestParam @NotNull @Min(2000) Integer year) {
        
        logger.debug("获取月度借阅统计，年份: {}", year);
        List<MonthlyBorrowStats> stats = borrowService.getMonthlyBorrowStats(year);
        return Result.success("查询成功", stats);
    }
}
```

### 5.2 创建借阅请求DTO

创建`src/main/java/com/demo/library/dto/BorrowRequestDTO.java`：

```java
package com.demo.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 借阅请求数据传输对象
 */
@Schema(description = "借阅请求信息")
public class BorrowRequestDTO {
    
    @Schema(description = "用户ID", example = "1", required = true)
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;
    
    @Schema(description = "图书ID", example = "1", required = true)
    @NotNull(message = "图书ID不能为空")
    @Min(value = 1, message = "图书ID必须大于0")
    private Long bookId;
    
    @Schema(description = "借阅备注")
    private String borrowNotes;
    
    // 构造器
    public BorrowRequestDTO() {}
    
    public BorrowRequestDTO(Long userId, Long bookId, String borrowNotes) {
        this.userId = userId;
        this.bookId = bookId;
        this.borrowNotes = borrowNotes;
    }
    
    // Getter和Setter方法
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getBookId() {
        return bookId;
    }
    
    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    
    public String getBorrowNotes() {
        return borrowNotes;
    }
    
    public void setBorrowNotes(String borrowNotes) {
        this.borrowNotes = borrowNotes;
    }
    
    @Override
    public String toString() {
        return "BorrowRequestDTO{" +
                "userId=" + userId +
                ", bookId=" + bookId +
                ", borrowNotes='" + borrowNotes + '\'' +
                '}';
    }
}
```

## 6. 统计和报表API

### 6.1 创建统计Controller

创建`src/main/java/com/demo/library/controller/StatsController.java`：

```java
package com.demo.library.controller;

import com.demo.library.common.Result;
import com.demo.library.dto.*;
import com.demo.library.service.UserService;
import com.demo.library.service.BookService;
import com.demo.library.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表Controller
 */
@Tag(name = "统计报表", description = "系统统计和报表相关接口")
@RestController
@RequestMapping("/api/stats")
@Validated
public class StatsController {
    
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BorrowService borrowService;
    
    @Operation(summary = "获取系统概览", description = "获取系统的基本统计信息")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getSystemOverview() {
        logger.debug("获取系统概览统计");
        
        Map<String, Object> overview = new HashMap<>();
        
        // 用户统计
        List<UserDTO> allUsers = userService.getAllUsers();
        List<UserDTO> activeUsers = userService.getActiveUsers(30);
        List<UserDTO> adminUsers = userService.getUsersByRole("ADMIN");
        
        overview.put("totalUsers", allUsers.size());
        overview.put("activeUsers", activeUsers.size());
        overview.put("adminUsers", adminUsers.size());
        
        // 图书统计
        List<BookDTO> allBooks = bookService.getAllBooks();
        List<BookDTO> availableBooks = bookService.getAvailableBooks();
        List<String> categories = bookService.getAllCategories();
        
        overview.put("totalBooks", allBooks.size());
        overview.put("availableBooks", availableBooks.size());
        overview.put("totalCategories", categories.size());
        
        // 借阅统计
        List<BorrowRecordDetailDTO> overdueRecords = borrowService.getOverdueRecords();
        List<BorrowRecordDetailDTO> soonExpireRecords = borrowService.getSoonExpireRecords(3);
        
        overview.put("overdueRecords", overdueRecords.size());
        overview.put("soonExpireRecords", soonExpireRecords.size());
        
        return Result.success("查询成功", overview);
    }
    
    @Operation(summary = "获取图书分类统计", description = "获取各分类的图书数量统计")
    @GetMapping("/books/categories")
    public Result<List<BookCategoryStats>> getBookCategoryStats() {
        logger.debug("获取图书分类统计");
        List<BookCategoryStats> stats = bookService.getCategoryStats();
        return Result.success("查询成功", stats);
    }
    
    @Operation(summary = "获取热门图书统计", description = "获取借阅次数最多的图书统计")
    @GetMapping("/books/popular")
    public Result<List<BookBorrowStats>> getPopularBooksStats(
            @Parameter(description = "返回数量限制", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        
        logger.debug("获取热门图书统计，限制数量: {}", limit);
        List<BookBorrowStats> stats = borrowService.getBookBorrowStats(limit);
        return Result.success("查询成功", stats);
    }
    
    @Operation(summary = "获取月度借阅趋势", description = "获取指定年份的月度借阅趋势")
    @GetMapping("/borrows/monthly/{year}")
    public Result<List<MonthlyBorrowStats>> getMonthlyBorrowTrend(
            @Parameter(description = "年份", example = "2023", required = true)
            @PathVariable @NotNull @Min(2000) Integer year) {
        
        logger.debug("获取月度借阅趋势，年份: {}", year);
        List<MonthlyBorrowStats> stats = borrowService.getMonthlyBorrowStats(year);
        return Result.success("查询成功", stats);
    }
    
    @Operation(summary = "获取用户活跃度统计", description = "获取用户活跃度统计信息")
    @GetMapping("/users/activity")
    public Result<Map<String, Object>> getUserActivityStats() {
        logger.debug("获取用户活跃度统计");
        
        Map<String, Object> activityStats = new HashMap<>();
        
        // 各时间段的活跃用户数
        List<UserDTO> last7Days = userService.getActiveUsers(7);
        List<UserDTO> last30Days = userService.getActiveUsers(30);
        List<UserDTO> last90Days = userService.getActiveUsers(90);
        
        activityStats.put("activeUsersLast7Days", last7Days.size());
        activityStats.put("activeUsersLast30Days", last30Days.size());
        activityStats.put("activeUsersLast90Days", last90Days.size());
        
        return Result.success("查询成功", activityStats);
    }
}
```

## 7. API测试

### 7.1 创建集成测试

创建`src/test/java/com/demo/library/controller/UserControllerTest.java`：

```java
package com.demo.library.controller;

import com.demo.library.dto.UserCreateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCreateUser() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("testuser");
        createDTO.setPassword("password123");
        createDTO.setEmail("test@example.com");
        createDTO.setFullName("Test User");
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
    
    @Test
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void testGetUsersByPage() throws Exception {
        mockMvc.perform(get("/api/users/page")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }
    
    @Test
    void testCreateUserWithInvalidData() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        // 缺少必填字段
        createDTO.setUsername("");
        createDTO.setPassword("123"); // 太短
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.code").value(10001)); // 参数无效
    }
}
```

## 8. API文档配置

### 8.1 访问API文档

启动应用后，可以通过以下URL访问API文档：

- **Swagger UI：** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON：** http://localhost:8080/v3/api-docs

### 8.2 自定义API文档

在`application.yml`中添加Swagger配置：

```yaml
springdoc:
  # API文档路径
  api-docs:
    path: /v3/api-docs
  # Swagger UI路径
  swagger-ui:
    path: /swagger-ui.html
    # 是否启用
    enabled: true
    # 默认展开级别
    docExpansion: none
    # 操作排序
    operationsSorter: method
    # 标签排序
    tagsSorter: alpha
  # 扫描包路径
  packages-to-scan: com.demo.library.controller
  # 路径匹配
  paths-to-match: /api/**
```

## 9. 常见问题解决

### 9.1 参数验证不生效

**问题：** @Valid注解不起作用

**解决方法：**
1. 确保添加了validation依赖
2. 在Controller类上添加@Validated注解
3. 检查参数绑定是否正确

### 9.2 跨域问题

**问题：** 前端请求被CORS策略阻止

**解决方法：**
1. 配置CorsConfig类
2. 检查allowed-origins配置
3. 确保请求头设置正确

### 9.3 JSON序列化问题

**问题：** 日期格式或null值处理异常

**解决方法：**
在`application.yml`中配置：

```yaml
spring:
  jackson:
    # 日期格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 时区
    time-zone: GMT+8
    # 序列化配置
    serialization:
      # 是否缩进JSON
      indent-output: true
      # 忽略null值
      write-null-map-values: false
    # 反序列化配置
    deserialization:
      # 忽略未知属性
      fail-on-unknown-properties: false
```

### 9.4 API文档不显示

**问题：** Swagger页面无法访问

**解决方法：**
1. 检查springdoc依赖版本
2. 确认扫描包路径正确
3. 检查是否有安全配置拦截

## 10. 性能优化建议

### 10.1 分页查询优化

- 合理设置默认分页大小
- 避免深度分页
- 使用游标分页处理大数据量

### 10.2 缓存策略

```java
@Service
public class BookServiceImpl implements BookService {
    
    @Cacheable(value = "books", key = "#id")
    public BookDTO getById(Long id) {
        // 实现代码
    }
    
    @CacheEvict(value = "books", key = "#id")
    public void deleteBook(Long id) {
        // 实现代码
    }
}
```

### 10.3 异步处理

```java
@Async
@EventListener
public void handleBookBorrowEvent(BookBorrowEvent event) {
    // 异步处理借阅相关业务
}
```

## 11. 总结

通过本步骤，我们完成了：

✅ **RESTful API设计**
- 遵循REST设计原则
- 合理的URL设计和HTTP方法使用
- 统一的请求响应格式

✅ **完整的CRUD接口**
- 用户管理API（增删改查、登录、密码管理）
- 图书管理API（图书信息管理、库存管理、搜索）
- 借阅管理API（借阅、归还、续借、统计）

✅ **参数验证和异常处理**
- 使用JSR-303验证注解
- 全局异常处理机制
- 友好的错误信息返回

✅ **API文档自动生成**
- Swagger/OpenAPI集成
- 详细的接口文档
- 在线API测试功能

✅ **跨域和配置**
- CORS跨域支持
- 统一的响应格式
- 性能优化配置

✅ **测试和调试**
- 集成测试编写
- API接口测试
- 问题排查方法

## 下一步

Controller层和REST API开发完成后，我们将在[Step 8](step8.md)中学习Thymeleaf模板与前端页面开发，包括：
- Thymeleaf模板引擎使用
- 前端页面设计和开发
- 表单处理和数据展示
- 静态资源管理
- 前后端数据交互

---

**提示**：RESTful API是现代Web应用的标准接口设计方式，要注重API的语义化、一致性和易用性。同时要关注安全性、性能和文档的完整性。