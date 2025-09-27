package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.shared.common.Result;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class McqQuestionStrategyTest {

    private McqQuestionStrategy strategy;
    private UpsertQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        strategy = new McqQuestionStrategy();
        validCommand = createValidCommand();
    }

    @Nested
    @DisplayName("Successful Processing Tests")
    class SuccessfulProcessingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should successfully process valid MCQ data")
        @Description("Verifies that strategy creates QuestionAggregate when provided with valid MCQ data")
        void shouldSuccessfullyProcessValidMcqData() {
            // Act
            var result = strategy.processQuestionData(validCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a");
            assertThat(aggregate.getMcqData()).isNotNull();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should support MCQ question type")
        @Description("Verifies that strategy correctly identifies MCQ as supported type")
        void shouldSupportMcqQuestionType() {
            // Act & Assert
            assertThat(strategy.supports(QuestionType.MCQ)).isTrue();
            assertThat(strategy.supports(QuestionType.ESSAY)).isFalse();
            assertThat(strategy.supports(QuestionType.TRUE_FALSE)).isFalse();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should return correct strategy name")
        @Description("Verifies that strategy returns appropriate name for identification")
        void shouldReturnCorrectStrategyName() {
            // Act
            var name = strategy.getStrategyName();

            // Assert
            assertThat(name).isEqualTo("MCQ Strategy");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should handle maximum allowed options")
        @Description("Verifies that strategy accepts MCQ with 10 options (maximum limit)")
        void shouldHandleMaximumAllowedOptions() {
            // Arrange
            var mcqData = createMcqDataWithOptions(10);
            var command = createCommandWithMcqData(mcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should handle multiple correct answers when allowed")
        @Description("Verifies that strategy accepts multiple correct answers when MCQ is configured to allow them")
        void shouldHandleMultipleCorrectAnswersWhenAllowed() {
            // Arrange
            var options = Arrays.asList(
                new McqOption("A", "Option A", true, 1.0),
                new McqOption("B", "Option B", true, 1.0),
                new McqOption("C", "Option C", false, 0.0)
            );
            var mcqData = new McqData(options, false, true, false, 60); // Allow multiple correct
            var command = createCommandWithMcqData(mcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("MCQ Data Validation Tests")
    class McqDataValidationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail when MCQ data is missing")
        @Description("Verifies that strategy returns failure when MCQ data is not provided for MCQ question")
        void shouldFailWhenMcqDataIsMissing() {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .build(); // No MCQ data
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_DATA_REQUIRED");
            assertThat(result.getError()).contains("MCQ data is required for MCQ questions");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail when no options provided")
        @Description("Verifies that strategy fails when MCQ has no options")
        void shouldFailWhenNoOptionsProvided() {
            assertThatThrownBy(() -> new McqData(Collections.emptyList(), false, false, false, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one option");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with insufficient options")
        @Description("Verifies that strategy fails when MCQ has less than 2 options")
        void shouldFailWithInsufficientOptions() {
            // Arrange
            var options = Arrays.asList(new McqOption("A", "Option A", true, 1.0));
            var mcqData = new McqData(options, false, false, false, 60);
            var command = createCommandWithMcqData(mcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_INSUFFICIENT_OPTIONS");
            assertThat(result.getError()).contains("must have at least 2 options");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with too many options")
        @Description("Verifies that strategy fails when MCQ has more than 10 options")
        void shouldFailWithTooManyOptions() {
            // Arrange
            var mcqData = createMcqDataWithOptions(11); // Exceeds maximum
            var command = createCommandWithMcqData(mcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_TOO_MANY_OPTIONS");
            assertThat(result.getError()).contains("cannot have more than 10 options");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail when no correct answer provided")
        @Description("Verifies that strategy fails when MCQ has no correct answers")
        void shouldFailWhenNoCorrectAnswerProvided() {
            var options = Arrays.asList(
                new McqOption("A", "Option A", false, 0.0),
                new McqOption("B", "Option B", false, 0.0)
            );
            assertThatThrownBy(() -> new McqData(options, false, false, false, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one correct answer");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with multiple correct answers when not allowed")
        @Description("Verifies that strategy fails when MCQ has multiple correct answers but is not configured to allow them")
        void shouldFailWithMultipleCorrectAnswersWhenNotAllowed() {
            // Arrange
            var options = Arrays.asList(
                new McqOption("A", "Option A", true, 1.0),
                new McqOption("B", "Option B", true, 1.0),
                new McqOption("C", "Option C", false, 0.0)
            );
            var mcqData = new McqData(options, false, false, false, 60); // Multiple correct NOT allowed
            var command = createCommandWithMcqData(mcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_MULTIPLE_CORRECT_NOT_ALLOWED");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with empty option text")
        @Description("Verifies that strategy fails when MCQ option has empty or null text")
        void shouldFailWithEmptyOptionText() {
            assertThatThrownBy(() -> new McqOption("A", "", true, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text cannot be null or empty");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with invalid time limit")
        @Description("Verifies that strategy fails when MCQ has invalid time limit")
        void shouldFailWithInvalidTimeLimit() {
            // Arrange
            var mcqData = createValidMcqData();
            var invalidMcqData = new McqData(
                mcqData.getOptions(),
                mcqData.isShuffleOptions(),
                mcqData.isAllowMultipleCorrect(),
                mcqData.isAllowPartialCredit(),
                -30 // Invalid negative time limit
            );
            var command = createCommandWithMcqData(invalidMcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_INVALID_TIME_LIMIT");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("McqQuestionStrategyTest.Should fail with excessive time limit")
        @Description("Verifies that strategy fails when MCQ time limit exceeds maximum allowed")
        void shouldFailWithExcessiveTimeLimit() {
            // Arrange
            var mcqData = createValidMcqData();
            var invalidMcqData = new McqData(
                mcqData.getOptions(),
                mcqData.isShuffleOptions(),
                mcqData.isAllowMultipleCorrect(),
                mcqData.isAllowPartialCredit(),
                7200 // 2 hours, exceeds 1 hour limit
            );
            var command = createCommandWithMcqData(invalidMcqData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("MCQ_TIME_LIMIT_TOO_LONG");
        }
    }

    // Helper methods
    private UpsertQuestionCommand createValidCommand() {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("MCQ")
            .title("Test MCQ Question")
            .content("Choose the correct answer")
            .mcqData(createValidMcqData())
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private McqData createValidMcqData() {
        var options = Arrays.asList(
            new McqOption("A", "Option A - Correct Answer", true, 1.0),
            new McqOption("B", "Option B - Incorrect", false, 0.0),
            new McqOption("C", "Option C - Incorrect", false, 0.0),
            new McqOption("D", "Option D - Incorrect", false, 0.0)
        );
        return new McqData(options, false, false, false, 60);
    }

    private UpsertQuestionCommand createCommandWithMcqData(McqData mcqData) {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("MCQ")
            .title("Test MCQ Question")
            .content("Choose the correct answer")
            .mcqData(mcqData)
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private McqData createMcqDataWithOptions(int optionCount) {
        var options = new java.util.ArrayList<McqOption>();
        for (int i = 0; i < optionCount; i++) {
            boolean isCorrect = i == 0; // First option is correct
            options.add(new McqOption(
                String.valueOf((char)('A' + i)),
                "Option " + (char)('A' + i),
                isCorrect,
                isCorrect ? 1.0 : 0.0
            ));
        }
        return new McqData(options, false, false, false, 60);
    }
}