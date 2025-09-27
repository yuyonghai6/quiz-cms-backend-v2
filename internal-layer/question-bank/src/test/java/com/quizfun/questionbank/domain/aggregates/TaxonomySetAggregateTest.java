package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.CategoryLevels;
import com.quizfun.questionbank.domain.entities.CategoryLevels.Category;
import com.quizfun.questionbank.domain.entities.DifficultyLevel;
import com.quizfun.questionbank.domain.entities.Tag;
import com.quizfun.questionbank.domain.entities.Quiz;
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

class TaxonomySetAggregateTest {

    @Nested
    @DisplayName("Taxonomy Reference Validation")
    class TaxonomyReferenceValidation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate all taxonomy references when all are valid")
        @Description("Tests that validateTaxonomyReferences returns true when all provided taxonomy IDs exist in the taxonomy set")
        void shouldValidateAllTaxonomyReferencesWhenAllAreValid() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();
            List<String> validTaxonomyIds = Arrays.asList("tech", "prog", "js-arrays", "101", "easy");

            // When
            boolean result = aggregate.validateTaxonomyReferences(validTaxonomyIds);

            // Then
            assertTrue(result, "All valid taxonomy references should be validated successfully");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject invalid taxonomy references")
        @Description("Tests that validateTaxonomyReferences returns false when some provided taxonomy IDs do not exist")
        void shouldRejectInvalidTaxonomyReferences() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();
            List<String> mixedTaxonomyIds = Arrays.asList("tech", "invalid-id", "js-arrays");

            // When
            boolean result = aggregate.validateTaxonomyReferences(mixedTaxonomyIds);

            // Then
            assertFalse(result, "Invalid taxonomy references should be rejected");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should return list of invalid taxonomy references")
        @Description("Tests that findInvalidTaxonomyReferences returns exactly the taxonomy IDs that don't exist")
        void shouldReturnListOfInvalidTaxonomyReferences() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();
            List<String> mixedTaxonomyIds = Arrays.asList("tech", "invalid-id", "js-arrays", "another-invalid");

            // When
            List<String> invalidIds = aggregate.findInvalidTaxonomyReferences(mixedTaxonomyIds);

            // Then
            assertEquals(2, invalidIds.size(), "Should return exactly 2 invalid IDs");
            assertTrue(invalidIds.contains("invalid-id"), "Should contain 'invalid-id'");
            assertTrue(invalidIds.contains("another-invalid"), "Should contain 'another-invalid'");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate single taxonomy reference successfully")
        @Description("Tests that validateSingleTaxonomyReference works correctly for individual taxonomy ID validation")
        void shouldValidateSingleTaxonomyReferenceSuccessfully() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When & Then
            assertTrue(aggregate.validateSingleTaxonomyReference("tech"), "Valid category ID should be validated");
            assertTrue(aggregate.validateSingleTaxonomyReference("js-arrays"), "Valid tag ID should be validated");
            assertTrue(aggregate.validateSingleTaxonomyReference("101"), "Valid quiz ID should be validated");
            assertTrue(aggregate.validateSingleTaxonomyReference("easy"), "Valid difficulty level should be validated");
            assertFalse(aggregate.validateSingleTaxonomyReference("invalid"), "Invalid ID should be rejected");
        }
    }

    @Nested
    @DisplayName("Category Hierarchy Validation")
    class CategoryHierarchyValidation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate category hierarchy when properly structured")
        @Description("Tests that validateCategoryHierarchy returns true for valid category hierarchy")
        void shouldValidateCategoryHierarchyWhenProperlyStructured() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When
            boolean result = aggregate.validateCategoryHierarchy();

            // Then
            assertTrue(result, "Valid category hierarchy should be validated successfully");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject invalid category hierarchy")
        @Description("Tests that validateCategoryHierarchy returns false when hierarchy has gaps (e.g., Level3 without Level2)")
        void shouldRejectInvalidCategoryHierarchy() {
            // Given - Create aggregate with invalid hierarchy (Level3 without Level2)
            CategoryLevels invalidCategories = new CategoryLevels(
                new Category("tech", "Technology", "technology", null),
                null, // Missing Level 2
                new Category("web_dev", "Web Development", "web-development", "prog"), // Level 3 without Level 2
                null
            );

            TaxonomySetAggregate aggregate = TaxonomySetAggregate.create(
                new ObjectId(),
                12345L,
                789L,
                invalidCategories,
                Arrays.asList(new Tag("js-arrays", "JavaScript Arrays", "#f7df1e")),
                Arrays.asList(new Quiz(101L, "JavaScript Fundamentals", "js-fundamentals")),
                new DifficultyLevel("easy", 1, "Suitable for beginners"),
                Arrays.asList(new DifficultyLevel("easy", 1, "Suitable for beginners"))
            );

            // When
            boolean result = aggregate.validateCategoryHierarchy();

            // Then
            assertFalse(result, "Invalid category hierarchy should be rejected");
        }
    }

    @Nested
    @DisplayName("User and Question Bank Isolation")
    class UserAndQuestionBankIsolation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate ownership for correct user")
        @Description("Tests that belongsToUser returns true when user ID matches aggregate's user ID")
        void shouldValidateOwnershipForCorrectUser() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When
            boolean result = aggregate.belongsToUser(12345L);

            // Then
            assertTrue(result, "Aggregate should belong to the correct user");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject access for different user")
        @Description("Tests that belongsToUser returns false when user ID doesn't match")
        void shouldRejectAccessForDifferentUser() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When
            boolean result = aggregate.belongsToUser(99999L);

            // Then
            assertFalse(result, "Aggregate should not belong to different user");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate ownership for correct question bank")
        @Description("Tests that belongsToQuestionBank returns true when question bank ID matches")
        void shouldValidateOwnershipForCorrectQuestionBank() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When
            boolean result = aggregate.belongsToQuestionBank(789L);

            // Then
            assertTrue(result, "Aggregate should belong to the correct question bank");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject access for different question bank")
        @Description("Tests that belongsToQuestionBank returns false when question bank ID doesn't match")
        void shouldRejectAccessForDifferentQuestionBank() {
            // Given
            TaxonomySetAggregate aggregate = createValidTaxonomySetAggregate();

            // When
            boolean result = aggregate.belongsToQuestionBank(99999L);

            // Then
            assertFalse(result, "Aggregate should not belong to different question bank");
        }
    }

    private TaxonomySetAggregate createValidTaxonomySetAggregate() {
        // Create valid category hierarchy
        CategoryLevels categories = new CategoryLevels(
            new Category("tech", "Technology", "technology", null),
            new Category("prog", "Programming", "programming", "tech"),
            new Category("web_dev", "Web Development", "web-development", "prog"),
            new Category("javascript", "JavaScript", "javascript", "web_dev")
        );

        // Create tags
        List<Tag> tags = Arrays.asList(
            new Tag("js-arrays", "JavaScript Arrays", "#f7df1e"),
            new Tag("array-methods", "Array Methods", "#61dafb")
        );

        // Create quizzes
        List<Quiz> quizzes = Arrays.asList(
            new Quiz(101L, "JavaScript Fundamentals Quiz", "js-fundamentals"),
            new Quiz(205L, "Array Methods Mastery", "array-methods-mastery")
        );

        // Create difficulty levels
        DifficultyLevel currentDifficultyLevel = new DifficultyLevel("easy", 1, "Suitable for beginners");
        List<DifficultyLevel> availableDifficultyLevels = Arrays.asList(
            new DifficultyLevel("easy", 1, "Suitable for beginners"),
            new DifficultyLevel("medium", 2, "Intermediate knowledge required"),
            new DifficultyLevel("hard", 3, "Advanced understanding needed")
        );

        return TaxonomySetAggregate.create(
            new ObjectId(),
            12345L,
            789L,
            categories,
            tags,
            quizzes,
            currentDifficultyLevel,
            availableDifficultyLevels
        );
    }
}