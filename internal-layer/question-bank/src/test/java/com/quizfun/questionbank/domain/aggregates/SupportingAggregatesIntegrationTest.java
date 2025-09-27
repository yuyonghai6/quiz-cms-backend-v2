package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.*;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupportingAggregatesIntegrationTest {

    @Nested
    @DisplayName("Cross-Aggregate Validation Workflows")
    class CrossAggregateValidationWorkflows {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate complete taxonomy workflow with all aggregates")
        @Description("Tests integration between TaxonomySetAggregate, QuestionTaxonomyRelationshipAggregate, and QuestionBanksPerUserAggregate")
        void shouldValidateCompleteTaxonomyWorkflowWithAllAggregates() {
            // Given - Set up all supporting aggregates
            Long userId = 12345L;
            Long questionBankId = 789L;
            ObjectId questionId = new ObjectId();

            // Create user's question banks
            QuestionBanksPerUserAggregate userBanks = createUserBanksAggregate(userId, questionBankId);

            // Create taxonomy set for validation
            TaxonomySetAggregate taxonomySet = createTaxonomySetAggregate(userId, questionBankId);

            // Create command with complete taxonomy data
            UpsertQuestionCommand command = createCompleteCommand(userId, questionBankId);

            // When - Validate ownership
            boolean ownershipValid = userBanks.validateOwnership(userId, questionBankId);

            // Then - User should own the question bank
            assertTrue(ownershipValid, "User should own the question bank");

            // When - Validate taxonomy references
            List<String> taxonomyIds = command.extractTaxonomyIds();
            boolean taxonomyValid = taxonomySet.validateTaxonomyReferences(taxonomyIds);

            // Then - All taxonomy references should be valid
            assertTrue(taxonomyValid, "All taxonomy references should be valid");

            // When - Create relationships from command
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, command);

            // Then - Should create correct number of relationships
            assertEquals(12, relationships.size(), "Should create 12 relationships for complete taxonomy");

            // And all relationships should belong to correct user and question bank
            assertTrue(relationships.stream().allMatch(r -> r.belongsToUser(userId)),
                "All relationships should belong to user");
            assertTrue(relationships.stream().allMatch(r -> r.belongsToQuestionBank(questionBankId)),
                "All relationships should belong to question bank");
            assertTrue(relationships.stream().allMatch(r -> r.belongsToQuestion(questionId)),
                "All relationships should belong to question");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should handle validation failures gracefully")
        @Description("Tests that validation failures are properly detected across aggregates")
        void shouldHandleValidationFailuresGracefully() {
            // Given - Different user IDs to simulate unauthorized access
            Long authorizedUserId = 12345L;
            Long unauthorizedUserId = 99999L;
            Long questionBankId = 789L;

            QuestionBanksPerUserAggregate userBanks = createUserBanksAggregate(authorizedUserId, questionBankId);
            TaxonomySetAggregate taxonomySet = createTaxonomySetAggregate(authorizedUserId, questionBankId);

            // When - Try to validate ownership with wrong user
            boolean ownershipValid = userBanks.validateOwnership(unauthorizedUserId, questionBankId);

            // Then - Should reject unauthorized user
            assertFalse(ownershipValid, "Should reject unauthorized user");

            // When - Try to validate invalid taxonomy references
            List<String> invalidTaxonomyIds = Arrays.asList("tech", "invalid-ref", "js-arrays");
            boolean taxonomyValid = taxonomySet.validateTaxonomyReferences(invalidTaxonomyIds);
            List<String> invalidIds = taxonomySet.findInvalidTaxonomyReferences(invalidTaxonomyIds);

            // Then - Should identify invalid references
            assertFalse(taxonomyValid, "Should reject invalid taxonomy references");
            assertEquals(1, invalidIds.size(), "Should identify 1 invalid ID");
            assertTrue(invalidIds.contains("invalid-ref"), "Should identify 'invalid-ref' as invalid");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should handle partial taxonomy data correctly")
        @Description("Tests that aggregates handle partial taxonomy data without errors")
        void shouldHandlePartialTaxonomyDataCorrectly() {
            // Given
            Long userId = 12345L;
            Long questionBankId = 789L;
            ObjectId questionId = new ObjectId();

            UpsertQuestionCommand partialCommand = createPartialCommand(userId, questionBankId);
            TaxonomySetAggregate taxonomySet = createTaxonomySetAggregate(userId, questionBankId);

            // When - Validate partial taxonomy
            List<String> taxonomyIds = partialCommand.extractTaxonomyIds();
            boolean taxonomyValid = taxonomySet.validateTaxonomyReferences(taxonomyIds);

            // Then - Should validate successfully
            assertTrue(taxonomyValid, "Partial taxonomy should validate successfully");

            // When - Create relationships from partial command
            List<QuestionTaxonomyRelationshipAggregate> relationships =
                QuestionTaxonomyRelationshipAggregate.createFromCommand(questionId, partialCommand);

            // Then - Should create fewer relationships
            assertEquals(3, relationships.size(), "Should create 3 relationships for partial taxonomy");

            // And all relationships should be valid types
            assertTrue(relationships.stream().allMatch(QuestionTaxonomyRelationshipAggregate::isValidRelationshipType),
                "All relationship types should be valid");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate category hierarchy rules")
        @Description("Tests that category hierarchy validation works correctly with invalid hierarchies")
        void shouldValidateCategoryHierarchyRules() {
            // Given - Create taxonomy set with invalid hierarchy
            Long userId = 12345L;
            Long questionBankId = 789L;

            CategoryLevels invalidCategories = new CategoryLevels(
                new CategoryLevels.Category("tech", "Technology", "technology", null),
                null, // Missing Level 2
                new CategoryLevels.Category("web_dev", "Web Development", "web-development", "prog"), // Level 3 without Level 2
                null
            );

            TaxonomySetAggregate invalidTaxonomySet = TaxonomySetAggregate.create(
                new ObjectId(),
                userId,
                questionBankId,
                invalidCategories,
                Arrays.asList(new Tag("js-arrays", "JavaScript Arrays", "#f7df1e")),
                Arrays.asList(new Quiz(101L, "JavaScript Fundamentals", "js-fundamentals")),
                new DifficultyLevel("easy", 1, "Suitable for beginners"),
                Arrays.asList(new DifficultyLevel("easy", 1, "Suitable for beginners"))
            );

            // When - Validate hierarchy
            boolean hierarchyValid = invalidTaxonomySet.validateCategoryHierarchy();

            // Then - Should reject invalid hierarchy
            assertFalse(hierarchyValid, "Invalid category hierarchy should be rejected");

            // When - Create valid hierarchy
            CategoryLevels validCategories = new CategoryLevels(
                new CategoryLevels.Category("tech", "Technology", "technology", null),
                new CategoryLevels.Category("prog", "Programming", "programming", "tech"),
                new CategoryLevels.Category("web_dev", "Web Development", "web-development", "prog"),
                null // Level 4 is optional
            );

            TaxonomySetAggregate validTaxonomySet = TaxonomySetAggregate.create(
                new ObjectId(),
                userId,
                questionBankId,
                validCategories,
                Arrays.asList(new Tag("js-arrays", "JavaScript Arrays", "#f7df1e")),
                Arrays.asList(new Quiz(101L, "JavaScript Fundamentals", "js-fundamentals")),
                new DifficultyLevel("easy", 1, "Suitable for beginners"),
                Arrays.asList(new DifficultyLevel("easy", 1, "Suitable for beginners"))
            );

            // Then - Valid hierarchy should pass
            assertTrue(validTaxonomySet.validateCategoryHierarchy(), "Valid category hierarchy should be accepted");
        }
    }

    @Nested
    @DisplayName("Value Object Integration Testing")
    class ValueObjectIntegrationTesting {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should handle all value object types correctly")
        @Description("Tests that all value objects work correctly within aggregates")
        void shouldHandleAllValueObjectTypesCorrectly() {
            // Given - Create comprehensive value objects
            CategoryLevels.Category category1 = new CategoryLevels.Category("tech", "Technology", "technology", null);
            CategoryLevels.Category category2 = new CategoryLevels.Category("prog", "Programming", "programming", "tech");
            CategoryLevels categories = new CategoryLevels(category1, category2, null, null);

            Tag tag1 = new Tag("js-arrays", "JavaScript Arrays", "#f7df1e");
            Tag tag2 = new Tag("array-methods", "Array Methods", "#61dafb");

            Quiz quiz1 = new Quiz(101L, "JavaScript Fundamentals", "js-fundamentals");
            Quiz quiz2 = new Quiz(205L, "Array Methods Mastery", "array-methods-mastery");

            DifficultyLevel difficulty = new DifficultyLevel("easy", 1, "Suitable for beginners");

            Instant now = Instant.now();
            QuestionBank bank1 = new QuestionBank(789L, "JavaScript Fundamentals", "Core concepts", true, now, now);
            QuestionBank bank2 = new QuestionBank(790L, "Advanced React", "Complex patterns", true, now, now);

            // When - Use in aggregates
            TaxonomySetAggregate taxonomySet = TaxonomySetAggregate.create(
                new ObjectId(),
                12345L,
                789L,
                categories,
                Arrays.asList(tag1, tag2),
                Arrays.asList(quiz1, quiz2),
                difficulty,
                Arrays.asList(difficulty)
            );

            QuestionBanksPerUserAggregate userBanks = QuestionBanksPerUserAggregate.create(
                new ObjectId(),
                12345L,
                789L,
                Arrays.asList(bank1, bank2)
            );

            // Then - All value objects should function correctly
            assertTrue(categories.isValidHierarchy(), "Categories should have valid hierarchy");
            assertEquals("tech", category1.getId(), "Category ID should be correct");
            assertEquals("js-arrays", tag1.getId(), "Tag ID should be correct");
            assertEquals(101L, quiz1.getQuizId(), "Quiz ID should be correct");
            assertEquals("easy", difficulty.getLevel(), "Difficulty level should be correct");
            assertTrue(bank1.isActive(), "Question bank should be active");

            // And aggregates should use value objects correctly
            assertTrue(taxonomySet.belongsToUser(12345L), "Taxonomy set should belong to user");
            assertTrue(userBanks.validateOwnership(12345L, 789L), "User should own question bank");
        }
    }

    // Helper methods
    private QuestionBanksPerUserAggregate createUserBanksAggregate(Long userId, Long questionBankId) {
        Instant now = Instant.now();
        List<QuestionBank> questionBanks = Arrays.asList(
            new QuestionBank(questionBankId, "JavaScript Fundamentals", "Core JavaScript concepts", true, now, now),
            new QuestionBank(790L, "Advanced React Patterns", "Complex React patterns", true, now, now)
        );

        return QuestionBanksPerUserAggregate.create(
            new ObjectId(),
            userId,
            questionBankId,
            questionBanks
        );
    }

    private TaxonomySetAggregate createTaxonomySetAggregate(Long userId, Long questionBankId) {
        CategoryLevels categories = new CategoryLevels(
            new CategoryLevels.Category("tech", "Technology", "technology", null),
            new CategoryLevels.Category("prog", "Programming", "programming", "tech"),
            new CategoryLevels.Category("web_dev", "Web Development", "web-development", "prog"),
            new CategoryLevels.Category("javascript", "JavaScript", "javascript", "web_dev")
        );

        List<Tag> tags = Arrays.asList(
            new Tag("js-arrays", "JavaScript Arrays", "#f7df1e"),
            new Tag("array-methods", "Array Methods", "#61dafb"),
            new Tag("methods", "Methods", "#764abc"),
            new Tag("beginner", "Beginner", "#28a745")
        );

        List<Quiz> quizzes = Arrays.asList(
            new Quiz(101L, "JavaScript Fundamentals Quiz", "js-fundamentals"),
            new Quiz(205L, "Array Methods Mastery", "array-methods-mastery"),
            new Quiz(312L, "Frontend Developer Assessment", "frontend-dev-assessment")
        );

        DifficultyLevel currentDifficultyLevel = new DifficultyLevel("easy", 1, "Suitable for beginners");
        List<DifficultyLevel> availableDifficultyLevels = Arrays.asList(currentDifficultyLevel);

        return TaxonomySetAggregate.create(
            new ObjectId(),
            userId,
            questionBankId,
            categories,
            tags,
            quizzes,
            currentDifficultyLevel,
            availableDifficultyLevels
        );
    }

    private UpsertQuestionCommand createCompleteCommand(Long userId, Long questionBankId) {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-question-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test Content");
        request.setTaxonomy(createCompleteTaxonomy());

        return new UpsertQuestionCommand(userId, questionBankId, request);
    }

    private UpsertQuestionCommand createPartialCommand(Long userId, Long questionBankId) {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-question-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test Content");
        request.setTaxonomy(createPartialTaxonomy());

        return new UpsertQuestionCommand(userId, questionBankId, request);
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

    private TaxonomyData createPartialTaxonomy() {
        TaxonomyData taxonomy = new TaxonomyData();

        // Only level 1 category and difficulty
        TaxonomyData.Categories categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "technology", null));
        taxonomy.setCategories(categories);

        // Tags
        taxonomy.setTags(Arrays.asList(
            new TaxonomyData.Tag("js-arrays", "JavaScript Arrays", "#f7df1e")
        ));

        // Difficulty
        taxonomy.setDifficultyLevel(new TaxonomyData.DifficultyLevel("easy", 1, "Suitable for beginners"));

        return taxonomy;
    }
}