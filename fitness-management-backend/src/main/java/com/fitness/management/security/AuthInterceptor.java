package com.fitness.management.security;

import com.fitness.management.common.ResultCode;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权：解析 Bearer Token，将 userId 写入请求属性 {@link AuthInterceptor#ATTR_USER_ID}。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "LOGIN_USER_ID";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireAuth onMethod = handlerMethod.getMethodAnnotation(RequireAuth.class);
        RequireAuth onClass = handlerMethod.getBeanType().getAnnotation(RequireAuth.class);
        if (onMethod == null && onClass == null) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或令牌缺失");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌无效");
        }
        request.setAttribute(ATTR_USER_ID, userId);
        return true;
    }
}
