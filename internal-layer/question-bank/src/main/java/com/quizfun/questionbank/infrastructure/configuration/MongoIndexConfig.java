package com.quizfun.questionbank.infrastructure.configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB index configuration for the question-bank command module.
 *
 * This configuration establishes critical database indexes for the COMMAND side
 * of the CQRS architecture, focusing on write operations and data integrity.
 *
 * Key indexes created:
 * - questions collection: unique compound index on (user_id, question_bank_id, source_question_id)
 * - question_taxonomy_relationships: unique compound index on relationship keys
 *
 * Architecture Note:
 * - This belongs in the INTERNAL LAYER (question-bank module), NOT orchestration layer
 * - Follows hexagonal architecture: infrastructure concerns stay in infrastructure layer
 * - Profile: !test - Uses production MongoDB (dev/prod), TestContainers create indexes programmatically
 *
 * @see com.quizfun.questionbank.infrastructure.persistence.repositories.MongoQuestionRepository
 */
@Configuration
@Profile("!test")
public class MongoIndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    /**
     * Constructor injection for MongoDB template.
     *
     * @param mongoTemplate The Spring Data MongoDB template for index operations
     */
    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates all command-side indexes on application startup.
     *
     * This method runs once during application context initialization to ensure
     * all required indexes exist before any business operations execute.
     */
    @PostConstruct
    public void initializeCommandIndexes() {
        logger.info("Initializing MongoDB indexes for question-bank command module");

        createQuestionsIndexes();
        createQuestionTaxonomyRelationshipIndexes();

        logger.info("MongoDB command-side index initialization completed successfully");
    }

    /**
     * Creates indexes for the questions collection (write model).
     *
     * Critical Index: ux_user_bank_source_id
     * - Purpose: Enforces uniqueness of source_question_id within a user's question bank
     * - Enables proper upsert semantics in MongoQuestionRepository
     * - Prevents duplicate questions with same source ID
     * - Supports efficient query: find question by (user_id, question_bank_id, source_question_id)
     *
     * Without this index:
     * - Race conditions can create duplicate source_question_id entries
     * - Upsert operations may fail to find existing questions
     * - Data integrity violations can occur under concurrent load
     *
     * Performance Index: ix_user_bank_status
     * - Purpose: Optimizes listing questions by user, bank, and status
     * - Used for queries like "show me all draft questions"
     */
    private void createQuestionsIndexes() {
        try {
            logger.info("Creating indexes for 'questions' collection (command side)");

            var questions = mongoTemplate.getDb().getCollection("questions");

            // Unique compound index for upsert operations
            questions.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("user_id"),
                    Indexes.ascending("question_bank_id"),
                    Indexes.ascending("source_question_id")
                ),
                new IndexOptions().name("ux_user_bank_source_id").unique(true)
            );
            logger.info("Ensured unique index 'ux_user_bank_source_id' on questions collection");

            // Additional performance index for listing questions by user, bank, and status
            questions.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("user_id"),
                    Indexes.ascending("question_bank_id"),
                    Indexes.ascending("status")
                ),
                new IndexOptions().name("ix_user_bank_status")
            );
            logger.info("Ensured listing index 'ix_user_bank_status' on questions collection");

        } catch (Exception ex) {
            logger.error("Failed to create indexes for questions collection", ex);
            throw new IllegalStateException("MongoDB index creation failed for questions collection", ex);
        }
    }

    /**
     * Creates indexes for the question_taxonomy_relationships collection (write model).
     *
     * Critical Index: ux_user_bank_question_taxonomy
     * - Purpose: Enforces uniqueness of taxonomy relationships
     * - Prevents duplicate relationship entries
     * - Supports efficient relationship queries and replacements
     *
     * Performance Index: ix_user_bank_question
     * - Purpose: Optimizes queries for all relationships of a specific question
     * - Used during relationship replacement operations
     */
    private void createQuestionTaxonomyRelationshipIndexes() {
        try {
            logger.info("Creating indexes for 'question_taxonomy_relationships' collection (command side)");

            var rel = mongoTemplate.getDb().getCollection("question_taxonomy_relationships");

            // Unique compound index to prevent duplicate relationships
            rel.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("user_id"),
                    Indexes.ascending("question_bank_id"),
                    Indexes.ascending("question_id"),
                    Indexes.ascending("taxonomy_type"),
                    Indexes.ascending("taxonomy_id")
                ),
                new IndexOptions().name("ux_user_bank_question_taxonomy").unique(true)
            );
            logger.info("Ensured unique index 'ux_user_bank_question_taxonomy' on question_taxonomy_relationships");

            // Performance index for querying all relationships of a question
            rel.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("user_id"),
                    Indexes.ascending("question_bank_id"),
                    Indexes.ascending("question_id")
                ),
                new IndexOptions().name("ix_user_bank_question")
            );
            logger.info("Ensured query index 'ix_user_bank_question' on question_taxonomy_relationships");

        } catch (Exception ex) {
            logger.error("Failed to create indexes for question_taxonomy_relationships collection", ex);
            throw new IllegalStateException("MongoDB index creation failed for question_taxonomy_relationships", ex);
        }
    }
}
