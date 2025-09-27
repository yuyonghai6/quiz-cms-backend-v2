package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.events.QuestionUpdatedEvent;
import com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException;
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

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class QuestionAggregateUpdateLogicTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should update basic content properties")
    @Description("Validates updating title, content, and points while preserving immutable properties")
    void shouldUpdateBasicContentProperties() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );
        Instant originalCreatedAt = aggregate.getCreatedAt();
        Instant originalUpdatedAt = aggregate.getUpdatedAt();

        // Act
        aggregate.updateBasicContent("Updated Title", "Updated Content", 10);

        // Assert
        assertThat(aggregate.getTitle()).isEqualTo("Updated Title");
        assertThat(aggregate.getContent()).isEqualTo("Updated Content");
        assertThat(aggregate.getPoints()).isEqualTo(10);

        // Immutable properties should remain unchanged
        assertThat(aggregate.getUserId()).isEqualTo(1001L);
        assertThat(aggregate.getQuestionBankId()).isEqualTo(2002L);
        assertThat(aggregate.getSourceQuestionId()).isEqualTo(validSourceQuestionId);
        assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(aggregate.getCreatedAt()).isEqualTo(originalCreatedAt);

        // Updated timestamp should be modified
        assertThat(aggregate.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should generate QuestionUpdatedEvent on update")
    @Description("Verifies that domain events are generated when content is updated")
    void shouldGenerateQuestionUpdatedEventOnUpdate() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Clear creation events to isolate update events
        aggregate.markEventsAsCommitted();

        // Act
        aggregate.updateBasicContent("Updated Title", "Updated Content", 10);

        // Assert
        var events = aggregate.getUncommittedEvents();
        assertThat(events).hasSize(1);

        var event = events.get(0);
        assertThat(event).isInstanceOf(QuestionUpdatedEvent.class);

        var updatedEvent = (QuestionUpdatedEvent) event;
        assertThat(updatedEvent.getSourceQuestionId()).isEqualTo(validSourceQuestionId);
        assertThat(updatedEvent.getQuestionType()).isEqualTo(QuestionType.MCQ);
        assertThat(updatedEvent.getUserId()).isEqualTo(1001L);
        assertThat(updatedEvent.getQuestionBankId()).isEqualTo(2002L);
        assertThat(updatedEvent.getFieldsUpdated()).contains("title", "content", "points");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should update status and optional fields")
    @Description("Validates updating status, displayOrder, and solutionExplanation")
    void shouldUpdateStatusAndOptionalFields() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Test Title", "Test Content", 5
        );

        // Act
        aggregate.updateStatusAndMetadata("published", 1, "This is the solution explanation");

        // Assert
        assertThat(aggregate.getStatus()).isEqualTo("published");
        assertThat(aggregate.getDisplayOrder()).isEqualTo(1);
        assertThat(aggregate.getSolutionExplanation()).isEqualTo("This is the solution explanation");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should auto-trim whitespace during update")
    @Description("Ensures whitespace is automatically trimmed from string fields during updates")
    void shouldAutoTrimWhitespaceOnUpdate() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Act
        aggregate.updateBasicContent("  Updated Title  ", "  Updated Content  ", 10);

        // Assert
        assertThat(aggregate.getTitle()).isEqualTo("Updated Title");
        assertThat(aggregate.getContent()).isEqualTo("Updated Content");
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @ValueSource(strings = {"", "   "})
    @NullSource
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should reject empty title during update")
    @Description("Validates that empty titles are rejected during content updates")
    void shouldRejectEmptyTitleDuringUpdate(String invalidTitle) {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Act & Assert
        assertThatThrownBy(() ->
            aggregate.updateBasicContent(invalidTitle, "Valid Content", 10)
        ).isInstanceOf(InvalidQuestionDataException.class)
         .hasMessageContaining("title is required");
    }

    @ParameterizedTest
    @Epic("Enabler Epic-Core Infrastructure")
    @ValueSource(strings = {"", "   "})
    @NullSource
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should reject empty content during update")
    @Description("Validates that empty content is rejected during content updates")
    void shouldRejectEmptyContentDuringUpdate(String invalidContent) {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Act & Assert
        assertThatThrownBy(() ->
            aggregate.updateBasicContent("Valid Title", invalidContent, 10)
        ).isInstanceOf(InvalidQuestionDataException.class)
         .hasMessageContaining("content is required");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should reject negative points during update")
    @Description("Validates that negative points are rejected during content updates")
    void shouldRejectNegativePointsDuringUpdate() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Act & Assert
        assertThatThrownBy(() ->
            aggregate.updateBasicContent("Valid Title", "Valid Content", -1)
        ).isInstanceOf(InvalidQuestionDataException.class)
         .hasMessageContaining("Points cannot be negative");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should respect character limits during update")
    @Description("Validates title and content length limits during updates")
    void shouldRespectCharacterLimitsDuringUpdate() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        String longTitle = "a".repeat(256); // 256 characters, exceeds 255 limit
        String longContent = "b".repeat(4001); // 4001 characters, exceeds 4000 limit

        // Act & Assert - Title too long
        assertThatThrownBy(() ->
            aggregate.updateBasicContent(longTitle, "Valid Content", 10)
        ).isInstanceOf(InvalidQuestionDataException.class)
         .hasMessageContaining("title cannot exceed 255 characters");

        // Act & Assert - Content too long
        assertThatThrownBy(() ->
            aggregate.updateBasicContent("Valid Title", longContent, 10)
        ).isInstanceOf(InvalidQuestionDataException.class)
         .hasMessageContaining("content cannot exceed 4,000 characters");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should allow null points to default to zero")
    @Description("Validates that null points default to 0 during updates")
    void shouldAllowNullPointsToDefaultToZero() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        // Act
        aggregate.updateBasicContent("Updated Title", "Updated Content", null);

        // Assert
        assertThat(aggregate.getPoints()).isEqualTo(0);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should handle concurrent updates safely")
    @Description("Verifies thread safety of aggregate updates under concurrent load")
    void shouldHandleConcurrentUpdatesSafely() throws InterruptedException {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        var completedUpdates = new ConcurrentLinkedQueue<Integer>();
        var executor = Executors.newFixedThreadPool(10);
        var latch = new CountDownLatch(20);

        // Act
        for (int i = 0; i < 20; i++) {
            final int updateId = i;
            executor.submit(() -> {
                try {
                    aggregate.updateBasicContent(
                        "Title " + updateId,
                        "Content " + updateId,
                        updateId
                    );
                    completedUpdates.add(updateId);
                } catch (Exception e) {
                    System.err.println("Failed to update aggregate " + updateId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Assert
        latch.await(10, TimeUnit.SECONDS);
        assertThat(completedUpdates).hasSize(20);

        // Final state should reflect one of the updates
        assertThat(aggregate.getTitle()).matches("Title \\d+");
        assertThat(aggregate.getContent()).matches("Content \\d+");
        assertThat(aggregate.getPoints()).isBetween(0, 19);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should preserve creation audit information")
    @Description("Ensures creation timestamps and user information remain unchanged during updates")
    void shouldPreserveCreationAuditInformation() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );

        Instant originalCreatedAt = aggregate.getCreatedAt();
        Long originalUserId = aggregate.getUserId();
        Long originalQuestionBankId = aggregate.getQuestionBankId();

        // Act
        aggregate.updateBasicContent("Updated Title", "Updated Content", 10);

        // Assert - Creation audit information should be preserved
        assertThat(aggregate.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(aggregate.getUserId()).isEqualTo(originalUserId);
        assertThat(aggregate.getQuestionBankId()).isEqualTo(originalQuestionBankId);

        // But updated timestamp should be different
        assertThat(aggregate.getUpdatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateUpdateLogicTest.Should track multiple field updates in events")
    @Description("Verifies that update events contain information about which fields were changed")
    void shouldTrackMultipleFieldUpdatesInEvents() {
        // Arrange
        String validSourceQuestionId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(
            1001L, 2002L, validSourceQuestionId, QuestionType.MCQ,
            "Original Title", "Original Content", 5
        );
        aggregate.markEventsAsCommitted(); // Clear creation events

        // Act - Update only some fields
        aggregate.updateStatusAndMetadata("published", null, "Solution explanation");

        // Assert
        var events = aggregate.getUncommittedEvents();
        assertThat(events).hasSize(1);

        var updatedEvent = (QuestionUpdatedEvent) events.get(0);
        assertThat(updatedEvent.getFieldsUpdated()).contains("status", "solutionExplanation");
        assertThat(updatedEvent.getFieldsUpdated()).doesNotContain("displayOrder");
    }
}