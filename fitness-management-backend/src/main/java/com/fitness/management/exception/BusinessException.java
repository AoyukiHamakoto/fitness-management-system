package com.fitness.management.exception;

import com.fitness.management.common.ResultCode;
import lombok.Getter;

/**
 * 业务规则不满足时抛出，由 {@link GlobalExceptionHandler} 统一转换为 {@link com.fitness.management.common.Result}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
