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