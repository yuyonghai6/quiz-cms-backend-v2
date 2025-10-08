package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionBank;
import com.quizfun.shared.domain.AggregateRoot;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionBanksPerUserAggregate extends AggregateRoot {

    // Default question bank constants
    private static final String DEFAULT_BANK_NAME = "Default Question Bank";
    private static final String DEFAULT_BANK_DESCRIPTION = "Your default question bank for getting started with quiz creation";

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

    /**
     * Factory method for creating default question bank aggregate for new users.
     *
     * Creates a single default question bank with standard name and description,
     * sets it as the default bank, and marks it as active.
     *
     * @param userId The user ID for the new user
     * @param questionBankId The generated question bank ID
     * @param timestamp The creation timestamp
     * @return New aggregate with default question bank
     * @throws NullPointerException if any parameter is null
     */
    public static QuestionBanksPerUserAggregate createDefault(
            Long userId,
            Long questionBankId,
            Instant timestamp) {

        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(questionBankId, "Question Bank ID cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        // Create default question bank entity
        QuestionBank defaultBank = new QuestionBank(
            questionBankId,
            DEFAULT_BANK_NAME,
            DEFAULT_BANK_DESCRIPTION,
            true,  // isActive
            timestamp,
            timestamp
        );

        // Create aggregate with default bank
        return create(
            new ObjectId(),
            userId,
            questionBankId,
            List.of(defaultBank)
        );
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