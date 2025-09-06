# Step 5: MyBatis Mapper接口与SQL映射开发

## 学习目标

通过本步骤，你将学会：
- MyBatis框架的核心概念和工作原理
- 创建Mapper接口和XML映射文件
- 编写复杂SQL查询和动态SQL
- 实现分页查询和结果映射
- MyBatis与Spring Boot的集成配置
- 数据访问层的最佳实践

## 前置要求

确保已完成：
- Step 1: 环境准备与OpenGauss安装
- Step 2: Spring Boot基础配置
- Step 3: MyBatis与OpenGauss集成
- Step 4: 实体类设计与数据库表创建

## 1. MyBatis基础概念

### 1.1 MyBatis核心组件

**SqlSessionFactory：** MyBatis的核心，负责创建SqlSession
**SqlSession：** 执行SQL操作的会话
**Mapper接口：** 定义数据访问方法的接口
**XML映射文件：** 包含SQL语句和结果映射配置

### 1.2 工作流程

```
1. 加载配置 → 2. 创建SqlSessionFactory → 3. 创建SqlSession
4. 获取Mapper → 5. 执行方法 → 6. 返回结果 → 7. 关闭会话
```

## 2. 配置MyBatis映射文件路径

### 2.1 更新application.yml

编辑`src/main/resources/application.yml`，添加MyBatis配置：

```yaml
# MyBatis配置
mybatis:
  # 映射文件位置
  mapper-locations: classpath:mapper/*.xml
  # 实体类包路径
  type-aliases-package: com.demo.library.entity
  configuration:
    # 开启驼峰命名转换
    map-underscore-to-camel-case: true
    # 开启二级缓存
    cache-enabled: true
    # 延迟加载全局开关
    lazy-loading-enabled: true
    # 延迟加载触发方法
    aggressive-lazy-loading: false
    # 是否允许单条sql返回多个数据集
    multiple-result-sets-enabled: true
    # 使用列标签代替列名
    use-column-label: true
    # 允许JDBC生成主键
    use-generated-keys: true
    # 默认执行器类型
    default-executor-type: reuse
    # 超时时间
    default-statement-timeout: 25000
    # 日志实现
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 分页插件配置
pagehelper:
  # 指定数据库方言
  helper-dialect: postgresql
  # 分页合理化
  reasonable: true
  # 支持通过Mapper接口参数来传递分页参数
  support-methods-arguments: true
  # 分页参数
  params: count=countSql
```

### 2.2 创建映射文件目录

```bash
mkdir -p src/main/resources/mapper
```

## 3. 用户模块Mapper开发

### 3.1 创建UserMapper接口

创建`src/main/java/com/demo/library/mapper/UserMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.User;
import com.demo.library.dto.UserQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 用户数据访问层接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") Long id);
    
    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);
    
    /**
     * 查询所有用户
     */
    List<User> selectAll();
    
    /**
     * 条件查询用户
     */
    List<User> selectByCondition(UserQueryDTO queryDTO);
    
    /**
     * 分页查询用户
     */
    List<User> selectByPage(@Param("offset") int offset, 
                           @Param("limit") int limit,
                           @Param("queryDTO") UserQueryDTO queryDTO);
    
    /**
     * 统计用户总数
     */
    Long countAll();
    
    /**
     * 根据条件统计用户数量
     */
    Long countByCondition(UserQueryDTO queryDTO);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 批量插入用户
     */
    int insertBatch(@Param("users") List<User> users);
    
    /**
     * 更新用户信息
     */
    int updateById(User user);
    
    /**
     * 选择性更新用户信息
     */
    int updateByIdSelective(User user);
    
    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    /**
     * 增加登录次数
     */
    int incrementLoginCount(@Param("id") Long id);
    
    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 批量删除用户
     */
    int deleteBatch(@Param("ids") List<Long> ids);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * 查询活跃用户（最近30天有登录）
     */
    List<User> selectActiveUsers(@Param("days") int days);
    
    /**
     * 根据角色查询用户
     */
    List<User> selectByRole(@Param("role") String role);
}
```

### 3.2 创建UserQueryDTO

创建`src/main/java/com/demo/library/dto/UserQueryDTO.java`：

```java
package com.demo.library.dto;

import java.time.LocalDateTime;

/**
 * 用户查询数据传输对象
 */
public class UserQueryDTO {
    
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime createdAtStart;
    private LocalDateTime createdAtEnd;
    private LocalDateTime lastLoginAtStart;
    private LocalDateTime lastLoginAtEnd;
    private String keyword; // 关键词搜索
    
    // 构造器
    public UserQueryDTO() {}
    
    // Getter和Setter方法
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAtStart() {
        return createdAtStart;
    }
    
    public void setCreatedAtStart(LocalDateTime createdAtStart) {
        this.createdAtStart = createdAtStart;
    }
    
    public LocalDateTime getCreatedAtEnd() {
        return createdAtEnd;
    }
    
    public void setCreatedAtEnd(LocalDateTime createdAtEnd) {
        this.createdAtEnd = createdAtEnd;
    }
    
    public LocalDateTime getLastLoginAtStart() {
        return lastLoginAtStart;
    }
    
    public void setLastLoginAtStart(LocalDateTime lastLoginAtStart) {
        this.lastLoginAtStart = lastLoginAtStart;
    }
    
    public LocalDateTime getLastLoginAtEnd() {
        return lastLoginAtEnd;
    }
    
    public void setLastLoginAtEnd(LocalDateTime lastLoginAtEnd) {
        this.lastLoginAtEnd = lastLoginAtEnd;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    @Override
    public String toString() {
        return "UserQueryDTO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", createdAtStart=" + createdAtStart +
                ", createdAtEnd=" + createdAtEnd +
                ", lastLoginAtStart=" + lastLoginAtStart +
                ", lastLoginAtEnd=" + lastLoginAtEnd +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
```

### 3.3 创建UserMapper.xml映射文件

创建`src/main/resources/mapper/UserMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.demo.library.mapper.UserMapper">
    
    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.demo.library.entity.User">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="username" property="username" jdbcType="VARCHAR"/>
        <result column="password" property="password" jdbcType="VARCHAR"/>
        <result column="email" property="email" jdbcType="VARCHAR"/>
        <result column="full_name" property="fullName" jdbcType="VARCHAR"/>
        <result column="phone" property="phone" jdbcType="VARCHAR"/>
        <result column="address" property="address" jdbcType="VARCHAR"/>
        <result column="role" property="role" jdbcType="VARCHAR"/>
        <result column="status" property="status" jdbcType="VARCHAR"/>
        <result column="avatar_url" property="avatarUrl" jdbcType="VARCHAR"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
        <result column="last_login_at" property="lastLoginAt" jdbcType="TIMESTAMP"/>
        <result column="login_count" property="loginCount" jdbcType="INTEGER"/>
    </resultMap>
    
    <!-- 基础列 -->
    <sql id="Base_Column_List">
        id, username, password, email, full_name, phone, address, role, status, 
        avatar_url, created_at, updated_at, last_login_at, login_count
    </sql>
    
    <!-- 查询条件 -->
    <sql id="Query_Condition">
        <where>
            <if test="username != null and username != ''">
                AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="email != null and email != ''">
                AND email LIKE CONCAT('%', #{email}, '%')
            </if>
            <if test="fullName != null and fullName != ''">
                AND full_name LIKE CONCAT('%', #{fullName}, '%')
            </if>
            <if test="phone != null and phone != ''">
                AND phone LIKE CONCAT('%', #{phone}, '%')
            </if>
            <if test="role != null and role != ''">
                AND role = #{role}
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="createdAtStart != null">
                AND created_at >= #{createdAtStart}
            </if>
            <if test="createdAtEnd != null">
                AND created_at &lt;= #{createdAtEnd}
            </if>
            <if test="lastLoginAtStart != null">
                AND last_login_at >= #{lastLoginAtStart}
            </if>
            <if test="lastLoginAtEnd != null">
                AND last_login_at &lt;= #{lastLoginAtEnd}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (username LIKE CONCAT('%', #{keyword}, '%') 
                     OR full_name LIKE CONCAT('%', #{keyword}, '%')
                     OR email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
        </where>
    </sql>
    
    <!-- 根据ID查询用户 -->
    <select id="selectById" parameterType="java.lang.Long" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users 
        WHERE id = #{id}
    </select>
    
    <!-- 根据用户名查询用户 -->
    <select id="selectByUsername" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users 
        WHERE username = #{username}
    </select>
    
    <!-- 根据邮箱查询用户 -->
    <select id="selectByEmail" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users 
        WHERE email = #{email}
    </select>
    
    <!-- 查询所有用户 -->
    <select id="selectAll" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users
        ORDER BY created_at DESC
    </select>
    
    <!-- 条件查询用户 -->
    <select id="selectByCondition" parameterType="com.demo.library.dto.UserQueryDTO" 
            resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users
        <include refid="Query_Condition"/>
        ORDER BY created_at DESC
    </select>
    
    <!-- 分页查询用户 -->
    <select id="selectByPage" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users
        <if test="queryDTO != null">
            <where>
                <if test="queryDTO.username != null and queryDTO.username != ''">
                    AND username LIKE CONCAT('%', #{queryDTO.username}, '%')
                </if>
                <if test="queryDTO.email != null and queryDTO.email != ''">
                    AND email LIKE CONCAT('%', #{queryDTO.email}, '%')
                </if>
                <if test="queryDTO.fullName != null and queryDTO.fullName != ''">
                    AND full_name LIKE CONCAT('%', #{queryDTO.fullName}, '%')
                </if>
                <if test="queryDTO.role != null and queryDTO.role != ''">
                    AND role = #{queryDTO.role}
                </if>
                <if test="queryDTO.status != null and queryDTO.status != ''">
                    AND status = #{queryDTO.status}
                </if>
                <if test="queryDTO.keyword != null and queryDTO.keyword != ''">
                    AND (username LIKE CONCAT('%', #{queryDTO.keyword}, '%') 
                         OR full_name LIKE CONCAT('%', #{queryDTO.keyword}, '%')
                         OR email LIKE CONCAT('%', #{queryDTO.keyword}, '%'))
                </if>
            </where>
        </if>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
    
    <!-- 统计用户总数 -->
    <select id="countAll" resultType="java.lang.Long">
        SELECT COUNT(*) FROM users
    </select>
    
    <!-- 根据条件统计用户数量 -->
    <select id="countByCondition" parameterType="com.demo.library.dto.UserQueryDTO" 
            resultType="java.lang.Long">
        SELECT COUNT(*) FROM users
        <include refid="Query_Condition"/>
    </select>
    
    <!-- 插入用户 -->
    <insert id="insert" parameterType="com.demo.library.entity.User" 
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (
            username, password, email, full_name, phone, address, role, status,
            avatar_url, created_at, updated_at, login_count
        ) VALUES (
            #{username}, #{password}, #{email}, #{fullName}, #{phone}, #{address}, 
            #{role}, #{status}, #{avatarUrl}, #{createdAt}, #{updatedAt}, #{loginCount}
        )
    </insert>
    
    <!-- 批量插入用户 -->
    <insert id="insertBatch" parameterType="java.util.List">
        INSERT INTO users (
            username, password, email, full_name, phone, address, role, status,
            avatar_url, created_at, updated_at, login_count
        ) VALUES
        <foreach collection="users" item="user" separator=",">
        (
            #{user.username}, #{user.password}, #{user.email}, #{user.fullName}, 
            #{user.phone}, #{user.address}, #{user.role}, #{user.status},
            #{user.avatarUrl}, #{user.createdAt}, #{user.updatedAt}, #{user.loginCount}
        )
        </foreach>
    </insert>
    
    <!-- 更新用户信息 -->
    <update id="updateById" parameterType="com.demo.library.entity.User">
        UPDATE users SET
            username = #{username},
            password = #{password},
            email = #{email},
            full_name = #{fullName},
            phone = #{phone},
            address = #{address},
            role = #{role},
            status = #{status},
            avatar_url = #{avatarUrl},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    </update>
    
    <!-- 选择性更新用户信息 -->
    <update id="updateByIdSelective" parameterType="com.demo.library.entity.User">
        UPDATE users
        <set>
            <if test="username != null and username != ''">
                username = #{username},
            </if>
            <if test="password != null and password != ''">
                password = #{password},
            </if>
            <if test="email != null and email != ''">
                email = #{email},
            </if>
            <if test="fullName != null and fullName != ''">
                full_name = #{fullName},
            </if>
            <if test="phone != null and phone != ''">
                phone = #{phone},
            </if>
            <if test="address != null and address != ''">
                address = #{address},
            </if>
            <if test="role != null and role != ''">
                role = #{role},
            </if>
            <if test="status != null and status != ''">
                status = #{status},
            </if>
            <if test="avatarUrl != null and avatarUrl != ''">
                avatar_url = #{avatarUrl},
            </if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
    </update>
    
    <!-- 更新用户状态 -->
    <update id="updateStatus">
        UPDATE users SET 
            status = #{status},
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 更新最后登录时间 -->
    <update id="updateLastLoginTime">
        UPDATE users SET 
            last_login_at = #{lastLoginAt},
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 增加登录次数 -->
    <update id="incrementLoginCount">
        UPDATE users SET 
            login_count = login_count + 1,
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 根据ID删除用户 -->
    <delete id="deleteById" parameterType="java.lang.Long">
        DELETE FROM users WHERE id = #{id}
    </delete>
    
    <!-- 批量删除用户 -->
    <delete id="deleteBatch" parameterType="java.util.List">
        DELETE FROM users WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </delete>
    
    <!-- 检查用户名是否存在 -->
    <select id="existsByUsername" parameterType="java.lang.String" resultType="java.lang.Boolean">
        SELECT COUNT(*) > 0 FROM users WHERE username = #{username}
    </select>
    
    <!-- 检查邮箱是否存在 -->
    <select id="existsByEmail" parameterType="java.lang.String" resultType="java.lang.Boolean">
        SELECT COUNT(*) > 0 FROM users WHERE email = #{email}
    </select>
    
    <!-- 查询活跃用户 -->
    <select id="selectActiveUsers" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users
        WHERE last_login_at >= NOW() - INTERVAL '${days}' DAY
        ORDER BY last_login_at DESC
    </select>
    
    <!-- 根据角色查询用户 -->
    <select id="selectByRole" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM users
        WHERE role = #{role}
        ORDER BY created_at DESC
    </select>
    
</mapper>
```

## 4. 图书模块Mapper开发

### 4.1 创建BookMapper接口

创建`src/main/java/com/demo/library/mapper/BookMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.Book;
import com.demo.library.dto.BookQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.math.BigDecimal;

/**
 * 图书数据访问层接口
 */
@Mapper
public interface BookMapper {
    
    /**
     * 根据ID查询图书
     */
    Book selectById(@Param("id") Long id);
    
    /**
     * 根据ISBN查询图书
     */
    Book selectByIsbn(@Param("isbn") String isbn);
    
    /**
     * 查询所有图书
     */
    List<Book> selectAll();
    
    /**
     * 条件查询图书
     */
    List<Book> selectByCondition(BookQueryDTO queryDTO);
    
    /**
     * 分页查询图书
     */
    List<Book> selectByPage(@Param("offset") int offset, 
                           @Param("limit") int limit,
                           @Param("queryDTO") BookQueryDTO queryDTO);
    
    /**
     * 统计图书总数
     */
    Long countAll();
    
    /**
     * 根据条件统计图书数量
     */
    Long countByCondition(BookQueryDTO queryDTO);
    
    /**
     * 插入图书
     */
    int insert(Book book);
    
    /**
     * 批量插入图书
     */
    int insertBatch(@Param("books") List<Book> books);
    
    /**
     * 更新图书信息
     */
    int updateById(Book book);
    
    /**
     * 选择性更新图书信息
     */
    int updateByIdSelective(Book book);
    
    /**
     * 更新图书可借数量
     */
    int updateAvailableCopies(@Param("id") Long id, @Param("availableCopies") Integer availableCopies);
    
    /**
     * 增加图书可借数量
     */
    int increaseAvailableCopies(@Param("id") Long id, @Param("count") Integer count);
    
    /**
     * 减少图书可借数量
     */
    int decreaseAvailableCopies(@Param("id") Long id, @Param("count") Integer count);
    
    /**
     * 更新图书状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 根据ID删除图书
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 批量删除图书
     */
    int deleteBatch(@Param("ids") List<Long> ids);
    
    /**
     * 检查ISBN是否存在
     */
    boolean existsByIsbn(@Param("isbn") String isbn);
    
    /**
     * 根据分类查询图书
     */
    List<Book> selectByCategory(@Param("category") String category);
    
    /**
     * 根据作者查询图书
     */
    List<Book> selectByAuthor(@Param("author") String author);
    
    /**
     * 根据出版社查询图书
     */
    List<Book> selectByPublisher(@Param("publisher") String publisher);
    
    /**
     * 查询可借阅的图书
     */
    List<Book> selectAvailableBooks();
    
    /**
     * 查询热门图书（根据借阅次数）
     */
    List<Book> selectPopularBooks(@Param("limit") Integer limit);
    
    /**
     * 查询新上架图书
     */
    List<Book> selectNewBooks(@Param("days") Integer days, @Param("limit") Integer limit);
    
    /**
     * 按价格区间查询图书
     */
    List<Book> selectByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                 @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * 全文搜索图书
     */
    List<Book> searchBooks(@Param("keyword") String keyword);
    
    /**
     * 获取所有分类
     */
    List<String> selectAllCategories();
    
    /**
     * 获取分类统计
     */
    List<BookCategoryStats> selectCategoryStats();
}
```

### 4.2 创建BookQueryDTO和BookCategoryStats

创建`src/main/java/com/demo/library/dto/BookQueryDTO.java`：

```java
package com.demo.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书查询数据传输对象
 */
public class BookQueryDTO {
    
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String category;
    private String language;
    private String status;
    private LocalDate publishDateStart;
    private LocalDate publishDateEnd;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Integer pageCountMin;
    private Integer pageCountMax;
    private String keyword; // 关键词搜索
    private Boolean availableOnly; // 仅查询可借阅图书
    
    // 构造器
    public BookQueryDTO() {}
    
    // Getter和Setter方法
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getPublisher() {
        return publisher;
    }
    
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDate getPublishDateStart() {
        return publishDateStart;
    }
    
    public void setPublishDateStart(LocalDate publishDateStart) {
        this.publishDateStart = publishDateStart;
    }
    
    public LocalDate getPublishDateEnd() {
        return publishDateEnd;
    }
    
    public void setPublishDateEnd(LocalDate publishDateEnd) {
        this.publishDateEnd = publishDateEnd;
    }
    
    public BigDecimal getPriceMin() {
        return priceMin;
    }
    
    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }
    
    public BigDecimal getPriceMax() {
        return priceMax;
    }
    
    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }
    
    public Integer getPageCountMin() {
        return pageCountMin;
    }
    
    public void setPageCountMin(Integer pageCountMin) {
        this.pageCountMin = pageCountMin;
    }
    
    public Integer getPageCountMax() {
        return pageCountMax;
    }
    
    public void setPageCountMax(Integer pageCountMax) {
        this.pageCountMax = pageCountMax;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Boolean getAvailableOnly() {
        return availableOnly;
    }
    
    public void setAvailableOnly(Boolean availableOnly) {
        this.availableOnly = availableOnly;
    }
}
```

创建`src/main/java/com/demo/library/dto/BookCategoryStats.java`：

```java
package com.demo.library.dto;

/**
 * 图书分类统计数据传输对象
 */
public class BookCategoryStats {
    
    private String category;
    private Long bookCount;
    private Long totalCopies;
    private Long availableCopies;
    
    // 构造器
    public BookCategoryStats() {}
    
    public BookCategoryStats(String category, Long bookCount, Long totalCopies, Long availableCopies) {
        this.category = category;
        this.bookCount = bookCount;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }
    
    // Getter和Setter方法
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Long getBookCount() {
        return bookCount;
    }
    
    public void setBookCount(Long bookCount) {
        this.bookCount = bookCount;
    }
    
    public Long getTotalCopies() {
        return totalCopies;
    }
    
    public void setTotalCopies(Long totalCopies) {
        this.totalCopies = totalCopies;
    }
    
    public Long getAvailableCopies() {
        return availableCopies;
    }
    
    public void setAvailableCopies(Long availableCopies) {
        this.availableCopies = availableCopies;
    }
    
    @Override
    public String toString() {
        return "BookCategoryStats{" +
                "category='" + category + '\'' +
                ", bookCount=" + bookCount +
                ", totalCopies=" + totalCopies +
                ", availableCopies=" + availableCopies +
                '}';
    }
}
```

### 4.3 创建BookMapper.xml映射文件

创建`src/main/resources/mapper/BookMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.demo.library.mapper.BookMapper">
    
    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.demo.library.entity.Book">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="isbn" property="isbn" jdbcType="VARCHAR"/>
        <result column="title" property="title" jdbcType="VARCHAR"/>
        <result column="author" property="author" jdbcType="VARCHAR"/>
        <result column="publisher" property="publisher" jdbcType="VARCHAR"/>
        <result column="publish_date" property="publishDate" jdbcType="DATE"/>
        <result column="category" property="category" jdbcType="VARCHAR"/>
        <result column="description" property="description" jdbcType="VARCHAR"/>
        <result column="cover_image_url" property="coverImageUrl" jdbcType="VARCHAR"/>
        <result column="price" property="price" jdbcType="DECIMAL"/>
        <result column="total_copies" property="totalCopies" jdbcType="INTEGER"/>
        <result column="available_copies" property="availableCopies" jdbcType="INTEGER"/>
        <result column="language" property="language" jdbcType="VARCHAR"/>
        <result column="page_count" property="pageCount" jdbcType="INTEGER"/>
        <result column="status" property="status" jdbcType="VARCHAR"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
        <result column="created_by" property="createdBy" jdbcType="BIGINT"/>
    </resultMap>
    
    <!-- 分类统计结果映射 -->
    <resultMap id="CategoryStatsResultMap" type="com.demo.library.dto.BookCategoryStats">
        <result column="category" property="category" jdbcType="VARCHAR"/>
        <result column="book_count" property="bookCount" jdbcType="BIGINT"/>
        <result column="total_copies" property="totalCopies" jdbcType="BIGINT"/>
        <result column="available_copies" property="availableCopies" jdbcType="BIGINT"/>
    </resultMap>
    
    <!-- 基础列 -->
    <sql id="Base_Column_List">
        id, isbn, title, author, publisher, publish_date, category, description,
        cover_image_url, price, total_copies, available_copies, language, page_count,
        status, created_at, updated_at, created_by
    </sql>
    
    <!-- 查询条件 -->
    <sql id="Query_Condition">
        <where>
            <if test="isbn != null and isbn != ''">
                AND isbn = #{isbn}
            </if>
            <if test="title != null and title != ''">
                AND title LIKE CONCAT('%', #{title}, '%')
            </if>
            <if test="author != null and author != ''">
                AND author LIKE CONCAT('%', #{author}, '%')
            </if>
            <if test="publisher != null and publisher != ''">
                AND publisher LIKE CONCAT('%', #{publisher}, '%')
            </if>
            <if test="category != null and category != ''">
                AND category = #{category}
            </if>
            <if test="language != null and language != ''">
                AND language = #{language}
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="publishDateStart != null">
                AND publish_date >= #{publishDateStart}
            </if>
            <if test="publishDateEnd != null">
                AND publish_date &lt;= #{publishDateEnd}
            </if>
            <if test="priceMin != null">
                AND price >= #{priceMin}
            </if>
            <if test="priceMax != null">
                AND price &lt;= #{priceMax}
            </if>
            <if test="pageCountMin != null">
                AND page_count >= #{pageCountMin}
            </if>
            <if test="pageCountMax != null">
                AND page_count &lt;= #{pageCountMax}
            </if>
            <if test="availableOnly != null and availableOnly">
                AND available_copies > 0 AND status = 'AVAILABLE'
            </if>
            <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%') 
                     OR author LIKE CONCAT('%', #{keyword}, '%')
                     OR publisher LIKE CONCAT('%', #{keyword}, '%')
                     OR description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
        </where>
    </sql>
    
    <!-- 根据ID查询图书 -->
    <select id="selectById" parameterType="java.lang.Long" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books 
        WHERE id = #{id}
    </select>
    
    <!-- 根据ISBN查询图书 -->
    <select id="selectByIsbn" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books 
        WHERE isbn = #{isbn}
    </select>
    
    <!-- 查询所有图书 -->
    <select id="selectAll" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        ORDER BY created_at DESC
    </select>
    
    <!-- 条件查询图书 -->
    <select id="selectByCondition" parameterType="com.demo.library.dto.BookQueryDTO" 
            resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        <include refid="Query_Condition"/>
        ORDER BY created_at DESC
    </select>
    
    <!-- 分页查询图书 -->
    <select id="selectByPage" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        <if test="queryDTO != null">
            <where>
                <if test="queryDTO.title != null and queryDTO.title != ''">
                    AND title LIKE CONCAT('%', #{queryDTO.title}, '%')
                </if>
                <if test="queryDTO.author != null and queryDTO.author != ''">
                    AND author LIKE CONCAT('%', #{queryDTO.author}, '%')
                </if>
                <if test="queryDTO.category != null and queryDTO.category != ''">
                    AND category = #{queryDTO.category}
                </if>
                <if test="queryDTO.status != null and queryDTO.status != ''">
                    AND status = #{queryDTO.status}
                </if>
                <if test="queryDTO.availableOnly != null and queryDTO.availableOnly">
                    AND available_copies > 0 AND status = 'AVAILABLE'
                </if>
                <if test="queryDTO.keyword != null and queryDTO.keyword != ''">
                    AND (title LIKE CONCAT('%', #{queryDTO.keyword}, '%') 
                         OR author LIKE CONCAT('%', #{queryDTO.keyword}, '%')
                         OR publisher LIKE CONCAT('%', #{queryDTO.keyword}, '%'))
                </if>
            </where>
        </if>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
    
    <!-- 统计图书总数 -->
    <select id="countAll" resultType="java.lang.Long">
        SELECT COUNT(*) FROM books
    </select>
    
    <!-- 根据条件统计图书数量 -->
    <select id="countByCondition" parameterType="com.demo.library.dto.BookQueryDTO" 
            resultType="java.lang.Long">
        SELECT COUNT(*) FROM books
        <include refid="Query_Condition"/>
    </select>
    
    <!-- 插入图书 -->
    <insert id="insert" parameterType="com.demo.library.entity.Book" 
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO books (
            isbn, title, author, publisher, publish_date, category, description,
            cover_image_url, price, total_copies, available_copies, language, 
            page_count, status, created_at, updated_at, created_by
        ) VALUES (
            #{isbn}, #{title}, #{author}, #{publisher}, #{publishDate}, #{category}, 
            #{description}, #{coverImageUrl}, #{price}, #{totalCopies}, #{availableCopies}, 
            #{language}, #{pageCount}, #{status}, #{createdAt}, #{updatedAt}, #{createdBy}
        )
    </insert>
    
    <!-- 批量插入图书 -->
    <insert id="insertBatch" parameterType="java.util.List">
        INSERT INTO books (
            isbn, title, author, publisher, publish_date, category, description,
            cover_image_url, price, total_copies, available_copies, language, 
            page_count, status, created_at, updated_at, created_by
        ) VALUES
        <foreach collection="books" item="book" separator=",">
        (
            #{book.isbn}, #{book.title}, #{book.author}, #{book.publisher}, 
            #{book.publishDate}, #{book.category}, #{book.description}, 
            #{book.coverImageUrl}, #{book.price}, #{book.totalCopies}, 
            #{book.availableCopies}, #{book.language}, #{book.pageCount}, 
            #{book.status}, #{book.createdAt}, #{book.updatedAt}, #{book.createdBy}
        )
        </foreach>
    </insert>
    
    <!-- 更新图书信息 -->
    <update id="updateById" parameterType="com.demo.library.entity.Book">
        UPDATE books SET
            isbn = #{isbn},
            title = #{title},
            author = #{author},
            publisher = #{publisher},
            publish_date = #{publishDate},
            category = #{category},
            description = #{description},
            cover_image_url = #{coverImageUrl},
            price = #{price},
            total_copies = #{totalCopies},
            available_copies = #{availableCopies},
            language = #{language},
            page_count = #{pageCount},
            status = #{status},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    </update>
    
    <!-- 选择性更新图书信息 -->
    <update id="updateByIdSelective" parameterType="com.demo.library.entity.Book">
        UPDATE books
        <set>
            <if test="isbn != null and isbn != ''">
                isbn = #{isbn},
            </if>
            <if test="title != null and title != ''">
                title = #{title},
            </if>
            <if test="author != null and author != ''">
                author = #{author},
            </if>
            <if test="publisher != null and publisher != ''">
                publisher = #{publisher},
            </if>
            <if test="publishDate != null">
                publish_date = #{publishDate},
            </if>
            <if test="category != null and category != ''">
                category = #{category},
            </if>
            <if test="description != null">
                description = #{description},
            </if>
            <if test="coverImageUrl != null">
                cover_image_url = #{coverImageUrl},
            </if>
            <if test="price != null">
                price = #{price},
            </if>
            <if test="totalCopies != null">
                total_copies = #{totalCopies},
            </if>
            <if test="availableCopies != null">
                available_copies = #{availableCopies},
            </if>
            <if test="language != null and language != ''">
                language = #{language},
            </if>
            <if test="pageCount != null">
                page_count = #{pageCount},
            </if>
            <if test="status != null and status != ''">
                status = #{status},
            </if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
    </update>
    
    <!-- 更新图书可借数量 -->
    <update id="updateAvailableCopies">
        UPDATE books SET 
            available_copies = #{availableCopies},
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 增加图书可借数量 -->
    <update id="increaseAvailableCopies">
        UPDATE books SET 
            available_copies = available_copies + #{count},
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 减少图书可借数量 -->
    <update id="decreaseAvailableCopies">
        UPDATE books SET 
            available_copies = available_copies - #{count},
            updated_at = NOW()
        WHERE id = #{id} AND available_copies >= #{count}
    </update>
    
    <!-- 更新图书状态 -->
    <update id="updateStatus">
        UPDATE books SET 
            status = #{status},
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 根据ID删除图书 -->
    <delete id="deleteById" parameterType="java.lang.Long">
        DELETE FROM books WHERE id = #{id}
    </delete>
    
    <!-- 批量删除图书 -->
    <delete id="deleteBatch" parameterType="java.util.List">
        DELETE FROM books WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </delete>
    
    <!-- 检查ISBN是否存在 -->
    <select id="existsByIsbn" parameterType="java.lang.String" resultType="java.lang.Boolean">
        SELECT COUNT(*) > 0 FROM books WHERE isbn = #{isbn}
    </select>
    
    <!-- 根据分类查询图书 -->
    <select id="selectByCategory" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE category = #{category}
        ORDER BY title
    </select>
    
    <!-- 根据作者查询图书 -->
    <select id="selectByAuthor" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE author LIKE CONCAT('%', #{author}, '%')
        ORDER BY publish_date DESC
    </select>
    
    <!-- 根据出版社查询图书 -->
    <select id="selectByPublisher" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE publisher LIKE CONCAT('%', #{publisher}, '%')
        ORDER BY publish_date DESC
    </select>
    
    <!-- 查询可借阅的图书 -->
    <select id="selectAvailableBooks" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE status = 'AVAILABLE' AND available_copies > 0
        ORDER BY title
    </select>
    
    <!-- 查询热门图书 -->
    <select id="selectPopularBooks" resultMap="BaseResultMap">
        SELECT b.*, COALESCE(br.borrow_count, 0) as borrow_count
        FROM books b
        LEFT JOIN (
            SELECT book_id, COUNT(*) as borrow_count
            FROM borrow_records
            GROUP BY book_id
        ) br ON b.id = br.book_id
        ORDER BY br.borrow_count DESC, b.title
        <if test="limit != null and limit > 0">
            LIMIT #{limit}
        </if>
    </select>
    
    <!-- 查询新上架图书 -->
    <select id="selectNewBooks" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE created_at >= NOW() - INTERVAL '${days}' DAY
        ORDER BY created_at DESC
        <if test="limit != null and limit > 0">
            LIMIT #{limit}
        </if>
    </select>
    
    <!-- 按价格区间查询图书 -->
    <select id="selectByPriceRange" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE price >= #{minPrice} AND price &lt;= #{maxPrice}
        ORDER BY price
    </select>
    
    <!-- 全文搜索图书 -->
    <select id="searchBooks" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT 
        <include refid="Base_Column_List"/>
        FROM books
        WHERE title LIKE CONCAT('%', #{keyword}, '%')
           OR author LIKE CONCAT('%', #{keyword}, '%')
           OR publisher LIKE CONCAT('%', #{keyword}, '%')
           OR description LIKE CONCAT('%', #{keyword}, '%')
           OR category LIKE CONCAT('%', #{keyword}, '%')
        ORDER BY 
            CASE 
                WHEN title LIKE CONCAT('%', #{keyword}, '%') THEN 1
                WHEN author LIKE CONCAT('%', #{keyword}, '%') THEN 2
                WHEN publisher LIKE CONCAT('%', #{keyword}, '%') THEN 3
                ELSE 4
            END,
            title
    </select>
    
    <!-- 获取所有分类 -->
    <select id="selectAllCategories" resultType="java.lang.String">
        SELECT DISTINCT category 
        FROM books 
        WHERE category IS NOT NULL 
        ORDER BY category
    </select>
    
    <!-- 获取分类统计 -->
    <select id="selectCategoryStats" resultMap="CategoryStatsResultMap">
        SELECT 
            category,
            COUNT(*) as book_count,
            SUM(total_copies) as total_copies,
            SUM(available_copies) as available_copies
        FROM books
        WHERE category IS NOT NULL
        GROUP BY category
        ORDER BY book_count DESC
    </select>
    
</mapper>
```

## 5. 借阅记录模块Mapper开发

### 5.1 创建BorrowRecordMapper接口

创建`src/main/java/com/demo/library/mapper/BorrowRecordMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.BorrowRecord;
import com.demo.library.dto.BorrowRecordQueryDTO;
import com.demo.library.dto.BorrowRecordDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 借阅记录数据访问层接口
 */
@Mapper
public interface BorrowRecordMapper {
    
    /**
     * 根据ID查询借阅记录
     */
    BorrowRecord selectById(@Param("id") Long id);
    
    /**
     * 查询借阅记录详情（包含用户和图书信息）
     */
    BorrowRecordDetailDTO selectDetailById(@Param("id") Long id);
    
    /**
     * 查询所有借阅记录
     */
    List<BorrowRecord> selectAll();
    
    /**
     * 条件查询借阅记录
     */
    List<BorrowRecord> selectByCondition(BorrowRecordQueryDTO queryDTO);
    
    /**
     * 查询借阅记录详情列表
     */
    List<BorrowRecordDetailDTO> selectDetailsByCondition(BorrowRecordQueryDTO queryDTO);
    
    /**
     * 分页查询借阅记录
     */
    List<BorrowRecord> selectByPage(@Param("offset") int offset, 
                                   @Param("limit") int limit,
                                   @Param("queryDTO") BorrowRecordQueryDTO queryDTO);
    
    /**
     * 分页查询借阅记录详情
     */
    List<BorrowRecordDetailDTO> selectDetailsByPage(@Param("offset") int offset, 
                                                   @Param("limit") int limit,
                                                   @Param("queryDTO") BorrowRecordQueryDTO queryDTO);
    
    /**
     * 统计借阅记录总数
     */
    Long countAll();
    
    /**
     * 根据条件统计借阅记录数量
     */
    Long countByCondition(BorrowRecordQueryDTO queryDTO);
    
    /**
     * 插入借阅记录
     */
    int insert(BorrowRecord borrowRecord);
    
    /**
     * 批量插入借阅记录
     */
    int insertBatch(@Param("records") List<BorrowRecord> records);
    
    /**
     * 更新借阅记录
     */
    int updateById(BorrowRecord borrowRecord);
    
    /**
     * 更新归还信息
     */
    int updateReturnInfo(@Param("id") Long id, 
                        @Param("actualReturnDate") LocalDateTime actualReturnDate,
                        @Param("status") String status,
                        @Param("returnNotes") String returnNotes);
    
    /**
     * 更新借阅记录状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 根据ID删除借阅记录
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据用户ID查询借阅记录
     */
    List<BorrowRecord> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据图书ID查询借阅记录
     */
    List<BorrowRecord> selectByBookId(@Param("bookId") Long bookId);
    
    /**
     * 查询用户当前借阅的图书
     */
    List<BorrowRecord> selectCurrentBorrowsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户当前借阅的图书详情
     */
    List<BorrowRecordDetailDTO> selectCurrentBorrowDetailsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询逾期未还的借阅记录
     */
    List<BorrowRecord> selectOverdueRecords();
    
    /**
     * 查询逾期未还的借阅记录详情
     */
    List<BorrowRecordDetailDTO> selectOverdueRecordDetails();
    
    /**
     * 查询即将到期的借阅记录
     */
    List<BorrowRecord> selectSoonExpireRecords(@Param("days") Integer days);
    
    /**
     * 统计用户借阅次数
     */
    Long countUserBorrows(@Param("userId") Long userId);
    
    /**
     * 统计图书被借阅次数
     */
    Long countBookBorrows(@Param("bookId") Long bookId);
    
    /**
     * 检查用户是否已借阅该图书且未归还
     */
    boolean existsActiveBorrow(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    /**
     * 查询热门图书借阅统计
     */
    List<BookBorrowStats> selectPopularBooksStats(@Param("limit") Integer limit);
    
    /**
     * 查询用户借阅统计
     */
    List<UserBorrowStats> selectUserBorrowStats(@Param("limit") Integer limit);
    
    /**
     * 按月份统计借阅数量
     */
    List<MonthlyBorrowStats> selectMonthlyBorrowStats(@Param("year") Integer year);
}
```

### 5.2 创建相关DTO类

创建`src/main/java/com/demo/library/dto/BorrowRecordQueryDTO.java`：

```java
package com.demo.library.dto;

import java.time.LocalDateTime;

/**
 * 借阅记录查询数据传输对象
 */
public class BorrowRecordQueryDTO {
    
    private Long userId;
    private Long bookId;
    private String status;
    private LocalDateTime borrowDateStart;
    private LocalDateTime borrowDateEnd;
    private LocalDateTime dueDateStart;
    private LocalDateTime dueDateEnd;
    private LocalDateTime returnDateStart;
    private LocalDateTime returnDateEnd;
    private String userKeyword; // 用户关键词搜索
    private String bookKeyword; // 图书关键词搜索
    private Boolean overdue; // 是否逾期
    
    // 构造器和getter/setter方法
    public BorrowRecordQueryDTO() {}
    
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getBorrowDateStart() {
        return borrowDateStart;
    }
    
    public void setBorrowDateStart(LocalDateTime borrowDateStart) {
        this.borrowDateStart = borrowDateStart;
    }
    
    public LocalDateTime getBorrowDateEnd() {
        return borrowDateEnd;
    }
    
    public void setBorrowDateEnd(LocalDateTime borrowDateEnd) {
        this.borrowDateEnd = borrowDateEnd;
    }
    
    public LocalDateTime getDueDateStart() {
        return dueDateStart;
    }
    
    public void setDueDateStart(LocalDateTime dueDateStart) {
        this.dueDateStart = dueDateStart;
    }
    
    public LocalDateTime getDueDateEnd() {
        return dueDateEnd;
    }
    
    public void setDueDateEnd(LocalDateTime dueDateEnd) {
        this.dueDateEnd = dueDateEnd;
    }
    
    public LocalDateTime getReturnDateStart() {
        return returnDateStart;
    }
    
    public void setReturnDateStart(LocalDateTime returnDateStart) {
        this.returnDateStart = returnDateStart;
    }
    
    public LocalDateTime getReturnDateEnd() {
        return returnDateEnd;
    }
    
    public void setReturnDateEnd(LocalDateTime returnDateEnd) {
        this.returnDateEnd = returnDateEnd;
    }
    
    public String getUserKeyword() {
        return userKeyword;
    }
    
    public void setUserKeyword(String userKeyword) {
        this.userKeyword = userKeyword;
    }
    
    public String getBookKeyword() {
        return bookKeyword;
    }
    
    public void setBookKeyword(String bookKeyword) {
        this.bookKeyword = bookKeyword;
    }
    
    public Boolean getOverdue() {
        return overdue;
    }
    
    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }
}
```

创建其他统计DTO类（创建在同一个文件中）：

```java
// 在BorrowRecordQueryDTO.java文件末尾添加以下类：

/**
 * 借阅记录详情数据传输对象
 */
class BorrowRecordDetailDTO {
    // 借阅记录信息
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime actualReturnDate;
    private String status;
    private String borrowNotes;
    private String returnNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 用户信息
    private String username;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    
    // 图书信息
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private String bookCategory;
    
    // 构造器和getter/setter方法
    // ... 省略getter/setter方法
}

/**
 * 图书借阅统计
 */
class BookBorrowStats {
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Long borrowCount;
    private Long currentBorrowCount;
    
    // 构造器和getter/setter方法
    // ... 省略getter/setter方法
}

/**
 * 用户借阅统计
 */
class UserBorrowStats {
    private Long userId;
    private String username;
    private String userFullName;
    private Long totalBorrowCount;
    private Long currentBorrowCount;
    private Long overdueCount;
    
    // 构造器和getter/setter方法
    // ... 省略getter/setter方法
}

/**
 * 月度借阅统计
 */
class MonthlyBorrowStats {
    private Integer year;
    private Integer month;
    private Long borrowCount;
    private Long returnCount;
    private Long overdueCount;
    
    // 构造器和getter/setter方法
    // ... 省略getter/setter方法
}
```

## 6. 配置分页插件

### 6.1 添加PageHelper依赖

在`pom.xml`中添加PageHelper分页插件：

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.4.6</version>
</dependency>
```

### 6.2 分页使用示例

创建`src/main/java/com/demo/library/config/PageConfig.java`：

```java
package com.demo.library.config;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.context.annotation.Configuration;

/**
 * 分页配置类
 */
@Configuration
public class PageConfig {
    
    /**
     * 分页工具方法
     */
    public static <T> PageInfo<T> getPageInfo(int pageNum, int pageSize, java.util.function.Supplier<java.util.List<T>> supplier) {
        PageHelper.startPage(pageNum, pageSize);
        java.util.List<T> list = supplier.get();
        return new PageInfo<>(list);
    }
}
```

## 7. 测试Mapper功能

### 7.1 创建测试类

创建`src/test/java/com/demo/library/mapper/UserMapperTest.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.User;
import com.demo.library.dto.UserQueryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserMapperTest {
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    void testInsertUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        
        int result = userMapper.insert(user);
        assertEquals(1, result);
        assertNotNull(user.getId());
    }
    
    @Test
    void testSelectById() {
        // 先插入测试数据
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        
        userMapper.insert(user);
        
        // 测试查询
        User found = userMapper.selectById(user.getId());
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }
    
    @Test
    void testSelectByCondition() {
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setRole("USER");
        queryDTO.setStatus("ACTIVE");
        
        List<User> users = userMapper.selectByCondition(queryDTO);
        assertNotNull(users);
    }
    
    @Test
    void testExistsByUsername() {
        String username = "existstest";
        
        // 应该不存在
        assertFalse(userMapper.existsByUsername(username));
        
        // 插入数据
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail("exists@example.com");
        user.setFullName("Exists Test");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        
        userMapper.insert(user);
        
        // 现在应该存在
        assertTrue(userMapper.existsByUsername(username));
    }
}
```

### 7.2 运行测试

```bash
# 运行单个测试类
mvn test -Dtest=UserMapperTest

# 运行所有测试
mvn test
```

## 8. 常见问题解决

### 8.1 映射文件找不到

**错误：** `org.apache.ibatis.binding.BindingException: Invalid bound statement`

**解决方法：**
1. 检查`application.yml`中的`mapper-locations`配置
2. 确认XML文件路径正确
3. 检查namespace是否与Mapper接口完全匹配

### 8.2 参数绑定错误

**错误：** `ParameterMappingException`

**解决方法：**
1. 使用`@Param`注解明确参数名
2. 检查XML中的参数引用是否正确
3. 确认参数类型匹配

### 8.3 SQL语法错误

**错误：** SQL语法异常

**解决方法：**
1. 在控制台查看生成的SQL语句
2. 在数据库中直接测试SQL
3. 注意OpenGauss的特殊语法

### 8.4 结果映射错误

**错误：** 查询结果为空或字段值不正确

**解决方法：**
1. 检查ResultMap配置
2. 确认数据库字段名与实体类属性名映射
3. 启用驼峰命名转换

## 9. 性能优化建议

### 9.1 SQL优化
- 合理使用索引
- 避免全表扫描
- 使用EXPLAIN分析执行计划
- 优化复杂查询

### 9.2 MyBatis优化
- 启用二级缓存
- 合理使用懒加载
- 批量操作优化
- 连接池配置优化

### 9.3 分页优化
- 使用数据库原生分页
- 避免深度分页
- 考虑使用游标分页

## 10. 总结

通过本步骤，我们完成了：

✅ **MyBatis配置**
- 配置映射文件路径和基础参数
- 集成分页插件
- 配置缓存和性能参数

✅ **Mapper接口开发**
- 用户管理Mapper接口和XML映射
- 图书管理Mapper接口和XML映射
- 借阅记录Mapper接口和XML映射

✅ **高级功能实现**
- 动态SQL查询条件
- 复杂关联查询
- 分页查询实现
- 统计和报表查询

✅ **数据传输对象**
- 创建查询条件DTO
- 创建详情和统计DTO
- 实现数据封装和传输

✅ **测试和调试**
- 编写单元测试
- 性能优化配置
- 常见问题解决方案

## 下一步

数据访问层开发完成后，我们将在[Step 6](step6.md)中学习Service层业务逻辑实现，包括：
- 创建Service接口和实现类
- 业务逻辑设计和实现
- 事务管理配置
- 异常处理机制
- 数据验证和转换

---

**提示**：MyBatis是一个强大的持久层框架，掌握其核心概念和最佳实践对于构建高效的数据访问层至关重要。建议多练习复杂查询和性能优化技巧。