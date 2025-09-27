package com.quizfun.questionbank.config;

import com.mongodb.client.MongoClients;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootConfiguration
@EnableAutoConfiguration
public class TestContainersConfig {

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
    public MongoTemplate testMongoTemplate() {
        if (!mongoContainer.isRunning()) {
            mongoContainer.start();
        }
        return new MongoTemplate(
                MongoClients.create(mongoContainer.getReplicaSetUrl()),
                "quiz_cms_test"
        );
    }

    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}