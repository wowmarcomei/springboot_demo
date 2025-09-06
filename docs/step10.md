# Step 10: 异常处理与日志配置

## 学习目标

通过本步骤，你将学会：
- 设计全局异常处理机制
- 创建自定义异常类体系
- 配置日志系统和管理策略
- 实现错误页面和响应格式
- 集成系统监控和诊断工具
- 优化异常处理性能

## 前置要求

- 已完成前9个步骤
- 理解Spring Boot异常处理机制
- 熟悉日志框架基本概念
- 了解HTTP状态码和错误响应格式

## 1. 异常处理架构设计

### 1.1 异常分类
- **业务异常**：用户操作引起的可预期异常
- **系统异常**：系统内部错误
- **参数异常**：请求参数验证失败
- **安全异常**：权限验证失败
- **第三方异常**：外部服务调用异常

### 1.2 异常处理策略
```
Request → Controller → Service → Exception
                    ↓
         Global Exception Handler
                    ↓
         Unified Error Response
```

## 2. 自定义异常类设计

### 2.1 基础异常类

创建`src/main/java/com/demo/library/exception/BaseException.java`：

```java
package com.demo.library.exception;

/**
 * 基础异常类
 */
public class BaseException extends RuntimeException {
    
    private final String code;
    private final String message;
    private final Object data;
    
    public BaseException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = null;
    }
    
    public BaseException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public BaseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.data = null;
    }
    
    public BaseException(String code, String message, Object data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public Object getData() {
        return data;
    }
}
```

### 2.2 业务异常类

创建`src/main/java/com/demo/library/exception/BusinessException.java`：

```java
package com.demo.library.exception;

/**
 * 业务异常类
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }
    
    public BusinessException(String code, String message) {
        super(code, message);
    }
    
    public BusinessException(String code, String message, Object data) {
        super(code, message, data);
    }
    
    public BusinessException(String message, Throwable cause) {
        super("BUSINESS_ERROR", message, cause);
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

### 2.3 参数验证异常类

创建`src/main/java/com/demo/library/exception/ValidationException.java`：

```java
package com.demo.library.exception;

import java.util.Map;

/**
 * 参数验证异常类
 */
public class ValidationException extends BaseException {
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", message, fieldErrors);
    }
    
    public ValidationException(String code, String message, Map<String, String> fieldErrors) {
        super(code, message, fieldErrors);
    }
}
```

### 2.4 资源异常类

创建`src/main/java/com/demo/library/exception/ResourceException.java`：

```java
package com.demo.library.exception;

/**
 * 资源相关异常类
 */
public class ResourceException extends BaseException {
    
    public static class ResourceNotFoundException extends ResourceException {
        public ResourceNotFoundException(String resourceName, String resourceId) {
            super("RESOURCE_NOT_FOUND", String.format("%s with id %s not found", resourceName, resourceId));
        }
        
        public ResourceNotFoundException(String message) {
            super("RESOURCE_NOT_FOUND", message);
        }
    }
    
    public static class ResourceAlreadyExistsException extends ResourceException {
        public ResourceAlreadyExistsException(String resourceName, String resourceId) {
            super("RESOURCE_ALREADY_EXISTS", String.format("%s with id %s already exists", resourceName, resourceId));
        }
        
        public ResourceAlreadyExistsException(String message) {
            super("RESOURCE_ALREADY_EXISTS", message);
        }
    }
    
    public static class ResourceAccessDeniedException extends ResourceException {
        public ResourceAccessDeniedException(String resourceName) {
            super("RESOURCE_ACCESS_DENIED", String.format("Access denied to resource: %s", resourceName));
        }
        
        public ResourceAccessDeniedException(String message) {
            super("RESOURCE_ACCESS_DENIED", message);
        }
    }
    
    protected ResourceException(String code, String message) {
        super(code, message);
    }
    
    protected ResourceException(String code, String message, Object data) {
        super(code, message, data);
    }
}
```

### 2.5 认证授权异常类

创建`src/main/java/com/demo/library/exception/SecurityException.java`：

```java
package com.demo.library.exception;

/**
 * 安全相关异常类
 */
public class SecurityException extends BaseException {
    
    public static class AuthenticationException extends SecurityException {
        public AuthenticationException(String message) {
            super("AUTHENTICATION_ERROR", message);
        }
        
        public AuthenticationException(String message, Throwable cause) {
            super("AUTHENTICATION_ERROR", message, cause);
        }
    }
    
    public static class AuthorizationException extends SecurityException {
        public AuthorizationException(String message) {
            super("AUTHORIZATION_ERROR", message);
        }
        
        public AuthorizationException(String message, Throwable cause) {
            super("AUTHORIZATION_ERROR", message, cause);
        }
    }
    
    public static class TokenExpiredException extends SecurityException {
        public TokenExpiredException(String message) {
            super("TOKEN_EXPIRED", message);
        }
    }
    
    public static class InvalidTokenException extends SecurityException {
        public InvalidTokenException(String message) {
            super("INVALID_TOKEN", message);
        }
    }
    
    protected SecurityException(String code, String message) {
        super(code, message);
    }
    
    protected SecurityException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

## 3. 统一响应格式

### 3.1 响应结果类

创建`src/main/java/com/demo/library/common/Result.java`：

```java
package com.demo.library.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    
    private Boolean success;
    private String code;
    private String message;
    private T data;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    
    // 私有构造函数
    private Result() {
        this.timestamp = LocalDateTime.now();
    }
    
    private Result(Boolean success, String code, String message, T data) {
        this();
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // 成功响应
    public static <T> Result<T> success() {
        return new Result<>(true, "SUCCESS", "操作成功", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "SUCCESS", "操作成功", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, "SUCCESS", message, data);
    }
    
    public static <T> Result<T> success(String code, String message, T data) {
        return new Result<>(true, code, message, data);
    }
    
    // 失败响应
    public static <T> Result<T> error(String message) {
        return new Result<>(false, "ERROR", message, null);
    }
    
    public static <T> Result<T> error(String code, String message) {
        return new Result<>(false, code, message, null);
    }
    
    public static <T> Result<T> error(String code, String message, T data) {
        return new Result<>(false, code, message, data);
    }
    
    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
```

### 3.2 错误详情类

创建`src/main/java/com/demo/library/common/ErrorDetail.java`：

```java
package com.demo.library.common;

import java.util.Map;

/**
 * 错误详情类
 */
public class ErrorDetail {
    
    private String field;
    private String message;
    private Object rejectedValue;
    private Map<String, Object> parameters;
    
    public ErrorDetail() {}
    
    public ErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }
    
    public ErrorDetail(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }
    
    // Getters and Setters
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Object getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
```

## 4. 全局异常处理器

创建`src/main/java/com/demo/library/handler/GlobalExceptionHandler.java`：

```java
package com.demo.library.handler;

import com.demo.library.common.ErrorDetail;
import com.demo.library.common.Result;
import com.demo.library.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常: {}", e.getMessage(), e);
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage(), e.getData());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 资源异常处理
     */
    @ExceptionHandler(ResourceException.ResourceNotFoundException.class)
    public ResponseEntity<Result<Object>> handleResourceNotFoundException(
            ResourceException.ResourceNotFoundException e, HttpServletRequest request) {
        logger.warn("资源不存在: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
    
    @ExceptionHandler(ResourceException.ResourceAlreadyExistsException.class)
    public ResponseEntity<Result<Object>> handleResourceAlreadyExistsException(
            ResourceException.ResourceAlreadyExistsException e, HttpServletRequest request) {
        logger.warn("资源已存在: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }
    
    @ExceptionHandler(ResourceException.ResourceAccessDeniedException.class)
    public ResponseEntity<Result<Object>> handleResourceAccessDeniedException(
            ResourceException.ResourceAccessDeniedException e, HttpServletRequest request) {
        logger.warn("资源访问被拒绝: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
    
    /**
     * 验证异常处理
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Result<Object>> handleValidationException(ValidationException e, HttpServletRequest request) {
        logger.warn("参数验证异常: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage(), e.getData());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 方法参数验证异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        logger.warn("方法参数验证失败: {}", e.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<ErrorDetail> errorDetails = new ArrayList<>();
        
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            errorDetails.add(new ErrorDetail(fieldError.getField(), 
                fieldError.getDefaultMessage(), fieldError.getRejectedValue()));
        }
        
        Result<Object> result = Result.error("VALIDATION_ERROR", "参数验证失败", errorDetails);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e, HttpServletRequest request) {
        logger.warn("参数绑定异常: {}", e.getMessage());
        
        List<ErrorDetail> errorDetails = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorDetails.add(new ErrorDetail(fieldError.getField(), 
                fieldError.getDefaultMessage(), fieldError.getRejectedValue()));
        }
        
        Result<Object> result = Result.error("BIND_ERROR", "参数绑定失败", errorDetails);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 约束验证异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        logger.warn("约束验证异常: {}", e.getMessage());
        
        List<ErrorDetail> errorDetails = e.getConstraintViolations().stream()
            .map(violation -> new ErrorDetail(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()))
            .collect(Collectors.toList());
        
        Result<Object> result = Result.error("CONSTRAINT_VIOLATION", "约束验证失败", errorDetails);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 安全异常处理
     */
    @ExceptionHandler(SecurityException.AuthenticationException.class)
    public ResponseEntity<Result<Object>> handleAuthenticationException(
            SecurityException.AuthenticationException e, HttpServletRequest request) {
        logger.warn("认证异常: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
    
    @ExceptionHandler(SecurityException.AuthorizationException.class)
    public ResponseEntity<Result<Object>> handleAuthorizationException(
            SecurityException.AuthorizationException e, HttpServletRequest request) {
        logger.warn("授权异常: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
    
    @ExceptionHandler(SecurityException.TokenExpiredException.class)
    public ResponseEntity<Result<Object>> handleTokenExpiredException(
            SecurityException.TokenExpiredException e, HttpServletRequest request) {
        logger.warn("令牌过期异常: {}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
    
    /**
     * Spring Security异常处理
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Object>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        logger.warn("Spring Security认证异常: {}", e.getMessage());
        
        Result<Object> result = Result.error("AUTHENTICATION_ERROR", "认证失败: " + e.getMessage());
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Object>> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        logger.warn("凭据错误: {}", e.getMessage());
        
        Result<Object> result = Result.error("BAD_CREDENTIALS", "用户名或密码错误");
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Object>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        logger.warn("访问被拒绝: {}", e.getMessage());
        
        Result<Object> result = Result.error("ACCESS_DENIED", "访问被拒绝，权限不足");
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
    
    /**
     * HTTP方法不支持异常处理
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        logger.warn("HTTP方法不支持: {}", e.getMessage());
        
        String supportedMethods = String.join(", ", e.getSupportedMethods());
        String message = String.format("请求方法 %s 不支持，支持的方法: %s", e.getMethod(), supportedMethods);
        
        Result<Object> result = Result.error("METHOD_NOT_SUPPORTED", message);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(result);
    }
    
    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        logger.warn("缺少请求参数: {}", e.getMessage());
        
        String message = String.format("缺少必需的请求参数: %s", e.getParameterName());
        Result<Object> result = Result.error("MISSING_PARAMETER", message);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 方法参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        logger.warn("方法参数类型不匹配: {}", e.getMessage());
        
        String message = String.format("参数 %s 的值 %s 类型不正确，期望类型: %s", 
            e.getName(), e.getValue(), e.getRequiredType().getSimpleName());
        
        Result<Object> result = Result.error("ARGUMENT_TYPE_MISMATCH", message);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
    
    /**
     * 404异常处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("找不到处理器: {}", e.getMessage());
        
        String message = String.format("找不到对应的处理器: %s %s", e.getHttpMethod(), e.getRequestURL());
        Result<Object> result = Result.error("NO_HANDLER_FOUND", message);
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
    
    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleGenericException(Exception e, HttpServletRequest request) {
        logger.error("系统异常: ", e);
        
        Result<Object> result = Result.error("INTERNAL_ERROR", "系统内部错误，请联系管理员");
        result.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
```

## 5. 日志配置

### 5.1 Logback配置

创建`src/main/resources/logback-spring.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 定义日志文件的存储地址和前缀 -->
    <property name="LOG_PATH" value="logs" />
    <property name="LOG_PREFIX" value="library-system" />
    
    <!-- 彩色日志依赖的渲染类 -->
    <conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter" />
    <conversionRule conversionWord="wex" converterClass="org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter" />
    <conversionRule conversionWord="wEx" converterClass="org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter" />
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} 
                %clr([%15.15t]){faint} 
                %clr(%-5level){red} 
                %clr(%-40.40logger{39}){cyan} 
                %clr(:){faint} %m%n%wEx
            </pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 普通日志文件输出 -->
    <appender name="INFO_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_PREFIX}-info.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_PREFIX}-info.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>50MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>INFO</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
    </appender>
    
    <!-- 错误日志文件输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_PREFIX}-error.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_PREFIX}-error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>50MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>500MB</totalSizeCap>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
    </appender>
    
    <!-- 调试日志文件输出 -->
    <appender name="DEBUG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_PREFIX}-debug.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_PREFIX}-debug.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>7</maxHistory>
            <totalSizeCap>500MB</totalSizeCap>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>DEBUG</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
    </appender>
    
    <!-- 异步输出 -->
    <appender name="ASYNC_INFO" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>1024</queueSize>
        <appender-ref ref="INFO_FILE"/>
    </appender>
    
    <appender name="ASYNC_ERROR" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>1024</queueSize>
        <appender-ref ref="ERROR_FILE"/>
    </appender>
    
    <!-- 特定包的日志配置 -->
    <logger name="com.demo.library" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_INFO"/>
        <appender-ref ref="ASYNC_ERROR"/>
        <appender-ref ref="DEBUG_FILE"/>
    </logger>
    
    <!-- SQL日志 -->
    <logger name="com.demo.library.mapper" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="DEBUG_FILE"/>
    </logger>
    
    <!-- Spring框架日志 -->
    <logger name="org.springframework" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_INFO"/>
    </logger>
    
    <!-- Spring Security日志 -->
    <logger name="org.springframework.security" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="DEBUG_FILE"/>
    </logger>
    
    <!-- Hibernate日志 -->
    <logger name="org.hibernate" level="WARN" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_INFO"/>
    </logger>
    
    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_INFO"/>
        <appender-ref ref="ASYNC_ERROR"/>
    </root>
    
    <!-- 环境特定配置 -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="DEBUG_FILE"/>
        </root>
    </springProfile>
    
    <springProfile name="test">
        <root level="WARN">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="ASYNC_INFO"/>
            <appender-ref ref="ASYNC_ERROR"/>
        </root>
    </springProfile>
</configuration>
```

### 5.2 应用配置更新

在`application.yml`中添加日志配置：

```yaml
# 日志配置
logging:
  config: classpath:logback-spring.xml
  level:
    com.demo.library: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.apache.ibatis: DEBUG
    com.demo.library.mapper: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"

---
# 开发环境配置
spring:
  config:
    activate:
      on-profile: dev
logging:
  level:
    root: DEBUG

---
# 测试环境配置
spring:
  config:
    activate:
      on-profile: test
logging:
  level:
    root: WARN
    com.demo.library: INFO

---
# 生产环境配置
spring:
  config:
    activate:
      on-profile: prod
logging:
  level:
    root: INFO
    com.demo.library: INFO
  file:
    name: logs/library-system.log
```

## 6. 操作日志记录

### 6.1 操作日志实体

创建`src/main/java/com/demo/library/entity/OperationLog.java`：

```java
package com.demo.library.entity;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
public class OperationLog {
    
    private Long id;
    private String username;
    private String operation;
    private String method;
    private String params;
    private String result;
    private String ip;
    private String userAgent;
    private Long executionTime;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    
    // 构造函数
    public OperationLog() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### 6.2 操作日志注解

创建`src/main/java/com/demo/library/annotation/OperationLog.java`：

```java
package com.demo.library.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    
    /**
     * 操作描述
     */
    String value() default "";
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 操作类型
     */
    OperationType type() default OperationType.OTHER;
    
    /**
     * 是否记录参数
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean recordResult() default true;
    
    /**
     * 操作类型枚举
     */
    enum OperationType {
        QUERY("查询"),
        CREATE("新增"),
        UPDATE("修改"),
        DELETE("删除"),
        LOGIN("登录"),
        LOGOUT("登出"),
        EXPORT("导出"),
        IMPORT("导入"),
        OTHER("其他");
        
        private final String description;
        
        OperationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
```

### 6.3 操作日志切面

创建`src/main/java/com/demo/library/aspect/OperationLogAspect.java`：

```java
package com.demo.library.aspect;

import com.demo.library.annotation.OperationLog;
import com.demo.library.entity.OperationLog as OperationLogEntity;
import com.demo.library.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class OperationLogAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ThreadLocal<Long> startTime = new ThreadLocal<>();
    private ThreadLocal<OperationLogEntity> operationLogThreadLocal = new ThreadLocal<>();
    
    @Before("@annotation(operationLog)")
    public void before(JoinPoint joinPoint, OperationLog operationLog) {
        startTime.set(System.currentTimeMillis());
        
        try {
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            // 获取当前用户
            String username = getCurrentUsername();
            
            // 创建操作日志记录
            OperationLogEntity logEntity = new OperationLogEntity();
            logEntity.setUsername(username);
            logEntity.setOperation(operationLog.value());
            logEntity.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
            logEntity.setIp(getClientIP(request));
            logEntity.setUserAgent(request.getHeader("User-Agent"));
            logEntity.setCreatedAt(LocalDateTime.now());
            
            // 记录请求参数
            if (operationLog.recordParams()) {
                String params = objectMapper.writeValueAsString(joinPoint.getArgs());
                logEntity.setParams(params);
            }
            
            operationLogThreadLocal.set(logEntity);
            
        } catch (Exception e) {
            logger.error("记录操作日志前置处理异常", e);
        }
    }
    
    @AfterReturning(pointcut = "@annotation(operationLog)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, OperationLog operationLog, Object result) {
        try {
            OperationLogEntity logEntity = operationLogThreadLocal.get();
            if (logEntity != null) {
                Long executionTime = System.currentTimeMillis() - startTime.get();
                logEntity.setExecutionTime(executionTime);
                logEntity.setStatus("SUCCESS");
                
                // 记录返回结果
                if (operationLog.recordResult() && result != null) {
                    String resultStr = objectMapper.writeValueAsString(result);
                    logEntity.setResult(resultStr);
                }
                
                // 异步保存日志
                operationLogService.saveAsync(logEntity);
            }
        } catch (Exception e) {
            logger.error("记录操作日志后置处理异常", e);
        } finally {
            cleanup();
        }
    }
    
    @AfterThrowing(pointcut = "@annotation(operationLog)", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, OperationLog operationLog, Throwable exception) {
        try {
            OperationLogEntity logEntity = operationLogThreadLocal.get();
            if (logEntity != null) {
                Long executionTime = System.currentTimeMillis() - startTime.get();
                logEntity.setExecutionTime(executionTime);
                logEntity.setStatus("FAILED");
                logEntity.setErrorMessage(exception.getMessage());
                
                // 异步保存日志
                operationLogService.saveAsync(logEntity);
            }
        } catch (Exception e) {
            logger.error("记录操作日志异常处理异常", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn("获取当前用户名异常", e);
        }
        return "ANONYMOUS";
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多级代理情况下，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 清理ThreadLocal
     */
    private void cleanup() {
        startTime.remove();
        operationLogThreadLocal.remove();
    }
}
```

## 7. 系统监控集成

### 7.1 健康检查端点

创建`src/main/java/com/demo/library/health/DatabaseHealthIndicator.java`：

```java
package com.demo.library.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库健康检查
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "OpenGauss")
                    .withDetail("status", "连接正常")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "OpenGauss")
                    .withDetail("error", "数据库连接无效")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "OpenGauss")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 7.2 自定义监控指标

创建`src/main/java/com/demo/library/metrics/CustomMetrics.java`：

```java
package com.demo.library.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自定义指标
 */
@Component
public class CustomMetrics {
    
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Timer requestTimer;
    
    @Autowired
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("login.success")
            .description("登录成功次数")
            .register(meterRegistry);
        
        this.loginFailureCounter = Counter.builder("login.failure")
            .description("登录失败次数")
            .register(meterRegistry);
        
        this.requestTimer = Timer.builder("request.duration")
            .description("请求处理时间")
            .register(meterRegistry);
    }
    
    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
    }
    
    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }
    
    public Timer.Sample startRequestTimer() {
        return Timer.start();
    }
    
    public void recordRequestTime(Timer.Sample sample) {
        sample.stop(requestTimer);
    }
}
```

## 8. 错误页面配置

### 8.1 错误控制器

创建`src/main/java/com/demo/library/controller/ErrorController.java`：

```java
package com.demo.library.controller;

import com.demo.library.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

/**
 * 自定义错误控制器
 */
@Controller
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
        
        // 根据Accept头判断返回格式
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return handleJsonError(statusCode, requestUri, errorMessage);
        } else {
            return handleViewError(statusCode, requestUri, errorMessage, model);
        }
    }
    
    @ResponseBody
    private ResponseEntity<Result<Object>> handleJsonError(Integer statusCode, String requestUri, String errorMessage) {
        HttpStatus status = HttpStatus.valueOf(statusCode != null ? statusCode : 500);
        
        String message;
        String code;
        switch (status.value()) {
            case 404:
                code = "NOT_FOUND";
                message = "请求的资源不存在";
                break;
            case 403:
                code = "FORBIDDEN";
                message = "访问被禁止";
                break;
            case 401:
                code = "UNAUTHORIZED";
                message = "未授权访问";
                break;
            case 500:
            default:
                code = "INTERNAL_ERROR";
                message = "服务器内部错误";
                break;
        }
        
        Result<Object> result = Result.error(code, message);
        result.setPath(requestUri);
        result.setTimestamp(LocalDateTime.now());
        
        return ResponseEntity.status(status).body(result);
    }
    
    private String handleViewError(Integer statusCode, String requestUri, String errorMessage, Model model) {
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestUri", requestUri);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("timestamp", LocalDateTime.now());
        
        // 根据状态码返回不同的错误页面
        if (statusCode != null) {
            switch (statusCode) {
                case 404:
                    return "error/404";
                case 403:
                    return "error/403";
                case 500:
                    return "error/500";
                default:
                    return "error/error";
            }
        }
        
        return "error/error";
    }
}
```

### 8.2 404错误页面

创建`src/main/resources/templates/error/404.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>页面不存在 - 图书管理系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .error-page {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #f8f9fa;
        }
        .error-content {
            text-align: center;
        }
        .error-code {
            font-size: 8rem;
            font-weight: bold;
            color: #6c757d;
            line-height: 1;
        }
    </style>
</head>
<body>
    <div class="error-page">
        <div class="error-content">
            <div class="error-code">404</div>
            <h2 class="mb-4">页面不存在</h2>
            <p class="text-muted mb-4">抱歉，您访问的页面不存在或已被删除。</p>
            <div>
                <a href="/" class="btn btn-primary">返回首页</a>
                <a href="javascript:history.back()" class="btn btn-outline-secondary">返回上页</a>
            </div>
            <div class="mt-4 text-muted small">
                <p>请求路径: <span th:text="${requestUri}"></span></p>
                <p>时间: <span th:text="${#temporals.format(timestamp, 'yyyy-MM-dd HH:mm:ss')}"></span></p>
            </div>
        </div>
    </div>
</body>
</html>
```

## 9. 测试与验证

### 9.1 异常处理测试

创建`src/test/java/com/demo/library/handler/GlobalExceptionHandlerTest.java`：

```java
package com.demo.library.handler;

import com.demo.library.exception.BusinessException;
import com.demo.library.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 全局异常处理器测试
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testBusinessException() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
            .andExpect(jsonPath("$.message").value("业务处理失败"));
    }
    
    @Test
    void testResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpected(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
    
    @RestController
    static class TestController {
        
        @GetMapping("/test/business-exception")
        public void throwBusinessException() {
            throw new BusinessException("业务处理失败");
        }
        
        @GetMapping("/test/resource-not-found")
        public void throwResourceNotFoundException() {
            throw new ResourceException.ResourceNotFoundException("用户", "123");
        }
    }
}
```

### 9.2 日志配置测试

创建测试类来验证日志配置：

```java
package com.demo.library.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 日志配置测试
 */
@SpringBootTest
class LoggingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingTest.class);
    
    @Test
    void testDifferentLogLevels() {
        logger.trace("这是TRACE级别日志");
        logger.debug("这是DEBUG级别日志");
        logger.info("这是INFO级别日志");
        logger.warn("这是WARN级别日志");
        logger.error("这是ERROR级别日志");
    }
    
    @Test
    void testStructuredLogging() {
        logger.info("用户登录成功: username={}, ip={}, time={}", 
            "testuser", "192.168.1.100", System.currentTimeMillis());
    }
}
```

## 10. 生产环境优化

### 10.1 异常处理优化配置

在`application-prod.yml`中添加：

```yaml
# 生产环境异常处理配置
server:
  error:
    include-exception: false
    include-stacktrace: never
    include-message: never

# 日志级别调整
logging:
  level:
    root: WARN
    com.demo.library: INFO
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{50} - %msg%n"

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
```

### 10.2 性能监控

创建`src/main/java/com/demo/library/config/MonitoringConfig.java`：

```java
package com.demo.library.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控配置
 */
@Configuration
public class MonitoringConfig {
    
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }
}
```

## 11. 常见问题解决

### 11.1 日志文件权限问题
```bash
# 确保日志目录权限
mkdir -p logs
chmod 755 logs
```

### 11.2 异常信息泄露问题
```java
// 生产环境中隐藏敏感异常信息
@ExceptionHandler(Exception.class)
public ResponseEntity<Result<Object>> handleGenericException(Exception e, HttpServletRequest request) {
    logger.error("系统异常: ", e);
    
    // 生产环境不暴露具体异常信息
    String message = "系统内部错误";
    if (isDevelopmentMode()) {
        message = e.getMessage();
    }
    
    Result<Object> result = Result.error("INTERNAL_ERROR", message);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
}
```

### 11.3 日志性能优化
```xml
<!-- 使用异步日志提升性能 -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <includeCallerData>false</includeCallerData>
    <appender-ref ref="FILE"/>
</appender>
```

## 12. 总结

通过本步骤，我们完成了：

✅ **异常处理体系**
- 自定义异常类层次结构
- 全局异常处理器实现
- 统一响应格式设计

✅ **日志管理系统**
- Logback配置和日志分级
- 异步日志和性能优化
- 操作日志记录和审计

✅ **系统监控集成**
- Spring Boot Actuator集成
- 自定义健康检查
- 监控指标和告警

✅ **错误页面设计**
- 自定义错误控制器
- 友好的错误页面
- JSON和HTML错误响应

## 下一步

异常处理与日志配置完成后，我们将在[Step 11](step11.md)中学习单元测试与集成测试，包括：
- JUnit 5测试框架详解
- MockMvc API测试实践
- Testcontainers数据库测试
- 测试覆盖率分析
- 测试最佳实践和规范

---

**最佳实践**：
1. 合理设计异常层次结构
2. 避免在生产环境暴露敏感信息
3. 使用异步日志提升性能
4. 定期清理和归档日志文件
5. 监控系统关键指标和告警设置