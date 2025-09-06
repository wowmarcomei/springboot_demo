package com.demo.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot主启动类
 * 
 * @SpringBootApplication 复合注解，包含：
 * - @Configuration：标记配置类
 * - @EnableAutoConfiguration：启用自动配置
 * - @ComponentScan：组件扫描
 */
@SpringBootApplication
@MapperScan("com.demo.library.mapper")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
        System.out.println("====================================");
        System.out.println("图书管理系统启动成功!");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("====================================");
    }
}