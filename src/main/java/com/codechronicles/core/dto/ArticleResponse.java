package com.codechronicles.core.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表和详情共用响应结构。
 */
public record ArticleResponse(
        Long id,
        String title,
        String summary,
        String cover,
        String category,
        String content,
        String status,
        Long authorUserId,
        LocalDateTime reviewTime,
        Long reviewerUserId,
        String rejectReason,
        List<String> tags,
        List<String> tagNames,
        LocalDate publishedAt,
        LocalDate updatedAt,
        LocalDate date,
        Integer views,
        Integer likes,
        Integer comments
) {
}
