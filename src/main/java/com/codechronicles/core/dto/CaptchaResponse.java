package com.codechronicles.core.dto;

/**
 * 图形验证码响应。image 为 data URL，前端可直接放到 img.src。
 */
public record CaptchaResponse(
        String captchaKey,
        String image
) {
}
