package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.CategoryLevels;
import com.quizfun.questionbank.domain.entities.DifficultyLevel;
import com.quizfun.questionbank.domain.entities.Tag;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1002.domain-aggregates")
@DisplayName("TaxonomySetAggregate Default Creation Tests")
class TaxonomySetAggregateDefaultCreationTest {

    @Test
    @DisplayName("Should create default taxonomy aggregate with valid inputs")
    void shouldCreateDefaultTaxonomyAggregateWithValidInputs() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // Then
        assertThat(aggregate).isNotNull();
        assertThat(aggregate.getUserId()).isEqualTo(userId);
        assertThat(aggregate.getQuestionBankId()).isEqualTo(questionBankId);
    }

    @Test
    @DisplayName("Should create default category level 1 only")
    void shouldCreateDefaultCategoryLevel1Only() {
        // Given
        Long userId = 123L;
        Long questionBankId = 456L;

        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, Instant.now());

        // Then
        CategoryLevels categories = aggregate.getCategories();
        assertThat(categories.getLevel1()).isNotNull();
        assertThat(categories.getLevel1().getId()).isEqualTo("general");
        assertThat(categories.getLevel1().getName()).isEqualTo("General");
        assertThat(categories.getLevel1().getSlug()).isEqualTo("general");
        assertThat(categories.getLevel1().getParentId()).isNull();

        assertThat(categories.getLevel2()).isNull();
        assertThat(categories.getLevel3()).isNull();
        assertThat(categories.getLevel4()).isNull();
    }

    @Test
    @DisplayName("Should create three default tags")
    void shouldCreateThreeDefaultTags() {
        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // Then
        assertThat(aggregate.getTags()).hasSize(3);

        Tag beginnerTag = findTagById(aggregate, "beginner");
        assertThat(beginnerTag.getName()).isEqualTo("Beginner");
        assertThat(beginnerTag.getColor()).isEqualTo("#28a745");

        Tag practiceTag = findTagById(aggregate, "practice");
        assertThat(practiceTag.getName()).isEqualTo("Practice");
        assertThat(practiceTag.getColor()).isEqualTo("#007bff");

        Tag quickTestTag = findTagById(aggregate, "quick-test");
        assertThat(quickTestTag.getName()).isEqualTo("Quick Test");
        assertThat(quickTestTag.getColor()).isEqualTo("#6f42c1");
    }

    @Test
    @DisplayName("Should create empty quizzes list")
    void shouldCreateEmptyQuizzesList() {
        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // Then
        assertThat(aggregate.getQuizzes()).isEmpty();
    }

    @Test
    @DisplayName("Should set current difficulty level to easy")
    void shouldSetCurrentDifficultyLevelToEasy() {
        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // Then
        DifficultyLevel currentLevel = aggregate.getCurrentDifficultyLevel();
        assertThat(currentLevel.getLevel()).isEqualTo("easy");
        assertThat(currentLevel.getNumericValue()).isEqualTo(1);
        assertThat(currentLevel.getDescription())
            .isEqualTo("Suitable for beginners and initial learning");
    }

    @Test
    @DisplayName("Should create three available difficulty levels")
    void shouldCreateThreeAvailableDifficultyLevels() {
        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // Then
        assertThat(aggregate.getAvailableDifficultyLevels()).hasSize(3);

        DifficultyLevel easy = findDifficultyByLevel(aggregate, "easy");
        assertThat(easy.getNumericValue()).isEqualTo(1);

        DifficultyLevel medium = findDifficultyByLevel(aggregate, "medium");
        assertThat(medium.getNumericValue()).isEqualTo(2);

        DifficultyLevel hard = findDifficultyByLevel(aggregate, "hard");
        assertThat(hard.getNumericValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should validate taxonomy references for default values")
    void shouldValidateTaxonomyReferencesForDefaultValues() {
        // Given
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // When & Then - All default values should be valid
        assertThat(aggregate.validateSingleTaxonomyReference("general")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("beginner")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("practice")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("quick-test")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("easy")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("medium")).isTrue();
        assertThat(aggregate.validateSingleTaxonomyReference("hard")).isTrue();
    }

    @Test
    @DisplayName("Should validate category hierarchy is valid")
    void shouldValidateCategoryHierarchyIsValid() {
        // When
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // Then - Hierarchy should be valid (only level_1, no gaps)
        assertThat(aggregate.validateCategoryHierarchy()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when userId is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            TaxonomySetAggregate.createDefault(null, 123L, Instant.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when questionBankId is null")
    void shouldThrowExceptionWhenQuestionBankIdIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            TaxonomySetAggregate.createDefault(123L, null, Instant.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Question Bank ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when timestamp is null")
    void shouldThrowExceptionWhenTimestampIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            TaxonomySetAggregate.createDefault(123L, 456L, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Timestamp cannot be null");
    }

    // Helper methods
    private Tag findTagById(TaxonomySetAggregate aggregate, String id) {
        return aggregate.getTags().stream()
            .filter(tag -> tag.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Tag not found: " + id));
    }

    private DifficultyLevel findDifficultyByLevel(TaxonomySetAggregate aggregate, String level) {
        return aggregate.getAvailableDifficultyLevels().stream()
            .filter(diff -> diff.getLevel().equals(level))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Difficulty not found: " + level));
    }
}
