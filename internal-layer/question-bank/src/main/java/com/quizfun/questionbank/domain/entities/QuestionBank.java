package com.quizfun.questionbank.domain.entities;

import java.time.Instant;
import java.util.Objects;

public class QuestionBank {
    private final Long bankId;
    private final String name;
    private final String description;
    private final boolean isActive;
    private final Instant createdAt;
    private final Instant updatedAt;

    public QuestionBank(Long bankId, String name, String description, boolean isActive, Instant createdAt, Instant updatedAt) {
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.name = Objects.requireNonNull(name, "Bank name cannot be null");
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getBankId() {
        return bankId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
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
        QuestionBank that = (QuestionBank) o;
        return isActive == that.isActive &&
               Objects.equals(bankId, that.bankId) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankId, name, description, isActive, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "QuestionBank{bankId=" + bankId + ", name='" + name + "', isActive=" + isActive + "}";
    }
}