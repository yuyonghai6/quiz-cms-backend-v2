package com.quizfun.questionbank.config;

import com.quizfun.questionbank.testutil.QuestionBankTestDataLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {TestContainersConfig.class})
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseTestConfiguration {

    protected QuestionBankTestDataLoader testDataLoader;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @BeforeEach
    void setupTestData() {
        testDataLoader = new QuestionBankTestDataLoader(mongoTemplate);
        testDataLoader.cleanupTestData();
        testDataLoader.loadTestData();
    }

    @AfterEach
    void cleanupTestData() {
        if (testDataLoader != null) {
            testDataLoader.cleanupTestData();
        }
    }
}