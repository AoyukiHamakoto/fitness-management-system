package com.fitness.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 {@link JwtProperties} 为 Spring Bean，供 {@link com.fitness.management.utils.JwtUtils} 注入。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {
}
