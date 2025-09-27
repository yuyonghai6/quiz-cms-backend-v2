package com.quizfun.questionbank.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for MongoDB transaction management.
 *
 * This configuration provides transaction managers for production and development environments.
 * Test-specific transaction management is configured separately in test sources.
 *
 * MongoDB transactions require a replica set or sharded cluster.
 */
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoTransactionConfig.class);

    /**
     * Creates a MongoDB transaction manager for production and development environments.
     * Uses the auto-configured MongoDatabaseFactory from Spring Boot.
     *
     * @param factory The auto-configured MongoDatabaseFactory
     * @return MongoTransactionManager configured for ACID transactions
     */
    @Bean
    @Profile("!test")
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        logger.info("Configuring MongoDB transaction manager for production/development environment");

        MongoTransactionManager transactionManager = new MongoTransactionManager(factory);

        logger.info("MongoDB transaction manager configured successfully for production/development");
        return transactionManager;
    }
}