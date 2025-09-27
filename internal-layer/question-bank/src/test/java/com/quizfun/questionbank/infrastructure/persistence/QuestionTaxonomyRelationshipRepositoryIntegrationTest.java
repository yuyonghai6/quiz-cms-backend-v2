package com.quizfun.questionbank.infrastructure.persistence;

import com.quizfun.questionbank.application.ports.out.QuestionTaxonomyRelationshipRepository;
import com.quizfun.questionbank.config.TestContainersConfig;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.infrastructure.configuration.QuestionBankConfiguration;
import com.quizfun.questionbank.infrastructure.persistence.repositories.MongoQuestionRepository;
import com.quizfun.questionbank.infrastructure.persistence.repositories.MongoQuestionTaxonomyRelationshipRepository;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestContainersConfig.class})
@Import({MongoQuestionRepository.class, MongoQuestionTaxonomyRelationshipRepository.class})
class QuestionTaxonomyRelationshipRepositoryIntegrationTest {

    @Autowired
    private MongoQuestionRepository questionRepository;

    @Autowired
    private QuestionTaxonomyRelationshipRepository relationshipRepository;

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-005.repository-layer-implementation")
    @DisplayName("QuestionTaxonomyRelationshipRepositoryIntegrationTest.Should replace and find relationships")
    @Description("Verifies delete+insert replacement and find operations for taxonomy relationships")
    void shouldReplaceAndFindRelationships() {
        Long userId = 10L;
        Long bankId = 20L;
        String sourceId = "018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5b";

        var agg = QuestionAggregate.createNew(userId, bankId, sourceId, QuestionType.MCQ, "T", "C", 3);
        var upsert = questionRepository.upsertBySourceQuestionId(agg);
        assertThat(upsert.isSuccess()).isTrue();
        var saved = upsert.getValue();

        var rel1 = QuestionTaxonomyRelationshipAggregate.create(userId, bankId, saved.getId(), "category_level_1", "tech");
        var rel2 = QuestionTaxonomyRelationshipAggregate.create(userId, bankId, saved.getId(), "tag", "js-arrays");

        var replaceResult = relationshipRepository.replaceRelationshipsForQuestion(saved.getId(), java.util.List.of(rel1, rel2));
        assertThat(replaceResult.isSuccess()).isTrue();

        var findResult = relationshipRepository.findByQuestionId(saved.getId());
        assertThat(findResult.isSuccess()).isTrue();
        assertThat(findResult.getValue()).hasSize(2);

        var deleteResult = relationshipRepository.deleteByQuestionId(saved.getId());
        assertThat(deleteResult.isSuccess()).isTrue();

        var findAfterDelete = relationshipRepository.findByQuestionId(saved.getId());
        assertThat(findAfterDelete.isSuccess()).isTrue();
        assertThat(findAfterDelete.getValue()).isEmpty();
    }
}


