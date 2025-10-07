package com.quizfun.questionbank.application.services;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.globalshared.utils.LongIdGenerator;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.application.ports.out.DefaultQuestionBankRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultQuestionBankApplicationService.
 *
 * Tests the orchestration logic using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1006.application-service")
@DisplayName("Default Question Bank Application Service Tests")
class DefaultQuestionBankApplicationServiceTest {

    @Mock
    private DefaultQuestionBankRepository repository;

    @Mock
    private LongIdGenerator longIdGenerator;

    private DefaultQuestionBankApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultQuestionBankApplicationService(repository, longIdGenerator);
    }

    @Test
    @DisplayName("Should create default question bank successfully")
    void shouldCreateDefaultQuestionBankSuccessfully() {
        // Given
        Long userId = 123456789L;
        Long generatedBankId = 1730832000000000L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(generatedBankId);

        DefaultQuestionBankResponseDto mockResponse = DefaultQuestionBankResponseDto.builder()
            .userId(userId)
            .questionBankId(generatedBankId)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .build();

        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success("Success", mockResponse));

        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(userId);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getUserId()).isEqualTo(userId);
        assertThat(result.data().getQuestionBankId()).isEqualTo(generatedBankId);

        verify(longIdGenerator, times(1)).generateQuestionBankId();
        verify(repository, times(1)).createDefaultQuestionBank(any(), any());
    }

    @Test
    @DisplayName("Should generate unique question bank ID")
    void shouldGenerateUniqueQuestionBankId() {
        // Given
        Long userId = 999L;
        Long expectedBankId = 1730832111111111L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(expectedBankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        service.createDefaultQuestionBank(userId);

        // Then
        ArgumentCaptor<QuestionBanksPerUserAggregate> qbCaptor =
            ArgumentCaptor.forClass(QuestionBanksPerUserAggregate.class);
        ArgumentCaptor<TaxonomySetAggregate> taxCaptor =
            ArgumentCaptor.forClass(TaxonomySetAggregate.class);

        verify(repository).createDefaultQuestionBank(qbCaptor.capture(), taxCaptor.capture());

        assertThat(qbCaptor.getValue().getDefaultQuestionBankId()).isEqualTo(expectedBankId);
        assertThat(taxCaptor.getValue().getQuestionBankId()).isEqualTo(expectedBankId);
    }

    @Test
    @DisplayName("Should create both aggregates with same timestamp")
    void shouldCreateBothAggregatesWithSameTimestamp() {
        // Given
        Long userId = 555L;
        Long bankId = 1730832222222222L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(bankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        Instant beforeCall = Instant.now();
        service.createDefaultQuestionBank(userId);
        Instant afterCall = Instant.now();

        // Then
        ArgumentCaptor<QuestionBanksPerUserAggregate> qbCaptor =
            ArgumentCaptor.forClass(QuestionBanksPerUserAggregate.class);
        ArgumentCaptor<TaxonomySetAggregate> taxCaptor =
            ArgumentCaptor.forClass(TaxonomySetAggregate.class);

        verify(repository).createDefaultQuestionBank(qbCaptor.capture(), taxCaptor.capture());

        Instant qbTimestamp = qbCaptor.getValue().getQuestionBanks().get(0).getCreatedAt();
        Instant taxTimestamp = taxCaptor.getValue().getCreatedAt();

        // Both should have the same timestamp (or very close)
        assertThat(qbTimestamp).isNotNull();
        assertThat(taxTimestamp).isNotNull();
        assertThat(qbTimestamp).isBetween(beforeCall, afterCall);
        assertThat(taxTimestamp).isBetween(beforeCall, afterCall);
    }

    @Test
    @DisplayName("Should call repository with correct aggregates")
    void shouldCallRepositoryWithCorrectAggregates() {
        // Given
        Long userId = 777L;
        Long bankId = 1730832333333333L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(bankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        service.createDefaultQuestionBank(userId);

        // Then
        ArgumentCaptor<QuestionBanksPerUserAggregate> qbCaptor =
            ArgumentCaptor.forClass(QuestionBanksPerUserAggregate.class);
        ArgumentCaptor<TaxonomySetAggregate> taxCaptor =
            ArgumentCaptor.forClass(TaxonomySetAggregate.class);

        verify(repository).createDefaultQuestionBank(qbCaptor.capture(), taxCaptor.capture());

        // Verify QuestionBanksPerUserAggregate
        QuestionBanksPerUserAggregate qbAgg = qbCaptor.getValue();
        assertThat(qbAgg.getUserId()).isEqualTo(userId);
        assertThat(qbAgg.getDefaultQuestionBankId()).isEqualTo(bankId);
        assertThat(qbAgg.getQuestionBanks()).hasSize(1);
        assertThat(qbAgg.getQuestionBanks().get(0).getName()).isEqualTo("Default Question Bank");

        // Verify TaxonomySetAggregate
        TaxonomySetAggregate taxAgg = taxCaptor.getValue();
        assertThat(taxAgg.getUserId()).isEqualTo(userId);
        assertThat(taxAgg.getQuestionBankId()).isEqualTo(bankId);
        assertThat(taxAgg.getTags()).hasSize(3);
        assertThat(taxAgg.getAvailableDifficultyLevels()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle repository failure")
    void shouldHandleRepositoryFailure() {
        // Given
        Long userId = 888L;
        Long bankId = 1730832444444444L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(bankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.failure("DATABASE_ERROR: Connection timeout"));

        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(userId);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DATABASE_ERROR");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("Should handle ID generation failure")
    void shouldHandleIdGenerationFailure() {
        // Given
        Long userId = 111L;

        when(longIdGenerator.generateQuestionBankId())
            .thenThrow(new RuntimeException("ID generation failed"));

        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(userId);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("INTERNAL_ERROR");
        assertThat(result.message()).contains("ID generation failed");
        assertThat(result.data()).isNull();

        verify(repository, never()).createDefaultQuestionBank(any(), any());
    }

    @Test
    @DisplayName("Should propagate duplicate user error from repository")
    void shouldPropagateDuplicateUserErrorFromRepository() {
        // Given
        Long userId = 222L;
        Long bankId = 1730832555555555L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(bankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.failure("DUPLICATE_USER: User 222 already has a default question bank"));

        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(userId);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DUPLICATE_USER");
        assertThat(result.message()).contains("222");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("Should return error when userId is null")
    void shouldReturnErrorWhenUserIdIsNull() {
        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(null);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("INTERNAL_ERROR");

        verify(longIdGenerator, never()).generateQuestionBankId();
        verify(repository, never()).createDefaultQuestionBank(any(), any());
    }

    @Test
    @DisplayName("Should use constructor injection for dependencies")
    void shouldUseConstructorInjectionForDependencies() {
        // This test verifies constructor injection is used
        DefaultQuestionBankApplicationService newService =
            new DefaultQuestionBankApplicationService(repository, longIdGenerator);

        assertThat(newService).isNotNull();
    }

    @Test
    @DisplayName("Should create service with all required dependencies")
    void shouldCreateServiceWithAllRequiredDependencies() {
        // Given
        Long userId = 333L;
        Long bankId = 1730832666666666L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(bankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success("Success", mock(DefaultQuestionBankResponseDto.class)));

        // When
        Result<DefaultQuestionBankResponseDto> result = service.createDefaultQuestionBank(userId);

        // Then
        assertThat(result.success()).isTrue();
        verify(longIdGenerator).generateQuestionBankId();
        verify(repository).createDefaultQuestionBank(any(), any());
    }
}
