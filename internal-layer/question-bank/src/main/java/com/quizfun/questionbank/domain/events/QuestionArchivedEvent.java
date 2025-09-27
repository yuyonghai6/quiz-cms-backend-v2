package com.quizfun.questionbank.domain.events;

import com.quizfun.shared.domain.BaseDomainEvent;

public class QuestionArchivedEvent extends BaseDomainEvent {
    private final String sourceQuestionId;

    public QuestionArchivedEvent(String aggregateId, String sourceQuestionId) {
        super("QuestionArchived", aggregateId);
        this.sourceQuestionId = sourceQuestionId;
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }
}


