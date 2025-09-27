package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.*;
import com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException;
import com.quizfun.globalshared.utils.UUIDv7Generator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class QuestionAggregateTypeSpecificDataTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should set MCQ data for MCQ questions")
    @Description("Validates setting MCQ-specific data including options and correct answers")
    void shouldSetMcqDataForMcqQuestions() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "MCQ Question", "Choose the correct answer", 5
        );

        var options = List.of(
            new McqOption("A", "Option A", false, 0.0),
            new McqOption("B", "Option B", true, 1.0),
            new McqOption("C", "Option C", false, 0.0),
            new McqOption("D", "Option D", false, 0.0)
        );
        var mcqData = new McqData(options, true, false, false, null);

        // Act
        aggregate.setMcqData(mcqData);

        // Assert
        assertThat(aggregate.getMcqData()).isEqualTo(mcqData);
        assertThat(aggregate.getMcqData().getOptions()).hasSize(4);
        assertThat(aggregate.getMcqData().getOptions().stream()
            .filter(McqOption::isCorrect).count()).isEqualTo(1);
        assertThat(aggregate.getMcqData().isShuffleOptions()).isTrue();
        assertThat(aggregate.getMcqData().isAllowMultipleCorrect()).isFalse();

        // Other type-specific data should remain null
        assertThat(aggregate.getEssayData()).isNull();
        assertThat(aggregate.getTrueFalseData()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should set Essay data for Essay questions")
    @Description("Validates setting Essay-specific data including word limits and guidelines")
    void shouldSetEssayDataForEssayQuestions() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.ESSAY,
            "Essay Question", "Write a comprehensive essay", 10
        );

        var essayData = new EssayData(500, 2000, true, null);

        // Act
        aggregate.setEssayData(essayData);

        // Assert
        assertThat(aggregate.getEssayData()).isEqualTo(essayData);
        assertThat(aggregate.getEssayData().getMinWordCount()).isEqualTo(500);
        assertThat(aggregate.getEssayData().getMaxWordCount()).isEqualTo(2000);
        assertThat(aggregate.getEssayData().isAllowRichText()).isTrue();

        // Other type-specific data should remain null
        assertThat(aggregate.getMcqData()).isNull();
        assertThat(aggregate.getTrueFalseData()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should set TrueFalse data for TrueFalse questions")
    @Description("Validates setting TrueFalse-specific data including correct answer and explanation")
    void shouldSetTrueFalseDataForTrueFalseQuestions() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.TRUE_FALSE,
            "True/False Question", "Is this statement correct?", 2
        );

        var trueFalseData = new TrueFalseData(true, "This is correct because...", null);

        // Act
        aggregate.setTrueFalseData(trueFalseData);

        // Assert
        assertThat(aggregate.getTrueFalseData()).isEqualTo(trueFalseData);
        assertThat(aggregate.getTrueFalseData().getCorrectAnswer()).isTrue();
        assertThat(aggregate.getTrueFalseData().getExplanation()).isEqualTo("This is correct because...");

        // Other type-specific data should remain null
        assertThat(aggregate.getMcqData()).isNull();
        assertThat(aggregate.getEssayData()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should reject mismatched type-specific data")
    @Description("Ensures that setting MCQ data on Essay questions throws validation error")
    void shouldRejectMismatchedTypeSpecificData() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var essayAggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.ESSAY,
            "Essay Question", "Write an essay", 10
        );

        var mcqData = new McqData(
            List.of(new McqOption("A", "Option A", true, 1.0)),
            false, false, false, null
        );

        // Act & Assert
        assertThatThrownBy(() -> essayAggregate.setMcqData(mcqData))
            .isInstanceOf(InvalidQuestionDataException.class)
            .hasMessageContaining("MCQ data can only be set on MCQ questions");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should maintain question type immutability")
    @Description("Validates that question type remains immutable after creation")
    void shouldMaintainQuestionTypeImmutability() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var mcqAggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "MCQ Question", "Choose the correct answer", 5
        );

        var essayAggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.ESSAY,
            "Essay Question", "Write an essay", 10
        );

        // Act & Assert - Question type should remain unchanged
        assertThat(mcqAggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(essayAggregate.getQuestionType()).isEqualTo(QuestionType.ESSAY);

        // Setting appropriate type-specific data should work
        var mcqData = new McqData(List.of(new McqOption("A", "Option A", true, 1.0)), false, false, false, null);
        assertThatNoException().isThrownBy(() -> mcqAggregate.setMcqData(mcqData));

        var essayData = new EssayData(100, 500, true, null);
        assertThatNoException().isThrownBy(() -> essayAggregate.setEssayData(essayData));
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @EnumSource(QuestionType.class)
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should validate type-specific data consistency")
    @Description("Ensures each question type can only have its corresponding type-specific data")
    void shouldValidateTypeSpecificDataConsistency(QuestionType questionType) {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, questionType,
            "Test Question", "Test Content", 5
        );

        // Act & Assert based on question type
        switch (questionType) {
            case MCQ -> {
                var mcqData = new McqData(
                    List.of(new McqOption("A", "Option A", true, 1.0)),
                    false, false, false, null
                );

                // Should allow MCQ data
                assertThatNoException().isThrownBy(() -> aggregate.setMcqData(mcqData));

                // Should reject other types
                assertThatThrownBy(() -> aggregate.setEssayData(new EssayData(100, 500, true, null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
                assertThatThrownBy(() -> aggregate.setTrueFalseData(new TrueFalseData(true, "Explanation", null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
            }
            case ESSAY -> {
                var essayData = new EssayData(100, 500, true, null);

                // Should allow Essay data
                assertThatNoException().isThrownBy(() -> aggregate.setEssayData(essayData));

                // Should reject other types
                assertThatThrownBy(() -> aggregate.setMcqData(new McqData(List.of(new McqOption("A", "Test", true, 1.0)), false, false, false, null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
                assertThatThrownBy(() -> aggregate.setTrueFalseData(new TrueFalseData(true, "Explanation", null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
            }
            case TRUE_FALSE -> {
                var trueFalseData = new TrueFalseData(true, "Explanation", null);

                // Should allow TrueFalse data
                assertThatNoException().isThrownBy(() -> aggregate.setTrueFalseData(trueFalseData));

                // Should reject other types
                assertThatThrownBy(() -> aggregate.setMcqData(new McqData(List.of(new McqOption("A", "Test", true, 1.0)), false, false, false, null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
                assertThatThrownBy(() -> aggregate.setEssayData(new EssayData(100, 500, true, null)))
                    .isInstanceOf(InvalidQuestionDataException.class);
            }
        }
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should update type-specific data and generate events")
    @Description("Validates that updating type-specific data generates appropriate domain events")
    void shouldUpdateTypeSpecificDataAndGenerateEvents() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "MCQ Question", "Choose the correct answer", 5
        );

        var initialMcqData = new McqData(
            List.of(new McqOption("A", "Option A", true, 1.0)),
            false, false, false, null
        );
        aggregate.setMcqData(initialMcqData);
        aggregate.markEventsAsCommitted(); // Clear creation events

        var updatedMcqData = new McqData(
            List.of(
                new McqOption("A", "Option A", false, 0.0),
                new McqOption("B", "Option B", true, 1.0),
                new McqOption("C", "Option C", false, 0.0)
            ),
            true, false, false, null
        );

        // Act
        aggregate.setMcqData(updatedMcqData);

        // Assert
        assertThat(aggregate.getMcqData()).isEqualTo(updatedMcqData);

        var events = aggregate.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(com.quizfun.questionbank.domain.events.QuestionUpdatedEvent.class);

        var updateEvent = (com.quizfun.questionbank.domain.events.QuestionUpdatedEvent) events.get(0);
        assertThat(updateEvent.getFieldsUpdated()).contains("mcqData");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should validate MCQ data completeness")
    @Description("Ensures MCQ questions have at least one correct option")
    void shouldValidateMcqDataCompleteness() {
        // Act & Assert - validation happens at value object construction
        assertThatThrownBy(() -> new McqData(
            List.of(
                new McqOption("A", "Option A", false, 0.0),
                new McqOption("B", "Option B", false, 0.0)
            ),
            false, false, false, null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have at least one correct answer");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should validate Essay data word limits")
    @Description("Ensures Essay questions have valid min/max word limits")
    void shouldValidateEssayDataWordLimits() {
        // Act & Assert - validation happens at value object construction
        assertThatThrownBy(() -> new EssayData(1000, 500, true, null)) // min > max
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimum word count cannot exceed maximum word count");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should handle null type-specific data gracefully")
    @Description("Validates that setting null type-specific data clears existing data")
    void shouldHandleNullTypeSpecificDataGracefully() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "MCQ Question", "Choose the correct answer", 5
        );

        var mcqData = new McqData(
            List.of(new McqOption("A", "Option A", true, 1.0)),
            false, false, false, null
        );
        aggregate.setMcqData(mcqData);

        // Act
        aggregate.setMcqData(null);

        // Assert
        assertThat(aggregate.getMcqData()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should support complex MCQ configurations")
    @Description("Validates support for multiple correct answers and option shuffling")
    void shouldSupportComplexMcqConfigurations() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "MCQ Question", "Select all correct answers", 10
        );

        var complexMcqData = new McqData(
            List.of(
                new McqOption("A", "Option A", true, 1.0),
                new McqOption("B", "Option B", false, 0.0),
                new McqOption("C", "Option C", true, 1.0),
                new McqOption("D", "Option D", false, 0.0),
                new McqOption("E", "Option E", true, 1.0)
            ),
            true, // shuffle options
            true, // allow multiple correct
            false, // allow partial credit
            null // time limit
        );

        // Act
        aggregate.setMcqData(complexMcqData);

        // Assert
        assertThat(aggregate.getMcqData().getOptions()).hasSize(5);
        assertThat(aggregate.getMcqData().getOptions().stream()
            .filter(McqOption::isCorrect).count()).isEqualTo(3);
        assertThat(aggregate.getMcqData().isShuffleOptions()).isTrue();
        assertThat(aggregate.getMcqData().isAllowMultipleCorrect()).isTrue();
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @EnumSource(QuestionType.class)
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateTypeSpecificDataTest.Should evaluate hasValidTypeSpecificData correctly")
    @Description("Ensures hasValidTypeSpecificData returns true only when corresponding type-specific data is set")
    void shouldEvaluateHasValidTypeSpecificDataCorrectly(QuestionType questionType) {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, questionType,
            "Test Question", "Test Content", 5
        );

        // Initially, no type-specific data is set
        assertThat(aggregate.hasValidTypeSpecificData()).isFalse();

        // Act & Assert per type
        switch (questionType) {
            case MCQ -> {
                var mcqData = new McqData(List.of(new McqOption("A", "Option A", true, 1.0)), false, false, false, null);
                aggregate.setMcqData(mcqData);
                assertThat(aggregate.hasValidTypeSpecificData()).isTrue();
                aggregate.setMcqData(null);
                assertThat(aggregate.hasValidTypeSpecificData()).isFalse();
            }
            case ESSAY -> {
                var essayData = new EssayData(100, 500, true, null);
                aggregate.setEssayData(essayData);
                assertThat(aggregate.hasValidTypeSpecificData()).isTrue();
                aggregate.setEssayData(null);
                assertThat(aggregate.hasValidTypeSpecificData()).isFalse();
            }
            case TRUE_FALSE -> {
                var tfData = new TrueFalseData(true, "Explanation", null);
                aggregate.setTrueFalseData(tfData);
                assertThat(aggregate.hasValidTypeSpecificData()).isTrue();
                aggregate.setTrueFalseData(null);
                assertThat(aggregate.hasValidTypeSpecificData()).isFalse();
            }
        }
    }
}