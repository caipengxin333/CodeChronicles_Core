package com.codechronicles.core.dto;

/**
 * 新增评论请求。parentCommentId 为空时发表一级评论，否则回复指定评论。
 */
public record CreateCommentRequest(
        String content,
        Long parentCommentId
) {
}
