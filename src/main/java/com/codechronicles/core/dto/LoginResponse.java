package com.codechronicles.core.dto;

/**
 * 登录成功响应数据。
 */
public record LoginResponse(String token, String role) {
}
