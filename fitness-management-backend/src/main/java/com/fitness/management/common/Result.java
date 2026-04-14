package com.fitness.management.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 REST 响应体，与 Spring Boot 3.2 控制器返回类型配合使用。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 响应码，与 {@link ResultCode} 取值一致 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据，无数据时可为 null */
    private T data;

    /**
     * 成功：无数据，消息为 {@link ResultCode#SUCCESS} 默认文案。
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功：携带数据，消息为默认成功文案。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功：自定义提示文案并携带数据。
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败：使用 {@link ResultCode#INTERNAL_SERVER_ERROR} 的码与默认消息。
     */
    public static <T> Result<T> error() {
        return new Result<>(
                ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                ResultCode.INTERNAL_SERVER_ERROR.getMessage(),
                null);
    }

    /**
     * 失败：HTTP 500 语义，自定义错误说明。
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), msg, null);
    }

    /**
     * 失败：自定义响应码与说明（如 400/401/403 等）。
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
