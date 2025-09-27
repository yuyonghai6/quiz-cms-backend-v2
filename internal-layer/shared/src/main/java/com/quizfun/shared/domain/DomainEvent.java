package com.quizfun.shared.domain;

import java.time.Instant;

public interface DomainEvent {
    String getEventType();
    Instant getOccurredOn();
    String getAggregateId();
}