package com.quizfun.questionbankquery.application.dto;

import java.util.List;

/**
 * Response DTO containing questions and pagination metadata.
 */
public record QueryQuestionsResponse(
        List<QuestionDTO> questions,
        PaginationMetadata pagination
) {}
