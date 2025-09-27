package com.quizfun.questionbank.integration;

import com.quizfun.questionbank.config.BaseTestConfiguration;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseIntegrationTest")
public class BaseIntegrationTest extends BaseTestConfiguration {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("BaseIntegrationTest.Should provide working test infrastructure")
    @Description("Validates that the complete test infrastructure works end-to-end including TestContainers, MongoDB, test data loading, and cleanup functionality")
    void shouldProvideWorkingTestInfrastructure() {
        assertThat(mongoTemplate.getCollection("test")).isNotNull();

        testDataLoader.loadTestData();

        var questionBanks = mongoTemplate.findAll(Document.class, "question_banks_per_user");
        assertThat(questionBanks).isNotEmpty();

        testDataLoader.cleanupTestData();
        questionBanks = mongoTemplate.findAll(Document.class, "question_banks_per_user");
        assertThat(questionBanks).isEmpty();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("BaseIntegrationTest.Should validate test data loading functionality")
    @Description("Validates that all test data files are properly loaded and accessible through the test data loader with correct data structure and content")
    void shouldValidateTestDataLoadingFunctionality() {
        var questionBanks = mongoTemplate.findAll(Document.class, "question_banks_per_user");
        var taxonomySets = mongoTemplate.findAll(Document.class, "taxonomy_sets");
        var questions = mongoTemplate.findAll(Document.class, "questions");
        var relationships = mongoTemplate.findAll(Document.class, "question_taxonomy_relationships");

        assertThat(questionBanks).hasSizeGreaterThan(0);
        assertThat(taxonomySets).hasSizeGreaterThan(0);
        assertThat(questions).hasSizeGreaterThan(0);
        assertThat(relationships).hasSizeGreaterThan(0);

        var sampleBank = questionBanks.get(0);
        assertThat(sampleBank.getString("userId")).isNotNull();
        assertThat(sampleBank.getString("bankId")).isNotNull();
        assertThat(sampleBank.getString("bankName")).isNotNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("BaseIntegrationTest.Should ensure test isolation between methods")
    @Description("Validates that test methods are properly isolated with clean database state between test executions")
    void shouldEnsureTestIsolationBetweenMethods() {
        var testDoc = new Document("isolation_test", "method_specific_data");
        mongoTemplate.save(testDoc, "isolation_collection");

        var saved = mongoTemplate.findAll(Document.class, "isolation_collection");
        assertThat(saved).hasSize(1);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("BaseIntegrationTest.Should support complex integration scenarios")
    @Description("Validates that the infrastructure can support complex integration test scenarios with multiple data operations and cross-collection queries")
    void shouldSupportComplexIntegrationScenarios() {
        var questions = mongoTemplate.findAll(Document.class, "questions");
        var relationships = mongoTemplate.findAll(Document.class, "question_taxonomy_relationships");

        assertThat(questions).isNotEmpty();
        assertThat(relationships).isNotEmpty();

        var questionId = questions.get(0).getString("questionId");
        var relatedTaxonomies = relationships.stream()
                .filter(rel -> questionId.equals(rel.getString("questionId")))
                .toList();

        assertThat(relatedTaxonomies).isNotEmpty();
    }
}