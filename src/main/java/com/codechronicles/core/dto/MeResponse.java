package com.codechronicles.core.dto;

import java.util.List;

/**
 * 当前登录用户信息响应，用于前端初始化用户状态和个人内容统计。
 * articleCount 与 publishedArticleCount 当前含义相同，前者用于兼容已有前端。
 * questionCount 为兼容已有前端保留字段名，实际表示当前用户发布的文章评论数量。
 */
public record MeResponse(
        Long id,
        String phone,
        String nickname,
        String name,
        String avatar,
        String role,
        String bio,
        String location,
        Integer followers,
        Integer articleCount,
        Integer publishedArticleCount,
        Integer tagCount,
        Integer questionCount,
        List<String> skills,
        List<LinkResponse> links
) {
}
