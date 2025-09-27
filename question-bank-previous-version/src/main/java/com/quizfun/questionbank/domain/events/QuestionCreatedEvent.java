package com.quizfun.questionbank.domain.events;

import com.quizfun.shared.domain.BaseDomainEvent;
import com.quizfun.questionbank.domain.entities.QuestionType;

import java.util.Objects;

public class QuestionCreatedEvent extends BaseDomainEvent {
    private final String sourceQuestionId;
    private final QuestionType questionType;
    private final Long userId;
    private final Long questionBankId;

    public QuestionCreatedEvent(String aggregateId, String sourceQuestionId,
                               QuestionType questionType, Long userId, Long questionBankId) {
        super("QuestionCreated", aggregateId);

        if (sourceQuestionId == null || sourceQuestionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceQuestionId cannot be null or empty");
        }

        this.sourceQuestionId = Objects.requireNonNull(sourceQuestionId, "sourceQuestionId cannot be null");
        this.questionType = Objects.requireNonNull(questionType, "questionType cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.questionBankId = Objects.requireNonNull(questionBankId, "questionBankId cannot be null");
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionCreatedEvent that = (QuestionCreatedEvent) o;
        return Objects.equals(getAggregateId(), that.getAggregateId()) &&
               Objects.equals(sourceQuestionId, that.sourceQuestionId) &&
               questionType == that.questionType &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(questionBankId, that.questionBankId) &&
               Objects.equals(getOccurredOn(), that.getOccurredOn());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAggregateId(), sourceQuestionId, questionType, userId, questionBankId, getOccurredOn());
    }
}