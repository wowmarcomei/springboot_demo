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