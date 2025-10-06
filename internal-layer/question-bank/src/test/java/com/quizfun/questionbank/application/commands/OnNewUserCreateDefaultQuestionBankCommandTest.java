package com.quizfun.questionbank.application.commands;

import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1001.command-and-validation")
@DisplayName("OnNewUserCreateDefaultQuestionBankCommand Tests")
class OnNewUserCreateDefaultQuestionBankCommandTest {

    @Test
    @DisplayName("Should create command with valid userId")
    void shouldCreateCommandWithValidUserId() {
        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123456789L, "user@example.com", Map.of());

        // Then
        assertThat(command.getUserId()).isEqualTo(123456789L);
        assertThat(command.getUserEmail()).isEqualTo("user@example.com");
        assertThat(command.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when userId is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // When & Then
        assertThatThrownBy(() ->
            new OnNewUserCreateDefaultQuestionBankCommand(null, "user@example.com", Map.of())
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId cannot be null");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, -100L, -999999L})
    @DisplayName("Should throw exception when userId is negative")
    void shouldThrowExceptionWhenUserIdIsNegative(Long invalidUserId) {
        // When & Then
        assertThatThrownBy(() ->
            new OnNewUserCreateDefaultQuestionBankCommand(invalidUserId, "user@example.com", Map.of())
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId must be positive");
    }

    @Test
    @DisplayName("Should throw exception when userId is zero")
    void shouldThrowExceptionWhenUserIdIsZero() {
        // When & Then
        assertThatThrownBy(() ->
            new OnNewUserCreateDefaultQuestionBankCommand(0L, "user@example.com", Map.of())
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId must be positive");
    }

    @Test
    @DisplayName("Should create command with valid email")
    void shouldCreateCommandWithValidEmail() {
        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123L, "test.user@domain.com", null);

        // Then
        assertThat(command.getUserEmail()).isEqualTo("test.user@domain.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "missing@domain", "@nodomain.com", "no-at-symbol.com"})
    @DisplayName("Should throw exception for invalid email format")
    void shouldThrowExceptionForInvalidEmailFormat(String invalidEmail) {
        // When & Then
        assertThatThrownBy(() ->
            new OnNewUserCreateDefaultQuestionBankCommand(123L, invalidEmail, null)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should handle null or empty email gracefully")
    void shouldHandleNullOrEmptyEmailGracefully(String email) {
        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123L, email, null);

        // Then
        assertThat(command.getUserEmail()).isNull();
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespaceFromEmail() {
        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123L, "  user@example.com  ", null);

        // Then
        assertThat(command.getUserEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should create immutable metadata map")
    void shouldCreateImmutableMetadataMap() {
        // Given
        Map<String, String> originalMetadata = new HashMap<>();
        originalMetadata.put("key1", "value1");

        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123L, "user@example.com", originalMetadata);

        // Then
        assertThatThrownBy(() -> command.getMetadata().put("key2", "value2"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should handle null metadata by creating empty map")
    void shouldHandleNullMetadataByCreatingEmptyMap() {
        // When
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123L, "user@example.com", null);

        // Then
        assertThat(command.getMetadata()).isNotNull();
        assertThat(command.getMetadata()).isEmpty();
    }
}
