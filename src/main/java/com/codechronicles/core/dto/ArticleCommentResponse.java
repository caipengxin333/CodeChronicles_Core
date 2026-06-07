package com.codechronicles.core.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增评论成功后的评论数据。
 */
public record ArticleCommentResponse(
        Long id,
        Long articleId,
        Long userId,
        String userName,
        String userAvatar,
        Long rootCommentId,
        Long replyToCommentId,
        Long replyToUserId,
        String replyToUserName,
        String content,
        LocalDateTime createdAt,
        List<ArticleCommentResponse> replies
) {
}
