package com.demo.library.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 欢迎页面控制器
 * 演示基础的Spring Boot Web功能
 */
@Controller
public class WelcomeController {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * 首页 - 返回Thymeleaf模板
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", applicationName);
        model.addAttribute("message", "欢迎使用图书管理系统！");
        return "index";
    }
    
    /**
     * API接口 - 返回JSON数据
     */
    @GetMapping("/api/welcome")
    @ResponseBody
    public WelcomeResponse welcome() {
        return new WelcomeResponse(
            applicationName, 
            "Spring Boot图书管理系统启动成功！",
            System.currentTimeMillis()
        );
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
    
    /**
     * 响应对象
     */
    public static class WelcomeResponse {
        private String applicationName;
        private String message;
        private Long timestamp;
        
        public WelcomeResponse(String applicationName, String message, Long timestamp) {
            this.applicationName = applicationName;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getApplicationName() { return applicationName; }
        public String getMessage() { return message; }
        public Long getTimestamp() { return timestamp; }
    }
}