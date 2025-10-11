package com.quizfun.questionbankquery.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Request DTO for querying questions with filters and pagination.
 */
@Getter
@Builder
public class QueryQuestionsRequest {

    @NotNull(message = "User ID must not be null")
    private final Long userId;

    @NotNull(message = "Question Bank ID must not be null")
    private final Long questionBankId;

    private final List<String> categories; // AND logic
    private final List<String> tags;       // OR logic
    private final List<String> quizzes;    // OR logic
    private final String searchText;       // full text

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    @Builder.Default
    private final Integer page = 0;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = 100, message = "Size must be less than or equal to 100")
    @Builder.Default
    private final Integer size = 20;

    @Pattern(regexp = "^(createdAt|updatedAt|questionText|relevance)$",
        message = "Sort by must be one of: createdAt, updatedAt, questionText, relevance")
    @Builder.Default
    private final String sortBy = "createdAt";

    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be either 'asc' or 'desc'")
    @Builder.Default
    private final String sortDirection = "desc";
}
