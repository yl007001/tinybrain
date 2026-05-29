package com.tinybrain.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 所有 API 响应都使用该类包装，保证前端解析格式统一。
 * 设计原则：
 * - 成功：code=200, message="success", data=业务数据
 * - 失败：code=非200, message=错误描述, data=null
 * - 分页：专用 PageResult 类型
 *
 * @param <T> 响应数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;

    private R() {}

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========== 成功响应 ==========

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    // ========== 失败响应 ==========

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    // ========== 便捷方法 ==========

    public boolean isSuccess() {
        return this.code == 200;
    }
}
