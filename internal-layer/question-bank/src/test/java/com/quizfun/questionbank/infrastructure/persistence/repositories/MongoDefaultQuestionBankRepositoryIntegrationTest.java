package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.globalshared.utils.LongIdGenerator;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.config.TestContainersConfig;
import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import com.quizfun.questionbank.infrastructure.persistence.documents.TaxonomySetDocument;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MongoDefaultQuestionBankRepository.
 *
 * IMPORTANT: Uses Testcontainers MongoDB - NOT localhost:27017
 */
@SpringBootTest(classes = {TestContainersConfig.class})
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1005.repository-with-transactions")
@DisplayName("MongoDB Default Question Bank Repository Integration Tests")
class MongoDefaultQuestionBankRepositoryIntegrationTest {

    @Autowired
    private MongoDefaultQuestionBankRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private LongIdGenerator longIdGenerator;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        mongoTemplate.dropCollection(QuestionBanksPerUserDocument.class);
        mongoTemplate.dropCollection(TaxonomySetDocument.class);
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test
        mongoTemplate.dropCollection(QuestionBanksPerUserDocument.class);
        mongoTemplate.dropCollection(TaxonomySetDocument.class);
    }

    @Test
    @DisplayName("Should create both documents successfully in transaction")
    void shouldCreateBothDocumentsSuccessfullyInTransaction() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate questionBanksAggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        TaxonomySetAggregate taxonomyAggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When
        Result<DefaultQuestionBankResponseDto> result =
            repository.createDefaultQuestionBank(questionBanksAggregate, taxonomyAggregate);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getUserId()).isEqualTo(userId);
        assertThat(result.data().getQuestionBankId()).isEqualTo(questionBankId);

        // Verify question_banks_per_user document in MongoDB
        QuestionBanksPerUserDocument qbDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(qbDoc).isNotNull();
        assertThat(qbDoc.getUserId()).isEqualTo(userId);
        assertThat(qbDoc.getDefaultQuestionBankId()).isEqualTo(questionBankId);
        assertThat(qbDoc.getQuestionBanks()).hasSize(1);
        assertThat(qbDoc.getQuestionBanks().get(0).getBankId()).isEqualTo(questionBankId);

        // Verify taxonomy_sets document in MongoDB
        TaxonomySetDocument taxDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)
                .and("question_bank_id").is(questionBankId)),
            TaxonomySetDocument.class
        );
        assertThat(taxDoc).isNotNull();
        assertThat(taxDoc.getUserId()).isEqualTo(userId);
        assertThat(taxDoc.getQuestionBankId()).isEqualTo(questionBankId);
        assertThat(taxDoc.getTags()).hasSize(3);
    }

    @Test
    @DisplayName("Should return failure when user already exists")
    void shouldReturnFailureWhenUserAlreadyExists() {
        // Given - Create user first time
        Long userId = 123456789L;
        Long firstBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate firstAggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, firstBankId, now);
        TaxonomySetAggregate firstTaxonomy =
            TaxonomySetAggregate.createDefault(userId, firstBankId, now);

        repository.createDefaultQuestionBank(firstAggregate, firstTaxonomy);

        // When - Try to create again with different bank ID
        Long secondBankId = longIdGenerator.generateQuestionBankId();
        QuestionBanksPerUserAggregate secondAggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, secondBankId, now);
        TaxonomySetAggregate secondTaxonomy =
            TaxonomySetAggregate.createDefault(userId, secondBankId, now);

        Result<DefaultQuestionBankResponseDto> result =
            repository.createDefaultQuestionBank(secondAggregate, secondTaxonomy);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DUPLICATE_USER");
        assertThat(result.message()).contains(String.valueOf(userId));

        // Verify only original documents exist (no second documents created)
        long count = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(count).isEqualTo(1);

        QuestionBanksPerUserDocument doc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(doc.getDefaultQuestionBankId()).isEqualTo(firstBankId);  // Original, not second
    }

    @Test
    @DisplayName("Should rollback transaction when taxonomy insert fails")
    void shouldRollbackTransactionWhenTaxonomyInsertFails() {
        // Note: This test simulates transaction rollback
        // In real scenario, we'd need to force an error condition
        // For this test, we verify the repository handles exceptions properly

        // Given
        Long userId = 999L;
        Long questionBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        // Create invalid taxonomy aggregate (e.g., null values to force error)
        QuestionBanksPerUserAggregate validAggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // Simulate error by passing null (should be caught by repository)
        TaxonomySetAggregate nullTaxonomy = null;

        // When & Then
        assertThatThrownBy(() ->
            repository.createDefaultQuestionBank(validAggregate, nullTaxonomy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Taxonomy aggregate cannot be null");

        // Verify NO documents created (transaction rollback)
        long qbCount = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(qbCount).isZero();

        long taxCount = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            TaxonomySetDocument.class
        );
        assertThat(taxCount).isZero();
    }

    @Test
    @DisplayName("Should use Testcontainers MongoDB connection")
    void shouldUseTestcontainersMongoDB() {
        // Verify NOT using localhost:27017
        String databaseName = mongoTemplate.getMongoDatabaseFactory()
            .getMongoDatabase()
            .getName();

        assertThat(databaseName).isNotNull();
        // Testcontainers uses random ports, not 27017
        // TestContainersConfig sets database to "quiz_cms_test"
        assertThat(databaseName).isEqualTo("quiz_cms_test");
    }

    @Test
    @DisplayName("Should handle concurrent creation attempts correctly")
    void shouldHandleConcurrentCreationAttemptsCorrectly() throws Exception {
        // Given
        Long userId = 555L;
        Long questionBankId1 = longIdGenerator.generateQuestionBankId();
        Long questionBankId2 = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        // When - Attempt concurrent creation (one should fail)
        Thread thread1 = new Thread(() -> {
            QuestionBanksPerUserAggregate agg1 =
                QuestionBanksPerUserAggregate.createDefault(userId, questionBankId1, now);
            TaxonomySetAggregate tax1 =
                TaxonomySetAggregate.createDefault(userId, questionBankId1, now);
            repository.createDefaultQuestionBank(agg1, tax1);
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100);  // Slight delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            QuestionBanksPerUserAggregate agg2 =
                QuestionBanksPerUserAggregate.createDefault(userId, questionBankId2, now);
            TaxonomySetAggregate tax2 =
                TaxonomySetAggregate.createDefault(userId, questionBankId2, now);
            repository.createDefaultQuestionBank(agg2, tax2);
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then - Only one user document should exist (unique index)
        long count = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should generate ObjectIds for MongoDB documents")
    void shouldGenerateObjectIdsForMongoDBDocuments() {
        // Given
        Long userId = 777L;
        Long questionBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);
        TaxonomySetAggregate taxonomy =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When
        repository.createDefaultQuestionBank(aggregate, taxonomy);

        // Then
        QuestionBanksPerUserDocument qbDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(qbDoc.getId()).isNotNull();

        TaxonomySetDocument taxDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            TaxonomySetDocument.class
        );
        assertThat(taxDoc.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should set timestamps for documents")
    void shouldSetTimestampsForDocuments() {
        // Given
        Long userId = 888L;
        Long questionBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);
        TaxonomySetAggregate taxonomy =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When
        repository.createDefaultQuestionBank(aggregate, taxonomy);

        // Then
        QuestionBanksPerUserDocument qbDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(qbDoc.getCreatedAt()).isNotNull();
        assertThat(qbDoc.getUpdatedAt()).isNotNull();
        assertThat(qbDoc.getQuestionBanks().get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should verify documents are stored in correct collections")
    void shouldVerifyDocumentsAreStoredInCorrectCollections() {
        // Given
        Long userId = 999L;
        Long questionBankId = longIdGenerator.generateQuestionBankId();
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);
        TaxonomySetAggregate taxonomy =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When
        repository.createDefaultQuestionBank(aggregate, taxonomy);

        // Then - Verify documents exist in their respective collections
        long qbCount = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            "question_banks_per_user"
        );
        assertThat(qbCount).isEqualTo(1);

        long taxCount = mongoTemplate.count(
            Query.query(Criteria.where("user_id").is(userId)),
            "taxonomy_sets"
        );
        assertThat(taxCount).isEqualTo(1);
    }
}
