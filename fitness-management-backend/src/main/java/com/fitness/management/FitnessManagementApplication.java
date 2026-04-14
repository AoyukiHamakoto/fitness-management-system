package com.fitness.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人智能健身管理系统 - 后端启动入口（JDK 17 / Spring Boot 3.2）。
 */
@SpringBootApplication
@MapperScan("com.fitness.management.mapper")
public class FitnessManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessManagementApplication.class, args);
    }
}
