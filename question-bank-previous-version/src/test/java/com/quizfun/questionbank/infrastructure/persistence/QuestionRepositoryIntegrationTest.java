package com.quizfun.questionbank.infrastructure.persistence;

import com.quizfun.questionbank.application.ports.out.QuestionRepository;
import com.quizfun.questionbank.config.TestContainersConfig;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.infrastructure.configuration.QuestionBankConfiguration;
import com.quizfun.questionbank.infrastructure.persistence.repositories.MongoQuestionRepository;
import com.quizfun.shared.common.Result;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestContainersConfig.class})
@Import(MongoQuestionRepository.class)
class QuestionRepositoryIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-005.repository-layer-implementation")
    @DisplayName("QuestionRepositoryIntegrationTest.Should upsert and find question by source ID")
    @Description("Verifies upsertBySourceQuestionId creates or updates and findBySourceQuestionId reads back the aggregate")
    void shouldUpsertAndFindQuestionBySourceId() {
        Long userId = 100L;
        Long bankId = 200L;
        String sourceId = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e7001"; // UUIDv7 format

        QuestionAggregate aggregate = QuestionAggregate.createNew(
            userId,
            bankId,
            sourceId,
            QuestionType.MCQ,
            "Sample Title",
            "<p>Sample Content</p>",
            5
        );

        Result<QuestionAggregate> upsertResult = questionRepository.upsertBySourceQuestionId(aggregate);
        assertThat(upsertResult.isSuccess()).isTrue();
        QuestionAggregate saved = upsertResult.getValue();
        assertThat(saved.getId()).isNotNull();

        var findResult = questionRepository.findBySourceQuestionId(userId, bankId, sourceId);
        assertThat(findResult.isSuccess()).isTrue();
        assertThat(findResult.getValue()).isPresent();
        assertThat(findResult.getValue().get().getSourceQuestionId()).isEqualTo(sourceId);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-005.repository-layer-implementation")
    @DisplayName("QuestionRepositoryIntegrationTest.Should upsert existing question scenario")
    @Description("Ensures upsert updates an existing question while preserving MongoDB _id")
    void shouldUpsertExistingQuestionScenario() {
        Long userId = 101L;
        Long bankId = 201L;
        String sourceId = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e7002";

        QuestionAggregate aggregate = QuestionAggregate.createNew(
            userId, bankId, sourceId, QuestionType.MCQ, "Title A", "<p>A</p>", 1
        );
        var first = questionRepository.upsertBySourceQuestionId(aggregate);
        assertThat(first.isSuccess()).isTrue();
        var saved1 = first.getValue();

        // update content and points; upsert again
        saved1.updateBasicContent("Title B", "<p>B</p>", 2);
        var second = questionRepository.upsertBySourceQuestionId(saved1);
        assertThat(second.isSuccess()).isTrue();
        var saved2 = second.getValue();

        assertThat(saved2.getId()).isEqualTo(saved1.getId());
        assertThat(saved2.getTitle()).isEqualTo("Title B");
        assertThat(saved2.getPoints()).isEqualTo(2);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-005.repository-layer-implementation")
    @DisplayName("QuestionRepositoryIntegrationTest.Should find by question bank")
    @Description("Verifies findByQuestionBank returns all questions scoped by (userId, questionBankId)")
    void shouldFindByQuestionBank() {
        Long userId = 102L;
        Long bankId = 202L;
        String sourceId1 = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e7003";
        String sourceId2 = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e7004";

        var agg1 = QuestionAggregate.createNew(userId, bankId, sourceId1, QuestionType.MCQ, "T1", "<p>C1</p>", 1);
        var agg2 = QuestionAggregate.createNew(userId, bankId, sourceId2, QuestionType.MCQ, "T2", "<p>C2</p>", 1);
        assertThat(questionRepository.upsertBySourceQuestionId(agg1).isSuccess()).isTrue();
        assertThat(questionRepository.upsertBySourceQuestionId(agg2).isSuccess()).isTrue();

        var listResult = questionRepository.findByQuestionBank(userId, bankId);
        assertThat(listResult.isSuccess()).isTrue();
        assertThat(listResult.getValue()).extracting(QuestionAggregate::getSourceQuestionId)
            .contains(sourceId1, sourceId2);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-005.repository-layer-implementation")
    @DisplayName("QuestionRepositoryIntegrationTest.Should delete question by id")
    @Description("Verifies delete removes the question and subsequent find returns empty")
    void shouldDeleteQuestionById() {
        Long userId = 103L;
        Long bankId = 203L;
        String sourceId = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e7005";

        var agg = QuestionAggregate.createNew(userId, bankId, sourceId, QuestionType.MCQ, "T", "<p>C</p>", 1);
        var upsert = questionRepository.upsertBySourceQuestionId(agg);
        assertThat(upsert.isSuccess()).isTrue();
        var saved = upsert.getValue();

        var deleteResult = questionRepository.delete(saved.getId());
        assertThat(deleteResult.isSuccess()).isTrue();

        var find = questionRepository.findBySourceQuestionId(userId, bankId, sourceId);
        assertThat(find.isSuccess()).isTrue();
        assertThat(find.getValue()).isEmpty();
    }
}


