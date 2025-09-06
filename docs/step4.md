# Step 4: 实体类设计与数据库表创建

## 学习目标

通过本步骤，你将学会：
- 分析业务需求，设计实体关系
- 创建符合规范的数据库表结构
- 编写Java实体类和数据传输对象
- 理解实体类与数据库表的映射关系
- 创建数据库初始化脚本

## 1. 业务需求分析

### 1.1 图书管理系统核心功能

**主要业务实体：**
- **图书（Book）**：系统核心实体，包含图书基本信息
- **用户（User）**：系统使用者，包括管理员和普通用户
- **借阅记录（BorrowRecord）**：记录用户借阅图书的信息

**业务关系：**
- 一个用户可以借阅多本图书（一对多）
- 一本图书可以被多个用户借阅过（一对多，通过借阅记录）
- 借阅记录连接用户和图书（多对多关系的中间表）

### 1.2 数据库设计原则

- **第一范式（1NF）**：每个字段都是原子的，不可再分
- **第二范式（2NF）**：在1NF基础上，非键字段完全依赖于主键
- **第三范式（3NF）**：在2NF基础上，非键字段不传递依赖于主键
- **BCNF范式**：消除所有函数依赖的左部非键属性

## 2. 数据库表结构设计

### 2.1 用户表（users）

```sql
-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,                    -- 用户ID，主键，自增
    username VARCHAR(50) UNIQUE NOT NULL,       -- 用户名，唯一
    password VARCHAR(100) NOT NULL,             -- 密码（加密存储）
    email VARCHAR(100) UNIQUE,                  -- 邮箱，唯一
    full_name VARCHAR(100) NOT NULL,            -- 真实姓名
    phone VARCHAR(20),                          -- 电话号码
    address TEXT,                               -- 地址
    role VARCHAR(20) DEFAULT 'USER',            -- 角色：ADMIN, USER
    status VARCHAR(20) DEFAULT 'ACTIVE',        -- 状态：ACTIVE, INACTIVE, LOCKED
    avatar_url VARCHAR(200),                    -- 头像URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    last_login_at TIMESTAMP,                    -- 最后登录时间
    login_count INTEGER DEFAULT 0,             -- 登录次数
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
```

### 2.2 图书表（books）

```sql
-- 图书表
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,                   -- 图书ID，主键，自增
    isbn VARCHAR(20) UNIQUE,                    -- ISBN号码，唯一
    title VARCHAR(200) NOT NULL,               -- 书名
    author VARCHAR(100) NOT NULL,              -- 作者
    publisher VARCHAR(100),                     -- 出版社
    publish_date DATE,                          -- 出版日期
    category VARCHAR(50),                       -- 分类
    description TEXT,                           -- 图书描述
    cover_image_url VARCHAR(200),              -- 封面图片URL
    price DECIMAL(10,2),                       -- 价格
    total_copies INTEGER DEFAULT 1,            -- 总册数
    available_copies INTEGER DEFAULT 1,        -- 可借册数
    language VARCHAR(20) DEFAULT 'zh-CN',      -- 语言
    page_count INTEGER,                         -- 页数
    status VARCHAR(20) DEFAULT 'AVAILABLE',    -- 状态：AVAILABLE, UNAVAILABLE, MAINTENANCE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    created_by BIGINT,                         -- 创建人ID
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies),
    CONSTRAINT chk_book_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE')),
    CONSTRAINT fk_books_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 创建索引
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_category ON books(category);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_publish_date ON books(publish_date);
```

### 2.3 借阅记录表（borrow_records）

```sql
-- 借阅记录表
CREATE TABLE borrow_records (
    id BIGSERIAL PRIMARY KEY,                   -- 记录ID，主键，自增
    user_id BIGINT NOT NULL,                   -- 借阅用户ID
    book_id BIGINT NOT NULL,                   -- 借阅图书ID
    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 借阅日期
    due_date TIMESTAMP NOT NULL,               -- 应还日期
    return_date TIMESTAMP,                      -- 实际还书日期
    status VARCHAR(20) DEFAULT 'BORROWED',      -- 状态：BORROWED, RETURNED, OVERDUE
    fine_amount DECIMAL(10,2) DEFAULT 0.00,   -- 罚金
    notes TEXT,                                 -- 备注
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    CONSTRAINT chk_borrow_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE')),
    CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_borrow_user_id ON borrow_records(user_id);
CREATE INDEX idx_borrow_book_id ON borrow_records(book_id);
CREATE INDEX idx_borrow_status ON borrow_records(status);
CREATE INDEX idx_borrow_due_date ON borrow_records(due_date);
CREATE INDEX idx_borrow_return_date ON borrow_records(return_date);

-- 创建复合索引
CREATE INDEX idx_borrow_user_status ON borrow_records(user_id, status);
CREATE INDEX idx_borrow_book_status ON borrow_records(book_id, status);
```

### 2.4 创建数据库初始化脚本

创建`src/main/resources/db/schema.sql`：

```sql
-- 图书管理系统数据库初始化脚本
-- 支持OpenGauss数据库

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(20) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    avatar_url VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    login_count INTEGER DEFAULT 0,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

-- 创建图书表
CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    publisher VARCHAR(100),
    publish_date DATE,
    category VARCHAR(50),
    description TEXT,
    cover_image_url VARCHAR(200),
    price DECIMAL(10,2),
    total_copies INTEGER DEFAULT 1,
    available_copies INTEGER DEFAULT 1,
    language VARCHAR(20) DEFAULT 'zh-CN',
    page_count INTEGER,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies),
    CONSTRAINT chk_book_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE')),
    CONSTRAINT fk_books_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 创建借阅记录表
CREATE TABLE IF NOT EXISTS borrow_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'BORROWED',
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_borrow_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE')),
    CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_books_author ON books(author);
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);
CREATE INDEX IF NOT EXISTS idx_books_status ON books(status);

CREATE INDEX IF NOT EXISTS idx_borrow_user_id ON borrow_records(user_id);
CREATE INDEX IF NOT EXISTS idx_borrow_book_id ON borrow_records(book_id);
CREATE INDEX IF NOT EXISTS idx_borrow_status ON borrow_records(status);
CREATE INDEX IF NOT EXISTS idx_borrow_due_date ON borrow_records(due_date);
```

## 3. Java实体类设计

### 3.1 基础实体类

创建`src/main/java/com/demo/library/entity/BaseEntity.java`：

```java
package com.demo.library.entity;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 包含所有实体的公共字段
 */
public abstract class BaseEntity {
    
    protected Long id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    
    public BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * 更新时自动设置更新时间
     */
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### 3.2 用户实体类

创建`src/main/java/com/demo/library/entity/User.java`：

```java
package com.demo.library.entity;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
public class User extends BaseEntity {
    
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private UserRole role;
    private UserStatus status;
    private String avatarUrl;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    
    public User() {
        super();
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.loginCount = 0;
    }
    
    // 用户角色枚举
    public enum UserRole {
        ADMIN("ADMIN", "管理员"),
        USER("USER", "普通用户");
        
        private final String code;
        private final String description;
        
        UserRole(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // 用户状态枚举
    public enum UserStatus {
        ACTIVE("ACTIVE", "激活"),
        INACTIVE("INACTIVE", "未激活"),
        LOCKED("LOCKED", "锁定");
        
        private final String code;
        private final String description;
        
        UserStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}
```

### 3.3 图书实体类

创建`src/main/java/com/demo/library/entity/Book.java`：

```java
package com.demo.library.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书实体类
 */
public class Book extends BaseEntity {
    
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String category;
    private String description;
    private String coverImageUrl;
    private BigDecimal price;
    private Integer totalCopies;
    private Integer availableCopies;
    private String language;
    private Integer pageCount;
    private BookStatus status;
    private Long createdBy;
    
    public Book() {
        super();
        this.totalCopies = 1;
        this.availableCopies = 1;
        this.language = "zh-CN";
        this.status = BookStatus.AVAILABLE;
    }
    
    // 图书状态枚举
    public enum BookStatus {
        AVAILABLE("AVAILABLE", "可借阅"),
        UNAVAILABLE("UNAVAILABLE", "不可借阅"),
        MAINTENANCE("MAINTENANCE", "维护中");
        
        private final String code;
        private final String description;
        
        BookStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // 业务方法
    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE && availableCopies > 0;
    }
    
    public void borrowCopy() {
        if (availableCopies > 0) {
            availableCopies--;
        }
    }
    
    public void returnCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }
    
    // Getters and Setters
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
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    
    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", status=" + status +
                ", availableCopies=" + availableCopies +
                '}';
    }
}
```

### 3.4 借阅记录实体类

创建`src/main/java/com/demo/library/entity/BorrowRecord.java`：

```java
package com.demo.library.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 借阅记录实体类
 */
public class BorrowRecord extends BaseEntity {
    
    private Long userId;
    private Long bookId;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private BorrowStatus status;
    private BigDecimal fineAmount;
    private String notes;
    
    // 关联对象（用于查询时填充）
    private User user;
    private Book book;
    
    public BorrowRecord() {
        super();
        this.borrowDate = LocalDateTime.now();
        this.status = BorrowStatus.BORROWED;
        this.fineAmount = BigDecimal.ZERO;
    }
    
    // 借阅状态枚举
    public enum BorrowStatus {
        BORROWED("BORROWED", "已借出"),
        RETURNED("RETURNED", "已归还"),
        OVERDUE("OVERDUE", "已逾期");
        
        private final String code;
        private final String description;
        
        BorrowStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // 业务方法
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDate) && status == BorrowStatus.BORROWED;
    }
    
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDateTime.now());
    }
    
    public BigDecimal calculateFine(BigDecimal dailyFineRate) {
        long overdueDays = getDaysOverdue();
        return dailyFineRate.multiply(BigDecimal.valueOf(overdueDays));
    }
    
    public void returnBook() {
        this.returnDate = LocalDateTime.now();
        this.status = BorrowStatus.RETURNED;
        this.preUpdate();
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    
    public BorrowStatus getStatus() { return status; }
    public void setStatus(BorrowStatus status) { this.status = status; }
    
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    
    @Override
    public String toString() {
        return "BorrowRecord{" +
                "id=" + id +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", status=" + status +
                '}';
    }
}
```

## 4. 数据传输对象（DTO）设计

### 4.1 用户DTO

创建`src/main/java/com/demo/library/dto/UserDTO.java`：

```java
package com.demo.library.dto;

import com.demo.library.entity.User;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 用于前端交互，不包含敏感信息
 */
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String role;
    private String status;
    private String avatarUrl;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    private LocalDateTime createdAt;
    
    public UserDTO() {}
    
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.role = user.getRole().getCode();
        this.status = user.getStatus().getCode();
        this.avatarUrl = user.getAvatarUrl();
        this.lastLoginAt = user.getLastLoginAt();
        this.loginCount = user.getLoginCount();
        this.createdAt = user.getCreatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### 4.2 图书DTO

创建`src/main/java/com/demo/library/dto/BookDTO.java`：

```java
package com.demo.library.dto;

import com.demo.library.entity.Book;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书数据传输对象
 */
public class BookDTO {
    
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String category;
    private String description;
    private String coverImageUrl;
    private BigDecimal price;
    private Integer totalCopies;
    private Integer availableCopies;
    private String language;
    private Integer pageCount;
    private String status;
    private LocalDateTime createdAt;
    private boolean available;  // 计算属性
    
    public BookDTO() {}
    
    public BookDTO(Book book) {
        this.id = book.getId();
        this.isbn = book.getIsbn();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.publisher = book.getPublisher();
        this.publishDate = book.getPublishDate();
        this.category = book.getCategory();
        this.description = book.getDescription();
        this.coverImageUrl = book.getCoverImageUrl();
        this.price = book.getPrice();
        this.totalCopies = book.getTotalCopies();
        this.availableCopies = book.getAvailableCopies();
        this.language = book.getLanguage();
        this.pageCount = book.getPageCount();
        this.status = book.getStatus().getCode();
        this.createdAt = book.getCreatedAt();
        this.available = book.isAvailable();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
```

## 5. 执行数据库初始化

### 5.1 连接数据库执行脚本

```bash
# 使用gsql连接数据库
gsql -d library_db -U library_user -h localhost -p 5432

# 执行初始化脚本
\i /path/to/schema.sql

# 查看创建的表
\dt

# 查看表结构
\d users
\d books  
\d borrow_records
```

### 5.2 创建测试数据脚本

创建`src/main/resources/db/data.sql`：

```sql
-- 测试数据脚本

-- 插入管理员用户
INSERT INTO users (username, password, email, full_name, role, status) 
VALUES ('admin', '$2a$10$HASH', 'admin@library.com', '系统管理员', 'ADMIN', 'ACTIVE');

-- 插入普通用户
INSERT INTO users (username, password, email, full_name, phone, role, status) VALUES 
('zhangsan', '$2a$10$HASH', 'zhangsan@example.com', '张三', '13800138001', 'USER', 'ACTIVE'),
('lisi', '$2a$10$HASH', 'lisi@example.com', '李四', '13800138002', 'USER', 'ACTIVE'),
('wangwu', '$2a$10$HASH', 'wangwu@example.com', '王五', '13800138003', 'USER', 'ACTIVE');

-- 插入图书数据
INSERT INTO books (isbn, title, author, publisher, publish_date, category, description, price, total_copies, available_copies) VALUES 
('9787111547976', 'Java核心技术', 'Cay S. Horstmann', '机械工业出版社', '2020-01-01', '计算机', 'Java编程经典教材', 89.00, 5, 5),
('9787115428028', 'Spring Boot实战', 'Craig Walls', '人民邮电出版社', '2019-06-01', '计算机', 'Spring Boot开发指南', 79.00, 3, 3),
('9787121366703', 'MySQL技术内幕', 'Baron Schwartz', '电子工业出版社', '2018-05-01', '数据库', 'MySQL性能优化', 108.00, 2, 2),
('9787040396638', '数据结构与算法', 'Thomas H. Cormen', '高等教育出版社', '2021-03-01', '计算机', '算法导论', 128.00, 4, 4),
('9787302473329', 'Python编程', 'Eric Matthes', '清华大学出版社', '2020-08-01', '计算机', 'Python入门教程', 69.00, 6, 6);
```

## 6. 测试实体类功能

### 6.1 创建实体类测试

创建`src/test/java/com/demo/library/entity/EntityTest.java`：

```java
package com.demo.library.entity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EntityTest {
    
    @Test
    public void testUserEntity() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("测试用户");
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.ACTIVE);
        
        assertEquals("testuser", user.getUsername());
        assertEquals(User.UserRole.USER, user.getRole());
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
    }
    
    @Test
    public void testBookEntity() {
        Book book = new Book();
        book.setTitle("测试图书");
        book.setAuthor("测试作者");
        book.setPrice(new BigDecimal("99.99"));
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        
        assertTrue(book.isAvailable());
        
        // 测试借书
        book.borrowCopy();
        assertEquals(Integer.valueOf(4), book.getAvailableCopies());
        
        // 测试还书
        book.returnCopy();
        assertEquals(Integer.valueOf(5), book.getAvailableCopies());
    }
    
    @Test
    public void testBorrowRecord() {
        BorrowRecord record = new BorrowRecord();
        record.setUserId(1L);
        record.setBookId(1L);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().minusDays(1)); // 设置为昨天到期
        
        assertTrue(record.isOverdue());
        assertEquals(1, record.getDaysOverdue());
        
        BigDecimal fine = record.calculateFine(new BigDecimal("1.00"));
        assertEquals(new BigDecimal("1.00"), fine);
    }
}
```

## 7. 总结

通过本步骤学习，你已经完成：

✅ **数据库设计**
- 完整的表结构设计
- 合理的索引和约束
- 符合范式的关系设计

✅ **实体类开发**
- 基础实体类抽象
- 业务实体类实现
- 枚举类型定义

✅ **DTO设计**
- 数据传输对象
- 实体转换逻辑
- 前端交互格式

✅ **数据库初始化**
- 建表脚本
- 测试数据
- 结构验证

## 下一步

实体设计完成后，我们将在[Step 5](step5.md)中学习MyBatis Mapper接口与SQL映射开发，包括：
- Mapper接口定义
- XML映射文件编写
- 复杂查询实现
- 动态SQL使用

---

**提示**：确保数据库表创建成功，实体类测试通过，这为后续的数据访问层开发奠定了基础。