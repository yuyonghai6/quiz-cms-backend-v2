package com.quizfun.questionbank.application.commands;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.application.services.DefaultQuestionBankApplicationService;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OnNewUserCreateDefaultQuestionBankCommandHandler.
 *
 * Tests the command handler's delegation to the application service
 * and proper result propagation.
 */
@ExtendWith(MockitoExtension.class)
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1007.command-handler")
@DisplayName("OnNewUserCreateDefaultQuestionBankCommandHandler Tests")
class OnNewUserCreateDefaultQuestionBankCommandHandlerTest {

    @Mock
    private DefaultQuestionBankApplicationService applicationService;

    private OnNewUserCreateDefaultQuestionBankCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OnNewUserCreateDefaultQuestionBankCommandHandler(applicationService);
    }

    @Test
    @DisplayName("Should delegate to application service successfully")
    void shouldDelegateToApplicationServiceSuccessfully() {
        // Given
        Long userId = 123456789L;
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, "test@example.com", Map.of());

        DefaultQuestionBankResponseDto mockResponse = DefaultQuestionBankResponseDto.builder()
            .userId(userId)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .build();

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.success("Success", mockResponse));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getUserId()).isEqualTo(userId);
        assertThat(result.data().getQuestionBankId()).isEqualTo(1730832000000000L);

        verify(applicationService, times(1)).createDefaultQuestionBank(userId);
    }

    @Test
    @DisplayName("Should pass userId from command to service")
    void shouldPassUserIdFromCommandToService() {
        // Given
        Long userId = 999L;
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, null, null);

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        handler.handle(command);

        // Then
        verify(applicationService).createDefaultQuestionBank(userId);
    }

    @Test
    @DisplayName("Should propagate failure from application service")
    void shouldPropagateFailureFromApplicationService() {
        // Given
        Long userId = 888L;
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, "user@test.com", Map.of());

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.failure("DATABASE_ERROR: Connection timeout"));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DATABASE_ERROR");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("Should propagate duplicate user error")
    void shouldPropagateDuplicateUserError() {
        // Given
        Long userId = 777L;
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, "duplicate@test.com", Map.of());

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.failure("DUPLICATE_USER: User 777 already has a default question bank"));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DUPLICATE_USER");
        assertThat(result.message()).contains("777");
    }

    @Test
    @DisplayName("Should handle command with email and metadata")
    void shouldHandleCommandWithEmailAndMetadata() {
        // Given
        Long userId = 555L;
        String email = "user@example.com";
        Map<String, String> metadata = Map.of("source", "registration", "plan", "free");

        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, email, metadata);

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        handler.handle(command);

        // Then
        // Handler should only use userId, ignoring email and metadata for DEFAULT question bank
        verify(applicationService).createDefaultQuestionBank(userId);
    }

    @Test
    @DisplayName("Should use constructor injection")
    void shouldUseConstructorInjection() {
        // When
        OnNewUserCreateDefaultQuestionBankCommandHandler newHandler =
            new OnNewUserCreateDefaultQuestionBankCommandHandler(applicationService);

        // Then
        assertThat(newHandler).isNotNull();
    }

    @Test
    @DisplayName("Should handle null response from service")
    void shouldHandleNullResponseFromService() {
        // Given
        Long userId = 111L;
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, null, null);

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.success("Success", null));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("Should preserve error message from service")
    void shouldPreserveErrorMessageFromService() {
        // Given
        Long userId = 222L;
        String errorMessage = "INTERNAL_ERROR: ID generation failed";
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(userId, null, null);

        when(applicationService.createDefaultQuestionBank(userId))
            .thenReturn(Result.failure(errorMessage));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo(errorMessage);
    }
}
