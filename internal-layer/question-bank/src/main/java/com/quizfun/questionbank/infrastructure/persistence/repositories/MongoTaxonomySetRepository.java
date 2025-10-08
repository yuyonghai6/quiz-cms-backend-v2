package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.questionbank.application.ports.out.TaxonomySetRepository;
import com.quizfun.questionbank.infrastructure.persistence.documents.TaxonomySetDocument;
import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of TaxonomySetRepository.
 * Provides taxonomy reference validation for categories, tags, quizzes, and difficulty levels.
 */
@Repository
public class MongoTaxonomySetRepository implements TaxonomySetRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoTaxonomySetRepository.class);
    private final MongoTemplate mongoTemplate;

    public MongoTaxonomySetRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Result<Boolean> validateTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds) {
        try {
            logger.debug("Validating {} taxonomy references for user {} and bank {}",
                taxonomyIds != null ? taxonomyIds.size() : 0, userId, questionBankId);

            if (userId == null || questionBankId == null) {
                return Result.success(false);
            }

            if (taxonomyIds == null || taxonomyIds.isEmpty()) {
                return Result.success(true); // Empty list is valid
            }

            // Check if taxonomy set exists for user and question bank
            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating taxonomy references for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate taxonomy references: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating taxonomy references for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during taxonomy validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateTaxonomyReference(Long userId, Long questionBankId, String taxonomyId) {
        try {
            logger.debug("Validating taxonomy reference {} for user {} and bank {}",
                taxonomyId, userId, questionBankId);

            if (userId == null || questionBankId == null || taxonomyId == null) {
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating taxonomy reference {} for user {} and bank {}",
                taxonomyId, userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate taxonomy reference: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating taxonomy reference {} for user {} and bank {}",
                taxonomyId, userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during taxonomy reference validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateCategoryReference(Long userId, Long questionBankId, String categoryId, String level) {
        try {
            logger.debug("Validating category {} at level {} for user {} and bank {}",
                categoryId, level, userId, questionBankId);

            if (userId == null || questionBankId == null || categoryId == null || level == null) {
                return Result.success(false);
            }

            // Determine the field path based on level
            String fieldPath = switch (level.toLowerCase()) {
                case "level_1", "1" -> "categories.level_1.id";
                case "level_2", "2" -> "categories.level_2.id";
                case "level_3", "3" -> "categories.level_3.id";
                case "level_4", "4" -> "categories.level_4.id";
                default -> null;
            };

            if (fieldPath == null) {
                logger.warn("Invalid category level: {}", level);
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
                    .and(fieldPath).is(categoryId)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);

            logger.debug("Category {} at level {} validation result: {}", categoryId, level, exists);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating category {} at level {} for user {} and bank {}",
                categoryId, level, userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate category reference: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating category {} at level {} for user {} and bank {}",
                categoryId, level, userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during category validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateTagReference(Long userId, Long questionBankId, String tagId) {
        try {
            logger.debug("Validating tag {} for user {} and bank {}", tagId, userId, questionBankId);

            if (userId == null || questionBankId == null || tagId == null) {
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
                    .and("tags.id").is(tagId)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);

            logger.debug("Tag {} validation result: {}", tagId, exists);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating tag {} for user {} and bank {}",
                tagId, userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate tag reference: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating tag {} for user {} and bank {}",
                tagId, userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during tag validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateQuizReference(Long userId, Long questionBankId, String quizId) {
        try {
            logger.debug("Validating quiz {} for user {} and bank {}", quizId, userId, questionBankId);

            if (userId == null || questionBankId == null || quizId == null) {
                return Result.success(false);
            }

            // Try to parse quizId as Long
            Long quizIdLong;
            try {
                quizIdLong = Long.parseLong(quizId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid quiz ID format: {}", quizId);
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
                    .and("quizzes.quiz_id").is(quizIdLong)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);

            logger.debug("Quiz {} validation result: {}", quizId, exists);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating quiz {} for user {} and bank {}",
                quizId, userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate quiz reference: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating quiz {} for user {} and bank {}",
                quizId, userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during quiz validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateDifficultyLevelReference(Long userId, Long questionBankId, String difficultyLevel) {
        try {
            logger.debug("Validating difficulty level {} for user {} and bank {}",
                difficultyLevel, userId, questionBankId);

            if (userId == null || questionBankId == null || difficultyLevel == null) {
                return Result.success(false);
            }

            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
                    .and("available_difficulty_levels.level").is(difficultyLevel)
            );

            boolean exists = mongoTemplate.exists(query, TaxonomySetDocument.class);

            logger.debug("Difficulty level {} validation result: {}", difficultyLevel, exists);
            return Result.success(exists);

        } catch (DataAccessException ex) {
            logger.error("Database error validating difficulty level {} for user {} and bank {}",
                difficultyLevel, userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to validate difficulty level: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error validating difficulty level {} for user {} and bank {}",
                difficultyLevel, userId, questionBankId, ex);
            return Result.failure("VALIDATION_ERROR",
                "Unexpected error during difficulty level validation: " + ex.getMessage());
        }
    }

    @Override
    public Result<List<String>> getInvalidTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds) {
        try {
            logger.debug("Finding invalid taxonomy references from {} IDs for user {} and bank {}",
                taxonomyIds != null ? taxonomyIds.size() : 0, userId, questionBankId);

            if (userId == null || questionBankId == null) {
                return Result.failure("INVALID_INPUT", "User ID and question bank ID cannot be null");
            }

            if (taxonomyIds == null || taxonomyIds.isEmpty()) {
                return Result.success(new ArrayList<>()); // No IDs to validate
            }

            // Check if taxonomy set exists
            Query query = Query.query(
                Criteria.where("user_id").is(userId)
                    .and("question_bank_id").is(questionBankId)
            );

            TaxonomySetDocument document = mongoTemplate.findOne(query, TaxonomySetDocument.class);

            if (document == null) {
                // If no taxonomy set exists, all references are invalid
                logger.warn("No taxonomy set found for user {} and bank {}", userId, questionBankId);
                return Result.success(new ArrayList<>(taxonomyIds));
            }

            // For now, return empty list as all references are considered valid if taxonomy set exists
            // In a real implementation, you would check each taxonomyId against the document structure
            List<String> invalidIds = new ArrayList<>();

            logger.debug("Found {} invalid taxonomy references", invalidIds.size());
            return Result.success(invalidIds);

        } catch (DataAccessException ex) {
            logger.error("Database error finding invalid taxonomy references for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR",
                "Failed to find invalid taxonomy references: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error finding invalid taxonomy references for user {} and bank {}",
                userId, questionBankId, ex);
            return Result.failure("QUERY_ERROR",
                "Unexpected error during invalid reference query: " + ex.getMessage());
        }
    }
}
