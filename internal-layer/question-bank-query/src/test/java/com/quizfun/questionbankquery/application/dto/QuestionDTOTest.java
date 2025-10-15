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
@DisplayName("QuestionDTO Tests")
class QuestionDTOTest {

    @Test
    @DisplayName("Should create QuestionDTO with all fields")
    void shouldCreateQuestionDTOWithAllFields() {
        // GIVEN: All required fields
        Long questionId = 1234567890123L;
        String questionText = "What is the capital of France?";
        String questionType = "MCQ";
        String difficultyLevel = "EASY";
        Map<String, Object> typeSpecificData = Map.of(
                "options", List.of("Paris", "London", "Berlin", "Madrid"),
                "correctAnswer", "Paris"
        );
        TaxonomyDTO taxonomy = new TaxonomyDTO(
                List.of("Geography", "Europe", "Capitals"),
                List.of("trivia", "beginner"),
                List.of("quick-quiz", "geography-101"),
                "EASY"
        );
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        // WHEN: Creating QuestionDTO
        QuestionDTO dto = new QuestionDTO(
                String.valueOf(questionId), // questionId as String
                null,                      // sourceQuestionId
                questionType,              // questionType
                questionText,              // title
                null,                      // content
                null,                      // points
                null,                      // status
                null,                      // solutionExplanation
                null,                      // displayOrder
                typeSpecificData,          // typeSpecificData
                new TaxonomyDTO(
                        taxonomy.categories(),
                        taxonomy.tags(),
                        taxonomy.quizzes(),
                        difficultyLevel          // difficultyLevel
                ),
                createdAt,                 // createdAt
                updatedAt,                 // updatedAt
                null,                      // publishedAt
                null                       // archivedAt
        );

        // THEN: All fields should be set correctly
        assertThat(dto.questionId()).isEqualTo(String.valueOf(questionId));
        assertThat(dto.title()).isEqualTo(questionText);
        assertThat(dto.questionType()).isEqualTo(questionType);
        assertThat(dto.taxonomy().difficultyLevel()).isEqualTo(difficultyLevel);
        assertThat(dto.typeSpecificData()).isEqualTo(typeSpecificData);
        assertThat(dto.taxonomy()).isEqualTo(taxonomy);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Should create QuestionDTO with null optional fields")
    void shouldCreateQuestionDTOWithNullOptionalFields() {
        // GIVEN: Only required fields (taxonomy can be null)
        Long questionId = 1234567890123L;
        String questionText = "What is 2 + 2?";
        String questionType = "ESSAY";

        // WHEN: Creating QuestionDTO with nulls
        Instant now = Instant.now();
        QuestionDTO dto = new QuestionDTO(
                String.valueOf(questionId),  // questionId as String
                null,                        // sourceQuestionId
                questionType,                // questionType
                questionText,                // title
                null,                        // content
                null,                        // points
                null,                        // status
                null,                        // solutionExplanation
                null,                        // displayOrder
                Map.of(),                    // empty type-specific data
                null,                        // taxonomy optional
                now,                         // createdAt
                now,                         // updatedAt
                null,                        // publishedAt
                null                         // archivedAt
        );

        // THEN: Should be created successfully
        // THEN: Should be created successfully
        assertThat(dto.questionId()).isEqualTo(String.valueOf(questionId));
        assertThat(dto.title()).isEqualTo(questionText);
        assertThat(dto.taxonomy()).isNull();
    }
}
