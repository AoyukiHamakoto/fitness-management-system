package com.fitness.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，与 {@code application.yml} 中 {@code jwt.*} 节点绑定。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HS256 签名密钥（建议至少 256 bit / 32 字节随机串，以满足 JJWT 强度要求）。
     */
    private String secret = "";

    /**
     * 访问令牌过期时间，单位：毫秒（例如 86400000 表示 24 小时）。
     */
    private long expiration = 86_400_000L;
}
