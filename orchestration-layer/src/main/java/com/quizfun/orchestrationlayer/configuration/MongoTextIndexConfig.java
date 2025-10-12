package com.quizfun.orchestrationlayer.configuration;

import com.mongodb.client.model.Indexes;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Ensures required MongoDB text index exists for full-text search queries
 * when running the application with the 'dev' profile (used by K6 tests).
 */
@Configuration
@Profile("dev")
public class MongoTextIndexConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoTextIndexConfig.class);

    @Bean
    ApplicationRunner ensureQuestionTextIndex(MongoTemplate mongoTemplate) {
        return args -> {
            String collection = mongoTemplate.getCollectionName(QuestionDocument.class);
            try {
                mongoTemplate.getDb().getCollection(collection).createIndex(Indexes.text("question_text"));
                log.info("Ensured text index on {}.question_text for dev profile", collection);
            } catch (Exception e) {
                log.warn("Could not ensure text index on {}.question_text: {}", collection, e.getMessage());
            }
        };
    }
}
