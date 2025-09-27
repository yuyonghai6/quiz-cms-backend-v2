package com.quizfun.shared.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AggregateRoot {
    private Long version;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    protected void addDomainEvent(DomainEvent event) {
        synchronized (domainEvents) {
            domainEvents.add(event);
        }
    }

    public List<DomainEvent> getUncommittedEvents() {
        synchronized (domainEvents) {
            return new ArrayList<>(domainEvents);
        }
    }

    public void markEventsAsCommitted() {
        synchronized (domainEvents) {
            domainEvents.clear();
        }
    }

    public Long getVersion() {
        return version;
    }

    protected void setVersion(Long version) {
        this.version = version;
    }

    protected void markCreatedNow() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void markUpdatedNow() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot that = (AggregateRoot) o;
        // Fallback to reference equality when no identity semantics are defined in subclass
        return Objects.equals(version, that.version) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, createdAt, updatedAt);
    }
}