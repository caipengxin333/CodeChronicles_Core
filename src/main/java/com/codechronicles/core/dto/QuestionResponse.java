package com.codechronicles.core.dto;

import java.time.LocalDate;
import java.util.List;

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
