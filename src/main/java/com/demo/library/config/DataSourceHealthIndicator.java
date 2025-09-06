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