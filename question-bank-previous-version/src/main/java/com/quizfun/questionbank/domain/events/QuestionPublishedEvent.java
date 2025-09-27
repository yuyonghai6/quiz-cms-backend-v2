package com.quizfun.questionbank.domain.events;

import com.quizfun.shared.domain.BaseDomainEvent;

public class QuestionPublishedEvent extends BaseDomainEvent {
    private final String sourceQuestionId;
    private final Long userId;
    private final Long questionBankId;

    public QuestionPublishedEvent(String aggregateId, String sourceQuestionId,
                                  Long userId, Long questionBankId) {
        super("QuestionPublished", aggregateId);
        this.sourceQuestionId = sourceQuestionId;
        this.userId = userId;
        this.questionBankId = questionBankId;
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }
}


