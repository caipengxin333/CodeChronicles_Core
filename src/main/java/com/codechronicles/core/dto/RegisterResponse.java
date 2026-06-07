package com.codechronicles.core.dto;

/**
 * 注册成功响应，不返回 token，前端注册成功后再调用登录接口。
 */
public record RegisterResponse(
        Long userId,
        Long profileId,
        String phone,
        String account
) {
}
