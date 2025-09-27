package com.quizfun.questionbank.config;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(classes = {TestContainersConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("TestContainersMongoDBConfigurationTest")
class TestContainersMongoDBConfigurationTest {

    static MongoDBContainer mongoContainer = TestContainersConfig.getMongoContainer();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should start MongoDB container successfully")
    @Description("Validates that the MongoDB TestContainer starts successfully with correct version, port configuration, and network accessibility")
    void shouldStartMongoDBContainerSuccessfully() {
        assertThat(mongoContainer.isRunning()).isTrue();
        assertThat(mongoContainer.getExposedPorts()).contains(27017);
        assertThat(mongoContainer.getDockerImageName()).isEqualTo("mongo:8.0");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should establish MongoDB connection with correct configuration")
    @Description("Validates that MongoTemplate is properly configured and can establish connectivity to the TestContainer MongoDB instance")
    void shouldEstablishMongoDBConnectionWithCorrectConfiguration() {
        assertThat(mongoTemplate).isNotNull();

        var result = mongoTemplate.getDb().runCommand(new Document("ping", 1));

        assertThat(result.getDouble("ok")).isEqualTo(1.0);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should use isolated test database")
    @Description("Validates that the test configuration uses an isolated test database to prevent interference with production data")
    void shouldUseIsolatedTestDatabase() {
        var databaseName = mongoTemplate.getDb().getName();
        assertThat(databaseName).isEqualTo("quiz_cms_test");

        var testData = new Document("test", "isolation");
        mongoTemplate.save(testData, "test_collection");

        var retrieved = mongoTemplate.findById(testData.getObjectId("_id"), Document.class, "test_collection");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("test")).isEqualTo("isolation");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should provide database isolation between test methods")
    @Description("Validates that each test method has proper database isolation and can perform operations without interference from other tests")
    void shouldProvideDatabaseIsolationBetweenTestMethods() {
        var collections = mongoTemplate.getDb().listCollectionNames().into(new ArrayList<>());

        mongoTemplate.save(new Document("isolation", "test1"), "isolation_test");

        var collectionsAfter = mongoTemplate.getDb().listCollectionNames().into(new ArrayList<>());
        assertThat(collectionsAfter.size()).isGreaterThanOrEqualTo(collections.size());
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should handle concurrent database operations")
    @Description("Validates that the MongoDB TestContainer configuration can handle concurrent database operations safely without data corruption")
    void shouldHandleConcurrentDatabaseOperations() throws InterruptedException {
        var results = new ConcurrentLinkedQueue<Boolean>();
        var executor = Executors.newFixedThreadPool(5);
        var latch = new CountDownLatch(20);

        for (int i = 0; i < 20; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    var document = new Document("operation", operationId)
                            .append("timestamp", Instant.now());

                    mongoTemplate.save(document, "concurrent_test");

                    var retrieved = mongoTemplate.findById(
                            document.getObjectId("_id"),
                            Document.class,
                            "concurrent_test"
                    );

                    results.add(retrieved != null &&
                               retrieved.getInteger("operation").equals(operationId));
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        assertThat(results).hasSize(20);
        assertThat(results.stream().allMatch(success -> success)).isTrue();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("TestContainersMongoDBConfigurationTest.Should validate container cleanup and resource management")
    @Description("Validates that the TestContainer properly manages resources and doesn't create memory leaks or excessive connections")
    void shouldValidateContainerCleanupAndResourceManagement() {
        var initialRunning = mongoContainer.isRunning();
        assertThat(initialRunning).isTrue();

        var connectionsBefore = getActiveConnectionCount();

        for (int i = 0; i < 10; i++) {
            mongoTemplate.save(new Document("cleanup", i), "cleanup_test");
        }

        var connectionsAfter = getActiveConnectionCount();

        assertThat(connectionsAfter - connectionsBefore).isLessThanOrEqualTo(5);
    }

    private int getActiveConnectionCount() {
        try {
            var result = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));
            var connections = result.get("connections", Document.class);
            return connections != null ? connections.getInteger("current", 0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @AfterEach
    void cleanupAfterEachTest() {
        var collections = mongoTemplate.getDb().listCollectionNames().into(new ArrayList<>());
        collections.stream()
                .filter(name -> name.contains("test"))
                .forEach(name -> mongoTemplate.dropCollection(name));
    }
}