# Step 9: Spring Security安全框架集成

## 学习目标

通过本步骤，你将学会：
- 配置Spring Security安全框架
- 实现用户认证和授权机制
- 集成JWT令牌认证
- 配置密码加密策略
- 实现基于角色的权限控制
- 开发登录登出功能

## 前置要求

- 已完成前8个步骤
- 理解Spring Boot基础概念
- 熟悉HTTP认证机制
- 了解JWT基本原理

## 1. Spring Security核心概念

### 1.1 认证与授权
- **认证（Authentication）**：验证用户身份
- **授权（Authorization）**：检查用户权限
- **主体（Principal）**：当前用户信息
- **凭证（Credentials）**：用户密码等认证信息

### 1.2 Security Filter Chain
Spring Security通过一系列过滤器链来处理安全请求：
```
HttpFirewall → SecurityFilterChain → DispatcherServlet
```

## 2. 添加JWT依赖

### 2.1 更新pom.xml

在`pom.xml`中添加JWT相关依赖：

```xml
<!-- JWT Dependencies -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- Apache Commons Lang for StringUtils -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
```

## 3. 用户实体与角色设计

### 3.1 创建用户实体

创建`src/main/java/com/demo/library/entity/User.java`：

```java
package com.demo.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Set;

public class User {
    private Long id;
    private String username;
    
    @JsonIgnore
    private String password;
    
    private String email;
    private String fullName;
    private String phone;
    private Boolean enabled;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    
    // 用户角色关联
    private Set<Role> roles;
    
    // 构造函数
    public User() {}
    
    public User(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getAccountNonExpired() { return accountNonExpired; }
    public void setAccountNonExpired(Boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    
    public Boolean getAccountNonLocked() { return accountNonLocked; }
    public void setAccountNonLocked(Boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    
    public Boolean getCredentialsNonExpired() { return credentialsNonExpired; }
    public void setCredentialsNonExpired(Boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
```

### 3.2 创建角色实体

创建`src/main/java/com/demo/library/entity/Role.java`：

```java
package com.demo.library.entity;

import java.time.LocalDateTime;
import java.util.Set;

public class Role {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 角色权限关联
    private Set<Permission> permissions;
    
    // 构造函数
    public Role() {}
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}
```

### 3.3 创建权限实体

创建`src/main/java/com/demo/library/entity/Permission.java`：

```java
package com.demo.library.entity;

import java.time.LocalDateTime;

public class Permission {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造函数
    public Permission() {}
    
    public Permission(String name, String description, String resource, String action) {
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

## 4. 数据访问层

### 4.1 用户Mapper接口

创建`src/main/java/com/demo/library/mapper/UserMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.Optional;
import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "fullName", column = "full_name"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "enabled", column = "enabled"),
        @Result(property = "accountNonExpired", column = "account_non_expired"),
        @Result(property = "accountNonLocked", column = "account_non_locked"),
        @Result(property = "credentialsNonExpired", column = "credentials_non_expired"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "lastLoginAt", column = "last_login_at"),
        @Result(property = "roles", column = "id", 
                javaType = java.util.Set.class,
                many = @Many(select = "findRolesByUserId"))
    })
    Optional<User> findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "fullName", column = "full_name"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "enabled", column = "enabled"),
        @Result(property = "accountNonExpired", column = "account_non_expired"),
        @Result(property = "accountNonLocked", column = "account_non_locked"),
        @Result(property = "credentialsNonExpired", column = "credentials_non_expired"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "lastLoginAt", column = "last_login_at")
    })
    Optional<User> findByEmail(String email);
    
    @Insert("INSERT INTO users (username, password, email, full_name, phone, enabled, " +
            "account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{email}, #{fullName}, #{phone}, #{enabled}, " +
            "#{accountNonExpired}, #{accountNonLocked}, #{credentialsNonExpired}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET password = #{password}, email = #{email}, full_name = #{fullName}, " +
            "phone = #{phone}, enabled = #{enabled}, account_non_expired = #{accountNonExpired}, " +
            "account_non_locked = #{accountNonLocked}, credentials_non_expired = #{credentialsNonExpired}, " +
            "updated_at = #{updatedAt}, last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int update(User user);
    
    @Update("UPDATE users SET last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int updateLastLoginTime(Long id, java.time.LocalDateTime lastLoginAt);
    
    @Select("SELECT r.* FROM roles r " +
            "JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "permissions", column = "id", 
                javaType = java.util.Set.class,
                many = @Many(select = "com.demo.library.mapper.RoleMapper.findPermissionsByRoleId"))
    })
    java.util.Set<com.demo.library.entity.Role> findRolesByUserId(Long userId);
    
    @Insert("INSERT INTO user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int addUserRole(Long userId, Long roleId);
    
    @Delete("DELETE FROM user_roles WHERE user_id = #{userId} AND role_id = #{roleId}")
    int removeUserRole(Long userId, Long roleId);
}
```

### 4.2 角色Mapper接口

创建`src/main/java/com/demo/library/mapper/RoleMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.Role;
import com.demo.library.entity.Permission;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper {
    
    @Select("SELECT * FROM roles WHERE name = #{name}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Role findByName(String name);
    
    @Select("SELECT * FROM roles")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Role> findAll();
    
    @Insert("INSERT INTO roles (name, description, created_at, updated_at) " +
            "VALUES (#{name}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);
    
    @Select("SELECT p.* FROM permissions p " +
            "JOIN role_permissions rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "resource", column = "resource"),
        @Result(property = "action", column = "action"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Set<Permission> findPermissionsByRoleId(Long roleId);
}
```

## 5. 自定义UserDetails实现

创建`src/main/java/com/demo/library/security/CustomUserDetails.java`：

```java
package com.demo.library.security;

import com.demo.library.entity.User;
import com.demo.library.entity.Role;
import com.demo.library.entity.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomUserDetails implements UserDetails {
    
    private final User user;
    
    public CustomUserDetails(User user) {
        this.user = user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // 添加角色权限
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                
                // 添加权限
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                    }
                }
            }
        }
        
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return user.getAccountNonExpired();
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountNonLocked();
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return user.getCredentialsNonExpired();
    }
    
    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
    
    // 获取原始用户对象
    public User getUser() {
        return user;
    }
}
```

## 6. UserDetailsService实现

创建`src/main/java/com/demo/library/service/CustomUserDetailsService.java`：

```java
package com.demo.library.service;

import com.demo.library.entity.User;
import com.demo.library.mapper.UserMapper;
import com.demo.library.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        return new CustomUserDetails(user);
    }
}
```

## 7. JWT工具类

创建`src/main/java/com/demo/library/util/JwtUtils.java`：

```java
package com.demo.library.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret:mySecretKey}")
    private String secret;
    
    @Value("${jwt.expiration:604800}") // 7天
    private int expiration;
    
    @Value("${jwt.issuer:library-system}")
    private String issuer;
    
    // 生成JWT令牌
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }
    
    // 生成带有额外信息的JWT令牌
    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        return createToken(claims, userDetails.getUsername());
    }
    
    // 创建JWT令牌
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }
    
    // 从令牌中提取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    // 从令牌中提取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    // 从令牌中提取特定的声明
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    // 从令牌中提取所有声明
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    // 检查令牌是否过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    // 验证令牌
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    
    // 验证令牌格式和签名
    public Boolean validateTokenFormat(String token) {
        try {
            if (StringUtils.isBlank(token)) {
                return false;
            }
            
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    // 获取签名密钥
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    // 从请求头中提取令牌
    public String extractTokenFromHeader(String authorizationHeader) {
        if (StringUtils.isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
```

## 8. JWT认证过滤器

创建`src/main/java/com/demo/library/security/JwtAuthenticationFilter.java`：

```java
package com.demo.library.security;

import com.demo.library.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        
        String username = null;
        String jwtToken = null;
        
        // 从请求头中提取JWT令牌
        if (StringUtils.isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7);
            try {
                username = jwtUtils.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                logger.error("无法从JWT令牌中获取用户名", e);
            }
        }
        
        // 验证令牌并设置安全上下文
        if (StringUtils.isNotBlank(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            if (jwtUtils.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

## 9. Spring Security配置

创建`src/main/java/com/demo/library/config/SecurityConfig.java`：

```java
package com.demo.library.config;

import com.demo.library.security.JwtAuthenticationFilter;
import com.demo.library.security.JwtAuthenticationEntryPoint;
import com.demo.library.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 公开访问的端点
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/login", "/register", "/").permitAll()
                
                // 管理员权限
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 图书管理员权限
                .requestMatchers("/api/books/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers("/api/categories/**").hasAnyRole("ADMIN", "LIBRARIAN")
                
                // 用户权限
                .requestMatchers("/api/users/profile").hasRole("USER")
                .requestMatchers("/api/borrowing/**").hasRole("USER")
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 9.1 JWT认证入口点

创建`src/main/java/com/demo/library/security/JwtAuthenticationEntryPoint.java`：

```java
package com.demo.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "认证失败，请提供有效的JWT令牌");
        body.put("path", request.getServletPath());
        
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
```

## 10. 认证相关DTO

### 10.1 登录请求DTO

创建`src/main/java/com/demo/library/dto/LoginRequest.java`：

```java
package com.demo.library.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    // 构造函数
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 10.2 注册请求DTO

创建`src/main/java/com/demo/library/dto/RegisterRequest.java`：

```java
package com.demo.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "姓名不能为空")
    private String fullName;
    
    private String phone;
    
    // 构造函数
    public RegisterRequest() {}
    
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
}
```

### 10.3 JWT响应DTO

创建`src/main/java/com/demo/library/dto/JwtResponse.java`：

```java
package com.demo.library.dto;

import java.util.List;

public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private List<String> roles;
    
    // 构造函数
    public JwtResponse(String accessToken, String username, String email, List<String> roles) {
        this.token = accessToken;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
    
    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
```

## 11. 认证控制器

创建`src/main/java/com/demo/library/controller/AuthController.java`：

```java
package com.demo.library.controller;

import com.demo.library.dto.*;
import com.demo.library.entity.User;
import com.demo.library.entity.Role;
import com.demo.library.mapper.UserMapper;
import com.demo.library.mapper.RoleMapper;
import com.demo.library.security.CustomUserDetails;
import com.demo.library.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    AuthenticationManager authenticationManager;
    
    @Autowired
    UserMapper userMapper;
    
    @Autowired
    RoleMapper roleMapper;
    
    @Autowired
    PasswordEncoder encoder;
    
    @Autowired
    JwtUtils jwtUtils;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken((CustomUserDetails) authentication.getPrincipal());
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        
        // 更新最后登录时间
        userMapper.updateLastLoginTime(userDetails.getUser().getId(), LocalDateTime.now());
        
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getUsername(),
                userDetails.getUser().getEmail(),
                roles));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        
        if (userMapper.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("错误：用户名已经被使用！"));
        }
        
        if (userMapper.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("错误：邮箱已经被使用！"));
        }
        
        // 创建新用户账户
        User user = new User(signUpRequest.getUsername(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getEmail(),
                signUpRequest.getFullName());
        
        user.setPhone(signUpRequest.getPhone());
        
        userMapper.insert(user);
        
        // 分配默认角色
        Role userRole = roleMapper.findByName("USER");
        if (userRole != null) {
            userMapper.addUserRole(user.getId(), userRole.getId());
        }
        
        return ResponseEntity.ok(new MessageResponse("用户注册成功！"));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("登出成功！"));
    }
}
```

## 12. 用户服务

创建`src/main/java/com/demo/library/service/UserService.java`：

```java
package com.demo.library.service;

import com.demo.library.entity.User;
import com.demo.library.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public Optional<User> findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
    
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return user;
    }
    
    public void updatePassword(String username, String newPassword) {
        Optional<User> userOpt = userMapper.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.update(user);
        }
    }
    
    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username).isPresent();
    }
    
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email).isPresent();
    }
}
```

## 13. 数据库表结构

### 13.1 创建安全相关表

创建SQL脚本 `src/main/resources/schema/security.sql`：

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(200),
    resource VARCHAR(100),
    action VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id SERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- 插入默认角色
INSERT INTO roles (name, description) VALUES 
('ADMIN', '系统管理员'),
('LIBRARIAN', '图书管理员'),
('USER', '普通用户')
ON CONFLICT (name) DO NOTHING;

-- 插入默认权限
INSERT INTO permissions (name, description, resource, action) VALUES 
('book:read', '查看图书', 'book', 'read'),
('book:write', '编辑图书', 'book', 'write'),
('book:delete', '删除图书', 'book', 'delete'),
('user:read', '查看用户', 'user', 'read'),
('user:write', '编辑用户', 'user', 'write'),
('user:delete', '删除用户', 'user', 'delete'),
('borrowing:read', '查看借阅记录', 'borrowing', 'read'),
('borrowing:write', '管理借阅', 'borrowing', 'write'),
('system:admin', '系统管理', 'system', 'admin')
ON CONFLICT (name) DO NOTHING;

-- 角色权限关联
-- 管理员权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 图书管理员权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'LIBRARIAN' AND p.name IN ('book:read', 'book:write', 'book:delete', 'borrowing:read', 'borrowing:write')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 普通用户权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'USER' AND p.name IN ('book:read', 'borrowing:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 创建默认管理员用户（密码：admin123）
INSERT INTO users (username, password, email, full_name) VALUES 
('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvntPEgW3Dzp0V8a8Qd4P3FzO', 'admin@library.com', '系统管理员')
ON CONFLICT (username) DO NOTHING;

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
```

## 14. 配置文件更新

### 14.1 更新application.yml

在`application.yml`中添加JWT配置：

```yaml
# JWT配置
jwt:
  secret: myJwtSecretKeyForLibrarySystemShouldBeLongEnoughForSecurity
  expiration: 604800 # 7天（秒）
  issuer: library-system

# 安全配置
security:
  ignored-paths: /css/**, /js/**, /images/**, /webjars/**, /favicon.ico
```

## 15. 测试与验证

### 15.1 创建测试控制器

创建`src/main/java/com/demo/library/controller/TestController.java`：

```java
package com.demo.library.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/public")
    public String publicEndpoint() {
        return "这是一个公开的端点，任何人都可以访问";
    }
    
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public String userEndpoint() {
        return "这是一个需要USER角色的端点";
    }
    
    @GetMapping("/librarian")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String librarianEndpoint() {
        return "这是一个需要LIBRARIAN角色的端点";
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "这是一个需要ADMIN角色的端点";
    }
}
```

### 15.2 测试步骤

1. **启动应用**：
   ```bash
   mvn spring-boot:run
   ```

2. **用户注册**：
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
   -H "Content-Type: application/json" \
   -d '{
     "username": "testuser",
     "password": "password123",
     "email": "test@example.com",
     "fullName": "Test User"
   }'
   ```

3. **用户登录**：
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
   -H "Content-Type: application/json" \
   -d '{
     "username": "admin",
     "password": "admin123"
   }'
   ```

4. **访问受保护的端点**：
   ```bash
   curl -X GET http://localhost:8080/api/test/admin \
   -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

## 16. 前端集成示例

### 16.1 登录页面

创建`src/main/resources/templates/login.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - 图书管理系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-6 col-lg-4">
                <div class="card mt-5">
                    <div class="card-header">
                        <h4 class="text-center">用户登录</h4>
                    </div>
                    <div class="card-body">
                        <form id="loginForm">
                            <div class="mb-3">
                                <label for="username" class="form-label">用户名</label>
                                <input type="text" class="form-control" id="username" name="username" required>
                            </div>
                            <div class="mb-3">
                                <label for="password" class="form-label">密码</label>
                                <input type="password" class="form-control" id="password" name="password" required>
                            </div>
                            <div class="d-grid">
                                <button type="submit" class="btn btn-primary">登录</button>
                            </div>
                        </form>
                        <div class="text-center mt-3">
                            <a href="/register">还没有账号？立即注册</a>
                        </div>
                        <div id="message" class="alert alert-danger mt-3" style="display: none;"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const messageDiv = document.getElementById('message');
            
            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username, password })
                });
                
                if (response.ok) {
                    const data = await response.json();
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('user', JSON.stringify({
                        username: data.username,
                        email: data.email,
                        roles: data.roles
                    }));
                    window.location.href = '/dashboard';
                } else {
                    const error = await response.json();
                    messageDiv.textContent = error.message || '登录失败';
                    messageDiv.style.display = 'block';
                }
            } catch (error) {
                messageDiv.textContent = '网络错误，请稍后重试';
                messageDiv.style.display = 'block';
            }
        });
    </script>
</body>
</html>
```

## 17. 常见问题解决

### 17.1 JWT密钥长度问题
```java
// 确保JWT密钥足够长（至少32个字符）
jwt.secret=myJwtSecretKeyForLibrarySystemShouldBeLongEnoughForSecurity
```

### 17.2 CORS跨域问题
```java
// 在SecurityConfig中正确配置CORS
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 17.3 密码加密问题
```java
// 使用BCrypt加密密码
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

## 18. 总结

通过本步骤，我们完成了：

✅ **安全框架集成**
- Spring Security配置
- 自定义UserDetailsService实现
- 密码加密策略

✅ **JWT认证机制**
- JWT工具类实现
- JWT过滤器配置
- 令牌生成和验证

✅ **权限控制系统**
- 基于角色的访问控制(RBAC)
- 方法级安全注解
- 权限细粒度控制

✅ **用户认证功能**
- 用户登录登出
- 用户注册功能
- 安全上下文管理

## 下一步

安全框架集成完成后，我们将在[Step 10](step10.md)中学习异常处理与日志配置，包括：
- 全局异常处理器
- 自定义异常类设计
- 日志配置和管理
- 错误页面设计
- 系统监控和诊断

---

**注意事项**：
1. 生产环境中务必使用强密钥
2. 定期更换JWT密钥
3. 合理设置令牌过期时间
4. 注意保护敏感信息不被泄露