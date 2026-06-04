package com.codechronicles.core.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一接口响应结构，前端可以固定按 code/message/data 三个字段处理所有业务接口。
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 构造成功响应，业务成功统一返回 code=200。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 构造自定义成功消息，登录等接口可返回更贴近业务的文案。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 兼容前端常见的 msg 命名，避免和既有 message 字段二选一导致接口破坏。
     */
    @JsonProperty("msg")
    public String msg() {
        return message;
    }
}
