package com.codechronicles.core.dto;

/**
 * 注册时可选提交的个人链接。
 */
public record RegisterLinkRequest(
        String label,
        String url
) {
}
