package com.quizfun.shared.domain;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEventInfrastructureTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("DomainEventInfrastructureTest.Should capture timestamp when event is created")
    @Description("Validates that domain events automatically capture a timestamp when created, ensuring proper temporal ordering and audit trail capabilities")
    void shouldCaptureTimestampWhenEventIsCreated() {
        var beforeCreation = Instant.now();
        var event = new TestDomainEvent("aggregate-123");
        var afterCreation = Instant.now();

        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOccurredOn()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("DomainEventInfrastructureTest.Should set event metadata correctly")
    @Description("Validates that domain events correctly store essential metadata including event type, aggregate ID, and occurrence timestamp")
    void shouldSetEventMetadataCorrectly() {
        var event = new TestDomainEvent("aggregate-456");

        assertThat(event.getEventType()).isEqualTo("TestEvent");
        assertThat(event.getAggregateId()).isEqualTo("aggregate-456");
        assertThat(event.getOccurredOn()).isNotNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("DomainEventInfrastructureTest.Should handle concurrent event creation")
    @Description("Validates that domain event creation is thread-safe and maintains data consistency when creating multiple events concurrently")
    void shouldHandleConcurrentEventCreation() throws InterruptedException {
        var events = new ConcurrentLinkedQueue<TestDomainEvent>();
        var executor = Executors.newFixedThreadPool(10);
        var latch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            final int eventId = i;
            executor.submit(() -> {
                try {
                    var event = new TestDomainEvent("aggregate-" + eventId);
                    events.add(event);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(events).hasSize(100);

        var now = Instant.now();
        assertThat(events.stream().allMatch(e -> Duration.between(e.getOccurredOn(), now).getSeconds() < 10))
            .isTrue();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("DomainEventInfrastructureTest.Should validate required event fields")
    @Description("Validates that domain event creation enforces required field validation, throwing appropriate exceptions for null or empty aggregate IDs")
    void shouldValidateRequiredEventFields() {
        assertThatThrownBy(() -> new TestDomainEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("aggregateId cannot be null or empty");

        assertThatThrownBy(() -> new TestDomainEvent(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("aggregateId cannot be null or empty");
    }

    private static class TestDomainEvent extends BaseDomainEvent {
        public TestDomainEvent(String aggregateId) {
            super("TestEvent", aggregateId);
        }
    }
}


