package com.codechronicles.core.dto;

import java.util.List;

/**
 * 新增文章请求体。标签可以由前端传入，也可以由后端通过 AI 从标题和摘要中自动提取。
 */
public record CreateArticleRequest(
        String title,
        String summary,
        String cover,
        String category,
        String content,
        List<Long> tagIds,
        List<String> tagNames
) {
}
