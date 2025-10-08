package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.QuestionBank;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1002.domain-aggregates")
@DisplayName("QuestionBanksPerUserAggregate Default Creation Tests")
class QuestionBanksPerUserAggregateDefaultCreationTest {

    @Test
    @DisplayName("Should create default aggregate with valid inputs")
    void shouldCreateDefaultAggregateWithValidInputs() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        // When
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // Then
        assertThat(aggregate).isNotNull();
        assertThat(aggregate.getUserId()).isEqualTo(userId);
        assertThat(aggregate.getDefaultQuestionBankId()).isEqualTo(questionBankId);
        assertThat(aggregate.getQuestionBanks()).hasSize(1);
    }

    @Test
    @DisplayName("Should create question bank with correct default values")
    void shouldCreateQuestionBankWithCorrectDefaultValues() {
        // Given
        Long userId = 123L;
        Long questionBankId = 1730832000000000L;
        Instant now = Instant.now();

        // When
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

        // Then
        QuestionBank bank = aggregate.getQuestionBanks().get(0);
        assertThat(bank.getBankId()).isEqualTo(questionBankId);
        assertThat(bank.getName()).isEqualTo("Default Question Bank");
        assertThat(bank.getDescription())
            .isEqualTo("Your default question bank for getting started with quiz creation");
        assertThat(bank.isActive()).isTrue();
        assertThat(bank.getCreatedAt()).isEqualTo(now);
        assertThat(bank.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set default question bank ID to match created bank ID")
    void shouldSetDefaultQuestionBankIdToMatchCreatedBankId() {
        // Given
        Long userId = 123L;
        Long questionBankId = 1730832000000000L;

        // When
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, Instant.now());

        // Then
        assertThat(aggregate.getDefaultQuestionBankId())
            .isEqualTo(aggregate.getQuestionBanks().get(0).getBankId());
    }

    @Test
    @DisplayName("Should validate ownership after default creation")
    void shouldValidateOwnershipAfterDefaultCreation() {
        // Given
        Long userId = 123L;
        Long questionBankId = 1730832000000000L;

        // When
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, Instant.now());

        // Then
        assertThat(aggregate.validateOwnership(userId, questionBankId)).isTrue();
        assertThat(aggregate.validateOwnership(userId, 999L)).isFalse();
        assertThat(aggregate.validateOwnership(999L, questionBankId)).isFalse();
    }

    @Test
    @DisplayName("Should identify default question bank correctly")
    void shouldIdentifyDefaultQuestionBankCorrectly() {
        // Given
        Long userId = 123L;
        Long questionBankId = 1730832000000000L;

        // When
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, Instant.now());

        // Then
        assertThat(aggregate.isDefaultQuestionBank(questionBankId)).isTrue();
        assertThat(aggregate.isDefaultQuestionBank(999L)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when userId is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            QuestionBanksPerUserAggregate.createDefault(null, 123L, Instant.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when questionBankId is null")
    void shouldThrowExceptionWhenQuestionBankIdIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            QuestionBanksPerUserAggregate.createDefault(123L, null, Instant.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Question Bank ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when timestamp is null")
    void shouldThrowExceptionWhenTimestampIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            QuestionBanksPerUserAggregate.createDefault(123L, 456L, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Timestamp cannot be null");
    }
}
