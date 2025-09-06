# Step 11: 单元测试与集成测试

## 学习目标

通过本步骤，你将学会：
- 使用JUnit 5进行单元测试开发
- 编写MockMvc API层集成测试
- 使用Testcontainers进行数据库集成测试
- 分析和提高测试覆盖率
- 掌握测试最佳实践和规范
- 实现持续集成的测试策略

## 前置要求

- 已完成前10个步骤
- 理解软件测试基本概念
- 熟悉JUnit和Mockito框架
- 了解Docker容器技术基础

## 1. 测试架构设计

### 1.1 测试金字塔
```
    E2E Tests (端到端测试)
      /\
     /  \
    /    \
   /Integration\ (集成测试)
  /____________\
 /              \
/   Unit Tests   \ (单元测试)
\________________/
```

### 1.2 测试分层策略
- **单元测试**：测试单个类或方法的功能
- **集成测试**：测试模块间的交互
- **端到端测试**：测试完整的用户场景

### 1.3 测试环境配置

在`pom.xml`中确保测试依赖已添加：

```xml
<!-- 测试依赖已在项目中配置 -->
<dependencies>
    <!-- Spring Boot Test Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- H2 Database for Testing -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 2. 测试配置文件

### 2.1 创建测试配置

创建`src/test/resources/application-test.yml`：

```yaml
spring:
  profiles:
    active: test
  
  # 测试数据源配置（使用H2内存数据库）
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    username: sa
    password: password
    
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true

# MyBatis配置
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# JWT配置（测试环境）
jwt:
  secret: testSecretKeyForJunitTestingShouldBeLongEnough
  expiration: 3600 # 1小时
  issuer: library-test-system

# 日志配置（测试环境）
logging:
  level:
    com.demo.library: DEBUG
    org.springframework.security: WARN
    org.springframework.web: WARN
    org.testcontainers: INFO
    com.zaxxer.hikari: WARN

# 测试数据配置
test:
  data:
    cleanup: true
    init-sql: classpath:test-data.sql
```

### 2.2 测试数据SQL

创建`src/test/resources/test-data.sql`：

```sql
-- 清理数据
DELETE FROM borrowing_records;
DELETE FROM user_roles;
DELETE FROM role_permissions;
DELETE FROM users;
DELETE FROM books;
DELETE FROM categories;
DELETE FROM roles;
DELETE FROM permissions;

-- 插入测试角色
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES 
(1, 'ADMIN', '系统管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'LIBRARIAN', '图书管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'USER', '普通用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试权限
INSERT INTO permissions (id, name, description, resource, action, created_at, updated_at) VALUES 
(1, 'book:read', '查看图书', 'book', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'book:write', '编辑图书', 'book', 'write', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'book:delete', '删除图书', 'book', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'user:read', '查看用户', 'user', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'borrowing:read', '查看借阅记录', 'borrowing', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 角色权限关联
INSERT INTO role_permissions (role_id, permission_id) VALUES 
-- 管理员拥有所有权限
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
-- 图书管理员权限
(2, 1), (2, 2), (2, 3), (2, 5),
-- 普通用户权限
(3, 1), (3, 5);

-- 插入测试用户（密码都是: password123）
INSERT INTO users (id, username, password, email, full_name, phone, enabled, 
                   account_non_expired, account_non_locked, credentials_non_expired, 
                   created_at, updated_at) VALUES 
(1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvntPEgW3Dzp0V8a8Qd4P3FzO', 
 'admin@test.com', '测试管理员', '13800000001', true, true, true, true, 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'librarian', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvntPEgW3Dzp0V8a8Qd4P3FzO', 
 'librarian@test.com', '测试图书管理员', '13800000002', true, true, true, true, 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'user', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvntPEgW3Dzp0V8a8Qd4P3FzO', 
 'user@test.com', '测试用户', '13800000003', true, true, true, true, 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 用户角色关联
INSERT INTO user_roles (user_id, role_id) VALUES 
(1, 1), -- admin用户为管理员
(2, 2), -- librarian用户为图书管理员
(3, 3); -- user用户为普通用户

-- 插入测试分类
INSERT INTO categories (id, name, description, parent_id, created_at, updated_at) VALUES 
(1, '计算机科学', '计算机相关书籍', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '文学', '文学类书籍', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Java编程', 'Java编程相关', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试图书
INSERT INTO books (id, isbn, title, subtitle, author, publisher, publish_date, 
                   pages, price, description, cover_image, category_id, location, 
                   total_copies, available_copies, status, created_at, updated_at) VALUES 
(1, '9787111544937', 'Spring Boot实战', '企业级应用开发实战', '张三', '机械工业出版社', 
 '2020-01-01', 500, 79.00, 'Spring Boot企业级开发指南', null, 3, 'A1-001', 
 5, 5, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '9787115392799', 'Java核心技术', '卷I 基础知识', '李四', '人民邮电出版社', 
 '2019-06-01', 800, 99.00, 'Java编程经典教材', null, 3, 'A1-002', 
 3, 2, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '9787020002207', '红楼梦', '中国古典四大名著之一', '曹雪芹', '人民文学出版社', 
 '2018-05-01', 1200, 45.00, '中国古典文学巨著', null, 2, 'B1-001', 
 2, 1, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### 2.3 测试基类

创建`src/test/java/com/demo/library/BaseTest.java`：

```java
package com.demo.library;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public abstract class BaseTest {
    // 公共测试方法和工具
}
```

## 3. 单元测试

### 3.1 Service层单元测试

创建`src/test/java/com/demo/library/service/BookServiceTest.java`：

```java
package com.demo.library.service;

import com.demo.library.entity.Book;
import com.demo.library.entity.Category;
import com.demo.library.exception.ResourceException;
import com.demo.library.mapper.BookMapper;
import com.demo.library.dto.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookService单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("图书服务单元测试")
class BookServiceTest {
    
    @Mock
    private BookMapper bookMapper;
    
    @InjectMocks
    private BookService bookService;
    
    private Book testBook;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("计算机科学");
        
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111544937");
        testBook.setTitle("Spring Boot实战");
        testBook.setAuthor("张三");
        testBook.setPublisher("机械工业出版社");
        testBook.setPublishDate(LocalDate.of(2020, 1, 1));
        testBook.setPages(500);
        testBook.setPrice(BigDecimal.valueOf(79.00));
        testBook.setCategoryId(1L);
        testBook.setCategory(testCategory);
        testBook.setLocation("A1-001");
        testBook.setTotalCopies(5);
        testBook.setAvailableCopies(5);
        testBook.setStatus(Book.BookStatus.AVAILABLE);
        testBook.setCreatedAt(LocalDateTime.now());
        testBook.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("根据ID查找图书 - 成功")
    void testFindById_Success() {
        // Given
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When
        Book result = bookService.findById(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(testBook.getId(), result.getId());
        assertEquals(testBook.getTitle(), result.getTitle());
        verify(bookMapper, times(1)).findById(1L);
    }
    
    @Test
    @DisplayName("根据ID查找图书 - 不存在")
    void testFindById_NotFound() {
        // Given
        when(bookMapper.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceException.ResourceNotFoundException.class, 
                    () -> bookService.findById(999L));
        verify(bookMapper, times(1)).findById(999L);
    }
    
    @Test
    @DisplayName("创建图书 - 成功")
    void testCreateBook_Success() {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787115392799");
        newBook.setTitle("Java核心技术");
        newBook.setAuthor("李四");
        newBook.setPublisher("人民邮电出版社");
        newBook.setPublishDate(LocalDate.of(2019, 6, 1));
        newBook.setPages(800);
        newBook.setPrice(BigDecimal.valueOf(99.00));
        newBook.setCategoryId(1L);
        newBook.setLocation("A1-002");
        newBook.setTotalCopies(3);
        newBook.setAvailableCopies(3);
        
        when(bookMapper.existsByIsbn("9787115392799")).thenReturn(false);
        when(bookMapper.insert(any(Book.class))).thenReturn(1);
        
        // When
        Book result = bookService.createBook(newBook);
        
        // Then
        assertNotNull(result);
        assertEquals(Book.BookStatus.AVAILABLE, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(bookMapper, times(1)).existsByIsbn("9787115392799");
        verify(bookMapper, times(1)).insert(any(Book.class));
    }
    
    @Test
    @DisplayName("创建图书 - ISBN已存在")
    void testCreateBook_ISBNExists() {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787111544937");
        
        when(bookMapper.existsByIsbn("9787111544937")).thenReturn(true);
        
        // When & Then
        assertThrows(ResourceException.ResourceAlreadyExistsException.class, 
                    () -> bookService.createBook(newBook));
        verify(bookMapper, times(1)).existsByIsbn("9787111544937");
        verify(bookMapper, never()).insert(any(Book.class));
    }
    
    @Test
    @DisplayName("更新图书 - 成功")
    void testUpdateBook_Success() {
        // Given
        Book updateBook = new Book();
        updateBook.setId(1L);
        updateBook.setTitle("Spring Boot实战（第二版）");
        updateBook.setPages(600);
        updateBook.setPrice(BigDecimal.valueOf(89.00));
        
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.update(any(Book.class))).thenReturn(1);
        
        // When
        Book result = bookService.updateBook(1L, updateBook);
        
        // Then
        assertNotNull(result);
        assertEquals("Spring Boot实战（第二版）", result.getTitle());
        assertEquals(600, result.getPages());
        assertEquals(BigDecimal.valueOf(89.00), result.getPrice());
        assertNotNull(result.getUpdatedAt());
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, times(1)).update(any(Book.class));
    }
    
    @Test
    @DisplayName("删除图书 - 成功")
    void testDeleteBook_Success() {
        // Given
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.delete(1L)).thenReturn(1);
        
        // When
        bookService.deleteBook(1L);
        
        // Then
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, times(1)).delete(1L);
    }
    
    @Test
    @DisplayName("分页查询图书")
    void testFindBooksWithPagination() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookMapper.findWithFilters(anyString(), anyLong(), anyString(), 
                                       anyInt(), anyInt())).thenReturn(books);
        when(bookMapper.countWithFilters(anyString(), anyLong(), anyString()))
                .thenReturn(1L);
        
        // When
        PageResult<Book> result = bookService.findBooks("Spring", 1L, 
                                                       "AVAILABLE", 0, 10);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getCurrentPage());
        assertEquals(10, result.getSize());
        verify(bookMapper, times(1)).findWithFilters(anyString(), anyLong(), 
                                                     anyString(), anyInt(), anyInt());
        verify(bookMapper, times(1)).countWithFilters(anyString(), anyLong(), anyString());
    }
    
    @Test
    @DisplayName("借阅图书 - 成功")
    void testBorrowBook_Success() {
        // Given
        testBook.setAvailableCopies(3);
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.updateAvailableCopies(1L, 2)).thenReturn(1);
        
        // When
        boolean result = bookService.borrowBook(1L);
        
        // Then
        assertTrue(result);
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, times(1)).updateAvailableCopies(1L, 2);
    }
    
    @Test
    @DisplayName("借阅图书 - 无可用副本")
    void testBorrowBook_NoAvailableCopies() {
        // Given
        testBook.setAvailableCopies(0);
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When
        boolean result = bookService.borrowBook(1L);
        
        // Then
        assertFalse(result);
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, never()).updateAvailableCopies(anyLong(), anyInt());
    }
    
    @Test
    @DisplayName("归还图书 - 成功")
    void testReturnBook_Success() {
        // Given
        testBook.setAvailableCopies(2);
        testBook.setTotalCopies(5);
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.updateAvailableCopies(1L, 3)).thenReturn(1);
        
        // When
        boolean result = bookService.returnBook(1L);
        
        // Then
        assertTrue(result);
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, times(1)).updateAvailableCopies(1L, 3);
    }
    
    @Test
    @DisplayName("归还图书 - 已达最大副本数")
    void testReturnBook_ExceedsMaxCopies() {
        // Given
        testBook.setAvailableCopies(5);
        testBook.setTotalCopies(5);
        when(bookMapper.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When
        boolean result = bookService.returnBook(1L);
        
        // Then
        assertFalse(result);
        verify(bookMapper, times(1)).findById(1L);
        verify(bookMapper, never()).updateAvailableCopies(anyLong(), anyInt());
    }
}
```

### 3.2 Util类单元测试

创建`src/test/java/com/demo/library/util/JwtUtilsTest.java`：

```java
package com.demo.library.util;

import com.demo.library.security.CustomUserDetails;
import com.demo.library.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具类单元测试
 */
@DisplayName("JWT工具类单元测试")
class JwtUtilsTest {
    
    private JwtUtils jwtUtils;
    private CustomUserDetails userDetails;
    
    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(jwtUtils, "secret", "testSecretKeyForJunitTestingShouldBeLongEnough");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600); // 1小时
        ReflectionTestUtils.setField(jwtUtils, "issuer", "library-test-system");
        
        // 创建测试用户
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        userDetails = new CustomUserDetails(user);
    }
    
    @Test
    @DisplayName("生成JWT令牌")
    void testGenerateToken() {
        // When
        String token = jwtUtils.generateToken(userDetails);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }
    
    @Test
    @DisplayName("从令牌中提取用户名")
    void testGetUsernameFromToken() {
        // Given
        String token = jwtUtils.generateToken(userDetails);
        
        // When
        String username = jwtUtils.getUsernameFromToken(token);
        
        // Then
        assertEquals("testuser", username);
    }
    
    @Test
    @DisplayName("从令牌中提取过期时间")
    void testGetExpirationDateFromToken() {
        // Given
        String token = jwtUtils.generateToken(userDetails);
        
        // When
        Date expirationDate = jwtUtils.getExpirationDateFromToken(token);
        
        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }
    
    @Test
    @DisplayName("验证有效令牌")
    void testValidateToken_ValidToken() {
        // Given
        String token = jwtUtils.generateToken(userDetails);
        
        // When
        Boolean isValid = jwtUtils.validateToken(token, userDetails);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    @DisplayName("验证无效用户名的令牌")
    void testValidateToken_InvalidUsername() {
        // Given
        String token = jwtUtils.generateToken(userDetails);
        
        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        CustomUserDetails anotherUserDetails = new CustomUserDetails(anotherUser);
        
        // When
        Boolean isValid = jwtUtils.validateToken(token, anotherUserDetails);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("验证令牌格式")
    void testValidateTokenFormat() {
        // Given
        String validToken = jwtUtils.generateToken(userDetails);
        String invalidToken = "invalid.token.format";
        String nullToken = null;
        String emptyToken = "";
        
        // When & Then
        assertTrue(jwtUtils.validateTokenFormat(validToken));
        assertFalse(jwtUtils.validateTokenFormat(invalidToken));
        assertFalse(jwtUtils.validateTokenFormat(nullToken));
        assertFalse(jwtUtils.validateTokenFormat(emptyToken));
    }
    
    @Test
    @DisplayName("从请求头中提取令牌")
    void testExtractTokenFromHeader() {
        // Given
        String validHeader = "Bearer eyJhbGciOiJIUzI1NiJ9...";
        String invalidHeader = "InvalidHeader";
        String nullHeader = null;
        
        // When & Then
        assertEquals("eyJhbGciOiJIUzI1NiJ9...", jwtUtils.extractTokenFromHeader(validHeader));
        assertNull(jwtUtils.extractTokenFromHeader(invalidHeader));
        assertNull(jwtUtils.extractTokenFromHeader(nullHeader));
    }
    
    @Test
    @DisplayName("生成带额外声明的令牌")
    void testGenerateTokenWithExtraClaims() {
        // Given
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("department", "IT");
        
        // When
        String token = jwtUtils.generateToken(userDetails, extraClaims);
        
        // Then
        assertNotNull(token);
        String username = jwtUtils.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }
    
    @Test
    @DisplayName("测试过期令牌")
    void testExpiredToken() {
        // Given - 创建已过期的JWT工具实例
        JwtUtils expiredJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(expiredJwtUtils, "secret", "testSecretKeyForJunitTestingShouldBeLongEnough");
        ReflectionTestUtils.setField(expiredJwtUtils, "expiration", -1); // 负数表示已过期
        ReflectionTestUtils.setField(expiredJwtUtils, "issuer", "library-test-system");
        
        String expiredToken = expiredJwtUtils.generateToken(userDetails);
        
        // When & Then
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtils.getUsernameFromToken(expiredToken);
        });
    }
}
```

## 4. 集成测试

### 4.1 Repository层集成测试

创建`src/test/java/com/demo/library/mapper/BookMapperIntegrationTest.java`：

```java
package com.demo.library.mapper;

import com.demo.library.BaseTest;
import com.demo.library.entity.Book;
import com.demo.library.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BookMapper集成测试
 */
@DisplayName("图书Mapper集成测试")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookMapperIntegrationTest extends BaseTest {
    
    @Autowired
    private BookMapper bookMapper;
    
    @Test
    @DisplayName("根据ID查找图书")
    void testFindById() {
        // When
        Optional<Book> bookOpt = bookMapper.findById(1L);
        
        // Then
        assertTrue(bookOpt.isPresent());
        Book book = bookOpt.get();
        assertEquals("Spring Boot实战", book.getTitle());
        assertEquals("9787111544937", book.getIsbn());
        assertNotNull(book.getCategory());
        assertEquals("Java编程", book.getCategory().getName());
    }
    
    @Test
    @DisplayName("根据ISBN查找图书")
    void testFindByIsbn() {
        // When
        Optional<Book> bookOpt = bookMapper.findByIsbn("9787111544937");
        
        // Then
        assertTrue(bookOpt.isPresent());
        Book book = bookOpt.get();
        assertEquals("Spring Boot实战", book.getTitle());
        assertEquals("张三", book.getAuthor());
    }
    
    @Test
    @DisplayName("检查ISBN是否存在")
    void testExistsByIsbn() {
        // When & Then
        assertTrue(bookMapper.existsByIsbn("9787111544937"));
        assertFalse(bookMapper.existsByIsbn("9999999999999"));
    }
    
    @Test
    @DisplayName("插入新图书")
    void testInsertBook() {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787302544234");
        newBook.setTitle("测试驱动开发");
        newBook.setAuthor("测试作者");
        newBook.setPublisher("清华大学出版社");
        newBook.setPublishDate(LocalDate.of(2021, 3, 1));
        newBook.setPages(400);
        newBook.setPrice(BigDecimal.valueOf(69.00));
        newBook.setCategoryId(3L);
        newBook.setLocation("A1-003");
        newBook.setTotalCopies(3);
        newBook.setAvailableCopies(3);
        newBook.setStatus(Book.BookStatus.AVAILABLE);
        newBook.setCreatedAt(LocalDateTime.now());
        newBook.setUpdatedAt(LocalDateTime.now());
        
        // When
        int result = bookMapper.insert(newBook);
        
        // Then
        assertEquals(1, result);
        assertNotNull(newBook.getId());
        
        // 验证插入成功
        Optional<Book> insertedBook = bookMapper.findById(newBook.getId());
        assertTrue(insertedBook.isPresent());
        assertEquals("测试驱动开发", insertedBook.get().getTitle());
    }
    
    @Test
    @DisplayName("更新图书信息")
    void testUpdateBook() {
        // Given
        Optional<Book> bookOpt = bookMapper.findById(1L);
        assertTrue(bookOpt.isPresent());
        
        Book book = bookOpt.get();
        book.setTitle("Spring Boot实战（更新版）");
        book.setPages(600);
        book.setPrice(BigDecimal.valueOf(89.00));
        book.setUpdatedAt(LocalDateTime.now());
        
        // When
        int result = bookMapper.update(book);
        
        // Then
        assertEquals(1, result);
        
        // 验证更新成功
        Optional<Book> updatedBook = bookMapper.findById(1L);
        assertTrue(updatedBook.isPresent());
        assertEquals("Spring Boot实战（更新版）", updatedBook.get().getTitle());
        assertEquals(600, updatedBook.get().getPages());
    }
    
    @Test
    @DisplayName("删除图书")
    void testDeleteBook() {
        // Given
        assertTrue(bookMapper.findById(3L).isPresent());
        
        // When
        int result = bookMapper.delete(3L);
        
        // Then
        assertEquals(1, result);
        assertFalse(bookMapper.findById(3L).isPresent());
    }
    
    @Test
    @DisplayName("按条件查找图书")
    void testFindWithFilters() {
        // When
        List<Book> books = bookMapper.findWithFilters("Spring", 3L, "AVAILABLE", 0, 10);
        
        // Then
        assertNotNull(books);
        assertEquals(1, books.size());
        assertTrue(books.get(0).getTitle().contains("Spring"));
    }
    
    @Test
    @DisplayName("统计符合条件的图书数量")
    void testCountWithFilters() {
        // When
        Long count = bookMapper.countWithFilters("Java", null, null);
        
        // Then
        assertEquals(2L, count);
    }
    
    @Test
    @DisplayName("按分类查找图书")
    void testFindByCategory() {
        // When
        List<Book> books = bookMapper.findByCategory(3L);
        
        // Then
        assertNotNull(books);
        assertEquals(2, books.size());
        books.forEach(book -> assertEquals(3L, book.getCategoryId()));
    }
    
    @Test
    @DisplayName("更新可用副本数")
    void testUpdateAvailableCopies() {
        // Given
        Optional<Book> bookOpt = bookMapper.findById(1L);
        assertTrue(bookOpt.isPresent());
        assertEquals(5, bookOpt.get().getAvailableCopies());
        
        // When
        int result = bookMapper.updateAvailableCopies(1L, 3);
        
        // Then
        assertEquals(1, result);
        
        // 验证更新成功
        Optional<Book> updatedBook = bookMapper.findById(1L);
        assertTrue(updatedBook.isPresent());
        assertEquals(3, updatedBook.get().getAvailableCopies());
    }
    
    @Test
    @DisplayName("按状态查找图书")
    void testFindByStatus() {
        // When
        List<Book> availableBooks = bookMapper.findByStatus("AVAILABLE");
        List<Book> borrowedBooks = bookMapper.findByStatus("BORROWED");
        
        // Then
        assertNotNull(availableBooks);
        assertEquals(3, availableBooks.size());
        
        assertNotNull(borrowedBooks);
        assertEquals(0, borrowedBooks.size());
    }
}
```

### 4.2 Web层集成测试

创建`src/test/java/com/demo/library/controller/BookControllerIntegrationTest.java`：

```java
package com.demo.library.controller;

import com.demo.library.BaseTest;
import com.demo.library.entity.Book;
import com.demo.library.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 图书控制器集成测试
 */
@DisplayName("图书控制器集成测试")
@AutoConfigureWebMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookControllerIntegrationTest extends BaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    private String adminToken;
    private String librarianToken;
    private String userToken;
    
    @BeforeEach
    void setUp() {
        // 这里应该创建真实的JWT令牌，但为了简化，使用@WithMockUser
    }
    
    @Test
    @DisplayName("获取图书列表 - 公开接口")
    void testGetBooks_Public() throws Exception {
        mockMvc.perform(get("/api/public/books")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }
    
    @Test
    @DisplayName("根据ID获取图书 - 成功")
    @WithMockUser(roles = "USER")
    void testGetBookById_Success() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.id").value(1))
                .andExpected(jsonPath("$.data.title").value("Spring Boot实战"))
                .andExpected(jsonPath("$.data.isbn").value("9787111544937"));
    }
    
    @Test
    @DisplayName("根据ID获取图书 - 不存在")
    @WithMockUser(roles = "USER")
    void testGetBookById_NotFound() throws Exception {
        mockMvc.perform(get("/api/books/999"))
                .andExpected(status().isNotFound())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(false))
                .andExpected(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
    
    @Test
    @DisplayName("创建图书 - 管理员权限")
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_AsAdmin() throws Exception {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787302544234");
        newBook.setTitle("测试驱动开发");
        newBook.setAuthor("测试作者");
        newBook.setPublisher("清华大学出版社");
        newBook.setPublishDate(LocalDate.of(2021, 3, 1));
        newBook.setPages(400);
        newBook.setPrice(BigDecimal.valueOf(69.00));
        newBook.setCategoryId(3L);
        newBook.setLocation("A1-003");
        newBook.setTotalCopies(3);
        newBook.setAvailableCopies(3);
        
        String requestBody = objectMapper.writeValueAsString(newBook);
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isCreated())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.isbn").value("9787302544234"))
                .andExpected(jsonPath("$.data.title").value("测试驱动开发"));
    }
    
    @Test
    @DisplayName("创建图书 - 图书管理员权限")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_AsLibrarian() throws Exception {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787302544235");
        newBook.setTitle("软件工程");
        newBook.setAuthor("管理员作者");
        newBook.setPublisher("人民邮电出版社");
        newBook.setPublishDate(LocalDate.of(2021, 4, 1));
        newBook.setPages(350);
        newBook.setPrice(BigDecimal.valueOf(59.00));
        newBook.setCategoryId(3L);
        newBook.setLocation("A1-004");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(2);
        
        String requestBody = objectMapper.writeValueAsString(newBook);
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isCreated())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true));
    }
    
    @Test
    @DisplayName("创建图书 - 普通用户无权限")
    @WithMockUser(roles = "USER")
    void testCreateBook_AsUser_Forbidden() throws Exception {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("9787302544236");
        newBook.setTitle("数据结构");
        
        String requestBody = objectMapper.writeValueAsString(newBook);
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isForbidden());
    }
    
    @Test
    @DisplayName("更新图书 - 成功")
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateBook_Success() throws Exception {
        // Given
        Book updateBook = new Book();
        updateBook.setTitle("Spring Boot实战（更新版）");
        updateBook.setPages(600);
        updateBook.setPrice(BigDecimal.valueOf(89.00));
        
        String requestBody = objectMapper.writeValueAsString(updateBook);
        
        // When & Then
        mockMvc.perform(put("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.title").value("Spring Boot实战（更新版）"));
    }
    
    @Test
    @DisplayName("删除图书 - 成功")
    @WithMockUser(roles = "ADMIN")
    void testDeleteBook_Success() throws Exception {
        mockMvc.perform(delete("/api/books/3"))
                .andExpected(status().isNoContent());
        
        // 验证图书已被删除
        mockMvc.perform(get("/api/books/3"))
                .andExpected(status().isNotFound());
    }
    
    @Test
    @DisplayName("搜索图书")
    void testSearchBooks() throws Exception {
        mockMvc.perform(get("/api/public/books/search")
                .param("keyword", "Spring")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.content").isArray())
                .andExpected(jsonPath("$.data.content[0].title").value("Spring Boot实战"));
    }
    
    @Test
    @DisplayName("按分类获取图书")
    void testGetBooksByCategory() throws Exception {
        mockMvc.perform(get("/api/public/books/category/3"))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data").isArray())
                .andExpected(jsonPath("$.data.length()").value(2));
    }
    
    @Test
    @DisplayName("参数验证 - 创建图书时缺少必需字段")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_ValidationError() throws Exception {
        // Given - 创建缺少必需字段的图书对象
        Book invalidBook = new Book();
        // 只设置title，缺少其他必需字段
        invalidBook.setTitle("不完整的图书");
        
        String requestBody = objectMapper.writeValueAsString(invalidBook);
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isBadRequest())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(false))
                .andExpected(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

## 5. Testcontainers数据库测试

### 5.1 Testcontainers配置

创建`src/test/java/com/demo/library/testcontainers/PostgreSQLTestContainer.java`：

```java
package com.demo.library.testcontainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL测试容器配置
 */
@Testcontainers
public abstract class PostgreSQLTestContainer {
    
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("test-schema.sql");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }
}
```

### 5.2 创建测试数据库脚本

创建`src/test/resources/test-schema.sql`：

```sql
-- 创建表结构（简化版）
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    id SERIAL PRIMARY KEY,
    isbn VARCHAR(13) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    subtitle VARCHAR(500),
    author VARCHAR(200) NOT NULL,
    publisher VARCHAR(200) NOT NULL,
    publish_date DATE,
    pages INTEGER,
    price DECIMAL(10,2),
    description TEXT,
    cover_image VARCHAR(500),
    category_id BIGINT REFERENCES categories(id),
    location VARCHAR(50),
    total_copies INTEGER DEFAULT 1,
    available_copies INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 其他必要的表...
```

### 5.3 Testcontainers集成测试

创建`src/test/java/com/demo/library/integration/BookServiceIntegrationTest.java`：

```java
package com.demo.library.integration;

import com.demo.library.entity.Book;
import com.demo.library.entity.Category;
import com.demo.library.service.BookService;
import com.demo.library.service.CategoryService;
import com.demo.library.testcontainers.PostgreSQLTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图书服务集成测试（使用Testcontainers）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("图书服务集成测试")
class BookServiceIntegrationTest extends PostgreSQLTestContainer {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private CategoryService categoryService;
    
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // 创建测试分类
        testCategory = new Category();
        testCategory.setName("集成测试分类");
        testCategory.setDescription("用于集成测试的分类");
        testCategory = categoryService.createCategory(testCategory);
    }
    
    @Test
    @DisplayName("完整的图书CRUD操作测试")
    void testCompleteBookCRUD() {
        // Create
        Book newBook = createTestBook();
        Book createdBook = bookService.createBook(newBook);
        
        assertNotNull(createdBook.getId());
        assertEquals(newBook.getTitle(), createdBook.getTitle());
        assertEquals(newBook.getIsbn(), createdBook.getIsbn());
        
        // Read
        Book foundBook = bookService.findById(createdBook.getId());
        assertNotNull(foundBook);
        assertEquals(createdBook.getTitle(), foundBook.getTitle());
        
        // Update
        foundBook.setTitle("更新后的标题");
        foundBook.setPrice(BigDecimal.valueOf(99.99));
        Book updatedBook = bookService.updateBook(foundBook.getId(), foundBook);
        assertEquals("更新后的标题", updatedBook.getTitle());
        assertEquals(BigDecimal.valueOf(99.99), updatedBook.getPrice());
        
        // Delete
        bookService.deleteBook(updatedBook.getId());
        assertThrows(Exception.class, () -> bookService.findById(updatedBook.getId()));
    }
    
    @Test
    @DisplayName("图书借阅归还流程测试")
    void testBookBorrowReturnFlow() {
        // 创建图书
        Book book = createTestBook();
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        Book createdBook = bookService.createBook(book);
        
        // 借阅图书
        boolean borrowed = bookService.borrowBook(createdBook.getId());
        assertTrue(borrowed);
        
        Book borrowedBook = bookService.findById(createdBook.getId());
        assertEquals(2, borrowedBook.getAvailableCopies());
        
        // 再次借阅
        bookService.borrowBook(createdBook.getId());
        bookService.borrowBook(createdBook.getId());
        
        Book fullyBorrowedBook = bookService.findById(createdBook.getId());
        assertEquals(0, fullyBorrowedBook.getAvailableCopies());
        
        // 尝试借阅已无库存的图书
        boolean cannotBorrow = bookService.borrowBook(createdBook.getId());
        assertFalse(cannotBorrow);
        
        // 归还图书
        boolean returned = bookService.returnBook(createdBook.getId());
        assertTrue(returned);
        
        Book returnedBook = bookService.findById(createdBook.getId());
        assertEquals(1, returnedBook.getAvailableCopies());
    }
    
    private Book createTestBook() {
        Book book = new Book();
        book.setIsbn("9787111544937");
        book.setTitle("集成测试图书");
        book.setAuthor("测试作者");
        book.setPublisher("测试出版社");
        book.setPublishDate(LocalDate.now());
        book.setPages(300);
        book.setPrice(BigDecimal.valueOf(59.99));
        book.setCategoryId(testCategory.getId());
        book.setLocation("TEST-001");
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        return book;
    }
}
```

## 6. 性能测试

### 6.1 基准测试

创建`src/test/java/com/demo/library/performance/BookServicePerformanceTest.java`：

```java
package com.demo.library.performance;

import com.demo.library.BaseTest;
import com.demo.library.entity.Book;
import com.demo.library.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图书服务性能测试
 */
@DisplayName("图书服务性能测试")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookServicePerformanceTest extends BaseTest {
    
    @Autowired
    private BookService bookService;
    
    @Test
    @DisplayName("批量查询图书性能测试")
    void testBatchQueryPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 执行100次查询
        for (int i = 0; i < 100; i++) {
            bookService.findBooks(null, null, null, 0, 10);
        }
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        
        System.out.println("100次查询总时间: " + totalTime + "ms");
        System.out.println("平均查询时间: " + (totalTime / 100.0) + "ms");
        
        // 断言平均查询时间小于100ms
        assertTrue(totalTime / 100.0 < 100, "平均查询时间应该小于100ms");
    }
    
    @Test
    @DisplayName("并发查询性能测试")
    void testConcurrentQueryPerformance() {
        int threadCount = 10;
        int operationsPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    bookService.findById(1L);
                }
            }, executor);
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        
        System.out.println("并发执行总时间: " + totalTime + "ms");
        System.out.println("总操作数: " + (threadCount * operationsPerThread));
        
        executor.shutdown();
        
        // 断言总时间小于5秒
        assertTrue(totalTime < 5000, "并发查询总时间应该小于5秒");
    }
}
```

## 7. 测试覆盖率分析

### 7.1 Jacoco配置验证

确保`pom.xml`中的Jacoco插件配置正确：

```xml
<!-- 已在pom.xml中配置 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 7.2 生成覆盖率报告

```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

### 7.3 覆盖率要求配置

创建`src/test/java/com/demo/library/coverage/CoverageTest.java`：

```java
package com.demo.library.coverage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 覆盖率要求测试
 * 
 * 覆盖率目标:
 * - 行覆盖率: >= 80%
 * - 分支覆盖率: >= 70%
 * - 方法覆盖率: >= 85%
 */
@DisplayName("测试覆盖率验证")
class CoverageTest {
    
    @Test
    @DisplayName("验证测试覆盖率达标")
    void verifyCoverageRequirements() {
        // 这个测试主要用于文档记录覆盖率要求
        // 实际的覆盖率检查由Jacoco插件和CI/CD流水线完成
        
        System.out.println("测试覆盖率要求:");
        System.out.println("- 行覆盖率: >= 80%");
        System.out.println("- 分支覆盖率: >= 70%");
        System.out.println("- 方法覆盖率: >= 85%");
        System.out.println("- 类覆盖率: >= 90%");
    }
}
```

## 8. 测试工具类

### 8.1 测试数据工厂

创建`src/test/java/com/demo/library/testutil/TestDataFactory.java`：

```java
package com.demo.library.testutil;

import com.demo.library.entity.Book;
import com.demo.library.entity.Category;
import com.demo.library.entity.User;
import com.demo.library.entity.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * 测试数据工厂
 */
public class TestDataFactory {
    
    public static Book createTestBook() {
        return createTestBook("Test Book", "9787111544937");
    }
    
    public static Book createTestBook(String title, String isbn) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor("Test Author");
        book.setPublisher("Test Publisher");
        book.setPublishDate(LocalDate.of(2020, 1, 1));
        book.setPages(300);
        book.setPrice(BigDecimal.valueOf(59.99));
        book.setCategoryId(1L);
        book.setLocation("TEST-001");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setStatus(Book.BookStatus.AVAILABLE);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        return book;
    }
    
    public static Category createTestCategory() {
        return createTestCategory("Test Category");
    }
    
    public static Category createTestCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription("Test category description");
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
    
    public static User createTestUser() {
        return createTestUser("testuser", "test@example.com");
    }
    
    public static User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setFullName("Test User");
        user.setPhone("13800000000");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRoles(new HashSet<>());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
    
    public static Role createTestRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setDescription("Test role: " + name);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }
}
```

### 8.2 测试断言工具

创建`src/test/java/com/demo/library/testutil/TestAssertions.java`：

```java
package com.demo.library.testutil;

import com.demo.library.entity.Book;
import com.demo.library.entity.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试断言工具类
 */
public class TestAssertions {
    
    /**
     * 断言两个图书对象的主要字段相等
     */
    public static void assertBooksEqual(Book expected, Book actual) {
        assertNotNull(actual);
        assertEquals(expected.getIsbn(), actual.getIsbn());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getAuthor(), actual.getAuthor());
        assertEquals(expected.getPublisher(), actual.getPublisher());
        assertEquals(expected.getPages(), actual.getPages());
        assertEquals(expected.getPrice(), actual.getPrice());
    }
    
    /**
     * 断言两个用户对象的主要字段相等
     */
    public static void assertUsersEqual(User expected, User actual) {
        assertNotNull(actual);
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getFullName(), actual.getFullName());
        assertEquals(expected.getPhone(), actual.getPhone());
        assertEquals(expected.getEnabled(), actual.getEnabled());
    }
    
    /**
     * 断言时间差在指定范围内
     */
    public static void assertTimeWithinRange(java.time.LocalDateTime expected, 
                                           java.time.LocalDateTime actual, 
                                           long maxDifferenceSeconds) {
        assertNotNull(actual);
        long difference = Math.abs(java.time.Duration.between(expected, actual).getSeconds());
        assertTrue(difference <= maxDifferenceSeconds, 
                  "时间差超出预期范围: " + difference + " seconds");
    }
}
```

## 9. Mock测试策略

### 9.1 外部服务Mock

创建`src/test/java/com/demo/library/service/EmailServiceTest.java`：

```java
package com.demo.library.service;

import com.demo.library.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 邮件服务测试（Mock外部依赖）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("邮件服务测试")
class EmailServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    @DisplayName("发送简单邮件")
    void testSendSimpleEmail() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        
        // When
        emailService.sendSimpleEmail(to, subject, content);
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
    
    @Test
    @DisplayName("批量发送邮件")
    void testSendBulkEmail() {
        // Given
        String[] recipients = {"user1@example.com", "user2@example.com"};
        String subject = "Bulk Email";
        String content = "Bulk Content";
        
        // When
        emailService.sendBulkEmail(recipients, subject, content);
        
        // Then
        verify(mailSender, times(recipients.length)).send(any(SimpleMailMessage.class));
    }
    
    @Test
    @DisplayName("邮件发送失败处理")
    void testEmailSendingFailure() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));
        
        // When & Then
        // 应该捕获异常并记录日志，不应该让异常传播
        try {
            emailService.sendSimpleEmail("test@example.com", "Subject", "Content");
        } catch (Exception e) {
            fail("邮件发送失败应该被妥善处理，不应该抛出异常");
        }
        
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
```

## 10. CI/CD集成测试

### 10.1 GitHub Actions配置

创建`.github/workflows/test.yml`：

```yaml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test
          POSTGRES_USER: test
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Run tests
      run: mvn clean test -Dspring.profiles.active=test
    
    - name: Generate test report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: true
    
    - name: Publish test results
      uses: dorny/test-reporter@v1
      if: always()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit
```

### 10.2 测试脚本

创建`scripts/run-tests.sh`：

```bash
#!/bin/bash

# 运行测试脚本
set -e

echo "开始运行测试..."

# 清理
echo "清理之前的构建..."
mvn clean

# 编译
echo "编译项目..."
mvn compile test-compile

# 运行单元测试
echo "运行单元测试..."
mvn test -Dtest=*Test

# 运行集成测试
echo "运行集成测试..."
mvn test -Dtest=*IntegrationTest

# 生成测试报告
echo "生成测试报告..."
mvn jacoco:report

# 检查覆盖率
echo "检查测试覆盖率..."
mvn jacoco:check

echo "测试完成！"
echo "测试报告: target/site/jacoco/index.html"
echo "单元测试结果: target/surefire-reports/"
```

## 11. 测试最佳实践

### 11.1 命名规范
- 测试类：`{被测试类}Test` 或 `{被测试类}IntegrationTest`
- 测试方法：`test{方法名}_{场景}` 或使用`@DisplayName`
- 测试数据：使用有意义的测试数据，避免魔法数字

### 11.2 测试结构（AAA模式）
```java
@Test
void testMethodName_Scenario() {
    // Arrange (Given) - 准备测试数据和环境
    // ...
    
    // Act (When) - 执行被测试的方法
    // ...
    
    // Assert (Then) - 验证结果
    // ...
}
```

### 11.3 测试隔离性
- 每个测试应该独立运行
- 使用`@Transactional`回滚数据库更改
- 清理测试产生的副作用

### 11.4 测试数据管理
- 使用工厂方法创建测试数据
- 避免测试之间的数据依赖
- 使用内存数据库进行快速测试

## 12. 常见问题解决

### 12.1 测试端口冲突
```yaml
# 使用随机端口
server:
  port: 0
```

### 12.2 数据库连接问题
```yaml
# 测试配置中使用内存数据库
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
```

### 12.3 Mock对象不生效
```java
// 确保使用正确的Mock注解
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
}
```

### 12.4 Testcontainers启动失败
```java
// 检查Docker是否运行
// 使用较小的镜像版本
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");
```

## 13. 总结

通过本步骤，我们完成了：

✅ **测试框架搭建**
- JUnit 5单元测试框架配置
- Mockito Mock测试框架集成
- Spring Boot Test集成测试支持

✅ **全面的测试覆盖**
- Service层单元测试
- Controller层集成测试
- Repository层数据库测试
- 工具类和组件测试

✅ **高级测试技术**
- Testcontainers容器化测试
- MockMvc Web层测试
- 性能和并发测试

✅ **测试质量保障**
- 测试覆盖率分析
- CI/CD集成测试
- 测试最佳实践规范

## 下一步

单元测试与集成测试完成后，我们将在[Step 12](step12.md)中学习项目打包与部署，包括：
- Maven多环境打包配置
- Docker容器化部署
- 生产环境配置优化
- 性能调优和监控
- 运维和故障排查

---

**测试金句**：
1. "测试不是为了证明没有bug，而是为了发现bug"
2. "好的测试是开发者的安全网"
3. "测试代码也是代码，需要维护和重构"
4. "覆盖率不是目标，质量才是"
5. "快速失败，早期发现问题"