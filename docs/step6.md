# Step 6: Service层业务逻辑实现

## 学习目标

通过本步骤，你将学会：
- Service层的设计原则和最佳实践
- 创建Service接口和实现类
- 实现复杂的业务逻辑
- 配置和使用Spring事务管理
- 异常处理和业务验证
- DTO与Entity之间的数据转换

## 前置要求

确保已完成：
- Step 1: 环境准备与OpenGauss安装
- Step 2: Spring Boot基础配置
- Step 3: MyBatis与OpenGauss集成
- Step 4: 实体类设计与数据库表创建
- Step 5: MyBatis Mapper接口与SQL映射开发

## 1. Service层架构设计

### 1.1 Service层职责

**业务逻辑处理：** 封装复杂的业务规则和流程
**事务管理：** 确保数据操作的一致性和完整性
**数据验证：** 验证输入参数的合法性
**异常处理：** 处理业务异常并转换为适当的响应
**数据转换：** DTO与Entity之间的转换

### 1.2 设计原则

- **单一职责原则**：每个Service类专注于一个业务域
- **接口隔离原则**：定义清晰的Service接口
- **依赖倒置原则**：面向接口编程
- **开闭原则**：对扩展开放，对修改关闭

## 2. 创建通用响应类和异常类

### 2.1 创建统一响应结果类

创建`src/main/java/com/demo/library/common/Result.java`：

```java
package com.demo.library.common;

import java.io.Serializable;

/**
 * 统一响应结果类
 */
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), 
                           ResultCode.INTERNAL_SERVER_ERROR.getMessage());
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }
    
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage());
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
    
    // Getter和Setter方法
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}
```

### 2.2 创建响应码枚举

创建`src/main/java/com/demo/library/common/ResultCode.java`：

```java
package com.demo.library.common;

/**
 * 响应码枚举
 */
public enum ResultCode {
    
    /* 成功状态码 */
    SUCCESS(200, "操作成功"),
    
    /* 参数错误：10001-19999 */
    PARAM_IS_INVALID(10001, "参数无效"),
    PARAM_IS_BLANK(10002, "参数为空"),
    PARAM_TYPE_BIND_ERROR(10003, "参数类型错误"),
    PARAM_NOT_COMPLETE(10004, "参数缺失"),
    
    /* 用户错误：20001-29999*/
    USER_NOT_LOGGED_IN(20001, "用户未登录"),
    USER_LOGIN_ERROR(20002, "账号不存在或密码错误"),
    USER_ACCOUNT_FORBIDDEN(20003, "账号已被禁用"),
    USER_NOT_EXIST(20004, "用户不存在"),
    USER_HAS_EXISTED(20005, "用户已存在"),
    USER_PASSWORD_ERROR(20006, "密码错误"),
    
    /* 业务错误：30001-39999 */
    BOOK_NOT_EXIST(30001, "图书不存在"),
    BOOK_NOT_AVAILABLE(30002, "图书不可借阅"),
    BOOK_ALREADY_BORROWED(30003, "图书已被借阅"),
    BOOK_NOT_BORROWED(30004, "图书未被借阅"),
    BORROW_LIMIT_EXCEEDED(30005, "借阅数量超限"),
    BORROW_RECORD_NOT_EXIST(30006, "借阅记录不存在"),
    RETURN_DATE_INVALID(30007, "归还日期无效"),
    
    /* 系统错误：40001-49999 */
    SYSTEM_INNER_ERROR(40001, "系统繁忙，请稍后重试"),
    
    /* 数据错误：50001-59999 */
    DATA_NONE(50001, "数据未找到"),
    DATA_WRONG(50002, "数据有误"),
    DATA_EXISTED(50003, "数据已存在"),
    
    /* 接口错误：60001-69999 */
    INTERFACE_INNER_INVOKE_ERROR(60001, "内部系统接口调用异常"),
    INTERFACE_OUTER_INVOKE_ERROR(60002, "外部系统接口调用异常"),
    INTERFACE_FORBID_VISIT(60003, "该接口禁止访问"),
    INTERFACE_ADDRESS_INVALID(60004, "接口地址无效"),
    INTERFACE_REQUEST_TIMEOUT(60005, "接口请求超时"),
    INTERFACE_EXCEED_LOAD(60006, "接口负载过高"),
    
    /* 权限错误：70001-79999 */
    PERMISSION_NO_ACCESS(70001, "无访问权限"),
    PERMISSION_TOKEN_EXPIRED(70002, "Token已过期"),
    PERMISSION_TOKEN_INVALID(70003, "Token无效"),
    PERMISSION_SIGNATURE_ERROR(70004, "签名验证失败"),
    PERMISSION_OPERATION_DENIED(70005, "操作被拒绝"),
    
    /* 其他错误：80001-89999 */
    FILE_UPLOAD_ERROR(80001, "文件上传失败"),
    FILE_TYPE_ERROR(80002, "文件类型错误"),
    FILE_SIZE_EXCEED(80003, "文件大小超限"),
    
    /* 通用错误 */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源未找到"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁");
    
    private Integer code;
    private String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
```

### 2.3 创建业务异常类

创建`src/main/java/com/demo/library/exception/BusinessException.java`：

```java
package com.demo.library.exception;

import com.demo.library.common.ResultCode;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
    
    public BusinessException() {
        super();
    }
    
    public BusinessException(String message) {
        super(message);
        this.message = message;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
    
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
```

## 3. 数据转换工具类

### 3.1 创建Bean拷贝工具

创建`src/main/java/com/demo/library/util/BeanUtils.java`：

```java
package com.demo.library.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * Bean工具类
 */
public class BeanUtils extends org.springframework.beans.BeanUtils {
    
    /**
     * 复制属性，忽略空值
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        copyProperties(source, target, getNullPropertyNames(source));
    }
    
    /**
     * 获取空属性名称数组
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
    
    /**
     * 复制集合
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<T> targetList = new ArrayList<>();
        for (Object source : sourceList) {
            try {
                T target = targetClass.getDeclaredConstructor().newInstance();
                copyProperties(source, target);
                targetList.add(target);
            } catch (Exception e) {
                throw new RuntimeException("复制对象失败", e);
            }
        }
        
        return targetList;
    }
}
```

## 4. 用户服务实现

### 4.1 创建UserService接口

创建`src/main/java/com/demo/library/service/UserService.java`：

```java
package com.demo.library.service;

import com.demo.library.entity.User;
import com.demo.library.dto.UserQueryDTO;
import com.demo.library.dto.UserCreateDTO;
import com.demo.library.dto.UserUpdateDTO;
import com.demo.library.dto.UserDTO;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据ID查询用户
     */
    UserDTO getById(Long id);
    
    /**
     * 根据用户名查询用户
     */
    UserDTO getByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    UserDTO getByEmail(String email);
    
    /**
     * 查询所有用户
     */
    List<UserDTO> getAllUsers();
    
    /**
     * 条件查询用户
     */
    List<UserDTO> getUsersByCondition(UserQueryDTO queryDTO);
    
    /**
     * 分页查询用户
     */
    PageInfo<UserDTO> getUsersByPage(int pageNum, int pageSize, UserQueryDTO queryDTO);
    
    /**
     * 创建用户
     */
    UserDTO createUser(UserCreateDTO createDTO);
    
    /**
     * 更新用户信息
     */
    UserDTO updateUser(Long id, UserUpdateDTO updateDTO);
    
    /**
     * 更新用户状态
     */
    void updateUserStatus(Long id, String status);
    
    /**
     * 删除用户
     */
    void deleteUser(Long id);
    
    /**
     * 批量删除用户
     */
    void deleteUsers(List<Long> ids);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 用户登录
     */
    UserDTO login(String username, String password);
    
    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(Long id);
    
    /**
     * 增加登录次数
     */
    void incrementLoginCount(Long id);
    
    /**
     * 修改密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);
    
    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);
    
    /**
     * 查询活跃用户
     */
    List<UserDTO> getActiveUsers(int days);
    
    /**
     * 根据角色查询用户
     */
    List<UserDTO> getUsersByRole(String role);
}
```

### 4.2 创建用户相关DTO类

创建`src/main/java/com/demo/library/dto/UserDTO.java`：

```java
package com.demo.library.dto;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    
    // 构造器
    public UserDTO() {}
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
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
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public Integer getLoginCount() {
        return loginCount;
    }
    
    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }
    
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastLoginAt=" + lastLoginAt +
                ", loginCount=" + loginCount +
                '}';
    }
}
```

创建`src/main/java/com/demo/library/dto/UserCreateDTO.java`：

```java
package com.demo.library.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户创建数据传输对象
 */
public class UserCreateDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 100, message = "真实姓名长度不能超过100个字符")
    private String fullName;
    
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phone;
    
    private String address;
    
    private String role = "USER";
    
    private String avatarUrl;
    
    // 构造器
    public UserCreateDTO() {}
    
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
```

创建`src/main/java/com/demo/library/dto/UserUpdateDTO.java`：

```java
package com.demo.library.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

/**
 * 用户更新数据传输对象
 */
public class UserUpdateDTO {
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 100, message = "真实姓名长度不能超过100个字符")
    private String fullName;
    
    @Size(max = 20, message = "电话号码长度不能超过20个字符")
    private String phone;
    
    private String address;
    
    private String role;
    
    private String avatarUrl;
    
    // 构造器
    public UserUpdateDTO() {}
    
    // Getter和Setter方法
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
```

### 4.3 创建UserService实现类

创建`src/main/java/com/demo/library/service/impl/UserServiceImpl.java`：

```java
package com.demo.library.service.impl;

import com.demo.library.entity.User;
import com.demo.library.dto.*;
import com.demo.library.mapper.UserMapper;
import com.demo.library.service.UserService;
import com.demo.library.common.ResultCode;
import com.demo.library.exception.BusinessException;
import com.demo.library.util.BeanUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional(readOnly = true)
    public UserDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        return convertToDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDTO getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        return convertToDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDTO getByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        return convertToDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        List<User> users = userMapper.selectAll();
        return BeanUtils.copyList(users, UserDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByCondition(UserQueryDTO queryDTO) {
        List<User> users = userMapper.selectByCondition(queryDTO);
        return BeanUtils.copyList(users, UserDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageInfo<UserDTO> getUsersByPage(int pageNum, int pageSize, UserQueryDTO queryDTO) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.selectByCondition(queryDTO);
        PageInfo<User> pageInfo = new PageInfo<>(users);
        
        // 转换为DTO分页信息
        PageInfo<UserDTO> dtoPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, dtoPageInfo);
        dtoPageInfo.setList(BeanUtils.copyList(users, UserDTO.class));
        
        return dtoPageInfo;
    }
    
    @Override
    public UserDTO createUser(UserCreateDTO createDTO) {
        if (createDTO == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 验证用户名和邮箱唯一性
        if (existsByUsername(createDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_HAS_EXISTED.getCode(), "用户名已存在");
        }
        
        if (StringUtils.hasText(createDTO.getEmail()) && existsByEmail(createDTO.getEmail())) {
            throw new BusinessException(ResultCode.USER_HAS_EXISTED.getCode(), "邮箱已存在");
        }
        
        // 转换为实体对象
        User user = new User();
        BeanUtils.copyProperties(createDTO, user);
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(createDTO.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "用户创建失败");
        }
        
        logger.info("用户创建成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return convertToDTO(user);
    }
    
    @Override
    public UserDTO updateUser(Long id, UserUpdateDTO updateDTO) {
        if (id == null || updateDTO == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 检查用户是否存在
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 检查邮箱唯一性
        if (StringUtils.hasText(updateDTO.getEmail()) && 
            !updateDTO.getEmail().equals(existingUser.getEmail()) &&
            existsByEmail(updateDTO.getEmail())) {
            throw new BusinessException(ResultCode.USER_HAS_EXISTED.getCode(), "邮箱已存在");
        }
        
        // 更新用户信息
        User user = new User();
        user.setId(id);
        BeanUtils.copyPropertiesIgnoreNull(updateDTO, user);
        user.setUpdatedAt(LocalDateTime.now());
        
        int result = userMapper.updateByIdSelective(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "用户更新失败");
        }
        
        logger.info("用户更新成功，用户ID: {}", id);
        return getById(id);
    }
    
    @Override
    public void updateUserStatus(Long id, String status) {
        if (id == null || !StringUtils.hasText(status)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 验证状态值
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "无效的状态值");
        }
        
        int result = userMapper.updateStatus(id, status);
        if (result <= 0) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        logger.info("用户状态更新成功，用户ID: {}, 状态: {}", id, status);
    }
    
    @Override
    public void deleteUser(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        int result = userMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        logger.info("用户删除成功，用户ID: {}", id);
    }
    
    @Override
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        int result = userMapper.deleteBatch(ids);
        logger.info("批量删除用户成功，删除数量: {}", result);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userMapper.existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return userMapper.existsByEmail(email);
    }
    
    @Override
    public UserDTO login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_LOGIN_ERROR);
        }
        
        // 检查账户状态
        if ("INACTIVE".equals(user.getStatus()) || "LOCKED".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_LOGIN_ERROR);
        }
        
        // 更新登录信息
        updateLastLoginTime(user.getId());
        incrementLoginCount(user.getId());
        
        logger.info("用户登录成功，用户名: {}", username);
        return convertToDTO(user);
    }
    
    @Override
    public void updateLastLoginTime(Long id) {
        if (id == null) {
            return;
        }
        
        userMapper.updateLastLoginTime(id, LocalDateTime.now());
    }
    
    @Override
    public void incrementLoginCount(Long id) {
        if (id == null) {
            return;
        }
        
        userMapper.incrementLoginCount(id);
    }
    
    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        if (id == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        int result = userMapper.updateByIdSelective(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "密码修改失败");
        }
        
        logger.info("用户密码修改成功，用户ID: {}", id);
    }
    
    @Override
    public void resetPassword(Long id, String newPassword) {
        if (id == null || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        int result = userMapper.updateByIdSelective(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        logger.info("用户密码重置成功，用户ID: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getActiveUsers(int days) {
        List<User> users = userMapper.selectActiveUsers(days);
        return BeanUtils.copyList(users, UserDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        List<User> users = userMapper.selectByRole(role);
        return BeanUtils.copyList(users, UserDTO.class);
    }
    
    /**
     * 转换为DTO对象
     */
    private UserDTO convertToDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
```

## 5. 图书服务实现

### 5.1 创建BookService接口

创建`src/main/java/com/demo/library/service/BookService.java`：

```java
package com.demo.library.service;

import com.demo.library.dto.*;
import com.github.pagehelper.PageInfo;
import java.math.BigDecimal;
import java.util.List;

/**
 * 图书服务接口
 */
public interface BookService {
    
    /**
     * 根据ID查询图书
     */
    BookDTO getById(Long id);
    
    /**
     * 根据ISBN查询图书
     */
    BookDTO getByIsbn(String isbn);
    
    /**
     * 查询所有图书
     */
    List<BookDTO> getAllBooks();
    
    /**
     * 条件查询图书
     */
    List<BookDTO> getBooksByCondition(BookQueryDTO queryDTO);
    
    /**
     * 分页查询图书
     */
    PageInfo<BookDTO> getBooksByPage(int pageNum, int pageSize, BookQueryDTO queryDTO);
    
    /**
     * 创建图书
     */
    BookDTO createBook(BookCreateDTO createDTO);
    
    /**
     * 更新图书信息
     */
    BookDTO updateBook(Long id, BookUpdateDTO updateDTO);
    
    /**
     * 更新图书状态
     */
    void updateBookStatus(Long id, String status);
    
    /**
     * 更新图书库存
     */
    void updateBookCopies(Long id, Integer totalCopies, Integer availableCopies);
    
    /**
     * 删除图书
     */
    void deleteBook(Long id);
    
    /**
     * 批量删除图书
     */
    void deleteBooks(List<Long> ids);
    
    /**
     * 检查ISBN是否存在
     */
    boolean existsByIsbn(String isbn);
    
    /**
     * 根据分类查询图书
     */
    List<BookDTO> getBooksByCategory(String category);
    
    /**
     * 根据作者查询图书
     */
    List<BookDTO> getBooksByAuthor(String author);
    
    /**
     * 查询可借阅的图书
     */
    List<BookDTO> getAvailableBooks();
    
    /**
     * 查询热门图书
     */
    List<BookDTO> getPopularBooks(Integer limit);
    
    /**
     * 查询新上架图书
     */
    List<BookDTO> getNewBooks(Integer days, Integer limit);
    
    /**
     * 按价格区间查询图书
     */
    List<BookDTO> getBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * 全文搜索图书
     */
    List<BookDTO> searchBooks(String keyword);
    
    /**
     * 获取所有分类
     */
    List<String> getAllCategories();
    
    /**
     * 获取分类统计
     */
    List<BookCategoryStats> getCategoryStats();
    
    /**
     * 借阅图书（减少可借数量）
     */
    void borrowBook(Long bookId, Integer count);
    
    /**
     * 归还图书（增加可借数量）
     */
    void returnBook(Long bookId, Integer count);
}
```

### 5.2 创建图书相关DTO类

创建`src/main/java/com/demo/library/dto/BookDTO.java`：

```java
package com.demo.library.dto;

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
    private LocalDateTime updatedAt;
    private Long createdBy;
    
    // 构造器
    public BookDTO() {}
    
    // Getter和Setter方法（此处省略，实际开发中需要完整实现）
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
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
```

### 5.3 创建BookService实现类

创建`src/main/java/com/demo/library/service/impl/BookServiceImpl.java`：

```java
package com.demo.library.service.impl;

import com.demo.library.entity.Book;
import com.demo.library.dto.*;
import com.demo.library.mapper.BookMapper;
import com.demo.library.service.BookService;
import com.demo.library.common.ResultCode;
import com.demo.library.exception.BusinessException;
import com.demo.library.util.BeanUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图书服务实现类
 */
@Service
@Transactional
public class BookServiceImpl implements BookService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);
    
    @Autowired
    private BookMapper bookMapper;
    
    @Override
    @Transactional(readOnly = true)
    public BookDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        return convertToDTO(book);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BookDTO getByIsbn(String isbn) {
        if (!StringUtils.hasText(isbn)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        Book book = bookMapper.selectByIsbn(isbn);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        return convertToDTO(book);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        List<Book> books = bookMapper.selectAll();
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCondition(BookQueryDTO queryDTO) {
        List<Book> books = bookMapper.selectByCondition(queryDTO);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageInfo<BookDTO> getBooksByPage(int pageNum, int pageSize, BookQueryDTO queryDTO) {
        PageHelper.startPage(pageNum, pageSize);
        List<Book> books = bookMapper.selectByCondition(queryDTO);
        PageInfo<Book> pageInfo = new PageInfo<>(books);
        
        // 转换为DTO分页信息
        PageInfo<BookDTO> dtoPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, dtoPageInfo);
        dtoPageInfo.setList(BeanUtils.copyList(books, BookDTO.class));
        
        return dtoPageInfo;
    }
    
    @Override
    public BookDTO createBook(BookCreateDTO createDTO) {
        if (createDTO == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 验证ISBN唯一性
        if (StringUtils.hasText(createDTO.getIsbn()) && existsByIsbn(createDTO.getIsbn())) {
            throw new BusinessException(ResultCode.DATA_EXISTED.getCode(), "ISBN已存在");
        }
        
        // 转换为实体对象
        Book book = new Book();
        BeanUtils.copyProperties(createDTO, book);
        
        book.setStatus("AVAILABLE");
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认值
        if (book.getTotalCopies() == null) {
            book.setTotalCopies(1);
        }
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        
        int result = bookMapper.insert(book);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "图书创建失败");
        }
        
        logger.info("图书创建成功，图书ID: {}, 书名: {}", book.getId(), book.getTitle());
        return convertToDTO(book);
    }
    
    @Override
    public BookDTO updateBook(Long id, BookUpdateDTO updateDTO) {
        if (id == null || updateDTO == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 检查图书是否存在
        Book existingBook = bookMapper.selectById(id);
        if (existingBook == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        // 检查ISBN唯一性
        if (StringUtils.hasText(updateDTO.getIsbn()) && 
            !updateDTO.getIsbn().equals(existingBook.getIsbn()) &&
            existsByIsbn(updateDTO.getIsbn())) {
            throw new BusinessException(ResultCode.DATA_EXISTED.getCode(), "ISBN已存在");
        }
        
        // 更新图书信息
        Book book = new Book();
        book.setId(id);
        BeanUtils.copyPropertiesIgnoreNull(updateDTO, book);
        book.setUpdatedAt(LocalDateTime.now());
        
        int result = bookMapper.updateByIdSelective(book);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "图书更新失败");
        }
        
        logger.info("图书更新成功，图书ID: {}", id);
        return getById(id);
    }
    
    @Override
    public void updateBookStatus(Long id, String status) {
        if (id == null || !StringUtils.hasText(status)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 验证状态值
        if (!"AVAILABLE".equals(status) && !"UNAVAILABLE".equals(status) && !"MAINTENANCE".equals(status)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "无效的状态值");
        }
        
        int result = bookMapper.updateStatus(id, status);
        if (result <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        logger.info("图书状态更新成功，图书ID: {}, 状态: {}", id, status);
    }
    
    @Override
    public void updateBookCopies(Long id, Integer totalCopies, Integer availableCopies) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        if (totalCopies != null && totalCopies < 0) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "总册数不能小于0");
        }
        
        if (availableCopies != null && availableCopies < 0) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "可借册数不能小于0");
        }
        
        if (totalCopies != null && availableCopies != null && availableCopies > totalCopies) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "可借册数不能大于总册数");
        }
        
        Book book = new Book();
        book.setId(id);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        book.setUpdatedAt(LocalDateTime.now());
        
        int result = bookMapper.updateByIdSelective(book);
        if (result <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        logger.info("图书库存更新成功，图书ID: {}, 总册数: {}, 可借册数: {}", id, totalCopies, availableCopies);
    }
    
    @Override
    public void deleteBook(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        int result = bookMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        logger.info("图书删除成功，图书ID: {}", id);
    }
    
    @Override
    public void deleteBooks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        int result = bookMapper.deleteBatch(ids);
        logger.info("批量删除图书成功，删除数量: {}", result);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByIsbn(String isbn) {
        if (!StringUtils.hasText(isbn)) {
            return false;
        }
        return bookMapper.existsByIsbn(isbn);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        List<Book> books = bookMapper.selectByCategory(category);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByAuthor(String author) {
        if (!StringUtils.hasText(author)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        List<Book> books = bookMapper.selectByAuthor(author);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getAvailableBooks() {
        List<Book> books = bookMapper.selectAvailableBooks();
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getPopularBooks(Integer limit) {
        List<Book> books = bookMapper.selectPopularBooks(limit);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getNewBooks(Integer days, Integer limit) {
        List<Book> books = bookMapper.selectNewBooks(days, limit);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Book> books = bookMapper.selectByPriceRange(minPrice, maxPrice);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookDTO> searchBooks(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ResultCode.PARAM_IS_BLANK);
        }
        
        List<Book> books = bookMapper.searchBooks(keyword);
        return BeanUtils.copyList(books, BookDTO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return bookMapper.selectAllCategories();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookCategoryStats> getCategoryStats() {
        return bookMapper.selectCategoryStats();
    }
    
    @Override
    public void borrowBook(Long bookId, Integer count) {
        if (bookId == null || count == null || count <= 0) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        if (!"AVAILABLE".equals(book.getStatus())) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }
        
        if (book.getAvailableCopies() < count) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE.getCode(), "可借数量不足");
        }
        
        int result = bookMapper.decreaseAvailableCopies(bookId, count);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "借阅操作失败");
        }
        
        logger.info("图书借阅成功，图书ID: {}, 借阅数量: {}", bookId, count);
    }
    
    @Override
    public void returnBook(Long bookId, Integer count) {
        if (bookId == null || count == null || count <= 0) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        int result = bookMapper.increaseAvailableCopies(bookId, count);
        if (result <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        
        logger.info("图书归还成功，图书ID: {}, 归还数量: {}", bookId, count);
    }
    
    /**
     * 转换为DTO对象
     */
    private BookDTO convertToDTO(Book book) {
        if (book == null) {
            return null;
        }
        
        BookDTO dto = new BookDTO();
        BeanUtils.copyProperties(book, dto);
        return dto;
    }
}
```

## 6. 借阅服务实现

### 6.1 创建BorrowService接口

创建`src/main/java/com/demo/library/service/BorrowService.java`：

```java
package com.demo.library.service;

import com.demo.library.dto.*;
import com.github.pagehelper.PageInfo;
import java.util.List;

/**
 * 借阅服务接口
 */
public interface BorrowService {
    
    /**
     * 借阅图书
     */
    BorrowRecordDTO borrowBook(Long userId, Long bookId, String borrowNotes);
    
    /**
     * 归还图书
     */
    BorrowRecordDTO returnBook(Long recordId, String returnNotes);
    
    /**
     * 续借图书
     */
    BorrowRecordDTO renewBook(Long recordId, int days);
    
    /**
     * 根据ID查询借阅记录
     */
    BorrowRecordDTO getById(Long id);
    
    /**
     * 查询借阅记录详情
     */
    BorrowRecordDetailDTO getDetailById(Long id);
    
    /**
     * 查询用户当前借阅记录
     */
    List<BorrowRecordDetailDTO> getCurrentBorrowsByUser(Long userId);
    
    /**
     * 查询用户借阅历史
     */
    List<BorrowRecordDetailDTO> getBorrowHistoryByUser(Long userId);
    
    /**
     * 分页查询借阅记录
     */
    PageInfo<BorrowRecordDetailDTO> getBorrowRecordsByPage(int pageNum, int pageSize, BorrowRecordQueryDTO queryDTO);
    
    /**
     * 查询逾期记录
     */
    List<BorrowRecordDetailDTO> getOverdueRecords();
    
    /**
     * 查询即将到期记录
     */
    List<BorrowRecordDetailDTO> getSoonExpireRecords(int days);
    
    /**
     * 检查用户是否可以借阅图书
     */
    boolean canBorrowBook(Long userId, Long bookId);
    
    /**
     * 获取用户借阅统计
     */
    UserBorrowStats getUserBorrowStats(Long userId);
    
    /**
     * 获取图书借阅统计
     */
    List<BookBorrowStats> getBookBorrowStats(Integer limit);
    
    /**
     * 获取月度借阅统计
     */
    List<MonthlyBorrowStats> getMonthlyBorrowStats(Integer year);
}
```

### 6.2 创建BorrowService实现类

创建`src/main/java/com/demo/library/service/impl/BorrowServiceImpl.java`：

```java
package com.demo.library.service.impl;

import com.demo.library.entity.BorrowRecord;
import com.demo.library.dto.*;
import com.demo.library.mapper.BorrowRecordMapper;
import com.demo.library.service.BorrowService;
import com.demo.library.service.UserService;
import com.demo.library.service.BookService;
import com.demo.library.common.ResultCode;
import com.demo.library.exception.BusinessException;
import com.demo.library.util.BeanUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅服务实现类
 */
@Service
@Transactional
public class BorrowServiceImpl implements BorrowService {
    
    private static final Logger logger = LoggerFactory.getLogger(BorrowServiceImpl.class);
    
    private static final int DEFAULT_BORROW_DAYS = 30; // 默认借阅天数
    private static final int MAX_BORROW_COUNT = 5; // 最大借阅数量
    
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Override
    public BorrowRecordDTO borrowBook(Long userId, Long bookId, String borrowNotes) {
        if (userId == null || bookId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 检查用户是否存在且状态正常
        UserDTO user = userService.getById(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }
        
        // 检查图书是否存在且可借阅
        BookDTO book = bookService.getById(bookId);
        if (!"AVAILABLE".equals(book.getStatus()) || book.getAvailableCopies() <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }
        
        // 检查用户是否可以借阅该图书
        if (!canBorrowBook(userId, bookId)) {
            throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
        }
        
        // 检查用户是否已借阅该图书且未归还
        if (borrowRecordMapper.existsActiveBorrow(userId, bookId)) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_BORROWED);
        }
        
        // 创建借阅记录
        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUserId(userId);
        borrowRecord.setBookId(bookId);
        borrowRecord.setBorrowDate(LocalDateTime.now());
        borrowRecord.setDueDate(LocalDateTime.now().plusDays(DEFAULT_BORROW_DAYS));
        borrowRecord.setStatus("BORROWED");
        borrowRecord.setBorrowNotes(borrowNotes);
        borrowRecord.setCreatedAt(LocalDateTime.now());
        borrowRecord.setUpdatedAt(LocalDateTime.now());
        
        int result = borrowRecordMapper.insert(borrowRecord);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "借阅记录创建失败");
        }
        
        // 减少图书可借数量
        bookService.borrowBook(bookId, 1);
        
        logger.info("图书借阅成功，用户ID: {}, 图书ID: {}, 记录ID: {}", userId, bookId, borrowRecord.getId());
        
        return convertToDTO(borrowRecord);
    }
    
    @Override
    public BorrowRecordDTO returnBook(Long recordId, String returnNotes) {
        if (recordId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        BorrowRecord borrowRecord = borrowRecordMapper.selectById(recordId);
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }
        
        if (!"BORROWED".equals(borrowRecord.getStatus())) {
            throw new BusinessException(ResultCode.BOOK_NOT_BORROWED);
        }
        
        // 更新借阅记录
        int result = borrowRecordMapper.updateReturnInfo(recordId, LocalDateTime.now(), "RETURNED", returnNotes);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "归还记录更新失败");
        }
        
        // 增加图书可借数量
        bookService.returnBook(borrowRecord.getBookId(), 1);
        
        logger.info("图书归还成功，记录ID: {}", recordId);
        
        // 重新查询更新后的记录
        borrowRecord = borrowRecordMapper.selectById(recordId);
        return convertToDTO(borrowRecord);
    }
    
    @Override
    public BorrowRecordDTO renewBook(Long recordId, int days) {
        if (recordId == null || days <= 0) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        BorrowRecord borrowRecord = borrowRecordMapper.selectById(recordId);
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }
        
        if (!"BORROWED".equals(borrowRecord.getStatus())) {
            throw new BusinessException(ResultCode.BOOK_NOT_BORROWED);
        }
        
        // 检查是否已逾期（逾期不能续借）
        if (borrowRecord.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.RETURN_DATE_INVALID.getCode(), "已逾期，不能续借");
        }
        
        // 更新到期日期
        borrowRecord.setDueDate(borrowRecord.getDueDate().plusDays(days));
        borrowRecord.setUpdatedAt(LocalDateTime.now());
        
        int result = borrowRecordMapper.updateById(borrowRecord);
        if (result <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_INNER_ERROR.getCode(), "续借失败");
        }
        
        logger.info("图书续借成功，记录ID: {}, 续借天数: {}", recordId, days);
        
        return convertToDTO(borrowRecord);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowRecordDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        BorrowRecord borrowRecord = borrowRecordMapper.selectById(id);
        if (borrowRecord == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }
        
        return convertToDTO(borrowRecord);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowRecordDetailDTO getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        BorrowRecordDetailDTO detail = borrowRecordMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }
        
        return detail;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordDetailDTO> getCurrentBorrowsByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        return borrowRecordMapper.selectCurrentBorrowDetailsByUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordDetailDTO> getBorrowHistoryByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        BorrowRecordQueryDTO queryDTO = new BorrowRecordQueryDTO();
        queryDTO.setUserId(userId);
        
        return borrowRecordMapper.selectDetailsByCondition(queryDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageInfo<BorrowRecordDetailDTO> getBorrowRecordsByPage(int pageNum, int pageSize, BorrowRecordQueryDTO queryDTO) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecordDetailDTO> records = borrowRecordMapper.selectDetailsByCondition(queryDTO);
        return new PageInfo<>(records);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordDetailDTO> getOverdueRecords() {
        return borrowRecordMapper.selectOverdueRecordDetails();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordDetailDTO> getSoonExpireRecords(int days) {
        List<BorrowRecord> records = borrowRecordMapper.selectSoonExpireRecords(days);
        // 这里需要转换为详情DTO，实际项目中可以在Mapper中直接查询详情
        return null; // 简化处理
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canBorrowBook(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            return false;
        }
        
        // 检查用户当前借阅数量是否超限
        List<BorrowRecord> currentBorrows = borrowRecordMapper.selectCurrentBorrowsByUserId(userId);
        if (currentBorrows.size() >= MAX_BORROW_COUNT) {
            return false;
        }
        
        // 检查用户是否有逾期记录
        List<BorrowRecord> overdueRecords = borrowRecordMapper.selectOverdueRecords();
        for (BorrowRecord record : overdueRecords) {
            if (record.getUserId().equals(userId)) {
                return false; // 有逾期记录不能借书
            }
        }
        
        return true;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserBorrowStats getUserBorrowStats(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        
        // 这里需要在Mapper中实现具体的统计查询
        // 简化处理，返回null
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookBorrowStats> getBookBorrowStats(Integer limit) {
        return borrowRecordMapper.selectPopularBooksStats(limit);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MonthlyBorrowStats> getMonthlyBorrowStats(Integer year) {
        return borrowRecordMapper.selectMonthlyBorrowStats(year);
    }
    
    /**
     * 转换为DTO对象
     */
    private BorrowRecordDTO convertToDTO(BorrowRecord borrowRecord) {
        if (borrowRecord == null) {
            return null;
        }
        
        BorrowRecordDTO dto = new BorrowRecordDTO();
        BeanUtils.copyProperties(borrowRecord, dto);
        return dto;
    }
}
```

## 7. 事务配置

### 7.1 添加事务管理配置

在`application.yml`中添加事务配置：

```yaml
spring:
  # 数据源配置
  datasource:
    # 事务相关配置
    hikari:
      # 连接超时时间
      connection-timeout: 30000
      # 连接最大空闲时间
      idle-timeout: 600000
      # 连接最大生命周期
      max-lifetime: 1800000
      # 最大连接数
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 5
      
  # JPA事务配置
  jpa:
    properties:
      hibernate:
        # 事务隔离级别
        connection.isolation: 2
        # 自动提交
        connection.autocommit: false

# 事务管理器配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 7.2 创建事务配置类

创建`src/main/java/com/demo/library/config/TransactionConfig.java`：

```java
package com.demo.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务配置类
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // Spring Boot自动配置事务管理器，这里只需要启用事务管理
}
```

## 8. 全局异常处理

### 8.1 创建全局异常处理器

创建`src/main/java/com/demo/library/exception/GlobalExceptionHandler.java`：

```java
package com.demo.library.exception;

import com.demo.library.common.Result;
import com.demo.library.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        StringBuilder message = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            message.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage()).append("; ");
        }
        logger.warn("参数验证异常: {}", message.toString());
        return Result.error(ResultCode.PARAM_IS_INVALID.getCode(), message.toString());
    }
    
    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e) {
        StringBuilder message = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            message.append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage()).append("; ");
        }
        logger.warn("参数绑定异常: {}", message.toString());
        return Result.error(ResultCode.PARAM_TYPE_BIND_ERROR.getCode(), message.toString());
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e) {
        StringBuilder message = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            message.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
        }
        logger.warn("约束违反异常: {}", message.toString());
        return Result.error(ResultCode.PARAM_IS_INVALID.getCode(), message.toString());
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("参数类型不匹配异常: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_TYPE_BIND_ERROR.getCode(), "参数类型错误");
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        logger.error("系统异常: ", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
```

## 9. 单元测试

### 9.1 创建Service测试类

创建`src/test/java/com/demo/library/service/UserServiceTest.java`：

```java
package com.demo.library.service;

import com.demo.library.dto.UserCreateDTO;
import com.demo.library.dto.UserDTO;
import com.demo.library.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testCreateUser() {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("testuser");
        createDTO.setPassword("password123");
        createDTO.setEmail("test@example.com");
        createDTO.setFullName("Test User");
        
        UserDTO userDTO = userService.createUser(createDTO);
        
        assertNotNull(userDTO);
        assertNotNull(userDTO.getId());
        assertEquals("testuser", userDTO.getUsername());
        assertEquals("test@example.com", userDTO.getEmail());
    }
    
    @Test
    void testCreateUserWithDuplicateUsername() {
        UserCreateDTO createDTO1 = new UserCreateDTO();
        createDTO1.setUsername("duplicate");
        createDTO1.setPassword("password123");
        createDTO1.setEmail("test1@example.com");
        createDTO1.setFullName("Test User 1");
        
        UserCreateDTO createDTO2 = new UserCreateDTO();
        createDTO2.setUsername("duplicate");
        createDTO2.setPassword("password123");
        createDTO2.setEmail("test2@example.com");
        createDTO2.setFullName("Test User 2");
        
        userService.createUser(createDTO1);
        
        assertThrows(BusinessException.class, () -> {
            userService.createUser(createDTO2);
        });
    }
    
    @Test
    void testGetById() {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("getbyidtest");
        createDTO.setPassword("password123");
        createDTO.setEmail("getbyid@example.com");
        createDTO.setFullName("GetById Test");
        
        UserDTO created = userService.createUser(createDTO);
        UserDTO retrieved = userService.getById(created.getId());
        
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getUsername(), retrieved.getUsername());
    }
    
    @Test
    void testLogin() {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("logintest");
        createDTO.setPassword("password123");
        createDTO.setEmail("login@example.com");
        createDTO.setFullName("Login Test");
        
        userService.createUser(createDTO);
        
        UserDTO loginResult = userService.login("logintest", "password123");
        assertNotNull(loginResult);
        assertEquals("logintest", loginResult.getUsername());
        
        assertThrows(BusinessException.class, () -> {
            userService.login("logintest", "wrongpassword");
        });
    }
}
```

## 10. 总结

通过本步骤，我们完成了：

✅ **通用组件开发**
- 统一响应结果类和响应码枚举
- 业务异常类和全局异常处理器
- Bean拷贝工具类

✅ **用户服务实现**
- UserService接口定义和实现
- 用户相关DTO类创建
- 用户管理的完整业务逻辑

✅ **图书服务实现**
- BookService接口定义和实现
- 图书相关DTO类创建
- 图书管理的完整业务逻辑

✅ **借阅服务实现**
- BorrowService接口定义和实现
- 借阅业务核心功能：借阅、归还、续借
- 业务规则和数据验证

✅ **事务和异常处理**
- Spring事务管理配置
- 全局异常处理机制
- 业务异常统一处理

✅ **数据验证和转换**
- 参数验证注解使用
- DTO与Entity转换
- 业务数据校验

## 下一步

Service层业务逻辑实现完成后，我们将在[Step 7](step7.md)中学习Controller层与REST API开发，包括：
- RESTful API设计原则
- Controller层实现
- 请求参数处理和响应格式统一
- API文档生成
- 接口测试

---

**提示**：Service层是业务逻辑的核心，要注重代码的可读性、可维护性和可测试性。合理使用事务管理和异常处理机制，确保系统的稳定性和数据一致性。