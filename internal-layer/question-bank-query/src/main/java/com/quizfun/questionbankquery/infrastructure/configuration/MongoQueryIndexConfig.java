package com.quizfun.questionbankquery.infrastructure.configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

/**
 * MongoDB index configuration for the question-bank-query module.
 *
 * This configuration establishes indexes for the QUERY side of the CQRS architecture,
 * focusing on read operations and query performance optimization.
 *
 * Key indexes created:
 * - questions collection: text index on question_text for full-text search
 * - Additional query-specific performance indexes as needed
 *
 * Architecture Note:
 * - This belongs in the INTERNAL LAYER (question-bank-query module), NOT orchestration layer
 * - Follows hexagonal architecture: infrastructure concerns stay in infrastructure layer
 * - Profile: !test - Uses production MongoDB (dev/prod), TestContainers create indexes programmatically
 * - CQRS Separation: Query-side indexes may differ from command-side indexes
 *
 * @see com.quizfun.questionbankquery.infrastructure.persistence.repositories.MongoQuestionQueryRepository
 */
@Configuration
@Profile("!test")
public class MongoQueryIndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoQueryIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    /**
     * Constructor injection for MongoDB template.
     *
     * @param mongoTemplate The Spring Data MongoDB template for index operations
     */
    public MongoQueryIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates all query-side indexes on application startup.
     *
     * This method runs once during application context initialization to ensure
     * all required indexes exist before any query operations execute.
     */
    @PostConstruct
    public void initializeQueryIndexes() {
        logger.info("Initializing MongoDB indexes for question-bank-query module");

        createFullTextSearchIndexes();

        logger.info("MongoDB query-side index initialization completed successfully");
    }

    /**
     * Creates full-text search indexes for the questions collection (read model).
     *
     * Text Index: question_text_text
     * - Purpose: Enables full-text search on question content
     * - Used by query operations: search questions by keyword/phrase
     * - Supports MongoDB $text queries with relevance scoring
     *
     * Query Use Cases:
     * - "Find all questions containing 'photosynthesis'"
     * - "Search questions related to 'Java programming'"
     * - Full-text search with ranking by relevance
     *
     * Note on CQRS:
     * - The query side may have different index strategies than command side
     * - Read models are optimized for query patterns, not write patterns
     * - Text indexes are read-heavy and typically only on query side
     */
    private void createFullTextSearchIndexes() {
        try {
            logger.info("Creating full-text search indexes for 'questions' collection (query side)");

            IndexOperations indexOps = mongoTemplate.indexOps("questions");

            // Text index for full-text search on question content
            // This enables MongoDB $text queries with relevance scoring
            TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("question_text")
                .named("question_text_text")
                .build();

            indexOps.ensureIndex(textIndex);
            logger.info("Successfully ensured text index 'question_text_text' on questions collection");

        } catch (Exception ex) {
            // Log warning instead of throwing exception
            // Text index creation might fail if index already exists or field doesn't exist yet
            logger.warn("Could not create text index on questions.question_text: {}", ex.getMessage());
            logger.debug("Full stack trace for text index creation failure", ex);
        }
    }
}