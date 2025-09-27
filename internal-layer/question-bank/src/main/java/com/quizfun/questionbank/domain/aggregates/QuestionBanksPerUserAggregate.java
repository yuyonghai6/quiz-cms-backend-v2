package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionBank;
import com.quizfun.shared.domain.AggregateRoot;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionBanksPerUserAggregate extends AggregateRoot {
    private ObjectId id;
    private Long userId;
    private Long defaultQuestionBankId;
    private List<QuestionBank> questionBanks;

    private QuestionBanksPerUserAggregate() {
        // Private constructor for frameworks
    }

    public static QuestionBanksPerUserAggregate create(
            ObjectId id,
            Long userId,
            Long defaultQuestionBankId,
            List<QuestionBank> questionBanks) {

        QuestionBanksPerUserAggregate aggregate = new QuestionBanksPerUserAggregate();
        aggregate.id = Objects.requireNonNull(id, "ID cannot be null");
        aggregate.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        aggregate.defaultQuestionBankId = Objects.requireNonNull(defaultQuestionBankId, "Default Question Bank ID cannot be null");
        aggregate.questionBanks = Objects.requireNonNull(questionBanks, "Question Banks cannot be null");
        aggregate.markCreatedNow();

        return aggregate;
    }

    public boolean validateOwnership(Long userId, Long questionBankId) {
        if (!this.userId.equals(userId)) {
            return false;
        }

        return questionBanks.stream()
            .anyMatch(bank -> bank.getBankId().equals(questionBankId) && bank.isActive());
    }

    public boolean isDefaultQuestionBank(Long questionBankId) {
        return defaultQuestionBankId.equals(questionBankId);
    }

    public Optional<QuestionBank> findQuestionBank(Long questionBankId) {
        return questionBanks.stream()
            .filter(bank -> bank.getBankId().equals(questionBankId))
            .findFirst();
    }

    public List<QuestionBank> getActiveQuestionBanks() {
        return questionBanks.stream()
            .filter(QuestionBank::isActive)
            .collect(Collectors.toList());
    }

    public boolean belongsToUser(Long userId) {
        return this.userId.equals(userId);
    }

    // Getters
    public ObjectId getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDefaultQuestionBankId() {
        return defaultQuestionBankId;
    }

    public List<QuestionBank> getQuestionBanks() {
        return questionBanks;
    }
}