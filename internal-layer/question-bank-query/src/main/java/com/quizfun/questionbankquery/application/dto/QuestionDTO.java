package com.quizfun.questionbankquery.application.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Question data for query responses (read model DTO).
 */
public record QuestionDTO(
        Long questionId,
        String questionText,
        String questionType,
        String difficultyLevel,
        Map<String, Object> typeSpecificData,
        TaxonomyDTO taxonomy,
        Instant createdAt,
        Instant updatedAt
) {}
