package com.quizfun.questionbankquery.application.dto;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1011.query-dtos-and-pagination-logic")
@DisplayName("QueryQuestionsResponse Tests")
class QueryQuestionsResponseTest {

    @Test
    @DisplayName("Should create response with questions and pagination")
    void shouldCreateResponseWithQuestionsAndPagination() {
    List<QuestionDTO> questions = List.of(
        new QuestionDTO(
            "1",                    // questionId
            null,                   // sourceQuestionId
            "MCQ",                 // questionType
            "What is 2 + 2?",     // title
            null,                   // content
            null,                   // points
            null,                   // status
            null,                   // solutionExplanation
            null,                   // displayOrder
            Map.of("options", List.of("3", "4")), // typeSpecificData
            null,                   // taxonomy
            Instant.now(),          // createdAt
            Instant.now(),          // updatedAt
            null,                   // publishedAt
            null                    // archivedAt
        ),
        new QuestionDTO(
            "2",
            null,
            "MCQ",
            "Capital of France?",
            null,
            null,
            null,
            null,
            null,
            Map.of("options", List.of("Paris", "London")),
            null,
            Instant.now(),
            Instant.now(),
            null,
            null
        )
    );
        PaginationMetadata pagination = new PaginationMetadata(0, 20, 50, 3);

        QueryQuestionsResponse response = new QueryQuestionsResponse(questions, pagination);

        assertThat(response.questions()).hasSize(2);
        assertThat(response.pagination().currentPage()).isZero();
        assertThat(response.pagination().totalItems()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should create response with empty questions list")
    void shouldCreateResponseWithEmptyQuestionsList() {
        List<QuestionDTO> emptyQuestions = List.of();
        PaginationMetadata pagination = new PaginationMetadata(0, 20, 0, 0);

        QueryQuestionsResponse response = new QueryQuestionsResponse(emptyQuestions, pagination);

        assertThat(response.questions()).isEmpty();
        assertThat(response.pagination().totalItems()).isZero();
    }
}
