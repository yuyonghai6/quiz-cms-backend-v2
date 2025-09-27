package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.events.QuestionCreatedEvent;
import com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException;
import com.quizfun.questionbank.domain.exceptions.InvalidSourceQuestionIdException;
import com.quizfun.globalshared.utils.UUIDv7Generator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

class QuestionAggregateCoreImplementationTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should create new question with valid data")
    @Description("Validates QuestionAggregate creation with proper factory method and all required fields")
    void shouldCreateNewQuestionWithValidData() {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "What is 2+2?", "Choose the correct answer", 5
        );

        assertThat(aggregate.getUserId()).isEqualTo(1001L);
        assertThat(aggregate.getQuestionBankId()).isEqualTo(2002L);
        assertThat(aggregate.getSourceQuestionId()).isEqualTo(validSourceQuestionId);
        assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(aggregate.getTitle()).isEqualTo("What is 2+2?");
        assertThat(aggregate.getContent()).isEqualTo("Choose the correct answer");
        assertThat(aggregate.getPoints()).isEqualTo(5);
        assertThat(aggregate.getStatus()).isEqualTo("draft");
        assertThat(aggregate.getCreatedAt()).isNotNull();
        assertThat(aggregate.getUpdatedAt()).isNotNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should set default values for optional fields")
    @Description("Ensures QuestionAggregate properly handles null optional parameters with sensible defaults")
    void shouldSetDefaultValuesForOptionalFields() {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Test Question", "Test Content", null
        );

        assertThat(aggregate.getPoints()).isEqualTo(0);
        assertThat(aggregate.getStatus()).isEqualTo("draft");
        assertThat(aggregate.getDisplayOrder()).isNull();
        assertThat(aggregate.getPublishedAt()).isNull();
        assertThat(aggregate.getArchivedAt()).isNull();
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @ValueSource(longs = {0L, -1L})
    @NullSource
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should reject invalid user IDs")
    @Description("Validates proper validation of user ID parameter during aggregate creation")
    void shouldRejectInvalidUserIds(Long invalidUserId) {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        assertThatThrownBy(() ->
            QuestionAggregate.createNew(
                invalidUserId, 2002L, validSourceQuestionId, QuestionType.MCQ,
                "Test Question", "Test Content", 5
            )
        ).isInstanceOf(com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException.class)
         .hasMessageContaining("user ID");
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @ValueSource(strings = {"", " ", "   "})
    @NullSource
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should reject invalid source question IDs")
    @Description("Ensures proper validation of source question ID parameter")
    void shouldRejectInvalidSourceQuestionIds(String invalidSourceId) {
        assertThatThrownBy(() ->
            QuestionAggregate.createNew(
                1001L, 2002L, invalidSourceId, QuestionType.MCQ,
                "Test Question", "Test Content", 5
            )
        ).isInstanceOf(com.quizfun.questionbank.domain.exceptions.InvalidSourceQuestionIdException.class)
         .hasMessageContaining("Source question ID is required");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should generate QuestionCreatedEvent on creation")
    @Description("Verifies that domain events are properly generated and contain correct information")
    void shouldGenerateQuestionCreatedEventOnCreation() {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Test Question", "Test Content", 5
        );

        var events = aggregate.getUncommittedEvents();
        assertThat(events).hasSize(1);

        var event = events.get(0);
        assertThat(event).isInstanceOf(QuestionCreatedEvent.class);

        var createdEvent = (QuestionCreatedEvent) event;
        assertThat(createdEvent.getSourceQuestionId()).isEqualTo(validSourceQuestionId);
        assertThat(createdEvent.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(createdEvent.getUserId()).isEqualTo(1001L);
        assertThat(createdEvent.getQuestionBankId()).isEqualTo(2002L);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should enforce immutability of core properties")
    @Description("Validates that key properties like ID, user, and creation time cannot be modified")
    void shouldEnforceImmutabilityOfCoreProperties() {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Test Question", "Test Content", 5
        );

        // These properties should not have setters
        assertThat(aggregate.getId()).isNotNull();
        assertThat(aggregate.getUserId()).isEqualTo(1001L);
        assertThat(aggregate.getQuestionBankId()).isEqualTo(2002L);
        assertThat(aggregate.getSourceQuestionId()).isEqualTo(validSourceQuestionId);
        assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(aggregate.getCreatedAt()).isNotNull();

        // Verify no setters exist for immutable properties
        var methods = Arrays.stream(QuestionAggregate.class.getMethods())
            .map(Method::getName)
            .collect(Collectors.toList());

        assertThat(methods).doesNotContain(
            "setUserId", "setQuestionBankId", "setSourceQuestionId",
            "setQuestionType", "setCreatedAt", "setId"
        );
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should validate business rules during creation")
    @Description("Ensures comprehensive business rule validation during aggregate creation")
    void shouldValidateBusinessRulesDuringCreation() {
        // Title cannot be empty
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        assertThatThrownBy(() ->
            QuestionAggregate.createNew(
                1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
                "", "Test Content", 5
            )
        ).isInstanceOf(com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException.class)
         .hasMessageContaining("title is required");

        // Content cannot be empty
        assertThatThrownBy(() ->
            QuestionAggregate.createNew(
                1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
                "Test Question", "", 5
            )
        ).isInstanceOf(com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException.class)
         .hasMessageContaining("content is required");

        // Points cannot be negative
        assertThatThrownBy(() ->
            QuestionAggregate.createNew(
                1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
                "Test Question", "Test Content", -1
            )
        ).isInstanceOf(com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException.class)
         .hasMessageContaining("Points cannot be negative");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should handle concurrent creation safely")
    @Description("Verifies thread safety of aggregate creation under concurrent load")
    void shouldHandleConcurrentCreationSafely() throws InterruptedException {
        var aggregates = new ConcurrentLinkedQueue<QuestionAggregate>();
        var executor = Executors.newFixedThreadPool(10);
        var latch = new CountDownLatch(50);

        for (int i = 0; i < 50; i++) {
            final int questionId = i;
            executor.submit(() -> {
                try {
                    String sourceQuestionId = UUIDv7Generator.generateAsString();
                    var aggregate = QuestionAggregate.createNew(
                        1001L, 2002L, sourceQuestionId, QuestionType.MCQ,
                        "Question " + questionId, "Content " + questionId, 5
                    );
                    aggregates.add(aggregate);
                } catch (Exception e) {
                    // Log the exception for debugging but don't fail the test
                    System.err.println("Failed to create aggregate " + questionId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        assertThat(aggregates).hasSize(50);

        // All aggregates should have unique source question IDs
        var sourceIds = aggregates.stream()
            .map(QuestionAggregate::getSourceQuestionId)
            .collect(Collectors.toSet());

        assertThat(sourceIds).hasSize(50);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should extend AggregateRoot properly")
    @Description("Validates proper inheritance from shared AggregateRoot base class")
    void shouldExtendAggregateRootProperly() {
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Test Question", "Test Content", 5
        );

        // Should extend AggregateRoot
        assertThat(aggregate).isInstanceOf(com.quizfun.shared.domain.AggregateRoot.class);

        // Should have version tracking
        assertThat(aggregate.getVersion()).isNull(); // New aggregates start with null version

        // Should have creation/update timestamps from base class
        assertThat(aggregate.getCreatedAt()).isNotNull();
        assertThat(aggregate.getUpdatedAt()).isNotNull();
        assertThat(aggregate.getUpdatedAt()).isEqualTo(aggregate.getCreatedAt());

        // Should have domain event functionality
        assertThat(aggregate.getUncommittedEvents()).isNotEmpty();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateCoreImplementationTest.Should handle all question types")
    @Description("Ensures aggregate creation works correctly for all supported question types")
    void shouldHandleAllQuestionTypes() {
        // MCQ question
        String mcqSourceId = UUIDv7Generator.generateAsString();
        var mcqAggregate = QuestionAggregate.createNew(
            1001L, 2002L, mcqSourceId, QuestionType.MCQ,
            "MCQ Question", "MCQ Content", 5
        );
        assertThat(mcqAggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);

        // Essay question
        String essaySourceId = UUIDv7Generator.generateAsString();
        var essayAggregate = QuestionAggregate.createNew(
            1001L, 2002L, essaySourceId, QuestionType.ESSAY,
            "Essay Question", "Essay Content", 10
        );
        assertThat(essayAggregate.getQuestionType()).isEqualTo(QuestionType.ESSAY);

        // True/False question
        String trueFalseSourceId = UUIDv7Generator.generateAsString();
        var trueFalseAggregate = QuestionAggregate.createNew(
            1001L, 2002L, trueFalseSourceId, QuestionType.TRUE_FALSE,
            "True/False Question", "True/False Content", 2
        );
        assertThat(trueFalseAggregate.getQuestionType()).isEqualTo(QuestionType.TRUE_FALSE);
    }
}