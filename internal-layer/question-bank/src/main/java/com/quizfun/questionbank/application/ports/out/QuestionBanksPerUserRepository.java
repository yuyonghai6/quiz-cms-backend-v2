package com.quizfun.questionbank.application.ports.out;

import com.quizfun.shared.common.Result;

/**
 * Repository interface for managing question bank ownership validation
 */
public interface QuestionBanksPerUserRepository {

    /**
     * Validates if a user owns the specified question bank
     *
     * @param userId The user ID to validate ownership for
     * @param questionBankId The question bank ID to validate ownership of
     * @return Result containing boolean true if user owns the question bank, false otherwise
     */
    Result<Boolean> validateOwnership(Long userId, Long questionBankId);

    /**
     * Retrieves the default question bank ID for a user
     *
     * @param userId The user ID to get the default question bank for
     * @return Result containing the default question bank ID, or failure if not found
     */
    Result<Long> getDefaultQuestionBankId(Long userId);

    /**
     * Checks if a question bank exists and is active
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID to check
     * @return Result containing boolean true if the question bank exists and is active
     */
    Result<Boolean> isQuestionBankActive(Long userId, Long questionBankId);
}