# Step 12: 项目打包与部署

## 学习目标

通过本步骤，你将学会：
- 配置Maven多环境打包策略
- 实现Docker容器化部署
- 优化生产环境配置
- 进行应用性能调优
- 建立监控和运维体系
- 处理生产环境故障和问题

## 前置要求

- 已完成前11个步骤
- 熟悉Docker容器技术
- 了解Linux服务器运维基础
- 理解CI/CD基本概念

## 1. 多环境配置管理

### 1.1 环境配置分离

#### 开发环境配置
更新`src/main/resources/application-dev.yml`：

```yaml
# 开发环境配置
spring:
  config:
    activate:
      on-profile: dev
  
  # 数据源配置
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://localhost:5432/library_db
    username: library_user
    password: library@2023
    hikari:
      minimum-idle: 5
      maximum-pool-size: 10
      idle-timeout: 300000
      connection-timeout: 30000
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  # Redis配置（如果使用）
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 服务器配置
server:
  port: 8080

# 日志配置
logging:
  level:
    com.demo.library: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# JWT配置
jwt:
  secret: devSecretKeyForLibrarySystemShouldBeLongEnough
  expiration: 7200 # 2小时
  issuer: library-dev-system

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

#### 测试环境配置
更新`src/main/resources/application-test.yml`：

```yaml
# 测试环境配置
spring:
  config:
    activate:
      on-profile: test
  
  # 数据源配置（使用测试数据库）
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://test-db:5432/library_test_db
    username: library_test_user
    password: library_test@2023
    hikari:
      minimum-idle: 2
      maximum-pool-size: 5
      idle-timeout: 300000
      connection-timeout: 30000
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# 服务器配置
server:
  port: 8081

# 日志配置
logging:
  level:
    com.demo.library: INFO
    root: WARN

# JWT配置
jwt:
  secret: testSecretKeyForLibrarySystemShouldBeLongEnough
  expiration: 3600 # 1小时
  issuer: library-test-system
```

#### 生产环境配置
创建`src/main/resources/application-prod.yml`：

```yaml
# 生产环境配置
spring:
  config:
    activate:
      on-profile: prod
  
  # 数据源配置
  datasource:
    driver-class-name: org.opengauss.Driver
    url: ${DB_URL:jdbc:opengauss://prod-db:5432/library_prod_db}
    username: ${DB_USERNAME:library_prod_user}
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        jdbc:
          batch_versioned_data: true
  
  # Redis配置
  redis:
    host: ${REDIS_HOST:prod-redis}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

# 服务器配置
server:
  port: 8080
  tomcat:
    threads:
      min-spare: 20
      max: 200
    max-connections: 8192
    accept-count: 100
  compression:
    enabled: true
    min-response-size: 1024
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
  http2:
    enabled: true

# 日志配置
logging:
  level:
    com.demo.library: INFO
    org.springframework.security: WARN
    org.springframework.web: WARN
    org.apache.ibatis: WARN
    com.zaxxer.hikari: WARN
    root: INFO
  file:
    name: /var/log/library-system/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 1GB

# JWT配置
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400} # 24小时
  issuer: library-prod-system

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    db:
      enabled: true
    redis:
      enabled: true

# 安全配置
security:
  require-ssl: true
```

### 1.2 配置文件加密

创建`src/main/java/com/demo/library/config/EncryptionConfig.java`：

```java
package com.demo.library.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置加密
 */
@Configuration
public class EncryptionConfig {
    
    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(getEncryptionPassword());
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
    
    private String getEncryptionPassword() {
        // 从环境变量或其他安全位置获取加密密码
        return System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
    }
}
```

## 2. Maven打包配置

### 2.1 多环境打包配置

更新`pom.xml`中的profiles部分：

```xml
<profiles>
    <!-- 开发环境 -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>dev</spring.profiles.active>
            <maven.test.skip>false</maven.test.skip>
        </properties>
    </profile>
    
    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <properties>
            <spring.profiles.active>test</spring.profiles.active>
            <maven.test.skip>false</maven.test.skip>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <jvmArguments>-Xmx512m -Xms256m</jvmArguments>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
    
    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
            <maven.test.skip>true</maven.test.skip>
        </properties>
        <build>
            <plugins>
                <!-- Spring Boot打包插件 -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <executable>true</executable>
                        <layers>
                            <enabled>true</enabled>
                        </layers>
                        <jvmArguments>-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication</jvmArguments>
                    </configuration>
                </plugin>
                
                <!-- 资源过滤 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-resources-plugin</artifactId>
                    <configuration>
                        <nonFilteredFileExtensions>
                            <nonFilteredFileExtension>zip</nonFilteredFileExtension>
                            <nonFilteredFileExtension>gz</nonFilteredFileExtension>
                            <nonFilteredFileExtension>pdf</nonFilteredFileExtension>
                            <nonFilteredFileExtension>xls</nonFilteredFileExtension>
                            <nonFilteredFileExtension>xlsx</nonFilteredFileExtension>
                            <nonFilteredFileExtension>doc</nonFilteredFileExtension>
                            <nonFilteredFileExtension>docx</nonFilteredFileExtension>
                            <nonFilteredFileExtension>ppt</nonFilteredFileExtension>
                            <nonFilteredFileExtension>pptx</nonFilteredFileExtension>
                        </nonFilteredFileExtensions>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
    
    <!-- Docker构建环境 -->
    <profile>
        <id>docker</id>
        <build>
            <plugins>
                <!-- Docker Maven插件 -->
                <plugin>
                    <groupId>com.spotify</groupId>
                    <artifactId>dockerfile-maven-plugin</artifactId>
                    <version>1.4.13</version>
                    <configuration>
                        <repository>library-system</repository>
                        <tag>${project.version}</tag>
                        <dockerfile>Dockerfile</dockerfile>
                        <contextDirectory>${project.basedir}</contextDirectory>
                    </configuration>
                    <executions>
                        <execution>
                            <id>default</id>
                            <goals>
                                <goal>build</goal>
                                <goal>push</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 2.2 打包脚本

创建`scripts/build.sh`：

```bash
#!/bin/bash

# 构建脚本
set -e

# 参数配置
PROFILE=${1:-prod}
VERSION=${2:-1.0.0}
REGISTRY=${3:-"library-registry"}

echo "开始构建项目..."
echo "环境: $PROFILE"
echo "版本: $VERSION"

# 清理
echo "清理之前的构建..."
mvn clean

# 运行测试（生产环境跳过）
if [ "$PROFILE" != "prod" ]; then
    echo "运行测试..."
    mvn test
fi

# 打包
echo "开始打包..."
mvn package -P$PROFILE -Dmaven.test.skip=true

# 检查打包结果
if [ -f "target/spring-boot-library-demo-$VERSION.jar" ]; then
    echo "打包成功: target/spring-boot-library-demo-$VERSION.jar"
    
    # 显示文件信息
    ls -lh target/spring-boot-library-demo-$VERSION.jar
else
    echo "打包失败！"
    exit 1
fi

echo "构建完成！"
```

### 2.3 版本管理

创建`scripts/version.sh`：

```bash
#!/bin/bash

# 版本管理脚本
set -e

CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "当前版本: $CURRENT_VERSION"

# 更新版本
if [ $# -eq 1 ]; then
    NEW_VERSION=$1
    echo "更新版本到: $NEW_VERSION"
    
    mvn versions:set -DnewVersion=$NEW_VERSION
    mvn versions:commit
    
    echo "版本更新完成"
else
    echo "使用方法: $0 <新版本号>"
    echo "例如: $0 1.1.0"
fi
```

## 3. Docker容器化部署

### 3.1 Dockerfile

创建项目根目录下的`Dockerfile`：

```dockerfile
# 多阶段构建
FROM openjdk:17-jdk-slim as builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下载依赖（利用Docker缓存）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 构建应用
RUN ./mvnw package -DskipTests -B

# 运行时镜像
FROM openjdk:17-jre-slim

# 安装必要工具
RUN apt-get update && apt-get install -y \
    curl \
    vim \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制构建结果
COPY --from=builder /app/target/spring-boot-library-demo-*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/log/library-system && \
    chown -R appuser:appuser /var/log/library-system /app

# 切换到应用用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM优化参数
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/var/log/library-system/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
```

### 3.2 Docker Compose配置

创建`docker-compose.yml`：

```yaml
version: '3.8'

services:
  # 应用服务
  library-app:
    build:
      context: .
      dockerfile: Dockerfile
    image: library-system:1.0.0
    container_name: library-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:opengauss://library-db:5432/library_prod_db
      - DB_USERNAME=library_user
      - DB_PASSWORD=library@2023
      - REDIS_HOST=library-redis
      - REDIS_PASSWORD=redis@2023
      - JWT_SECRET=prodSecretKeyForLibrarySystemShouldBeLongEnoughForProduction
    volumes:
      - ./logs:/var/log/library-system
      - ./config:/app/config
    depends_on:
      - library-db
      - library-redis
    networks:
      - library-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # OpenGauss数据库
  library-db:
    image: enmotech/opengauss:3.0.0
    container_name: library-db
    restart: unless-stopped
    environment:
      - GS_PASSWORD=Enmo@123
      - GS_DB=library_prod_db
      - GS_USER=library_user
      - GS_PASSWORD=library@2023
    ports:
      - "5432:5432"
    volumes:
      - library-db-data:/var/lib/opengauss/data
      - ./database/init:/docker-entrypoint-initdb.d
    networks:
      - library-network
    healthcheck:
      test: ["CMD-SHELL", "gsql -U library_user -d library_prod_db -c 'SELECT 1;' || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Redis缓存
  library-redis:
    image: redis:7-alpine
    container_name: library-redis
    restart: unless-stopped
    command: redis-server --appendonly yes --requirepass redis@2023
    ports:
      - "6379:6379"
    volumes:
      - library-redis-data:/data
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
    networks:
      - library-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "redis@2023", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Nginx反向代理
  library-nginx:
    image: nginx:alpine
    container_name: library-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
      - ./nginx/html:/usr/share/nginx/html
    depends_on:
      - library-app
    networks:
      - library-network

  # Prometheus监控
  prometheus:
    image: prom/prometheus:latest
    container_name: library-prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - library-network

  # Grafana可视化
  grafana:
    image: grafana/grafana:latest
    container_name: library-grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin@123
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards
    networks:
      - library-network

# 网络配置
networks:
  library-network:
    driver: bridge

# 数据卷配置
volumes:
  library-db-data:
  library-redis-data:
  prometheus-data:
  grafana-data:
```

### 3.3 Docker部署脚本

创建`scripts/deploy.sh`：

```bash
#!/bin/bash

# Docker部署脚本
set -e

# 参数配置
ENVIRONMENT=${1:-prod}
ACTION=${2:-up}

echo "Docker部署脚本"
echo "环境: $ENVIRONMENT"
echo "操作: $ACTION"

# 检查Docker和Docker Compose
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装"
    exit 1
fi

# 创建必要的目录
mkdir -p logs config database/init nginx/conf.d nginx/ssl monitoring

# 根据操作执行
case $ACTION in
    "build")
        echo "构建Docker镜像..."
        docker-compose build
        ;;
    "up")
        echo "启动服务..."
        docker-compose up -d
        ;;
    "down")
        echo "停止服务..."
        docker-compose down
        ;;
    "restart")
        echo "重启服务..."
        docker-compose down
        docker-compose up -d
        ;;
    "logs")
        echo "查看日志..."
        docker-compose logs -f library-app
        ;;
    "status")
        echo "查看服务状态..."
        docker-compose ps
        ;;
    *)
        echo "使用方法: $0 <environment> <action>"
        echo "action: build|up|down|restart|logs|status"
        exit 1
        ;;
esac

echo "操作完成"
```

## 4. 生产环境配置优化

### 4.1 JVM调优配置

创建`config/jvm-prod.conf`：

```bash
# JVM生产环境配置

# 堆内存设置
-Xms2g
-Xmx4g
-XX:NewRatio=3
-XX:SurvivorRatio=8

# 垃圾收集器设置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:+UseStringDeduplication

# GC日志
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCApplicationStoppedTime
-Xloggc:/var/log/library-system/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=100M

# 内存溢出处理
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/library-system/heapdump.hprof

# JIT编译优化
-XX:+AggressiveOpts
-XX:+UseFastAccessorMethods

# 其他优化
-Djava.security.egd=file:/dev/./urandom
-Dfile.encoding=UTF-8
-Duser.timezone=Asia/Shanghai
```

### 4.2 Nginx配置

创建`nginx/conf.d/library.conf`：

```nginx
# Library System Nginx配置
upstream library_backend {
    server library-app:8080 weight=1 max_fails=3 fail_timeout=30s;
    # 可以添加多个后端服务器实现负载均衡
    # server library-app-2:8080 weight=1 max_fails=3 fail_timeout=30s;
}

# HTTP到HTTPS重定向
server {
    listen 80;
    server_name library.example.com www.library.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS主站点
server {
    listen 443 ssl http2;
    server_name library.example.com www.library.example.com;
    
    # SSL证书配置
    ssl_certificate /etc/nginx/ssl/library.crt;
    ssl_certificate_key /etc/nginx/ssl/library.key;
    
    # SSL安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;
    
    # 静态文件处理
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        proxy_pass http://library_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # API请求
    location /api/ {
        proxy_pass http://library_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        
        # 缓冲设置
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        
        # 限流
        limit_req zone=api burst=20 nodelay;
    }
    
    # 健康检查
    location /actuator/health {
        proxy_pass http://library_backend;
        proxy_set_header Host $host;
        access_log off;
    }
    
    # 默认代理
    location / {
        proxy_pass http://library_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # 错误页面
    error_page 404 /404.html;
    error_page 500 502 503 504 /50x.html;
    
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}

# 限流配置
http {
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_status 429;
}
```

### 4.3 Redis配置

创建`redis/redis.conf`：

```bash
# Redis生产环境配置

# 基本配置
port 6379
bind 0.0.0.0
protected-mode yes
requirepass redis@2023

# 内存配置
maxmemory 2gb
maxmemory-policy allkeys-lru

# 持久化配置
save 900 1
save 300 10
save 60 10000

# AOF配置
appendonly yes
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 慢查询日志
slowlog-log-slower-than 10000
slowlog-max-len 128

# 客户端配置
maxclients 10000
timeout 300

# 日志配置
loglevel notice
logfile "/var/log/redis/redis.log"

# 安全配置
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG "CONFIG_9a8b7c6d"
```

## 5. 监控与运维

### 5.1 Prometheus配置

创建`monitoring/prometheus.yml`：

```yaml
# Prometheus监控配置
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

scrape_configs:
  # 应用监控
  - job_name: 'library-system'
    static_configs:
      - targets: ['library-app:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  # 数据库监控
  - job_name: 'opengauss'
    static_configs:
      - targets: ['library-db:5432']
    scrape_interval: 30s
    
  # Redis监控
  - job_name: 'redis'
    static_configs:
      - targets: ['library-redis:6379']
    scrape_interval: 30s
    
  # Nginx监控
  - job_name: 'nginx'
    static_configs:
      - targets: ['library-nginx:80']
    scrape_interval: 30s

# 告警规则
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### 5.2 应用监控指标

创建`src/main/java/com/demo/library/metrics/ApplicationMetrics.java`：

```java
package com.demo.library.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 应用监控指标
 */
@Component
public class ApplicationMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // 业务指标
    private Counter userRegistrationCounter;
    private Counter bookBorrowedCounter;
    private Counter bookReturnedCounter;
    private Timer bookSearchTimer;
    private AtomicLong activeUsersGauge;
    private AtomicLong totalBooksGauge;
    
    @PostConstruct
    public void init() {
        // 注册业务指标
        userRegistrationCounter = Counter.builder("library.user.registration")
                .description("用户注册数量")
                .register(meterRegistry);
        
        bookBorrowedCounter = Counter.builder("library.book.borrowed")
                .description("图书借阅数量")
                .register(meterRegistry);
        
        bookReturnedCounter = Counter.builder("library.book.returned")
                .description("图书归还数量")
                .register(meterRegistry);
        
        bookSearchTimer = Timer.builder("library.book.search")
                .description("图书搜索耗时")
                .register(meterRegistry);
        
        activeUsersGauge = new AtomicLong(0);
        Gauge.builder("library.users.active")
                .description("活跃用户数")
                .register(meterRegistry, activeUsersGauge, AtomicLong::get);
        
        totalBooksGauge = new AtomicLong(0);
        Gauge.builder("library.books.total")
                .description("图书总数")
                .register(meterRegistry, totalBooksGauge, AtomicLong::get);
    }
    
    // 业务方法
    public void incrementUserRegistration() {
        userRegistrationCounter.increment();
    }
    
    public void incrementBookBorrowed() {
        bookBorrowedCounter.increment();
    }
    
    public void incrementBookReturned() {
        bookReturnedCounter.increment();
    }
    
    public Timer.Sample startBookSearchTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void updateActiveUsers(long count) {
        activeUsersGauge.set(count);
    }
    
    public void updateTotalBooks(long count) {
        totalBooksGauge.set(count);
    }
}
```

### 5.3 健康检查

创建`src/main/java/com/demo/library/health/CustomHealthIndicator.java`：

```java
package com.demo.library.health;

import com.demo.library.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Autowired
    private BookService bookService;
    
    @Override
    public Health health() {
        try {
            // 执行业务检查
            long totalBooks = bookService.getTotalCount();
            
            if (totalBooks >= 0) {
                return Health.up()
                        .withDetail("totalBooks", totalBooks)
                        .withDetail("status", "服务正常")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "无法获取图书数量")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

## 6. CI/CD自动化部署

### 6.1 GitHub Actions CI/CD

创建`.github/workflows/deploy.yml`：

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    
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

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build application
      run: mvn clean package -Pprod -DskipTests
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha
          type=raw,value=latest,enable={{is_default_branch}}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to server
      uses: appleboy/ssh-action@v0.1.5
      with:
        host: ${{ secrets.HOST }}
        username: ${{ secrets.USERNAME }}
        key: ${{ secrets.PRIVATE_KEY }}
        script: |
          cd /opt/library-system
          docker-compose down
          docker-compose pull
          docker-compose up -d
          
          # 健康检查
          sleep 30
          curl -f http://localhost:8080/actuator/health || exit 1
          
          echo "部署成功！"
```

### 6.2 Jenkins Pipeline

创建`Jenkinsfile`：

```groovy
pipeline {
    agent any
    
    environment {
        MAVEN_HOME = tool 'Maven-3.8.6'
        JAVA_HOME = tool 'JDK-17'
        DOCKER_REGISTRY = 'your-registry.com'
        APP_NAME = 'library-system'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test') {
            steps {
                sh '''
                    ${MAVEN_HOME}/bin/mvn clean test
                    ${MAVEN_HOME}/bin/mvn jacoco:report
                '''
                
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/site/jacoco',
                    reportFiles: 'index.html',
                    reportName: 'Coverage Report'
                ])
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    ${MAVEN_HOME}/bin/mvn clean package -Pprod -DskipTests
                '''
                
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    def image = docker.build("${DOCKER_REGISTRY}/${APP_NAME}:${BUILD_NUMBER}")
                    image.push()
                    image.push("latest")
                }
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                sh '''
                    ssh user@staging-server "
                        cd /opt/library-system &&
                        docker-compose down &&
                        docker-compose pull &&
                        docker-compose up -d
                    "
                '''
            }
        }
        
        stage('Smoke Tests') {
            steps {
                sh '''
                    sleep 60
                    curl -f http://staging-server:8080/actuator/health
                '''
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: '确认部署到生产环境?', ok: '确认'
                
                sh '''
                    ssh user@prod-server "
                        cd /opt/library-system &&
                        docker-compose down &&
                        docker-compose pull &&
                        docker-compose up -d
                    "
                '''
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        failure {
            emailext (
                subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build failed. Please check the console output.",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
```

## 7. 性能调优

### 7.1 数据库优化

创建数据库优化脚本`database/optimization.sql`：

```sql
-- 数据库性能优化

-- 创建索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_isbn ON books(isbn);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_title ON books USING gin(to_tsvector('english', title));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_author ON books(author);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_category ON books(category_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_status ON books(status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_borrowing_user ON borrowing_records(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_borrowing_book ON borrowing_records(book_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_borrowing_status ON borrowing_records(status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_borrowing_dates ON borrowing_records(borrow_date, return_date);

-- 分区表（如果数据量大）
-- CREATE TABLE borrowing_records_2023 PARTITION OF borrowing_records
-- FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');

-- 统计信息更新
ANALYZE books;
ANALYZE users;
ANALYZE borrowing_records;

-- 数据库参数调优建议
-- shared_buffers = 256MB
-- effective_cache_size = 1GB
-- maintenance_work_mem = 64MB
-- checkpoint_completion_target = 0.9
-- wal_buffers = 16MB
-- default_statistics_target = 100
-- random_page_cost = 1.1
-- effective_io_concurrency = 200
```

### 7.2 应用缓存配置

创建`src/main/java/com/demo/library/config/CacheConfig.java`：

```java
package com.demo.library.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)));
        
        // 不同缓存的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 图书缓存 - 1小时
        cacheConfigurations.put("books", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 用户缓存 - 30分钟
        cacheConfigurations.put("users", 
            defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 分类缓存 - 24小时
        cacheConfigurations.put("categories", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

### 7.3 连接池优化

在`application-prod.yml`中添加连接池配置：

```yaml
spring:
  datasource:
    hikari:
      # 连接池配置
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
      # 连接测试
      connection-test-query: SELECT 1
      validation-timeout: 5000
      
      # 其他优化
      auto-commit: true
      pool-name: LibraryHikariCP
      
  # Redis连接池配置
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
      shutdown-timeout: 2000ms
```

## 8. 故障排查与运维

### 8.1 日志分析脚本

创建`scripts/log-analysis.sh`：

```bash
#!/bin/bash

# 日志分析脚本
LOG_DIR="/var/log/library-system"
LOG_FILE="$LOG_DIR/application.log"

echo "=== 图书管理系统日志分析 ==="

# 检查日志文件是否存在
if [ ! -f "$LOG_FILE" ]; then
    echo "错误: 日志文件不存在: $LOG_FILE"
    exit 1
fi

echo "日志文件: $LOG_FILE"
echo "文件大小: $(du -h $LOG_FILE | cut -f1)"
echo "最后修改时间: $(stat -c %y $LOG_FILE)"
echo ""

# 错误统计
echo "=== 错误统计 ==="
echo "ERROR级别日志数量: $(grep -c "ERROR" $LOG_FILE)"
echo "WARN级别日志数量: $(grep -c "WARN" $LOG_FILE)"
echo ""

# 最近的错误
echo "=== 最近10个错误 ==="
grep "ERROR" $LOG_FILE | tail -10
echo ""

# 接口响应时间分析
echo "=== 慢接口分析 ==="
grep -o "Completed [0-9]* [A-Z]* \"[^\"]*\" in [0-9]*ms" $LOG_FILE | \
    awk '$NF > 1000' | sort -k6 -nr | head -10
echo ""

# 数据库连接问题
echo "=== 数据库连接问题 ==="
grep -i "connection" $LOG_FILE | grep -i "error\|timeout\|refused" | tail -5
echo ""

# 内存使用情况
echo "=== 内存使用分析 ==="
grep -i "OutOfMemoryError\|heap\|memory" $LOG_FILE | tail -5
echo ""

# JVM GC分析
if [ -f "$LOG_DIR/gc.log" ]; then
    echo "=== GC分析 ==="
    echo "GC日志文件大小: $(du -h $LOG_DIR/gc.log | cut -f1)"
    tail -20 "$LOG_DIR/gc.log" | grep "GC" | tail -5
    echo ""
fi

echo "日志分析完成"
```

### 8.2 系统监控脚本

创建`scripts/monitor.sh`：

```bash
#!/bin/bash

# 系统监控脚本
APP_NAME="library-system"
HEALTH_URL="http://localhost:8080/actuator/health"

echo "=== $APP_NAME 系统监控 ==="
echo "时间: $(date)"
echo ""

# 检查应用是否运行
echo "=== 应用状态检查 ==="
if docker ps | grep -q $APP_NAME; then
    echo "✓ 应用容器运行中"
    
    # 健康检查
    if curl -f $HEALTH_URL > /dev/null 2>&1; then
        echo "✓ 应用健康检查通过"
    else
        echo "✗ 应用健康检查失败"
        exit 1
    fi
else
    echo "✗ 应用容器未运行"
    exit 1
fi
echo ""

# 系统资源使用
echo "=== 系统资源使用 ==="
echo "CPU使用率:"
top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//'

echo "内存使用:"
free -h | grep "Mem:"

echo "磁盘使用:"
df -h | grep -E "/$|/var"
echo ""

# Docker资源使用
echo "=== Docker资源使用 ==="
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
echo ""

# 数据库连接检查
echo "=== 数据库连接检查 ==="
if docker exec library-db gsql -U library_user -d library_prod_db -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✓ 数据库连接正常"
else
    echo "✗ 数据库连接失败"
fi
echo ""

# Redis连接检查
echo "=== Redis连接检查 ==="
if docker exec library-redis redis-cli -a redis@2023 ping > /dev/null 2>&1; then
    echo "✓ Redis连接正常"
else
    echo "✗ Redis连接失败"
fi
echo ""

# 网络连接检查
echo "=== 网络连接检查 ==="
netstat -tlnp | grep :8080
echo ""

echo "监控检查完成"
```

### 8.3 备份恢复脚本

创建`scripts/backup.sh`：

```bash
#!/bin/bash

# 数据库备份脚本
BACKUP_DIR="/backup/library-system"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="library_prod_db"
DB_USER="library_user"

echo "开始数据库备份..."

# 创建备份目录
mkdir -p $BACKUP_DIR

# 数据库备份
echo "备份数据库 $DB_NAME..."
docker exec library-db gs_dump -U $DB_USER -d $DB_NAME -f /tmp/backup_$DATE.sql

# 复制备份文件
docker cp library-db:/tmp/backup_$DATE.sql $BACKUP_DIR/

# 压缩备份文件
gzip $BACKUP_DIR/backup_$DATE.sql

# 清理旧备份（保留30天）
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete

echo "数据库备份完成: $BACKUP_DIR/backup_$DATE.sql.gz"

# Redis备份
echo "备份Redis数据..."
docker exec library-redis redis-cli -a redis@2023 BGSAVE
sleep 5
docker cp library-redis:/data/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

echo "Redis备份完成: $BACKUP_DIR/redis_$DATE.rdb"

# 应用配置备份
echo "备份应用配置..."
tar -czf $BACKUP_DIR/config_$DATE.tar.gz docker-compose.yml nginx/ config/

echo "配置备份完成: $BACKUP_DIR/config_$DATE.tar.gz"
echo "备份任务完成"
```

## 9. 安全加固

### 9.1 安全配置检查

创建`scripts/security-check.sh`：

```bash
#!/bin/bash

# 安全配置检查脚本
echo "=== 安全配置检查 ==="

# 检查默认密码
echo "检查默认密码..."
if docker-compose config | grep -q "password.*123\|password.*admin"; then
    echo "⚠️  发现默认密码，请修改"
else
    echo "✓ 未发现默认密码"
fi

# 检查SSL配置
echo "检查SSL配置..."
if [ -f "nginx/ssl/library.crt" ]; then
    echo "✓ SSL证书存在"
    openssl x509 -in nginx/ssl/library.crt -text -noout | grep "Not After"
else
    echo "⚠️  SSL证书不存在"
fi

# 检查端口暴露
echo "检查端口暴露..."
if docker-compose config | grep -q "5432:5432\|6379:6379"; then
    echo "⚠️  数据库端口暴露到外网，存在安全风险"
else
    echo "✓ 数据库端口未暴露"
fi

# 检查日志敏感信息
echo "检查日志敏感信息..."
if grep -q "password\|secret\|token" /var/log/library-system/application.log 2>/dev/null; then
    echo "⚠️  日志中可能包含敏感信息"
else
    echo "✓ 日志安全检查通过"
fi

echo "安全检查完成"
```

### 9.2 防火墙配置

创建`scripts/firewall-setup.sh`：

```bash
#!/bin/bash

# 防火墙配置脚本
echo "配置防火墙规则..."

# 清除现有规则
iptables -F
iptables -X
iptables -Z

# 设置默认策略
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# 允许本地回环
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT

# 允许已建立的连接
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 允许SSH（端口22）
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# 允许HTTP和HTTPS
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 允许应用端口（仅内网）
iptables -A INPUT -p tcp --dport 8080 -s 10.0.0.0/8 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -s 172.16.0.0/12 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -s 192.168.0.0/16 -j ACCEPT

# 限制连接速率
iptables -A INPUT -p tcp --dport 80 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT

# 保存规则
iptables-save > /etc/iptables/rules.v4

echo "防火墙配置完成"
```

## 10. 常见问题解决

### 10.1 启动问题

```bash
# 检查端口占用
netstat -tlnp | grep 8080

# 检查Docker容器状态
docker ps -a

# 查看容器日志
docker logs library-app

# 检查配置文件
docker exec library-app cat /app/application-prod.yml
```

### 10.2 性能问题

```bash
# JVM性能分析
docker exec library-app jstack 1 > thread_dump.txt
docker exec library-app jmap -dump:format=b,file=/tmp/heap.hprof 1

# 数据库性能分析
docker exec library-db gsql -U library_user -d library_prod_db -c "SELECT * FROM pg_stat_activity;"
```

### 10.3 网络问题

```bash
# 检查容器网络
docker network ls
docker network inspect library-network

# 测试容器间连接
docker exec library-app ping library-db
docker exec library-app telnet library-redis 6379
```

## 11. 总结

通过本步骤，我们完成了：

✅ **多环境配置管理**
- 开发、测试、生产环境分离
- 配置文件加密和安全管理
- 环境变量和外部配置

✅ **Maven打包策略**
- 多环境打包配置
- 构建脚本和版本管理
- 依赖优化和资源过滤

✅ **Docker容器化部署**
- 多阶段构建优化
- Docker Compose编排
- 容器网络和数据卷管理

✅ **生产环境优化**
- JVM参数调优
- Nginx反向代理配置
- 数据库连接池优化

✅ **监控与运维**
- Prometheus + Grafana监控
- 健康检查和告警
- 日志分析和故障排查

✅ **CI/CD自动化**
- GitHub Actions集成
- Jenkins Pipeline配置
- 自动化测试和部署

✅ **安全加固**
- 防火墙配置
- SSL/TLS加密
- 安全检查和审计

## 结语

恭喜你完成了Spring Boot图书管理系统的完整学习项目！通过这12个步骤，你已经掌握了：

1. **环境准备与配置** - 开发环境搭建
2. **Spring Boot基础** - 核心概念和配置
3. **数据库集成** - OpenGauss数据库操作
4. **MyBatis框架** - 数据持久化
5. **RESTful API** - Web服务开发
6. **前端集成** - Thymeleaf模板
7. **业务逻辑** - 核心功能实现
8. **前端优化** - 用户界面完善
9. **Spring Security** - 安全框架
10. **异常处理** - 错误管理和日志
11. **测试框架** - 质量保障
12. **部署运维** - 生产环境实施

这个项目涵盖了企业级Spring Boot应用开发的各个方面，是一个完整的学习和实践案例。继续深入学习，你将能够开发更复杂的企业级应用！

---

**项目特色**：
- 完整的企业级架构设计
- 生产环境就绪的配置
- 全面的测试覆盖
- 现代化的DevOps流程
- 详细的文档和最佳实践