# Step 3: OpenGauss数据库集成与MyBatis配置

## 学习目标

通过本步骤，你将学会：
- 配置OpenGauss数据源连接
- 集成MyBatis框架到Spring Boot项目
- 理解MyBatis的核心概念和工作原理
- 配置MyBatis的各种参数
- 创建数据库连接测试

## 1. MyBatis框架简介

### 1.1 什么是MyBatis

MyBatis是一个优秀的持久层框架，它：
- **SQL分离**：将SQL语句从Java代码中分离出来
- **灵活映射**：支持复杂的结果映射
- **简化开发**：减少JDBC样板代码
- **可控性强**：完全控制SQL执行

### 1.2 MyBatis vs JPA/Hibernate

| 特性 | MyBatis | JPA/Hibernate |
|------|---------|---------------|
| SQL控制 | 完全控制SQL | 自动生成SQL |
| 学习曲线 | 相对简单 | 较复杂 |
| 复杂查询 | 优势明显 | 相对困难 |
| 性能调优 | 容易优化 | 调优复杂 |
| 数据库迁移 | 需要修改SQL | 相对容易 |

### 1.3 为什么选择MyBatis

对于本项目选择MyBatis的原因：
- **学习友好**：更容易理解数据库操作
- **SQL透明**：可以清楚看到执行的SQL
- **灵活性高**：适合复杂业务逻辑
- **切换方便**：可以方便地切换不同数据库

## 2. 依赖配置

### 2.1 验证pom.xml配置

确保`pom.xml`中包含以下依赖：

```xml
<!-- MyBatis Spring Boot Starter -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- OpenGauss JDBC驱动 -->
<dependency>
    <groupId>org.opengauss</groupId>
    <artifactId>opengauss-jdbc</artifactId>
    <version>5.0.0</version>
</dependency>

<!-- 数据库连接池 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

### 2.2 添加MyBatis增强依赖（可选）

```xml
<!-- MyBatis Plus（可选，提供更多便利功能） -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.4</version>
</dependency>

<!-- PageHelper分页插件（可选） -->
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.4.7</version>
</dependency>
```

## 3. 数据源配置

### 3.1 更新application-dev.yml

完善开发环境的数据库配置：

```yaml
# 开发环境配置
spring:
  # 数据源配置
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://localhost:5432/library_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true
    username: library_user
    password: library@2023
    
    # HikariCP连接池配置
    hikari:
      pool-name: LibraryHikariCP                    # 连接池名称
      maximum-pool-size: 10                        # 最大连接数
      minimum-idle: 5                              # 最小空闲连接数
      connection-timeout: 30000                    # 获取连接超时时间(毫秒)
      idle-timeout: 600000                         # 连接空闲超时时间(毫秒)
      max-lifetime: 1800000                        # 连接最大生存时间(毫秒)
      connection-test-query: SELECT 1              # 连接测试查询
      validation-timeout: 5000                     # 连接验证超时时间
      leak-detection-threshold: 60000              # 连接泄漏检测阈值
  
  # MyBatis配置
mybatis:
  # Mapper XML文件位置
  mapper-locations: classpath:mybatis/mapper/*.xml
  
  # 实体类别名包路径
  type-aliases-package: com.demo.library.entity
  
  # MyBatis核心配置
  configuration:
    # 开启驼峰命名转换 (user_name -> userName)
    map-underscore-to-camel-case: true
    
    # 开启二级缓存
    cache-enabled: true
    
    # 延迟加载
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    
    # 允许多结果集
    multiple-result-sets-enabled: true
    
    # 使用列标签替代列名
    use-column-label: true
    
    # 允许JDBC生成主键
    use-generated-keys: true
    
    # 自动映射策略: NONE, PARTIAL, FULL
    auto-mapping-behavior: PARTIAL
    
    # 自动映射未知列: NONE, WARNING, FAILING
    auto-mapping-unknown-column-behavior: WARNING
    
    # 默认执行器类型: SIMPLE, REUSE, BATCH
    default-executor-type: SIMPLE
    
    # 默认语句超时时间
    default-statement-timeout: 25
    
    # 默认获取数据大小
    default-fetch-size: 100
    
    # 是否开启自动生成主键
    use-actual-param-name: true
    
    # 当结果集中值为null时是否调用映射对象的setter方法
    call-setters-on-nulls: true
    
    # 日志实现
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 日志配置 - 显示SQL执行
logging:
  level:
    com.demo.library.mapper: DEBUG                 # 显示Mapper SQL日志
    org.apache.ibatis: DEBUG                       # MyBatis框架日志
    java.sql.Connection: DEBUG                     # 数据库连接日志
    java.sql.Statement: DEBUG                      # SQL语句日志
    java.sql.PreparedStatement: DEBUG              # 预处理语句日志
```

### 3.2 生产环境配置优化

更新`application-prod.yml`：

```yaml
# 生产环境配置
spring:
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:library_db}?serverTimezone=UTC&useSSL=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
    hikari:
      pool-name: LibraryHikariCP-Prod
      maximum-pool-size: 20                        # 生产环境更大连接池
      minimum-idle: 10
      connection-timeout: 20000                    # 更短的超时时间
      idle-timeout: 300000
      max-lifetime: 900000
      connection-test-query: SELECT 1
      validation-timeout: 3000

mybatis:
  mapper-locations: classpath:mybatis/mapper/*.xml
  type-aliases-package: com.demo.library.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    use-generated-keys: true
    default-statement-timeout: 30
    # 生产环境关闭SQL日志
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

# 生产环境日志配置
logging:
  level:
    com.demo.library: INFO                         # 生产环境降低日志级别
    org.apache.ibatis: WARN
    java.sql: WARN
```

## 4. MyBatis配置类

### 4.1 创建MyBatis配置类

创建`src/main/java/com/demo/library/config/MyBatisConfig.java`：

```java
package com.demo.library.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * MyBatis配置类
 * 配置数据源、SqlSessionFactory、事务管理器
 */
@Configuration
@EnableTransactionManagement  // 启用事务管理
@MapperScan("com.demo.library.mapper")  // 扫描Mapper接口
public class MyBatisConfig {
    
    /**
     * 配置数据源
     * 使用HikariCP连接池
     */
    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * 配置SqlSessionFactory
     */
    @Primary
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) 
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        
        // 设置MyBatis配置文件位置（如果有单独的配置文件）
        // bean.setConfigLocation(new ClassPathResource("mybatis/mybatis-config.xml"));
        
        // 设置Mapper XML文件位置
        bean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath:mybatis/mapper/*.xml")
        );
        
        // 设置实体类别名包
        bean.setTypeAliasesPackage("com.demo.library.entity");
        
        return bean.getObject();
    }
    
    /**
     * 配置事务管理器
     */
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

### 4.2 创建数据源健康检查

创建`src/main/java/com/demo/library/config/DataSourceHealthIndicator.java`：

```java
package com.demo.library.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源健康检查
 */
@Component
public class DataSourceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "OpenGauss")
                    .withDetail("status", "Connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "OpenGauss")
                    .withDetail("status", "Connection Invalid")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "OpenGauss")
                .withDetail("status", "Connection Failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 5. 创建测试实体和Mapper

### 5.1 创建测试实体类

创建`src/main/java/com/demo/library/entity/TestConnection.java`：

```java
package com.demo.library.entity;

/**
 * 测试连接实体类
 */
public class TestConnection {
    private String result;
    private Long currentTime;
    
    public TestConnection() {}
    
    public TestConnection(String result, Long currentTime) {
        this.result = result;
        this.currentTime = currentTime;
    }
    
    // Getter and Setter
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public Long getCurrentTime() { return currentTime; }
    public void setCurrentTime(Long currentTime) { this.currentTime = currentTime; }
    
    @Override
    public String toString() {
        return "TestConnection{" +
                "result='" + result + '\'' +
                ", currentTime=" + currentTime +
                '}';
    }
}
```

### 5.2 创建测试Mapper接口

创建`src/main/java/com/demo/library/mapper/TestMapper.java`：

```java
package com.demo.library.mapper;

import com.demo.library.entity.TestConnection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 测试数据库连接Mapper
 */
@Mapper
public interface TestMapper {
    
    /**
     * 测试数据库连接
     * 使用注解方式编写SQL
     */
    @Select("SELECT 'Database Connected Successfully' as result, " +
            "extract(epoch from now()) * 1000 as current_time")
    TestConnection testConnection();
    
    /**
     * 获取数据库版本信息
     */
    @Select("SELECT version() as result, " +
            "extract(epoch from now()) * 1000 as current_time")
    TestConnection getDatabaseVersion();
    
    /**
     * 测试XML方式的SQL映射
     * 具体SQL在XML文件中定义
     */
    TestConnection testXmlMapping();
}
```

### 5.3 创建Mapper XML文件

创建`src/main/resources/mybatis/mapper/TestMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.demo.library.mapper.TestMapper">
    
    <!-- 结果映射 -->
    <resultMap id="TestConnectionResult" type="com.demo.library.entity.TestConnection">
        <result column="result" property="result"/>
        <result column="current_time" property="currentTime"/>
    </resultMap>
    
    <!-- 测试XML方式的SQL映射 -->
    <select id="testXmlMapping" resultMap="TestConnectionResult">
        SELECT 
            'XML Mapping Test Successful' as result,
            extract(epoch from now()) * 1000 as current_time
    </select>
    
    <!-- 复杂查询示例 -->
    <select id="getSystemInfo" resultMap="TestConnectionResult">
        SELECT 
            CONCAT('OpenGauss Version: ', version()) as result,
            extract(epoch from now()) * 1000 as current_time
    </select>
    
</mapper>
```

## 6. 创建测试Controller

### 6.1 数据库测试Controller

创建`src/main/java/com/demo/library/controller/DatabaseTestController.java`：

```java
package com.demo.library.controller;

import com.demo.library.entity.TestConnection;
import com.demo.library.mapper.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接测试控制器
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseTestController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private TestMapper testMapper;
    
    /**
     * 测试数据源连接
     */
    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            result.put("success", true);
            result.put("message", "数据库连接成功");
            result.put("driverName", connection.getMetaData().getDriverName());
            result.put("databaseName", connection.getMetaData().getDatabaseProductName());
            result.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            result.put("url", connection.getMetaData().getURL());
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试MyBatis注解方式
     */
    @GetMapping("/test-mybatis-annotation")
    public TestConnection testMyBatisAnnotation() {
        return testMapper.testConnection();
    }
    
    /**
     * 测试MyBatis XML方式
     */
    @GetMapping("/test-mybatis-xml")
    public TestConnection testMyBatisXml() {
        return testMapper.testXmlMapping();
    }
    
    /**
     * 获取数据库版本
     */
    @GetMapping("/version")
    public TestConnection getDatabaseVersion() {
        return testMapper.getDatabaseVersion();
    }
    
    /**
     * 获取连接池信息
     */
    @GetMapping("/pool-info")
    public Map<String, Object> getPoolInfo() {
        Map<String, Object> info = new HashMap<>();
        
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            com.zaxxer.hikari.HikariDataSource hikariDS = 
                (com.zaxxer.hikari.HikariDataSource) dataSource;
            
            info.put("poolName", hikariDS.getPoolName());
            info.put("maximumPoolSize", hikariDS.getMaximumPoolSize());
            info.put("minimumIdle", hikariDS.getMinimumIdle());
            info.put("connectionTimeout", hikariDS.getConnectionTimeout());
            info.put("idleTimeout", hikariDS.getIdleTimeout());
            info.put("maxLifetime", hikariDS.getMaxLifetime());
        }
        
        return info;
    }
}
```

## 7. 测试数据库集成

### 7.1 启动应用测试

```bash
# 确保OpenGauss数据库已启动
gs_ctl status -D /path/to/data

# 启动Spring Boot应用
mvn spring-boot:run
```

### 7.2 测试各项功能

**访问以下URL进行测试：**

1. **数据源连接测试**: 
   ```
   GET http://localhost:8080/api/database/test-connection
   ```

2. **MyBatis注解方式测试**: 
   ```
   GET http://localhost:8080/api/database/test-mybatis-annotation
   ```

3. **MyBatis XML方式测试**: 
   ```
   GET http://localhost:8080/api/database/test-mybatis-xml
   ```

4. **数据库版本信息**: 
   ```
   GET http://localhost:8080/api/database/version
   ```

5. **连接池信息**: 
   ```
   GET http://localhost:8080/api/database/pool-info
   ```

6. **健康检查**: 
   ```
   GET http://localhost:8080/actuator/health
   ```

### 7.3 查看SQL执行日志

启动应用后，控制台应该能看到类似日志：
```
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@...] was not registered for synchronization
JDBC Connection [HikariProxyConnection@...] will not be managed by Spring
==>  Preparing: SELECT 'Database Connected Successfully' as result, extract(epoch from now()) * 1000 as current_time
==> Parameters: 
<==    Columns: result, current_time
<==        Row: Database Connected Successfully, 1699123456789
```

## 8. 常见问题解决

### 8.1 连接失败问题

**错误**: `Connection refused`
```bash
# 检查OpenGauss服务状态
gs_ctl status -D /path/to/data

# 检查端口监听
netstat -an | grep 5432

# 检查防火墙
firewall-cmd --list-ports  # CentOS
ufw status                 # Ubuntu
```

**解决方案**:
1. 确保OpenGauss服务已启动
2. 检查`pg_hba.conf`配置允许连接
3. 检查防火墙设置

### 8.2 认证失败问题

**错误**: `Authentication failed`
```sql
-- 检查用户是否存在
SELECT usename FROM pg_user WHERE usename = 'library_user';

-- 重新创建用户和授权
DROP USER IF EXISTS library_user;
CREATE USER library_user WITH PASSWORD 'library@2023';
GRANT ALL PRIVILEGES ON DATABASE library_db TO library_user;
```

### 8.3 MyBatis映射问题

**错误**: `Invalid bound statement`
1. 检查Mapper接口和XML文件的namespace是否一致
2. 检查方法名是否一致
3. 检查XML文件路径配置
4. 重新编译项目: `mvn clean compile`

### 8.4 SQL语法问题

**错误**: OpenGauss特定语法
```sql
-- PostgreSQL/OpenGauss使用extract函数获取时间戳
SELECT extract(epoch from now()) * 1000 as timestamp;

-- 字符串连接使用CONCAT或||
SELECT CONCAT('Hello', ' World') as greeting;
SELECT 'Hello' || ' World' as greeting;
```

## 9. 性能优化配置

### 9.1 连接池优化

```yaml
spring:
  datasource:
    hikari:
      # 根据应用负载调整
      maximum-pool-size: 20
      minimum-idle: 5
      
      # 连接超时配置
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
      # 性能优化
      leak-detection-threshold: 60000
      register-mbeans: true
```

### 9.2 MyBatis缓存配置

```yaml
mybatis:
  configuration:
    # 开启二级缓存
    cache-enabled: true
    
    # 延迟加载
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    
    # 批量执行
    default-executor-type: SIMPLE  # SIMPLE, REUSE, BATCH
```

## 10. 总结

通过本步骤学习，你已经掌握：

✅ **数据库集成**
- OpenGauss连接配置
- HikariCP连接池优化
- 数据源健康检查

✅ **MyBatis框架**
- MyBatis配置和原理
- 注解和XML两种映射方式
- SqlSessionFactory配置

✅ **测试验证**
- 数据库连接测试
- MyBatis功能测试
- 性能监控配置

✅ **问题解决**
- 常见连接问题
- 配置优化方案
- 错误排查方法

## 下一步

数据库集成完成后，我们将在[Step 4](step4.md)中学习实体类设计与数据库表创建，包括：
- 图书管理系统实体设计
- OpenGauss表结构创建
- 实体类与数据库字段映射
- 数据库初始化脚本

---

**提示**：完成本步骤后，确保所有数据库连接测试都能正常运行，这为后续的业务开发奠定了坚实基础。