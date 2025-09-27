package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TrueFalseQuestionStrategyTest {

    private TrueFalseQuestionStrategy strategy;
    private UpsertQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        strategy = new TrueFalseQuestionStrategy();
        validCommand = createValidCommand();
    }

    @Nested
    @DisplayName("Successful Processing Tests")
    class SuccessfulProcessingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should successfully process valid True/False data")
        @Description("Verifies that strategy creates QuestionAggregate when provided with valid True/False data")
        void shouldSuccessfullyProcessValidTrueFalseData() {
            // Act
            var result = strategy.processQuestionData(validCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.TRUE_FALSE);
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a");
            assertThat(aggregate.getTrueFalseData()).isNotNull();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should support True/False question type")
        @Description("Verifies that strategy correctly identifies True/False as supported type")
        void shouldSupportTrueFalseQuestionType() {
            // Act & Assert
            assertThat(strategy.supports(QuestionType.TRUE_FALSE)).isTrue();
            assertThat(strategy.supports(QuestionType.MCQ)).isFalse();
            assertThat(strategy.supports(QuestionType.ESSAY)).isFalse();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should return correct strategy name")
        @Description("Verifies that strategy returns appropriate name for identification")
        void shouldReturnCorrectStrategyName() {
            // Act
            var name = strategy.getStrategyName();

            // Assert
            assertThat(name).isEqualTo("True/False Strategy");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should handle True/False without explanation")
        @Description("Verifies that strategy accepts True/False questions without explanation")
        void shouldHandleTrueFalseWithoutExplanation() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, null, 90); // No explanation
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should handle True/False with valid explanation")
        @Description("Verifies that strategy accepts True/False questions with proper explanation")
        void shouldHandleTrueFalseWithValidExplanation() {
            // Arrange
            var explanation = "This statement is true because it demonstrates correct principles.";
            var trueFalseData = new TrueFalseData(true, explanation, 90);
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should handle both true and false correct answers")
        @Description("Verifies that strategy accepts both true and false as valid correct answers")
        void shouldHandleBothTrueAndFalseCorrectAnswers() {
            // Test with true answer
            var trueFalseDataTrue = new TrueFalseData(true, "Explanation for true", 90);
            var commandTrue = createCommandWithTrueFalseData(trueFalseDataTrue);
            var resultTrue = strategy.processQuestionData(commandTrue);
            assertThat(resultTrue.isSuccess()).isTrue();

            // Test with false answer
            var trueFalseDataFalse = new TrueFalseData(false, "Explanation for false", 90);
            var commandFalse = createCommandWithTrueFalseData(trueFalseDataFalse);
            var resultFalse = strategy.processQuestionData(commandFalse);
            assertThat(resultFalse.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("True/False Data Validation Tests")
    class TrueFalseDataValidationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should fail when True/False data is missing")
        @Description("Verifies that strategy returns failure when True/False data is not provided")
        void shouldFailWhenTrueFalseDataIsMissing() {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
                .questionType("TRUE_FALSE")
                .title("Test Question")
                .content("Test Content")
                .build(); // No True/False data
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TRUE_FALSE_DATA_REQUIRED");
            assertThat(result.getError()).contains("True/False data is required for true/false questions");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should accept both true and false answers")
        @Description("Verifies that strategy accepts both true and false as valid correct answers")
        void shouldAcceptBothTrueAndFalseAnswers() {
            // Test true answer
            var trueFalseDataTrue = new TrueFalseData(true, "This is correct", 90);
            var commandTrue = createCommandWithTrueFalseData(trueFalseDataTrue);
            var resultTrue = strategy.processQuestionData(commandTrue);
            assertThat(resultTrue.isSuccess()).isTrue();

            // Test false answer
            var trueFalseDataFalse = new TrueFalseData(false, "This is also correct", 90);
            var commandFalse = createCommandWithTrueFalseData(trueFalseDataFalse);
            var resultFalse = strategy.processQuestionData(commandFalse);
            assertThat(resultFalse.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should fail when explanation is empty")
        @Description("Verifies that strategy fails when explanation is provided but empty")
        void shouldFailWhenExplanationIsEmpty() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, "   ", 90); // Empty explanation (whitespace)
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TRUE_FALSE_EMPTY_EXPLANATION");
            assertThat(result.getError()).contains("True/False explanation cannot be empty if provided");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should fail when explanation is too long")
        @Description("Verifies that strategy fails when explanation exceeds maximum length")
        void shouldFailWhenExplanationIsTooLong() {
            // Arrange
            var longExplanation = "A".repeat(2001); // Exceeds 2000 character limit
            var trueFalseData = new TrueFalseData(true, longExplanation, 90);
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TRUE_FALSE_EXPLANATION_TOO_LONG");
            assertThat(result.getError()).contains("cannot exceed 2000 characters");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should fail with invalid time limit")
        @Description("Verifies that strategy fails when time limit is zero or negative")
        void shouldFailWithInvalidTimeLimit() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, "Valid explanation", -30); // Negative time limit
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TRUE_FALSE_INVALID_TIME_LIMIT");
            assertThat(result.getError()).contains("Time limit must be positive if specified");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should fail with excessive time limit")
        @Description("Verifies that strategy fails when time limit exceeds maximum allowed")
        void shouldFailWithExcessiveTimeLimit() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, "Valid explanation", 7200); // 2 hours, exceeds 1 hour limit
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TRUE_FALSE_TIME_LIMIT_TOO_LONG");
            assertThat(result.getError()).contains("cannot exceed 3600 seconds");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should handle valid time limit")
        @Description("Verifies that strategy accepts reasonable time limits")
        void shouldHandleValidTimeLimit() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, "Valid explanation", 180); // 3 minutes
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("TrueFalseQuestionStrategyTest.Should handle maximum valid time limit")
        @Description("Verifies that strategy accepts time limit at maximum boundary")
        void shouldHandleMaximumValidTimeLimit() {
            // Arrange
            var trueFalseData = new TrueFalseData(true, "Valid explanation", 3600); // Exactly 1 hour
            var command = createCommandWithTrueFalseData(trueFalseData);

            // Act
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // Helper methods
    private UpsertQuestionCommand createValidCommand() {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("TRUE_FALSE")
            .title("Test True/False Question")
            .content("Statement to evaluate")
            .trueFalseData(createValidTrueFalseData())
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private TrueFalseData createValidTrueFalseData() {
        return new TrueFalseData(
            true, // Correct answer
            "This statement is true because it follows established principles.", // Explanation
            120 // Time limit in seconds
        );
    }

    private UpsertQuestionCommand createCommandWithTrueFalseData(TrueFalseData trueFalseData) {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("TRUE_FALSE")
            .title("Test True/False Question")
            .content("Statement to evaluate")
            .trueFalseData(trueFalseData)
            .build();
        return new UpsertQuestionCommand(1001L, 2002L, request);
    }
}