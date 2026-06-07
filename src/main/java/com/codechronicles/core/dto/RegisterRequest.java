package com.codechronicles.core.dto;

import java.util.List;

/**
 * 注册请求体。第一版只强制手机号、密码和昵称，其余资料可选。
 */
public record RegisterRequest(
        String phone,
        String password,
        String nickname,
        String avatar,
        String bio,
        String role,
        String location,
        List<String> skills,
        List<RegisterLinkRequest> links
) {
}
