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