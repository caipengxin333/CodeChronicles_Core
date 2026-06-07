package com.codechronicles.core.dto;

/**
 * 点赞或取消点赞后的文章点赞状态。
 */
public record ArticleLikeResponse(
        Long articleId,
        boolean liked,
        int likes
) {
}
