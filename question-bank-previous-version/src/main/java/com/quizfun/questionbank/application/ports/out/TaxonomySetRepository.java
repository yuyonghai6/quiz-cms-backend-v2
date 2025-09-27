package com.quizfun.questionbank.application.ports.out;

import com.quizfun.shared.common.Result;
import java.util.List;

/**
 * Repository interface for managing taxonomy set validation and operations
 */
public interface TaxonomySetRepository {

    /**
     * Validates all taxonomy references exist for a user's question bank
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param taxonomyIds List of taxonomy IDs to validate
     * @return Result containing boolean true if all references are valid
     */
    Result<Boolean> validateTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds);

    /**
     * Validates a single taxonomy reference exists
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param taxonomyId The taxonomy ID to validate
     * @return Result containing boolean true if the reference is valid
     */
    Result<Boolean> validateTaxonomyReference(Long userId, Long questionBankId, String taxonomyId);

    /**
     * Validates category reference exists at specific level
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param categoryId The category ID to validate
     * @param level The category level (level_1, level_2, level_3, level_4)
     * @return Result containing boolean true if category exists at the specified level
     */
    Result<Boolean> validateCategoryReference(Long userId, Long questionBankId, String categoryId, String level);

    /**
     * Validates tag reference exists
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param tagId The tag ID to validate
     * @return Result containing boolean true if tag exists
     */
    Result<Boolean> validateTagReference(Long userId, Long questionBankId, String tagId);

    /**
     * Validates quiz reference exists
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param quizId The quiz ID to validate
     * @return Result containing boolean true if quiz exists
     */
    Result<Boolean> validateQuizReference(Long userId, Long questionBankId, String quizId);

    /**
     * Validates difficulty level reference exists
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param difficultyLevel The difficulty level to validate
     * @return Result containing boolean true if difficulty level exists
     */
    Result<Boolean> validateDifficultyLevelReference(Long userId, Long questionBankId, String difficultyLevel);

    /**
     * Gets all taxonomy IDs that are invalid from the provided list
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param taxonomyIds List of taxonomy IDs to check
     * @return Result containing list of invalid taxonomy IDs
     */
    Result<List<String>> getInvalidTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds);
}