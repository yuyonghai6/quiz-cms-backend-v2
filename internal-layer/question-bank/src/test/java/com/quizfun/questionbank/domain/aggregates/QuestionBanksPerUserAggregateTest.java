package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionBank;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class QuestionBanksPerUserAggregateTest {

    @Nested
    @DisplayName("Question Bank Ownership Validation")
    class QuestionBankOwnershipValidation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate ownership for active question bank")
        @Description("Tests that validateOwnership returns true when user owns an active question bank")
        void shouldValidateOwnershipForActiveQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.validateOwnership(12345L, 789L);

            // Then
            assertTrue(result, "User should own the active question bank");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject inactive question bank")
        @Description("Tests that validateOwnership returns false when question bank is inactive")
        void shouldRejectInactiveQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.validateOwnership(12345L, 999L); // 999L is inactive

            // Then
            assertFalse(result, "Inactive question bank should be rejected");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject non-owned question bank")
        @Description("Tests that validateOwnership returns false when question bank doesn't belong to user")
        void shouldRejectNonOwnedQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.validateOwnership(99999L, 789L); // Different user

            // Then
            assertFalse(result, "Question bank owned by different user should be rejected");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject non-existent question bank")
        @Description("Tests that validateOwnership returns false when question bank doesn't exist")
        void shouldRejectNonExistentQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.validateOwnership(12345L, 88888L); // Non-existent bank

            // Then
            assertFalse(result, "Non-existent question bank should be rejected");
        }
    }

    @Nested
    @DisplayName("Default Question Bank Identification")
    class DefaultQuestionBankIdentification {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should identify default question bank")
        @Description("Tests that isDefaultQuestionBank returns true for the default question bank")
        void shouldIdentifyDefaultQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.isDefaultQuestionBank(789L);

            // Then
            assertTrue(result, "Should identify default question bank correctly");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should identify non-default question bank")
        @Description("Tests that isDefaultQuestionBank returns false for non-default question banks")
        void shouldIdentifyNonDefaultQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.isDefaultQuestionBank(790L);

            // Then
            assertFalse(result, "Should identify non-default question bank correctly");
        }
    }

    @Nested
    @DisplayName("Question Bank Retrieval")
    class QuestionBankRetrieval {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should find existing question bank")
        @Description("Tests that findQuestionBank returns the question bank when it exists")
        void shouldFindExistingQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            Optional<QuestionBank> result = aggregate.findQuestionBank(789L);

            // Then
            assertTrue(result.isPresent(), "Should find existing question bank");
            assertEquals(789L, result.get().getBankId(), "Should return correct question bank");
            assertEquals("JavaScript Fundamentals", result.get().getName(), "Should have correct name");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should return empty for non-existent question bank")
        @Description("Tests that findQuestionBank returns empty Optional when question bank doesn't exist")
        void shouldReturnEmptyForNonExistentQuestionBank() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            Optional<QuestionBank> result = aggregate.findQuestionBank(88888L);

            // Then
            assertFalse(result.isPresent(), "Should return empty for non-existent question bank");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should return only active question banks")
        @Description("Tests that getActiveQuestionBanks filters out inactive question banks")
        void shouldReturnOnlyActiveQuestionBanks() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            List<QuestionBank> activeBanks = aggregate.getActiveQuestionBanks();

            // Then
            assertEquals(2, activeBanks.size(), "Should return only 2 active question banks");
            assertTrue(activeBanks.stream().allMatch(QuestionBank::isActive), "All returned banks should be active");
        }
    }

    @Nested
    @DisplayName("User Isolation")
    class UserIsolation {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should validate correct user ownership")
        @Description("Tests that belongsToUser returns true for the correct user")
        void shouldValidateCorrectUserOwnership() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.belongsToUser(12345L);

            // Then
            assertTrue(result, "Aggregate should belong to correct user");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-006.supporting-aggregates-implementation")
        @DisplayName("Should reject different user")
        @Description("Tests that belongsToUser returns false for different user")
        void shouldRejectDifferentUser() {
            // Given
            QuestionBanksPerUserAggregate aggregate = createValidAggregate();

            // When
            boolean result = aggregate.belongsToUser(99999L);

            // Then
            assertFalse(result, "Aggregate should not belong to different user");
        }
    }

    private QuestionBanksPerUserAggregate createValidAggregate() {
        Instant now = Instant.now();

        List<QuestionBank> questionBanks = Arrays.asList(
            new QuestionBank(789L, "JavaScript Fundamentals", "Core JavaScript concepts", true, now, now),
            new QuestionBank(790L, "Advanced React Patterns", "Complex React patterns", true, now, now),
            new QuestionBank(999L, "Inactive Bank", "This bank is inactive", false, now, now) // Inactive bank
        );

        return QuestionBanksPerUserAggregate.create(
            new ObjectId(),
            12345L,
            789L, // Default question bank ID
            questionBanks
        );
    }
}