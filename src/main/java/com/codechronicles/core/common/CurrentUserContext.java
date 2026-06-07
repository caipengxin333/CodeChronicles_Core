package com.codechronicles.core.common;

/**
 * 当前请求中的登录用户上下文，由登录逻辑或 JWT 过滤器写入 ThreadLocal。
 */
public record CurrentUserContext(
        Long userId,
        String phone,
        Long profileId,
        String role,
        String token
) {
}
