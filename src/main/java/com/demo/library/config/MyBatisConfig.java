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