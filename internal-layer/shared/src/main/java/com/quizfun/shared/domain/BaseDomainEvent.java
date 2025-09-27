package com.quizfun.shared.domain;

import java.time.Instant;
import java.util.Objects;

public abstract class BaseDomainEvent implements DomainEvent {
    private final String eventType;
    private final Instant occurredOn;
    private final String aggregateId;

    protected BaseDomainEvent(String eventType, String aggregateId) {
        if (aggregateId == null || aggregateId.isEmpty()) {
            throw new IllegalArgumentException("aggregateId cannot be null or empty");
        }
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.aggregateId = aggregateId;
        this.occurredOn = Instant.now();
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }
}


