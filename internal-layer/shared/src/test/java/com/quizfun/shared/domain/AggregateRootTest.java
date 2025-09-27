package com.quizfun.shared.domain;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateRootTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("AggregateRootTest.Should add domain event to uncommitted events")
    @Description("Validates that domain events can be added to an aggregate root and are stored in the uncommitted events collection for later processing")
    void shouldAddDomainEventToUncommittedEvents() {
        // Test will fail - AggregateRoot doesn't exist yet
        var aggregate = new TestAggregate();
        var event = new TestDomainEvent("test-id");

        aggregate.addDomainEvent(event);

        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        assertThat(aggregate.getUncommittedEvents()).contains(event);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("AggregateRootTest.Should return defensive copy of uncommitted events")
    @Description("Validates that getUncommittedEvents() returns a defensive copy to prevent external modification of the internal event collection")
    void shouldReturnDefensiveCopyOfUncommittedEvents() {
        var aggregate = new TestAggregate();
        var event = new TestDomainEvent("test-id");
        aggregate.addDomainEvent(event);

        var events = aggregate.getUncommittedEvents();
        events.clear(); // Modify returned list

        // Original should be unchanged
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("AggregateRootTest.Should clear events when marked as committed")
    @Description("Validates that markEventsAsCommitted() clears the uncommitted events collection after events have been processed and persisted")
    void shouldClearEventsWhenMarkedAsCommitted() {
        var aggregate = new TestAggregate();
        aggregate.addDomainEvent(new TestDomainEvent("test-id"));

        aggregate.markEventsAsCommitted();

        assertThat(aggregate.getUncommittedEvents()).isEmpty();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("AggregateRootTest.Should handle concurrent access safely")
    @Description("Validates that the aggregate root handles concurrent access to domain events safely without race conditions or lost events in multi-threaded scenarios")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
        var aggregate = new TestAggregate();
        var executor = Executors.newFixedThreadPool(10);
        var latch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            final int eventId = i;
            executor.submit(() -> {
                try {
                    aggregate.addDomainEvent(new TestDomainEvent("event-" + eventId));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(aggregate.getUncommittedEvents()).hasSize(100);
    }

    private static class TestAggregate extends AggregateRoot {
        public void addDomainEvent(DomainEvent event) {
            super.addDomainEvent(event);
        }
    }

    private static class TestDomainEvent implements DomainEvent {
        private final String aggregateId;

        public TestDomainEvent(String aggregateId) {
            this.aggregateId = aggregateId;
        }

        @Override
        public String getEventType() {
            return "TestEvent";
        }

        @Override
        public java.time.Instant getOccurredOn() {
            return java.time.Instant.now();
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestDomainEvent that = (TestDomainEvent) obj;
            return aggregateId.equals(that.aggregateId);
        }

        @Override
        public int hashCode() {
            return aggregateId.hashCode();
        }
    }
}