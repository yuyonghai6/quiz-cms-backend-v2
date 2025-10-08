package com.quizfun.questionbank.infrastructure.persistence.mappers;

import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import com.quizfun.questionbank.infrastructure.persistence.documents.TaxonomySetDocument;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1003.mongodb-documents-and-mappers")
@DisplayName("TaxonomySetMapper Tests")
class TaxonomySetMapperTest {

    private TaxonomySetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TaxonomySetMapper();
    }

    @Test
    @DisplayName("Should map aggregate to document with all fields")
    void shouldMapAggregateToDocumentWithAllFields() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When
        TaxonomySetDocument document = mapper.toDocument(aggregate);

        // Then
        assertThat(document).isNotNull();
        assertThat(document.getId()).isNotNull();
        assertThat(document.getUserId()).isEqualTo(userId);
        assertThat(document.getQuestionBankId()).isEqualTo(questionBankId);
        assertThat(document.getCategories()).isNotNull();
        assertThat(document.getTags()).hasSize(3);
        assertThat(document.getQuizzes()).isEmpty();
        assertThat(document.getCurrentDifficultyLevel()).isNotNull();
        assertThat(document.getAvailableDifficultyLevels()).hasSize(3);
    }

    @Test
    @DisplayName("Should map category levels correctly")
    void shouldMapCategoryLevelsCorrectly() {
        // Given
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // When
        TaxonomySetDocument document = mapper.toDocument(aggregate);

        // Then
        var categories = document.getCategories();
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
    @DisplayName("Should map tags correctly")
    void shouldMapTagsCorrectly() {
        // Given
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // When
        TaxonomySetDocument document = mapper.toDocument(aggregate);

        // Then
        assertThat(document.getTags()).hasSize(3);

        var beginnerTag = document.getTags().stream()
            .filter(t -> t.getId().equals("beginner"))
            .findFirst()
            .orElseThrow();
        assertThat(beginnerTag.getName()).isEqualTo("Beginner");
        assertThat(beginnerTag.getColor()).isEqualTo("#28a745");
    }

    @Test
    @DisplayName("Should map difficulty levels correctly")
    void shouldMapDifficultyLevelsCorrectly() {
        // Given
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // When
        TaxonomySetDocument document = mapper.toDocument(aggregate);

        // Then
        assertThat(document.getCurrentDifficultyLevel().getLevel()).isEqualTo("easy");
        assertThat(document.getCurrentDifficultyLevel().getNumericValue()).isEqualTo(1);

        assertThat(document.getAvailableDifficultyLevels()).hasSize(3);
        var hardLevel = document.getAvailableDifficultyLevels().stream()
            .filter(d -> d.getLevel().equals("hard"))
            .findFirst()
            .orElseThrow();
        assertThat(hardLevel.getNumericValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should map document back to aggregate")
    void shouldMapDocumentBackToAggregate() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        TaxonomySetAggregate originalAggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);
        TaxonomySetDocument document = mapper.toDocument(originalAggregate);

        // When
        TaxonomySetAggregate reconstructedAggregate = mapper.toAggregate(document);

        // Then
        assertThat(reconstructedAggregate).isNotNull();
        assertThat(reconstructedAggregate.getUserId()).isEqualTo(userId);
        assertThat(reconstructedAggregate.getQuestionBankId()).isEqualTo(questionBankId);
        assertThat(reconstructedAggregate.getTags()).hasSize(3);
        assertThat(reconstructedAggregate.getAvailableDifficultyLevels()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle round-trip mapping without data loss")
    void shouldHandleRoundTripMappingWithoutDataLoss() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        TaxonomySetAggregate originalAggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, now);

        // When - Round trip: aggregate → document → aggregate
        TaxonomySetDocument document = mapper.toDocument(originalAggregate);
        TaxonomySetAggregate reconstructedAggregate = mapper.toAggregate(document);

        // Then - Verify all collections preserved
        assertThat(reconstructedAggregate.getUserId()).isEqualTo(originalAggregate.getUserId());
        assertThat(reconstructedAggregate.getQuestionBankId()).isEqualTo(originalAggregate.getQuestionBankId());
        assertThat(reconstructedAggregate.getTags().size()).isEqualTo(originalAggregate.getTags().size());
        assertThat(reconstructedAggregate.getQuizzes().size()).isEqualTo(originalAggregate.getQuizzes().size());
        assertThat(reconstructedAggregate.getAvailableDifficultyLevels().size())
            .isEqualTo(originalAggregate.getAvailableDifficultyLevels().size());

        // Verify category hierarchy preserved
        assertThat(reconstructedAggregate.getCategories().getLevel1().getId())
            .isEqualTo(originalAggregate.getCategories().getLevel1().getId());
    }

    @Test
    @DisplayName("Should preserve empty quizzes list")
    void shouldPreserveEmptyQuizzesList() {
        // Given
        TaxonomySetAggregate aggregate =
            TaxonomySetAggregate.createDefault(123L, 456L, Instant.now());

        // When
        TaxonomySetDocument document = mapper.toDocument(aggregate);
        TaxonomySetAggregate reconstructed = mapper.toAggregate(document);

        // Then
        assertThat(document.getQuizzes()).isEmpty();
        assertThat(reconstructed.getQuizzes()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when mapping null aggregate")
    void shouldThrowExceptionWhenMappingNullAggregate() {
        // When & Then
        assertThatThrownBy(() -> mapper.toDocument(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Aggregate cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when mapping null document")
    void shouldThrowExceptionWhenMappingNullDocument() {
        // When & Then
        assertThatThrownBy(() -> mapper.toAggregate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Document cannot be null");
    }
}
