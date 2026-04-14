package com.fitness.management.exception;

import com.fitness.management.common.Result;
import com.fitness.management.common.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：统一日志与 {@link Result} 响应体，适配 Spring Boot 3.2。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常 | code={} | message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败(MethodArgumentNotValidException) | {}", detail);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), detail.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : detail);
    }

    @ExceptionHandler(JwtException.class)
    public Result<Void> handleJwtException(JwtException e) {
        log.error("JWT 异常 | type={} | message={}", e.getClass().getSimpleName(), e.getMessage(), e);
        String msg;
        if (e instanceof ExpiredJwtException) {
            msg = "令牌已过期";
        } else if (e instanceof UnsupportedJwtException) {
            msg = "不支持的令牌";
        } else if (e instanceof MalformedJwtException) {
            msg = "令牌格式错误";
        } else if (e instanceof SignatureException) {
            msg = "令牌签名无效";
        } else {
            msg = "未授权或令牌无效";
        }
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未捕获异常 | {}", e.getMessage(), e);
        return Result.error();
    }
}
