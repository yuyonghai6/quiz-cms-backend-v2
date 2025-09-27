package com.quizfun.questionbank.domain.events;

import com.quizfun.shared.domain.BaseDomainEvent;
import com.quizfun.questionbank.domain.entities.QuestionType;

import java.util.List;
import java.util.Objects;

public class QuestionUpdatedEvent extends BaseDomainEvent {
    private final String sourceQuestionId;
    private final QuestionType questionType;
    private final Long userId;
    private final Long questionBankId;
    private final List<String> fieldsUpdated;

    public QuestionUpdatedEvent(String aggregateId, String sourceQuestionId,
                               QuestionType questionType, Long userId, Long questionBankId,
                               List<String> fieldsUpdated) {
        super("QuestionUpdated", aggregateId);

        this.sourceQuestionId = Objects.requireNonNull(sourceQuestionId, "sourceQuestionId cannot be null");
        this.questionType = Objects.requireNonNull(questionType, "questionType cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.questionBankId = Objects.requireNonNull(questionBankId, "questionBankId cannot be null");
        this.fieldsUpdated = List.copyOf(Objects.requireNonNull(fieldsUpdated, "fieldsUpdated cannot be null"));
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

    public List<String> getFieldsUpdated() {
        return fieldsUpdated;
    }

    public boolean hasActualChanges() {
        return !fieldsUpdated.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionUpdatedEvent that = (QuestionUpdatedEvent) o;
        return Objects.equals(getAggregateId(), that.getAggregateId()) &&
               Objects.equals(sourceQuestionId, that.sourceQuestionId) &&
               Objects.equals(questionType, that.questionType) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(questionBankId, that.questionBankId) &&
               Objects.equals(fieldsUpdated, that.fieldsUpdated) &&
               Objects.equals(getOccurredOn(), that.getOccurredOn());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAggregateId(), sourceQuestionId, questionType, userId, questionBankId, fieldsUpdated, getOccurredOn());
    }
}