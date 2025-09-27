package com.quizfun.orchestrationlayer.application.handlers;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.application.services.QuestionApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UpsertQuestionCommandHandler that verify CQRS command handling
 * without requiring full Spring context or business layer dependencies.
 *
 * This test focuses on the command handler's core responsibility:
 * - Delegating to the application service
 * - Converting between Result types
 * - Error handling and logging
 */
@ExtendWith(MockitoExtension.class)
class UpsertQuestionCommandHandlerUnitTest {

    @Mock
    private QuestionApplicationService questionApplicationService;

    @InjectMocks
    private UpsertQuestionCommandHandler commandHandler;

    private UpsertQuestionCommand validCommand;
    private QuestionResponseDto mockResponse;

    @BeforeEach
    void setUp() {
        // Create minimal valid command for testing
        var requestDto = createMinimalValidRequest();
        validCommand = new UpsertQuestionCommand(1001L, 2002L, requestDto);

        // Create mock response
        mockResponse = QuestionResponseDto.builder()
                .questionId("507f1f77bcf86cd799439011")
                .sourceQuestionId("test-source-id")
                .operation("created")
                .taxonomyRelationshipsCount(3)
                .build();
    }

    @Test
    void shouldSuccessfullyDelegateToApplicationService() {
        // Given
        var successResult = com.quizfun.shared.common.Result.success(mockResponse);
        when(questionApplicationService.upsertQuestion(any(UpsertQuestionCommand.class)))
                .thenReturn(successResult);

        // When
        Result<QuestionResponseDto> result = commandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(mockResponse);
        assertThat(result.data().getQuestionId()).isEqualTo("507f1f77bcf86cd799439011");

        verify(questionApplicationService).upsertQuestion(validCommand);
    }

    @Test
    void shouldHandleApplicationServiceFailure() {
        // Given
        var failureResult = com.quizfun.shared.common.Result.<QuestionResponseDto>failure("VALIDATION_ERROR", "Invalid data");
        when(questionApplicationService.upsertQuestion(any(UpsertQuestionCommand.class)))
                .thenReturn(failureResult);

        // When
        Result<QuestionResponseDto> result = commandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid data");
        assertThat(result.data()).isNull();

        verify(questionApplicationService).upsertQuestion(validCommand);
    }

    @Test
    void shouldHandleUnexpectedExceptions() {
        // Given
        when(questionApplicationService.upsertQuestion(any(UpsertQuestionCommand.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        Result<QuestionResponseDto> result = commandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("COMMAND_HANDLER_ERROR");
        assertThat(result.message()).contains("Database connection failed");
        assertThat(result.data()).isNull();

        verify(questionApplicationService).upsertQuestion(validCommand);
    }

    @Test
    void shouldVerifyCommandHandlerIsProperlyInitialized() {
        // Given/When - handler created via @InjectMocks

        // Then
        assertThat(commandHandler).isNotNull();

        // Verify it can handle a basic operation
        var successResult = com.quizfun.shared.common.Result.success(mockResponse);
        when(questionApplicationService.upsertQuestion(any(UpsertQuestionCommand.class)))
                .thenReturn(successResult);

        Result<QuestionResponseDto> result = commandHandler.handle(validCommand);
        assertThat(result.success()).isTrue();
    }

    private UpsertQuestionRequestDto createMinimalValidRequest() {
        var request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("test-source-id");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("Test content");
        request.setStatus("draft");

        // Create minimal taxonomy data to satisfy command validation
        var taxonomy = new com.quizfun.questionbank.application.dto.TaxonomyData();
        var categories = new com.quizfun.questionbank.application.dto.TaxonomyData.Categories();
        var level1 = new com.quizfun.questionbank.application.dto.TaxonomyData.Category();
        level1.setId("test-category");
        level1.setName("Test Category");
        categories.setLevel1(level1);
        taxonomy.setCategories(categories);
        request.setTaxonomy(taxonomy);

        return request;
    }
}