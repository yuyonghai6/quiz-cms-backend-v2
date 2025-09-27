package com.quizfun.questionbank.application.ports.out;

import com.quizfun.shared.common.Result;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for managing {@link QuestionAggregate} persistence.
 * Defines bank-scoped operations and idempotent upsert by source question ID.
 */
public interface QuestionRepository {

    /**
     * Creates or updates a question identified by its sourceQuestionId within the user's question bank scope.
     *
     * @param aggregate fully validated aggregate to upsert
     * @return Result with the persisted aggregate (including persisted identifiers)
     */
    Result<QuestionAggregate> upsertBySourceQuestionId(QuestionAggregate aggregate);

    /**
     * Finds a question by (userId, questionBankId, sourceQuestionId).
     *
     * @param userId owner of the question
     * @param questionBankId bank scope
     * @param sourceQuestionId external source identifier
     * @return Result with Optional aggregate
     */
    Result<Optional<QuestionAggregate>> findBySourceQuestionId(Long userId, Long questionBankId, String sourceQuestionId);

    /**
     * Lists questions for a user's question bank.
     *
     * @param userId owner of the bank
     * @param questionBankId bank identifier
     * @return Result with list of aggregates
     */
    Result<List<QuestionAggregate>> findByQuestionBank(Long userId, Long questionBankId);

    /**
     * Deletes a question by its MongoDB ObjectId.
     *
     * @param questionId MongoDB ObjectId
     * @return Result indicating success or failure
     */
    Result<Void> delete(ObjectId questionId);
}


