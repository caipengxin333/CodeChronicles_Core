package com.codechronicles.core.controller;

import com.codechronicles.core.common.ApiResponse;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 资源不存在时返回 404，例如查询不存在的文章详情。
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(NoSuchElementException exception) {
        return new ApiResponse<>(404, exception.getMessage(), null);
    }

    /**
     * 请求参数或请求体校验失败时返回 400，message 直接给前端展示/调试。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException exception) {
        return new ApiResponse<>(400, exception.getMessage(), null);
    }
}
