package com.tinybrain.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 继承 RuntimeException，由 GlobalExceptionHandler 统一捕获处理。
 * 抛出此异常时，前端收到对应的 code 和 message。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ========== 常见业务异常工厂 ==========

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }
}
