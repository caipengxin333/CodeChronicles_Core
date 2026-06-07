package com.codechronicles.core.dto;

/**
 * 登录请求体。password 为用户输入的原始密码，后端使用 BCrypt 与数据库密文比对。
 */
public record LoginRequest(
        String phone,
        String password,
        String captchaKey,
        String captcha
) {
}
