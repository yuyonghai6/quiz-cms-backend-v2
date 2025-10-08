package com.quizfun.questionbank.infrastructure.persistence.mappers;

import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1003.mongodb-documents-and-mappers")
@DisplayName("QuestionBanksPerUserMapper Tests")
class QuestionBanksPerUserMapperTest {

    private QuestionBanksPerUserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new QuestionBanksPerUserMapper();
    }

    @Test
    @DisplayName("Should map aggregate to document with all fields")
    void shouldMapAggregateToDocumentWithAllFields() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // When
        QuestionBanksPerUserDocument document = mapper.toDocument(aggregate);

        // Then
        assertThat(document).isNotNull();
        assertThat(document.getId()).isNotNull();
        assertThat(document.getUserId()).isEqualTo(userId);
        assertThat(document.getDefaultQuestionBankId()).isEqualTo(questionBankId);
        assertThat(document.getQuestionBanks()).hasSize(1);
        assertThat(document.getCreatedAt()).isNotNull();
        assertThat(document.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should map embedded question bank correctly")
    void shouldMapEmbeddedQuestionBankCorrectly() {
        // Given
        Long userId = 123L;
        Long questionBankId = 456L;
        Instant now = Instant.parse("2025-10-06T10:30:00Z");

        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // When
        QuestionBanksPerUserDocument document = mapper.toDocument(aggregate);

        // Then
        var embeddedBank = document.getQuestionBanks().get(0);
        assertThat(embeddedBank.getBankId()).isEqualTo(questionBankId);
        assertThat(embeddedBank.getName()).isEqualTo("Default Question Bank");
        assertThat(embeddedBank.getDescription())
            .isEqualTo("Your default question bank for getting started with quiz creation");
        assertThat(embeddedBank.isActive()).isTrue();
        assertThat(embeddedBank.getCreatedAt()).isEqualTo(now);
        assertThat(embeddedBank.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should preserve timestamps during mapping")
    void shouldPreserveTimestampsDuringMapping() {
        // Given
        Instant entityTime = Instant.parse("2025-10-06T10:30:15.123Z");
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(123L, 456L, entityTime);

        // When
        QuestionBanksPerUserDocument document = mapper.toDocument(aggregate);

        // Then
        // Aggregate timestamps come from markCreatedNow()
        assertThat(document.getCreatedAt()).isNotNull();
        assertThat(document.getUpdatedAt()).isNotNull();
        // Embedded entity timestamps should match the passed timestamp
        assertThat(document.getQuestionBanks().get(0).getCreatedAt()).isEqualTo(entityTime);
        assertThat(document.getQuestionBanks().get(0).getUpdatedAt()).isEqualTo(entityTime);
    }

    @Test
    @DisplayName("Should map document back to aggregate")
    void shouldMapDocumentBackToAggregate() {
        // Given
        QuestionBanksPerUserDocument document = new QuestionBanksPerUserDocument();
        document.setId(new ObjectId());
        document.setUserId(123456789L);
        document.setDefaultQuestionBankId(1730832000000000L);

        QuestionBanksPerUserDocument.QuestionBankEmbedded embedded =
            new QuestionBanksPerUserDocument.QuestionBankEmbedded();
        embedded.setBankId(1730832000000000L);
        embedded.setName("Default Question Bank");
        embedded.setDescription("Test description");
        embedded.setActive(true);
        embedded.setCreatedAt(Instant.now());
        embedded.setUpdatedAt(Instant.now());

        document.setQuestionBanks(java.util.List.of(embedded));
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        // When
        QuestionBanksPerUserAggregate aggregate = mapper.toAggregate(document);

        // Then
        assertThat(aggregate).isNotNull();
        assertThat(aggregate.getUserId()).isEqualTo(document.getUserId());
        assertThat(aggregate.getDefaultQuestionBankId()).isEqualTo(document.getDefaultQuestionBankId());
        assertThat(aggregate.getQuestionBanks()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle round-trip mapping without data loss")
    void shouldHandleRoundTripMappingWithoutDataLoss() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        QuestionBanksPerUserAggregate originalAggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // When - Round trip: aggregate → document → aggregate
        QuestionBanksPerUserDocument document = mapper.toDocument(originalAggregate);
        QuestionBanksPerUserAggregate reconstructedAggregate = mapper.toAggregate(document);

        // Then - All data preserved
        assertThat(reconstructedAggregate.getUserId()).isEqualTo(originalAggregate.getUserId());
        assertThat(reconstructedAggregate.getDefaultQuestionBankId())
            .isEqualTo(originalAggregate.getDefaultQuestionBankId());
        assertThat(reconstructedAggregate.getQuestionBanks())
            .hasSize(originalAggregate.getQuestionBanks().size());
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
