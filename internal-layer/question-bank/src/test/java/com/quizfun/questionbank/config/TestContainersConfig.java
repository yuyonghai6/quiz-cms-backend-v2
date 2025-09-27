package com.quizfun.questionbank.config;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import org.bson.Document;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import java.time.Duration;
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
import org.springframework.context.annotation.Profile;
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

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class
})
@EnableTransactionManagement
@ComponentScan(
    basePackages = {
        "com.quizfun.questionbank.application",
        "com.quizfun.questionbank.domain",
        "com.quizfun.questionbank.infrastructure"
    },
    excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, value = com.quizfun.questionbank.infrastructure.configuration.MongoTransactionConfig.class)
    }
)
@Testcontainers
public class TestContainersConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainersConfig.class);

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

            return transactionManager;

        } catch (Exception e) {
            logger.error("Failed to configure MongoDB transaction manager for test environment", e);
            throw new RuntimeException("Failed to configure test transaction manager", e);
        }
    }

    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }

    

    @Bean
    @Primary
    public QuestionBanksPerUserRepository questionBanksPerUserRepositoryStub() {
        return new QuestionBanksPerUserRepository() {
            @Override
            public Result<Boolean> validateOwnership(Long userId, Long questionBankId) {
                return Result.success(true);
            }

            @Override
            public Result<Long> getDefaultQuestionBankId(Long userId) {
                return Result.success(1L);
            }

            @Override
            public Result<Boolean> isQuestionBankActive(Long userId, Long questionBankId) {
                return Result.success(true);
            }
        };
    }

    @Bean
    @Primary
    public TaxonomySetRepository taxonomySetRepositoryStub() {
        return new TaxonomySetRepository() {
            @Override
            public Result<Boolean> validateTaxonomyReferences(Long userId, Long questionBankId, java.util.List<String> taxonomyIds) {
                return Result.success(true);
            }

            @Override
            public Result<Boolean> validateTaxonomyReference(Long userId, Long questionBankId, String taxonomyId) {
                return Result.success(true);
            }

            @Override
            public Result<Boolean> validateCategoryReference(Long userId, Long questionBankId, String categoryId, String level) {
                return Result.success(true);
            }

            @Override
            public Result<Boolean> validateTagReference(Long userId, Long questionBankId, String tagId) {
                return Result.success(true);
            }

            @Override
            public Result<Boolean> validateQuizReference(Long userId, Long questionBankId, String quizId) {
                return Result.success(true);
            }

            @Override
            public Result<Boolean> validateDifficultyLevelReference(Long userId, Long questionBankId, String difficultyLevel) {
                return Result.success(true);
            }

            @Override
            public Result<java.util.List<String>> getInvalidTaxonomyReferences(Long userId, Long questionBankId, java.util.List<String> taxonomyIds) {
                return Result.success(java.util.List.of());
            }
        };
    }
}