package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTaxonomyRelationshipAggregateTest {

    @Nested
    @DisplayName("Relationship Creation from Command")
    class RelationshipCreationFromCommand {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should create all relationships from complete command")
        @Description("Tests that createFromCommand creates relationships for all taxonomy elements when command has complete taxonomy data")
        void shouldCreateAllRelationshipsFromCompleteCommand() {
            // Given
            UpsertQuestionCommand command = createCompleteCommand();
            ObjectId questionId = new ObjectId();

            // When
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, command);

            // Then
            // 4 categories + 4 tags + 3 quizzes + 1 difficulty = 12 relationships
            assertEquals(12, relationships.size(), "Should create 12 relationships (4 categories + 4 tags + 3 quizzes + 1 difficulty)");

            // Verify category relationships
            assertTrue(hasRelationship(relationships, "category_level_1", "tech"), "Should have level 1 category relationship");
            assertTrue(hasRelationship(relationships, "category_level_2", "prog"), "Should have level 2 category relationship");
            assertTrue(hasRelationship(relationships, "category_level_3", "web_dev"), "Should have level 3 category relationship");
            assertTrue(hasRelationship(relationships, "category_level_4", "javascript"), "Should have level 4 category relationship");

            // Verify tag relationships
            assertTrue(hasRelationship(relationships, "tag", "js-arrays"), "Should have js-arrays tag relationship");
            assertTrue(hasRelationship(relationships, "tag", "array-methods"), "Should have array-methods tag relationship");
            assertTrue(hasRelationship(relationships, "tag", "methods"), "Should have methods tag relationship");
            assertTrue(hasRelationship(relationships, "tag", "beginner"), "Should have beginner tag relationship");

            // Verify quiz relationships
            assertTrue(hasRelationship(relationships, "quiz", "101"), "Should have quiz 101 relationship");
            assertTrue(hasRelationship(relationships, "quiz", "205"), "Should have quiz 205 relationship");
            assertTrue(hasRelationship(relationships, "quiz", "312"), "Should have quiz 312 relationship");

            // Verify difficulty relationship
            assertTrue(hasRelationship(relationships, "difficulty_level", "easy"), "Should have difficulty level relationship");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should create only non-null category relationships")
        @Description("Tests that createFromCommand only creates relationships for category levels that are not null")
        void shouldCreateOnlyNonNullCategoryRelationships() {
            // Given - Command with only level 1 and level 3 categories (skipping level 2)
            UpsertQuestionCommand command = createPartialCategoryCommand();
            ObjectId questionId = new ObjectId();

            // When
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, command);

            // Then
            assertEquals(3, relationships.size(), "Should create 3 relationships (2 categories + 1 difficulty)");
            assertTrue(hasRelationship(relationships, "category_level_1", "tech"), "Should have level 1 category");
            assertTrue(hasRelationship(relationships, "category_level_3", "web_dev"), "Should have level 3 category");
            assertFalse(hasRelationship(relationships, "category_level_2", "prog"), "Should not have level 2 category");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should handle empty taxonomy gracefully")
        @Description("Tests that createFromCommand returns empty list when command has null taxonomy")
        void shouldHandleEmptyTaxonomyGracefully() {
            // Given
            UpsertQuestionCommand command = createCommandWithNullTaxonomy();
            ObjectId questionId = new ObjectId();

            // When
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, command);

            // Then
            assertTrue(relationships.isEmpty(), "Should return empty list for null taxonomy");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should create multiple tag and quiz relationships")
        @Description("Tests that createFromCommand creates individual relationships for each tag and quiz")
        void shouldCreateMultipleTagAndQuizRelationships() {
            // Given
            UpsertQuestionCommand command = createCompleteCommand();
            ObjectId questionId = new ObjectId();

            // When
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, command);

            // Then
            long tagRelationships = relationships.stream()
                .filter(r -> "tag".equals(r.getTaxonomyType()))
                .count();
            long quizRelationships = relationships.stream()
                .filter(r -> "quiz".equals(r.getTaxonomyType()))
                .count();

            assertEquals(4, tagRelationships, "Should create 4 tag relationships");
            assertEquals(3, quizRelationships, "Should create 3 quiz relationships");
        }
    }

    @Nested
    @DisplayName("Relationship Type Validation")
    class RelationshipTypeValidation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate known relationship types")
        @Description("Tests that isValidRelationshipType returns true for all valid relationship types")
        void shouldValidateKnownRelationshipTypes() {
            // Given & When & Then
            assertTrue(createRelationshipWithType("category_level_1").isValidRelationshipType());
            assertTrue(createRelationshipWithType("category_level_2").isValidRelationshipType());
            assertTrue(createRelationshipWithType("category_level_3").isValidRelationshipType());
            assertTrue(createRelationshipWithType("category_level_4").isValidRelationshipType());
            assertTrue(createRelationshipWithType("tag").isValidRelationshipType());
            assertTrue(createRelationshipWithType("quiz").isValidRelationshipType());
            assertTrue(createRelationshipWithType("difficulty_level").isValidRelationshipType());
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject invalid relationship types")
        @Description("Tests that isValidRelationshipType returns false for unknown relationship types")
        void shouldRejectInvalidRelationshipTypes() {
            // Given & When & Then
            assertFalse(createRelationshipWithType("invalid_type").isValidRelationshipType());
            assertFalse(createRelationshipWithType("category_level_5").isValidRelationshipType());
            assertFalse(createRelationshipWithType("unknown").isValidRelationshipType());
        }
    }

    @Nested
    @DisplayName("Ownership Validation")
    class OwnershipValidation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate ownership correctly")
        @Description("Tests that belongsToUser, belongsToQuestionBank, and belongsToQuestion work correctly")
        void shouldValidateOwnershipCorrectly() {
            // Given
            Long userId = 12345L;
            Long questionBankId = 789L;
            ObjectId questionId = new ObjectId();

            QuestionTaxonomyRelationshipAggregate relationship = QuestionTaxonomyRelationshipAggregate.create(
                userId, questionBankId, questionId, "tag", "js-arrays"
            );

            // When & Then
            assertTrue(relationship.belongsToUser(userId), "Should belong to correct user");
            assertFalse(relationship.belongsToUser(99999L), "Should not belong to different user");

            assertTrue(relationship.belongsToQuestionBank(questionBankId), "Should belong to correct question bank");
            assertFalse(relationship.belongsToQuestionBank(99999L), "Should not belong to different question bank");

            assertTrue(relationship.belongsToQuestion(questionId), "Should belong to correct question");
            assertFalse(relationship.belongsToQuestion(new ObjectId()), "Should not belong to different question");
        }
    }

    // Helper methods
    private boolean hasRelationship(List<QuestionTaxonomyRelationshipAggregate> relationships,
                                   String taxonomyType, String taxonomyId) {
        return relationships.stream()
            .anyMatch(r -> taxonomyType.equals(r.getTaxonomyType()) &&
                          taxonomyId.equals(r.getTaxonomyId()));
    }

    private QuestionTaxonomyRelationshipAggregate createRelationshipWithType(String taxonomyType) {
        return QuestionTaxonomyRelationshipAggregate.create(
            12345L, 789L, new ObjectId(), taxonomyType, "test-id"
        );
    }

    private UpsertQuestionCommand createCompleteCommand() {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-question-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test Content");
        request.setTaxonomy(createCompleteTaxonomy());

        return new UpsertQuestionCommand(12345L, 789L, request);
    }

    private UpsertQuestionCommand createPartialCategoryCommand() {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-question-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test Content");
        request.setTaxonomy(createPartialCategoryTaxonomy());

        return new UpsertQuestionCommand(12345L, 789L, request);
    }

    private UpsertQuestionCommand createCommandWithNullTaxonomy() {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-question-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test Content");
        request.setTaxonomy(null);

        return new UpsertQuestionCommand(12345L, 789L, request);
    }

    private TaxonomyData createCompleteTaxonomy() {
        TaxonomyData taxonomy = new TaxonomyData();

        // Categories
        TaxonomyData.Categories categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "technology", null));
        categories.setLevel2(new TaxonomyData.Category("prog", "Programming", "programming", "tech"));
        categories.setLevel3(new TaxonomyData.Category("web_dev", "Web Development", "web-development", "prog"));
        categories.setLevel4(new TaxonomyData.Category("javascript", "JavaScript", "javascript", "web_dev"));
        taxonomy.setCategories(categories);

        // Tags
        taxonomy.setTags(Arrays.asList(
            new TaxonomyData.Tag("js-arrays", "JavaScript Arrays", "#f7df1e"),
            new TaxonomyData.Tag("array-methods", "Array Methods", "#61dafb"),
            new TaxonomyData.Tag("methods", "Methods", "#764abc"),
            new TaxonomyData.Tag("beginner", "Beginner", "#28a745")
        ));

        // Quizzes
        taxonomy.setQuizzes(Arrays.asList(
            new TaxonomyData.Quiz(101L, "JavaScript Fundamentals Quiz", "js-fundamentals"),
            new TaxonomyData.Quiz(205L, "Array Methods Mastery", "array-methods-mastery"),
            new TaxonomyData.Quiz(312L, "Frontend Developer Assessment", "frontend-dev-assessment")
        ));

        // Difficulty
        taxonomy.setDifficultyLevel(new TaxonomyData.DifficultyLevel("easy", 1, "Suitable for beginners"));

        return taxonomy;
    }

    private TaxonomyData createPartialCategoryTaxonomy() {
        TaxonomyData taxonomy = new TaxonomyData();

        // Only level 1 and level 3 categories (skipping level 2)
        TaxonomyData.Categories categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "technology", null));
        categories.setLevel3(new TaxonomyData.Category("web_dev", "Web Development", "web-development", "prog"));
        taxonomy.setCategories(categories);

        // Difficulty
        taxonomy.setDifficultyLevel(new TaxonomyData.DifficultyLevel("easy", 1, "Suitable for beginners"));

        return taxonomy;
    }
}