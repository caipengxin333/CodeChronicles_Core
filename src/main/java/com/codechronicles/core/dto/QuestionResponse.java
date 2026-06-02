package com.codechronicles.core.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 问答模块列表响应。
 */
public record QuestionResponse(
        Long id,
        String title,
        String description,
        String answer,
        List<String> tags,
        Integer answerCount,
        LocalDate updatedAt
) {
}
