package com.fitness.management.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务响应码，与 HTTP 语义对齐，供 {@link Result} 及全局异常处理使用。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 请求成功 */
    SUCCESS(200, "成功"),

    /** 参数错误或业务校验不通过 */
    BAD_REQUEST(400, "参数错误"),

    /** 未登录或令牌无效 */
    UNAUTHORIZED(401, "未授权"),

    /** 已认证但无权限访问资源 */
    FORBIDDEN(403, "禁止访问"),

    /** 服务端未捕获异常或依赖故障 */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
