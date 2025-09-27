package com.quizfun.questionbank.domain.events;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import com.quizfun.questionbank.domain.entities.QuestionType;

class QuestionDomainEventsTest {

    @Nested
    @DisplayName("QuestionCreatedEvent Tests")
    class QuestionCreatedEventTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should create event with valid properties")
        @Description("Validates that QuestionCreatedEvent is created correctly with all required properties")
        void shouldCreateEventWithValidProperties() {
            // This test will fail initially - QuestionCreatedEvent doesn't exist yet
            var event = new QuestionCreatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L
            );

            assertThat(event.getEventType()).isEqualTo("QuestionCreated");
            assertThat(event.getAggregateId()).isEqualTo("aggregate-123");
            assertThat(event.getSourceQuestionId()).isEqualTo("Q456");
            assertThat(event.getQuestionType()).isEqualTo(QuestionType.MCQ);
            assertThat(event.getUserId()).isEqualTo(1001L);
            assertThat(event.getQuestionBankId()).isEqualTo(2002L);
            assertThat(event.getOccurredOn()).isNotNull();
            assertThat(event.getOccurredOn()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should reject null aggregate ID")
        @Description("Ensures QuestionCreatedEvent validation rejects null aggregate IDs")
        void shouldRejectNullAggregateId() {
            assertThatThrownBy(() ->
                new QuestionCreatedEvent(
                    null,
                    "Q456",
                    QuestionType.MCQ,
                    1001L,
                    2002L
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("aggregateId cannot be null");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should reject empty source question ID")
        @Description("Validates that QuestionCreatedEvent rejects empty source question IDs")
        void shouldRejectEmptySourceQuestionId() {
            assertThatThrownBy(() ->
                new QuestionCreatedEvent(
                    "aggregate-123",
                    "",
                    QuestionType.MCQ,
                    1001L,
                    2002L
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("sourceQuestionId cannot be null or empty");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should be immutable after creation")
        @Description("Verifies that QuestionCreatedEvent properties cannot be modified after creation")
        void shouldBeImmutableAfterCreation() {
            var event = new QuestionCreatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L
            );

            var originalOccurredOn = event.getOccurredOn();

            // Properties should be immutable - no setters should exist
            var methods = event.getClass().getMethods();
            var setterMethods = java.util.Arrays.stream(methods)
                .filter(method -> method.getName().startsWith("set"))
                .count();

            assertThat(setterMethods).isEqualTo(0);
            assertThat(event.getOccurredOn()).isEqualTo(originalOccurredOn);
        }
    }

    @Nested
    @DisplayName("QuestionUpdatedEvent Tests")
    class QuestionUpdatedEventTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should create updated event with change tracking")
        @Description("Validates QuestionUpdatedEvent creation with field tracking")
        void shouldCreateUpdatedEventWithChangeTracking() {
            var event = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of("title", "content")
            );

            assertThat(event.getEventType()).isEqualTo("QuestionUpdated");
            assertThat(event.getAggregateId()).isEqualTo("aggregate-123");
            assertThat(event.getSourceQuestionId()).isEqualTo("Q456");
            assertThat(event.getQuestionType()).isEqualTo(QuestionType.MCQ);
            assertThat(event.getUserId()).isEqualTo(1001L);
            assertThat(event.getQuestionBankId()).isEqualTo(2002L);
            assertThat(event.getFieldsUpdated()).containsExactly("title", "content");
            assertThat(event.getOccurredOn()).isNotNull();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should handle empty field updates")
        @Description("Ensures QuestionUpdatedEvent properly handles empty field update lists")
        void shouldHandleEmptyFieldUpdates() {
            var event = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of()
            );

            assertThat(event.getFieldsUpdated()).isEmpty();
            assertThat(event.hasActualChanges()).isFalse();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should track meaningful changes only")
        @Description("Verifies that QuestionUpdatedEvent is created only when actual changes occur")
        void shouldTrackMeaningfulChangesOnly() {
            var event = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of("title", "points")
            );

            assertThat(event.getFieldsUpdated()).containsExactly("title", "points");
            assertThat(event.hasActualChanges()).isTrue();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should provide change detection utility")
        @Description("Tests utility method to detect if actual changes occurred")
        void shouldProvideChangeDetectionUtility() {
            var sameValueEvent = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of()
            );

            var changedEvent = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of("title")
            );

            assertThat(sameValueEvent.hasActualChanges()).isFalse();
            assertThat(changedEvent.hasActualChanges()).isTrue();
        }
    }

    @Nested
    @DisplayName("Event Metadata and Context Tests")
    class EventMetadataTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should include proper event metadata")
        @Description("Verifies that domain events include all required metadata for auditing")
        void shouldIncludeProperEventMetadata() {
            var createdEvent = new QuestionCreatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L
            );

            // Events should extend BaseDomainEvent
            assertThat(createdEvent).isInstanceOf(com.quizfun.shared.domain.BaseDomainEvent.class);
            assertThat(createdEvent).isInstanceOf(com.quizfun.shared.domain.DomainEvent.class);

            assertThat(createdEvent.getEventType()).isNotNull();
            assertThat(createdEvent.getAggregateId()).isNotNull();
            assertThat(createdEvent.getOccurredOn()).isNotNull();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionDomainEventsTest.Should maintain event ordering by timestamp")
        @Description("Ensures events created in sequence have proper temporal ordering")
        void shouldMaintainEventOrderingByTimestamp() throws InterruptedException {
            var event1 = new QuestionCreatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L
            );

            Thread.sleep(1); // Ensure different timestamps

            var event2 = new QuestionUpdatedEvent(
                "aggregate-123",
                "Q456",
                QuestionType.MCQ,
                1001L,
                2002L,
                java.util.List.of("title")
            );

            assertThat(event2.getOccurredOn()).isAfter(event1.getOccurredOn());
        }
    }

}