package com.codechronicles.core.dto;

/**
 * 管理员审核文章请求体。
 */
public record ReviewArticleRequest(
        Boolean approved,
        String rejectReason
) {
}
