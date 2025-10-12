package com.quizfun.questionbank.config;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.application.ports.out.TaxonomySetRepository;
import com.quizfun.shared.common.Result;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.ActiveProfiles;

@SpringBootConfiguration // Clear intent
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
})
@EnableTransactionManagement
@ComponentScan(
    basePackages = {
        "com.quizfun.questionbank.application",
        "com.quizfun.questionbank.domain",
        "com.quizfun.questionbank.infrastructure",
        "com.quizfun.questionbank.security",
        "com.quizfun.globalshared"
    },
    excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, value = com.quizfun.questionbank.infrastructure.configuration.MongoTransactionConfig.class),
        // Exclude production index configuration to avoid conflicts with test-specific indexes
        @Filter(type = FilterType.ASSIGNABLE_TYPE, value = com.quizfun.questionbank.infrastructure.configuration.MongoIndexConfig.class),
        // Exclude the main QuestionBankConfiguration to prevent it from re-scanning the entire package
        // which would bring back excluded beans like MongoIndexConfig
        @Filter(type = FilterType.ASSIGNABLE_TYPE, value = com.quizfun.questionbank.infrastructure.configuration.QuestionBankConfiguration.class)
    }
)
@Testcontainers
@ActiveProfiles("test")
public class TestContainersConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainersConfig.class);

    @SuppressWarnings("resource")
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
            .withExposedPorts(27017)
            .withReuse(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        mongoContainer.start();
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "quiz_cms_test");
    }

    @Bean
    @Primary
    public MongoClient testMongoClient() {
        if (!mongoContainer.isRunning()) {
            mongoContainer.start();
        }
        return MongoClients.create(mongoContainer.getReplicaSetUrl());
    }

    @Bean
    @Primary
    public MongoDatabaseFactory testMongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, "quiz_cms_test");
    }

    @Bean
    @Primary
    public MongoTemplate testMongoTemplate(MongoDatabaseFactory databaseFactory) {
        return new MongoTemplate(databaseFactory);
    }

    @Bean
    @Primary
    public MongoTransactionManager testTransactionManager(MongoDatabaseFactory databaseFactory) {
        logger.info("Configuring MongoDB transaction manager for test environment with TestContainers");

        try {
            MongoTransactionManager transactionManager = new MongoTransactionManager(databaseFactory);

            logger.info("MongoDB transaction manager configured successfully for test environment. " +
                       "Connection URL: {}, Database: quiz_cms_test",
                       mongoContainer.getReplicaSetUrl());

            // Create indexes programmatically for test environment
            createTestIndexes(testMongoTemplate(databaseFactory));

            return transactionManager;

        } catch (Exception e) {
            logger.error("Failed to configure MongoDB transaction manager for test environment", e);
            throw new RuntimeException("Failed to configure test transaction manager", e);
        }
    }

    /**
     * Creates indexes programmatically for test environment.
     *
     * This ensures test environment has same index structure as production,
     * validating that queries and upsert operations behave correctly.
     */
    private void createTestIndexes(MongoTemplate mongoTemplate) {
        logger.info("Creating indexes for test environment");

        try {
            // Questions collection indexes
            // Ensure indexes via Mongo driver for compatibility with Mongo 8 and to support partial unique indexes

            // Create partial unique index using Mongo driver to avoid duplicates for docs without these fields
            var questionsCollection = mongoTemplate.getDb().getCollection("questions");
            org.bson.Document partialFilter = new org.bson.Document()
                .append("user_id", new org.bson.Document("$exists", true))
                .append("question_bank_id", new org.bson.Document("$exists", true))
                .append("source_question_id", new org.bson.Document("$exists", true));

            questionsCollection.createIndex(
                com.mongodb.client.model.Indexes.compoundIndex(
                    com.mongodb.client.model.Indexes.ascending("user_id"),
                    com.mongodb.client.model.Indexes.ascending("question_bank_id"),
                    com.mongodb.client.model.Indexes.ascending("source_question_id")
                ),
                new com.mongodb.client.model.IndexOptions()
                    .name("ux_user_bank_source_id")
                    .unique(true)
                    .partialFilterExpression(partialFilter)
            );

            // Performance index for listing
            questionsCollection.createIndex(
                com.mongodb.client.model.Indexes.compoundIndex(
                    com.mongodb.client.model.Indexes.ascending("user_id"),
                    com.mongodb.client.model.Indexes.ascending("question_bank_id"),
                    com.mongodb.client.model.Indexes.ascending("status")
                ),
                new com.mongodb.client.model.IndexOptions().name("ix_user_bank_status")
            );

            // Question taxonomy relationships indexes
            var relCollection = mongoTemplate.getDb().getCollection("question_taxonomy_relationships");

            // Unique relationship index (partial to ignore seed docs without these fields)
            org.bson.Document relPartialFilter = new org.bson.Document()
                .append("user_id", new org.bson.Document("$exists", true))
                .append("question_bank_id", new org.bson.Document("$exists", true))
                .append("question_id", new org.bson.Document("$exists", true))
                .append("taxonomy_type", new org.bson.Document("$exists", true))
                .append("taxonomy_id", new org.bson.Document("$exists", true));

            relCollection.createIndex(
                com.mongodb.client.model.Indexes.compoundIndex(
                    com.mongodb.client.model.Indexes.ascending("user_id"),
                    com.mongodb.client.model.Indexes.ascending("question_bank_id"),
                    com.mongodb.client.model.Indexes.ascending("question_id"),
                    com.mongodb.client.model.Indexes.ascending("taxonomy_type"),
                    com.mongodb.client.model.Indexes.ascending("taxonomy_id")
                ),
                new com.mongodb.client.model.IndexOptions()
                    .name("ux_user_bank_question_taxonomy")
                    .unique(true)
                    .partialFilterExpression(relPartialFilter)
            );

            // Query performance index
            relCollection.createIndex(
                com.mongodb.client.model.Indexes.compoundIndex(
                    com.mongodb.client.model.Indexes.ascending("user_id"),
                    com.mongodb.client.model.Indexes.ascending("question_bank_id"),
                    com.mongodb.client.model.Indexes.ascending("question_id")
                ),
                new com.mongodb.client.model.IndexOptions().name("ix_user_bank_question")
            );

            logger.info("Successfully created test indexes");
        } catch (Exception e) {
            logger.warn("Failed to create test indexes: {}", e.getMessage());
        }
    }

    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}