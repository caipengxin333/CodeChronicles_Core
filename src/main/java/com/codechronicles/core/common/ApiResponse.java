package com.codechronicles.core.common;

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
}
