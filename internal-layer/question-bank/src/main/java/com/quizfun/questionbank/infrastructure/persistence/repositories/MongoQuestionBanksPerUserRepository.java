package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * MongoDB implementation of QuestionBanksPerUserRepository.
 * Provides ownership validation and question bank queries.
 */
@Repository
public class MongoQuestionBanksPerUserRepository implements QuestionBanksPerUserRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoQuestionBanksPerUserRepository.class);
    private final MongoTemplate mongoTemplate;

    public MongoQuestionBanksPerUserRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Result<Boolean> validateOwnership(Long userId, Long questionBankId) {
        try {
            logger.debug("Validating ownership for user {} and question bank {}", userId, questionBankId);

            if (userId == null || questionBankId == null) {
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_banks.bank_id").is(questionBankId)
            );

            boolean exists = mongoTemplate.exists(query, QuestionBanksPerUserDocument.class);

            logger.debug("Ownership validation result for user {} and bank {}: {}",
                userId, questionBankId, exists);

            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating ownership for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate ownership: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating ownership for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during ownership validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Long> getDefaultQuestionBankId(Long userId) {
        try {
            logger.debug("Getting default question bank ID for user {}", userId);

            if (userId == null) {
                return Result.failure("INVALID_INPUT", "User ID cannot be null");
            }

            Query query = Query.query(Criteria.where("user_id").is(userId));
            QuestionBanksPerUserDocument document = mongoTemplate.findOne(
                query,
                QuestionBanksPerUserDocument.class
            );

            if (document == null) {
                logger.warn("No question banks found for user {}", userId);
                return Result.failure("NOT_FOUND",
                    "No question banks found for user " + userId);
            }

            Long defaultBankId = document.getDefaultQuestionBankId();
            if (defaultBankId == null) {
                logger.warn("User {} has no default question bank set", userId);
                return Result.failure("NOT_FOUND",
                    "User has no default question bank set");
            }

            logger.debug("Found default question bank {} for user {}", defaultBankId, userId);
            return Result.success(defaultBankId);

        } catch (DataAccessException ex) {
            logger.error("Database error getting default question bank for user {}", userId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to get default question bank: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error getting default question bank for user {}", userId, ex);
            return Result.failure("QUERY_ERROR",
                "Unexpected error during query: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> isQuestionBankActive(Long userId, Long questionBankId) {
        try {
            logger.debug("Checking if question bank {} is active for user {}",
                questionBankId, userId);

            if (userId == null || questionBankId == null) {
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_banks").elemMatch(
                        Criteria.where("bank_id").is(questionBankId)
                            .and("is_active").is(true)
                    )
            );

            boolean isActive = mongoTemplate.exists(query, QuestionBanksPerUserDocument.class);

            logger.debug("Question bank {} active status for user {}: {}",
                questionBankId, userId, isActive);

            return Result.success(isActive);

        } catch (DataAccessException ex) {
            logger.error("Database error checking if bank {} is active for user {}",
                questionBankId, userId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to check question bank status: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error checking if bank {} is active for user {}",
                questionBankId, userId, ex);
            return Result.failure("STATUS_CHECK_ERROR",
                "Unexpected error during status check: " + ex.getMessage());
        }
    }
}
