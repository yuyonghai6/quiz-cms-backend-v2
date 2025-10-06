package com.quizfun.questionbank.infrastructure.persistence.mappers;

import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.entities.QuestionBank;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for QuestionBanksPerUserAggregate ↔ QuestionBanksPerUserDocument.
 */
@Component
public class QuestionBanksPerUserMapper {

    /**
     * Maps domain aggregate to MongoDB document.
     */
    public QuestionBanksPerUserDocument toDocument(QuestionBanksPerUserAggregate aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("Aggregate cannot be null");
        }

        QuestionBanksPerUserDocument document = new QuestionBanksPerUserDocument();
        document.setId(aggregate.getId());
        document.setUserId(aggregate.getUserId());
        document.setDefaultQuestionBankId(aggregate.getDefaultQuestionBankId());

        // Map embedded question banks
        List<QuestionBanksPerUserDocument.QuestionBankEmbedded> embeddedBanks =
            aggregate.getQuestionBanks().stream()
                .map(this::mapQuestionBankToEmbedded)
                .collect(Collectors.toList());
        document.setQuestionBanks(embeddedBanks);

        document.setCreatedAt(aggregate.getCreatedAt());
        document.setUpdatedAt(aggregate.getUpdatedAt());

        return document;
    }

    /**
     * Maps MongoDB document to domain aggregate.
     */
    public QuestionBanksPerUserAggregate toAggregate(QuestionBanksPerUserDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        List<QuestionBank> questionBanks = document.getQuestionBanks().stream()
            .map(this::mapEmbeddedToQuestionBank)
            .collect(Collectors.toList());

        return QuestionBanksPerUserAggregate.create(
            document.getId(),
            document.getUserId(),
            document.getDefaultQuestionBankId(),
            questionBanks
        );
    }

    /**
     * Maps domain QuestionBank entity to embedded document.
     */
    private QuestionBanksPerUserDocument.QuestionBankEmbedded mapQuestionBankToEmbedded(
            QuestionBank bank) {
        return new QuestionBanksPerUserDocument.QuestionBankEmbedded(
            bank.getBankId(),
            bank.getName(),
            bank.getDescription(),
            bank.isActive(),
            bank.getCreatedAt(),
            bank.getUpdatedAt()
        );
    }

    /**
     * Maps embedded document to domain QuestionBank entity.
     */
    private QuestionBank mapEmbeddedToQuestionBank(
            QuestionBanksPerUserDocument.QuestionBankEmbedded embedded) {
        return new QuestionBank(
            embedded.getBankId(),
            embedded.getName(),
            embedded.getDescription(),
            embedded.isActive(),
            embedded.getCreatedAt(),
            embedded.getUpdatedAt()
        );
    }
}
