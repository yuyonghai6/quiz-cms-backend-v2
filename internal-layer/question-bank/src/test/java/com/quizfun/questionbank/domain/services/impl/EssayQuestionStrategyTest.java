package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.EssayRubric;
import com.quizfun.questionbank.domain.entities.QuestionType;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class EssayQuestionStrategyTest {

    private EssayQuestionStrategy strategy;
    private UpsertQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        strategy = new EssayQuestionStrategy();
        validCommand = createValidCommand();
    }

    @Nested
    @DisplayName("Successful Processing Tests")
    class SuccessfulProcessingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should successfully process valid Essay data")
        @Description("Verifies that strategy creates QuestionAggregate when provided with valid Essay data")
        void shouldSuccessfullyProcessValidEssayData() {
            // Act
            var result = strategy.processQuestionData(validCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.ESSAY);
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a");
            assertThat(aggregate.getEssayData()).isNotNull();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should support Essay question type")
        @Description("Verifies that strategy correctly identifies Essay as supported type")
        void shouldSupportEssayQuestionType() {
            // Act & Assert
            assertThat(strategy.supports(QuestionType.ESSAY)).isTrue();
            assertThat(strategy.supports(QuestionType.MCQ)).isFalse();
            assertThat(strategy.supports(QuestionType.TRUE_FALSE)).isFalse();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should return correct strategy name")
        @Description("Verifies that strategy returns appropriate name for identification")
        void shouldReturnCorrectStrategyName() {
            // Act
            var name = strategy.getStrategyName();

            // Assert
            assertThat(name).isEqualTo("Essay Strategy");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should handle essay without rubric")
        @Description("Verifies that strategy accepts essay questions without rubric")
        void shouldHandleEssayWithoutRubric() {
            // Arrange
            var essayData = new EssayData(100, 500, false, null);
            var command = createCommandWithEssayData(essayData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should handle essay with valid rubric")
        @Description("Verifies that strategy accepts essay questions with properly structured rubric")
        void shouldHandleEssayWithValidRubric() {
            // Arrange
            var criteria = Arrays.asList(
                "Content Quality: Clear and well-structured content",
                "Grammar and Style: Proper grammar and writing style"
            );
            var rubric = new EssayRubric("Programming essay rubric", criteria, 40);
            var essayData = new EssayData(100, 500, false, rubric);
            var command = createCommandWithEssayData(essayData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Essay Data Validation Tests")
    class EssayDataValidationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should fail when Essay data is missing")
        @Description("Verifies that strategy returns failure when Essay data is not provided for Essay question")
        void shouldFailWhenEssayDataIsMissing() {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("ESSAY")
                .title("Test Question")
                .content("Test Content")
                .build(); // No Essay data
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("ESSAY_DATA_REQUIRED");
            assertThat(result.getError()).contains("Essay data is required for essay questions");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should fail with negative minimum word count")
        @Description("Verifies that strategy fails when minimum word count is negative")
        void shouldFailWithNegativeMinimumWordCount() {
            // Act & Assert - This should fail during EssayData construction
            assertThatThrownBy(() -> new EssayData(-10, 500, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum word count cannot be negative");
        }

        // Removed prompt length test: prompt is modeled in request content, not EssayData

        // Duplicate test removed; covered by constructor validation test above

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should fail with invalid maximum word count")
        @Description("Verifies that EssayData constructor guards invalid max word count")
        void shouldFailWithInvalidMaximumWordCount() {
            assertThatThrownBy(() -> new EssayData(100, 0, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum word count cannot exceed maximum word count");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should fail with excessive maximum word count")
        @Description("Verifies that strategy fails when maximum word count exceeds reasonable limit")
        void shouldFailWithExcessiveMaximumWordCount() {
            // Arrange
            var essayData = new EssayData(100, 15000, false, null); // Exceeds 10000 limit
            var command = createCommandWithEssayData(essayData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("ESSAY_MAX_WORDS_TOO_HIGH");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("EssayQuestionStrategyTest.Should fail when min words exceeds max words")
        @Description("Verifies that EssayData constructor guards min > max")
        void shouldFailWhenMinWordsExceedsMaxWords() {
            assertThatThrownBy(() -> new EssayData(500, 100, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum word count cannot exceed maximum");
        }
    }

    @Nested
    @DisplayName("Rubric Validation Tests")
    class RubricValidationTests {

        // Removed: EssayRubric constructor enforces non-empty criteria at construction time

        // Removed: EssayRubricCriteria type is not part of current model

        // Removed: Criteria points per-criterion not modeled; max points validated globally

        // Removed: relies on EssayRubricCriteria
    }

    // Helper methods
    private UpsertQuestionCommand createValidCommand() {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("ESSAY")
            .title("Test Essay Question")
            .content("Essay instructions")
            .essayData(createValidEssayData())
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private EssayData createValidEssayData() {
        var criteria = Arrays.asList(
            "Content Quality: Demonstrates clear understanding",
            "Organization: Well-structured and logical flow",
            "Grammar: Proper grammar and spelling"
        );
        var rubric = new EssayRubric("Essay grading rubric", criteria, 45);
        return new EssayData(200, 800, false, rubric);
    }

    private UpsertQuestionCommand createCommandWithEssayData(EssayData essayData) {
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("ESSAY")
            .title("Test Essay Question")
            .content("Essay instructions")
            .essayData(essayData)
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }
}