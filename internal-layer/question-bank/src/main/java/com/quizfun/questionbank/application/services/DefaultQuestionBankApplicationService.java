package com.quizfun.questionbank.application.services;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.globalshared.utils.LongIdGenerator;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.application.ports.out.DefaultQuestionBankRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Application service for creating default question banks for new users.
 *
 * This service orchestrates the creation workflow by:
 * 1. Generating a unique question bank ID
 * 2. Creating domain aggregates (QuestionBanksPerUser + TaxonomySet)
 * 3. Delegating persistence to the repository
 *
 * This is a use case orchestrator - it contains no business logic,
 * only coordination of infrastructure and domain components.
 */
@Service
public class DefaultQuestionBankApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuestionBankApplicationService.class);

    private final DefaultQuestionBankRepository repository;
    private final LongIdGenerator longIdGenerator;

    /**
     * Constructor injection for all dependencies.
     *
     * @param repository Repository for persisting aggregates
     * @param longIdGenerator Generator for unique question bank IDs
     */
    public DefaultQuestionBankApplicationService(
            DefaultQuestionBankRepository repository,
            LongIdGenerator longIdGenerator) {
        this.repository = repository;
        this.longIdGenerator = longIdGenerator;
    }

    /**
     * Creates a default question bank for a new user.
     *
     * Workflow:
     * 1. Generate unique question bank ID
     * 2. Create QuestionBanksPerUserAggregate with default data
     * 3. Create TaxonomySetAggregate with default taxonomy
     * 4. Persist both aggregates atomically via repository
     *
     * @param userId The ID of the new user
     * @return Result containing the created question bank details or error
     */
    public Result<DefaultQuestionBankResponseDto> createDefaultQuestionBank(Long userId) {
        try {
            // Validate input
            if (userId == null) {
                logger.warn("Attempted to create default question bank with null userId");
                return Result.failure("INTERNAL_ERROR: User ID cannot be null");
            }

            logger.info("Creating default question bank for user: {}", userId);

            // 1. Generate question bank ID
            Long questionBankId = longIdGenerator.generateQuestionBankId();
            logger.debug("Generated question bank ID: {} for user: {}", questionBankId, userId);

            // 2. Create timestamp for consistency across both aggregates
            Instant now = Instant.now();

            // 3. Create QuestionBanksPerUserAggregate
            QuestionBanksPerUserAggregate questionBanksAggregate =
                QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

            logger.debug("Created QuestionBanksPerUserAggregate for user: {}", userId);

            // 4. Create TaxonomySetAggregate
            TaxonomySetAggregate taxonomyAggregate =
                TaxonomySetAggregate.createDefault(userId, questionBankId, now);

            logger.debug("Created TaxonomySetAggregate for user: {}", userId);

            // 5. Persist via repository (atomic transaction)
            Result<DefaultQuestionBankResponseDto> result =
                repository.createDefaultQuestionBank(questionBanksAggregate, taxonomyAggregate);

            if (result.success()) {
                logger.info("Successfully created default question bank for user: {} with bank ID: {}",
                    userId, questionBankId);
            } else {
                logger.warn("Failed to create default question bank for user: {}. Reason: {}",
                    userId, result.message());
            }

            return result;

        } catch (Exception ex) {
            logger.error("Internal error creating default question bank for user: {}", userId, ex);
            return Result.failure("INTERNAL_ERROR: " + ex.getMessage());
        }
    }
}
