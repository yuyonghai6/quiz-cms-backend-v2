package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.application.ports.out.DefaultQuestionBankRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import com.quizfun.questionbank.infrastructure.persistence.documents.TaxonomySetDocument;
import com.quizfun.questionbank.infrastructure.persistence.mappers.QuestionBanksPerUserMapper;
import com.quizfun.questionbank.infrastructure.persistence.mappers.TaxonomySetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB implementation of DefaultQuestionBankRepository.
 *
 * Uses MongoDB transactions to ensure atomic creation of both
 * question_banks_per_user and taxonomy_sets documents.
 *
 * IMPORTANT: Requires MongoDB replica set for transaction support.
 * In tests, Testcontainers MongoDB provides replica set automatically.
 */
@Repository
public class MongoDefaultQuestionBankRepository implements DefaultQuestionBankRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoDefaultQuestionBankRepository.class);

    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient;
    private final QuestionBanksPerUserMapper questionBanksMapper;
    private final TaxonomySetMapper taxonomySetMapper;

    public MongoDefaultQuestionBankRepository(
            MongoTemplate mongoTemplate,
            MongoClient mongoClient,
            QuestionBanksPerUserMapper questionBanksMapper,
            TaxonomySetMapper taxonomySetMapper) {
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
        this.questionBanksMapper = questionBanksMapper;
        this.taxonomySetMapper = taxonomySetMapper;
    }

    @Override
    @Transactional
    public Result<DefaultQuestionBankResponseDto> createDefaultQuestionBank(
            QuestionBanksPerUserAggregate questionBanksAggregate,
            TaxonomySetAggregate taxonomyAggregate) {

        if (questionBanksAggregate == null) {
            throw new IllegalArgumentException("Question banks aggregate cannot be null");
        }
        if (taxonomyAggregate == null) {
            throw new IllegalArgumentException("Taxonomy aggregate cannot be null");
        }

        Long userId = questionBanksAggregate.getUserId();
        Long questionBankId = questionBanksAggregate.getDefaultQuestionBankId();

        logger.info("Creating default question bank for user {} with bank ID {}",
            userId, questionBankId);

        ClientSession session = null;
        try {
            session = mongoClient.startSession();

            return session.withTransaction(() -> {
                // 1. Check if user already exists
                if (checkUserExists(userId)) {
                    String errorMsg = String.format(
                        "DUPLICATE_USER: User %d already has a default question bank",
                        userId
                    );
                    logger.warn(errorMsg);
                    return Result.<DefaultQuestionBankResponseDto>failure(errorMsg);
                }

                // 2. Map and insert question_banks_per_user document
                QuestionBanksPerUserDocument qbDoc =
                    questionBanksMapper.toDocument(questionBanksAggregate);
                mongoTemplate.insert(qbDoc, "question_banks_per_user");
                logger.debug("Inserted question_banks_per_user document for user {}", userId);

                // 3. Map and insert taxonomy_sets document
                TaxonomySetDocument taxDoc =
                    taxonomySetMapper.toDocument(taxonomyAggregate);
                mongoTemplate.insert(taxDoc, "taxonomy_sets");
                logger.debug("Inserted taxonomy_sets document for user {} and bank {}",
                    userId, questionBankId);

                // 4. Build response DTO
                DefaultQuestionBankResponseDto responseDto = buildResponseDto(
                    questionBanksAggregate,
                    taxonomyAggregate
                );

                logger.info("Successfully created default question bank for user {}", userId);
                return Result.success(
                    "Default question bank created successfully for user " + userId,
                    responseDto
                );
            });

        } catch (Exception ex) {
            logger.error("Failed to create default question bank for user {}", userId, ex);
            return Result.failure(
                "DATABASE_ERROR: Failed to create default question bank - " + ex.getMessage()
            );
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Checks if user already has question banks.
     * Uses unique index on user_id for efficient lookup.
     */
    private boolean checkUserExists(Long userId) {
        Query query = Query.query(Criteria.where("user_id").is(userId));
        return mongoTemplate.exists(query, QuestionBanksPerUserDocument.class);
    }

    /**
     * Builds response DTO from aggregates.
     */
    private DefaultQuestionBankResponseDto buildResponseDto(
            QuestionBanksPerUserAggregate questionBanksAggregate,
            TaxonomySetAggregate taxonomyAggregate) {

        // Extract data from aggregates
        Long userId = questionBanksAggregate.getUserId();
        Long questionBankId = questionBanksAggregate.getDefaultQuestionBankId();
        var questionBank = questionBanksAggregate.getQuestionBanks().get(0);

        // Build available taxonomy info
        // TODO(human): Implement buildAvailableTaxonomy method
        Map<String, Object> availableTaxonomy = buildAvailableTaxonomy(taxonomyAggregate);

        // Build and return response DTO
        return DefaultQuestionBankResponseDto.builder()
            .userId(userId)
            .questionBankId(questionBankId)
            .questionBankName(questionBank.getName())
            .description(questionBank.getDescription())
            .isActive(questionBank.isActive())
            .taxonomySetCreated(true)
            .availableTaxonomy(availableTaxonomy)
            .createdAt(questionBank.getCreatedAt())
            .build();
    }

    // TODO(human): Implement this method to extract taxonomy data from aggregate
    private Map<String, Object> buildAvailableTaxonomy(TaxonomySetAggregate taxonomyAggregate) {
        // This method should extract categories, tags, and difficulty levels
        // from the taxonomyAggregate and build a Map structure for the response

        Map<String, Object> taxonomy = new HashMap<>();

        // TODO(human): Extract and populate categories, tags, and difficulty levels
        // Hint: Use taxonomyAggregate.getCategories(), getTags(), and getAvailableDifficultyLevels()

        // Temporary implementation - returns empty map
        // You should populate this with actual data from the aggregate

        return taxonomy;
    }
}
