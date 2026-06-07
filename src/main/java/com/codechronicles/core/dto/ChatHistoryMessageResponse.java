package com.codechronicles.core.dto;

/**
 * 返回给前端的单条聊天历史消息。
 */
public record ChatHistoryMessageResponse(
        String role,
        String content
) {
}
