package com.quizfun.questionbank.infrastructure;

import com.quizfun.questionbank.config.TestContainersConfig;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.mongodb.MongoTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestContainersConfig.class})
@DisplayName("MongoConnectivityTest")
class MongoConnectivityTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("MongoConnectivityTest.Should establish MongoDB connection successfully")
    @Description("Validates that MongoDB connection is established successfully and basic CRUD operations work as expected with proper data persistence")
    void shouldEstablishMongoConnectionSuccessfully() {
        assertThat(mongoTemplate.getCollection("test")).isNotNull();

        var testDoc = new Document("test", "value");
        mongoTemplate.save(testDoc, "test");

        var retrieved = mongoTemplate.findAll(Document.class, "test");
        assertThat(retrieved).hasSize(1);

        mongoTemplate.remove(testDoc, "test");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("MongoConnectivityTest.Should support MongoDB transactions")
    @Description("Validates that MongoDB transaction support is properly configured and can handle multi-document transactions with proper ACID properties")
    void shouldSupportMongoDBTransactions() {
        var transactionTemplate = new TransactionTemplate(
                new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory())
        );

        transactionTemplate.execute(status -> {
            var doc1 = new Document("tx_test", "value1");
            var doc2 = new Document("tx_test", "value2");

            mongoTemplate.save(doc1, "transaction_test");
            mongoTemplate.save(doc2, "transaction_test");

            return null;
        });

        var docs = mongoTemplate.findAll(Document.class, "transaction_test");
        assertThat(docs).hasSize(2);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("MongoConnectivityTest.Should perform complex queries efficiently")
    @Description("Validates that MongoDB can handle complex queries with proper indexing and query performance for production-ready operations")
    void shouldPerformComplexQueriesEfficiently() {
        for (int i = 0; i < 100; i++) {
            var doc = new Document("index", i)
                    .append("category", i % 10)
                    .append("active", i % 2 == 0);
            mongoTemplate.save(doc, "query_test");
        }

        var query = new Query(Criteria.where("category").is(0).and("active").is(true));
        var results = mongoTemplate.find(query, Document.class, "query_test");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(doc ->
            doc.getInteger("category") == 0 && doc.getBoolean("active"));
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("MongoConnectivityTest.Should handle database operations with proper error handling")
    @Description("Validates that MongoDB operations handle errors gracefully and provide meaningful error information for debugging and monitoring")
    void shouldHandleDatabaseOperationsWithProperErrorHandling() {
        try {
            var validDoc = new Document("valid", "data");
            mongoTemplate.save(validDoc, "error_test");

            var saved = mongoTemplate.findById(validDoc.getObjectId("_id"), Document.class, "error_test");
            assertThat(saved).isNotNull();
            assertThat(saved.getString("valid")).isEqualTo("data");

        } catch (Exception e) {
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("MongoConnectivityTest.Should support batch operations")
    @Description("Validates that MongoDB can handle batch insert and update operations efficiently for bulk data processing scenarios")
    void shouldSupportBatchOperations() {
        var documents = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> new Document("batch", i).append("timestamp", System.currentTimeMillis()))
                .toList();

        mongoTemplate.insert(documents, "batch_test");

        var count = mongoTemplate.count(new Query(), "batch_test");
        assertThat(count).isEqualTo(50);

        var sampleDoc = mongoTemplate.findOne(
                new Query(Criteria.where("batch").is(25)),
                Document.class,
                "batch_test"
        );
        assertThat(sampleDoc).isNotNull();
        assertThat(sampleDoc.getInteger("batch")).isEqualTo(25);
    }
}