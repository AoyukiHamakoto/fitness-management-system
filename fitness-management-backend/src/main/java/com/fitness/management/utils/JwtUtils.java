package com.fitness.management.utils;

import com.fitness.management.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发与解析访问令牌（jjwt 0.11.x），与论文中「携带用户标识、服务端校验签名与有效期」的流程一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private static final String CLAIM_USERNAME = "username";

    private final JwtProperties jwtProperties;

    /**
     * 构建 HS256 签名密钥；密钥长度不足时 JJWT 将抛出异常。
     */
    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT：subject 存放 userId，自定义 claim 存放 username，含签发时间与过期时间。
     *
     * @param userId 用户主键，不可为 null
     * @param username 登录名，不可为空
     * @return 紧凑序列化的 JWT 字符串
     * @throws IllegalArgumentException 参数非法
     * @throws IllegalStateException 未配置 {@code jwt.secret}
     * @throws JwtException             密钥过弱等签发阶段异常
     */
    public String generateToken(Long userId, String username) {
        if (userId == null || !StringUtils.hasText(username)) {
            throw new IllegalArgumentException("userId 与 username 不能为空");
        }
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("jwt.secret 未配置，无法签发 Token");
        }
        long ttlMillis = jwtProperties.getExpiration();
        if (ttlMillis <= 0) {
            throw new IllegalStateException("jwt.expiration 必须大于 0");
        }
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + ttlMillis);
        try {
            return Jwts.builder()
                    .setSubject(String.valueOf(userId))
                    .claim(CLAIM_USERNAME, username)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiresAt)
                    .signWith(signingKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (JwtException e) {
            log.error("签发 JWT 失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从 Token 中解析用户主键（subject）。若 Token 缺失、格式非法、签名错误、已过期等，返回 {@code null}。
     */
    public Long getUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = parseClaims(token);
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.debug("Token subject 无法解析为 Long型 userId: {}", e.getMessage());
            return null;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期，无法读取 userId: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的 Token，无法读取 userId: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.debug("Token 格式错误，无法读取 userId: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.debug("Token 签名错误，无法读取 userId: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("Token 解析失败: {} | {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.debug("Token 或密钥非法: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否当前有效：签名正确、结构合法且未过期。
     *
     * @return {@code true} 表示可信任；任意过期、篡改、格式问题均返回 {@code false}
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            log.warn("jwt.secret 未配置，拒绝校验 Token");
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的 Token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.debug("Token 格式错误: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.debug("Token 签名错误: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("Token 无效: {} | {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("Token 或密钥参数非法: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析并返回载荷；仅供内部复用，异常向上由调用方分类处理。
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
