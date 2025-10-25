package com.quizfun.questionbank.application.services;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.config.BaseTestConfiguration;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.shared.common.Result;
import io.qameta.allure.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Temporarily disabled due to MongoDB transaction WriteConflict with security_events collection. Requires replica set configuration or async security logging to be disabled in tests.")
@DisplayName("QuestionApplicationServiceIntegrationTest")
public class QuestionApplicationServiceIntegrationTest extends BaseTestConfiguration {

    @Autowired
    private QuestionApplicationService questionApplicationService;

    @BeforeEach
    void setUpAuthentication() {
        // Set up JWT authentication context for security validation
        var jwt = Jwt.withTokenValue("integration-test-token")
                .header("alg", "HS256")
                .claim("sub", "12345")  // Matches the user ID in createValidUpsertCommand
                .claim("iat", Instant.now())
                .claim("exp", Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Nested
    @DisplayName("Complete Business Workflow Integration Tests")
    class CompleteBusinessWorkflowIntegrationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should execute complete happy path workflow with real MongoDB transactions")
        @Description("Verifies the complete end-to-end workflow including validation, strategy processing, question persistence, and relationship management with real MongoDB transactions")
        void shouldExecuteCompleteHappyPathWorkflowWithRealMongoDbTransactions() {
            // Given: A user owns a question bank
            Document ownership = new Document("user_id", 12345L)
                .append("question_banks", List.of(
                    new Document("bank_id", 789L)
                        .append("name", "Integration Test Bank")
                        .append("is_active", true)
                        .append("is_default", false)
                ));
            mongoTemplate.insert(ownership, "question_banks_per_user");

            // And: The taxonomy set for the question bank exists
            Document taxonomySet = new Document("user_id", 12345L)
                .append("question_bank_id", 789L)
                .append("categories", new Document()
                    .append("level_1", new Document("id", "tech").append("name", "Technology"))
                    .append("level_2", new Document("id", "prog").append("name", "Programming"))
                    .append("level_3", new Document("id", "web_dev").append("name", "Web Development"))
                    .append("level_4", new Document("id", "spring").append("name", "Spring Framework")))
                .append("tags", List.of(
                    new Document("id", "spring-ioc").append("name", "Spring IoC"),
                    new Document("id", "dependency-injection").append("name", "Dependency Injection")
                ))
                .append("quizzes", List.of(
                    new Document("quiz_id", 201L).append("quiz_name", "Spring Framework Fundamentals")
                ))
                .append("available_difficulty_levels", List.of(
                    new Document("level", "medium").append("numeric_value", 2)
                ));
            mongoTemplate.insert(taxonomySet, "taxonomy_sets");
            
            // And: A valid command with complete taxonomy data
            UpsertQuestionCommand command = createValidUpsertCommand();

            // When: Executing complete upsert workflow
            Result<QuestionResponseDto> result = questionApplicationService.upsertQuestion(command);

            // Then: Should complete successfully with all data persisted
            assertThat(result.isSuccess()).isTrue();

            QuestionResponseDto response = result.getValue();
            assertThat(response.getQuestionId()).isNotNull();
            assertThat(response.getSourceQuestionId()).isEqualTo(command.getSourceQuestionId());
            assertThat(response.getOperation()).isEqualTo("created");
            assertThat(response.getTaxonomyRelationshipsCount()).isGreaterThan(0);

            // Verify data consistency across collections
            verifyQuestionPersistedInMongoDB(response.getQuestionId(), command);
            verifyTaxonomyRelationshipsPersistedInMongoDB(response.getQuestionId(), response.getTaxonomyRelationshipsCount());
        }

        // TODO(human): Please add the specific integration test scenarios you think would be most important here.
        // Consider scenarios like:
        // - Transaction rollback scenarios when repository operations fail
        // - Complete workflow with different question types (MCQ, Essay, True/False)
        // - Validation chain integration with real validation failures
        // - Strategy pattern integration with real strategy processing
        // - Performance testing under concurrent load
        // - Data consistency verification across multiple collections
        // - Update scenarios (upsert with existing source_question_id)
        // - Complex taxonomy scenarios with multiple levels and types
        // - Error scenarios that test transaction boundaries
        // - MongoDB replica set transaction behavior verification

        private UpsertQuestionCommand createValidUpsertCommand() {
            UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
            request.setSourceQuestionId("01234567-89ab-7def-0123-456789abcdef"); // Valid UUID v7 format
            request.setQuestionType("mcq");
            request.setTitle("Integration Test Question");
            request.setContent("<p>Which framework is used for dependency injection in Spring?</p>");
            request.setPoints(10);
            request.setSolutionExplanation("<p>Spring Framework uses IoC (Inversion of Control) container for dependency injection.</p>");
            request.setStatus("draft");
            request.setDisplayOrder(1);

            // Create MCQ options
            McqOption option1 = new McqOption("A", "Spring IoC Container", true, 10.0);
            McqOption option2 = new McqOption("B", "Hibernate", false, 0.0);

            // Create MCQ data with immutable constructor
            McqData mcqData = new McqData(
                List.of(option1, option2),
                false, // shuffleOptions
                false, // allowMultipleCorrect
                false, // allowPartialCredit
                60     // timeLimitSeconds
            );
            request.setMcqData(mcqData);

            // Create comprehensive taxonomy data
            TaxonomyData taxonomy = new TaxonomyData();

            // Categories
            TaxonomyData.Categories categories = new TaxonomyData.Categories();
            categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "technology", null));
            categories.setLevel2(new TaxonomyData.Category("prog", "Programming", "programming", "tech"));
            categories.setLevel3(new TaxonomyData.Category("web_dev", "Web Development", "web-development", "prog"));
            categories.setLevel4(new TaxonomyData.Category("spring", "Spring Framework", "spring-framework", "web_dev"));
            taxonomy.setCategories(categories);

            // Tags
            TaxonomyData.Tag tag1 = new TaxonomyData.Tag();
            tag1.setId("spring-ioc");
            tag1.setName("Spring IoC");
            tag1.setColor("#6db33f");

            TaxonomyData.Tag tag2 = new TaxonomyData.Tag();
            tag2.setId("dependency-injection");
            tag2.setName("Dependency Injection");
            tag2.setColor("#007396");

            taxonomy.setTags(List.of(tag1, tag2));

            // Quizzes
            TaxonomyData.Quiz quiz = new TaxonomyData.Quiz();
            quiz.setQuizId(201L);
            quiz.setQuizName("Spring Framework Fundamentals");
            quiz.setQuizSlug("spring-fundamentals");
            taxonomy.setQuizzes(List.of(quiz));

            // Difficulty level
            TaxonomyData.DifficultyLevel difficulty = new TaxonomyData.DifficultyLevel();
            difficulty.setLevel("medium");
            difficulty.setNumericValue(2);
            difficulty.setDescription("Intermediate knowledge required");
            taxonomy.setDifficultyLevel(difficulty);

            request.setTaxonomy(taxonomy);

            // Use test data user and question bank IDs
            return new UpsertQuestionCommand(12345L, 789L, request);
        }

        private void verifyQuestionPersistedInMongoDB(String questionId, UpsertQuestionCommand command) {
            var questions = mongoTemplate.findAll(Document.class, "questions");
            var questionOpt = questions.stream()
                .filter(q -> {
                    Object idVal = q.get("_id");
                    String idStr = (idVal instanceof ObjectId) ? ((ObjectId) idVal).toString() : String.valueOf(idVal);
                    return questionId.equals(idStr);
                })
                .findFirst();

            assertThat(questionOpt).isPresent();
            Document question = questionOpt.get();
            assertThat(question.getLong("user_id")).isEqualTo(command.getUserId());
            assertThat(question.getLong("question_bank_id")).isEqualTo(command.getQuestionBankId());
            assertThat(question.getString("source_question_id")).isEqualTo(command.getSourceQuestionId());
            assertThat(question.getString("question_type")).isEqualTo(command.getQuestionType().toString().toLowerCase());
            assertThat(question.getString("title")).isEqualTo(command.getTitle());
            assertThat(question.getString("content")).isEqualTo(command.getContent());
        }

        private void verifyTaxonomyRelationshipsPersistedInMongoDB(String questionId, int expectedCount) {
            var relationships = mongoTemplate.findAll(Document.class, "question_taxonomy_relationships");
            var questionRelationships = relationships.stream()
                .filter(r -> {
                    Object qid = r.get("question_id");
                    String idStr = (qid instanceof ObjectId) ? ((ObjectId) qid).toString() : String.valueOf(qid);
                    return questionId.equals(idStr);
                })
                .toList();

            assertThat(questionRelationships).hasSize(expectedCount);

            // Verify relationship types
            var relationshipTypes = questionRelationships.stream()
                .map(r -> r.getString("taxonomy_type"))
                .toList();

            assertThat(relationshipTypes).contains(
                "category_level_1", "category_level_2", "category_level_3", "category_level_4",
                "tag", "quiz", "difficulty_level"
            );
        }
    }
}