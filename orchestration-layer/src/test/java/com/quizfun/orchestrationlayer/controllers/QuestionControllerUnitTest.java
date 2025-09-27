package com.quizfun.orchestrationlayer.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.orchestrationlayer.dto.UpsertQuestionHttpRequestDto;
import com.quizfun.orchestrationlayer.dto.TaxonomyHttpDto;
import com.quizfun.orchestrationlayer.dto.McqHttpDto;
import com.quizfun.orchestrationlayer.mapper.UpsertQuestionDtoMapper;
import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QuestionController following TDD approach.
 * Tests the HTTP layer independently from business logic using mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
class QuestionControllerUnitTest {

    @Mock
    private IMediator mediator;

    @Mock
    private UpsertQuestionDtoMapper dtoMapper;

    @InjectMocks
    private QuestionController questionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with validation support
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(questionController)
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 200 OK for health check endpoint")
    @Description("Verifies that the health check endpoint returns OK status when all dependencies are properly configured")
    void shouldReturn200OkForHealthCheckEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/1/questionbanks/123/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK - QuestionController ready"));

        // Verify no interactions with business layer dependencies for health check
        verifyNoInteractions(mediator, dtoMapper);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should handle successful question creation request")
    @Description("Verifies that valid question creation requests are processed successfully with proper HTTP response")
    void shouldHandleSuccessfulQuestionCreationRequest() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();
        var mappedInternalDto = createMockInternalRequestDto();
        var successResult = createSuccessResult("created");

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenReturn(mappedInternalDto);
        when(mediator.send(any(UpsertQuestionCommand.class)))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Operation", "created"))
                .andExpect(header().string("X-Question-Id", "507f1f77bcf86cd799439011"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operation").value("created"));

        // Verify interactions
        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verify(mediator).send(any(UpsertQuestionCommand.class));
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should handle question update request")
    @Description("Verifies that valid question update requests are processed successfully with updated operation response")
    void shouldHandleQuestionUpdateRequest() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();
        httpRequest.setSourceQuestionId("existing-question-id");
        var mappedInternalDto = createMockInternalRequestDto();
        var successResult = createSuccessResult("updated");

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenReturn(mappedInternalDto);
        when(mediator.send(any(UpsertQuestionCommand.class)))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Operation", "updated"))
                .andExpect(jsonPath("$.data.operation").value("updated"));

        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verify(mediator).send(any(UpsertQuestionCommand.class));
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 400 Bad Request for missing required fields")
    @Description("Verifies that requests with missing required fields return validation error with appropriate HTTP status")
    void shouldReturn400BadRequestForMissingRequiredFields() throws Exception {
        // Given - Request with missing required fields
        var invalidRequest = new UpsertQuestionHttpRequestDto();
        // Missing sourceQuestionId, questionType, title, content, taxonomy

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));

        // Verify no interactions with business layer for validation failures
        verifyNoInteractions(mediator, dtoMapper);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 400 Bad Request for invalid path parameters")
    @Description("Verifies that invalid path parameters trigger constraint validation errors")
    void shouldReturn400BadRequestForInvalidPathParameters() throws Exception {
        // Given
        var validRequest = createValidHttpRequest();

        // When & Then - Invalid userId (negative)
        mockMvc.perform(post("/api/users/-1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        // Verify no interactions with business layer for validation failures
        verifyNoInteractions(mediator, dtoMapper);
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 422 Unprocessable Entity for unauthorized access")
    @Description("Verifies that unauthorized access errors are mapped to HTTP 422 status code")
    void shouldReturn422UnprocessableEntityForUnauthorizedAccess() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();
        var mappedInternalDto = createMockInternalRequestDto();
        var failureResult = Result.<QuestionResponseDto>failure("UNAUTHORIZED_ACCESS: User does not have access to question bank");

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenReturn(mappedInternalDto);
        when(mediator.send(any(UpsertQuestionCommand.class)))
                .thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/users/999/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("UNAUTHORIZED_ACCESS")));

        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verify(mediator).send(any(UpsertQuestionCommand.class));
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 409 Conflict for duplicate source question ID")
    @Description("Verifies that duplicate source question ID errors are mapped to HTTP 409 status code")
    void shouldReturn409ConflictForDuplicateSourceQuestionId() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();
        var mappedInternalDto = createMockInternalRequestDto();
        var failureResult = Result.<QuestionResponseDto>failure("DUPLICATE_SOURCE_QUESTION_ID: Source question ID already exists");

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenReturn(mappedInternalDto);
        when(mediator.send(any(UpsertQuestionCommand.class)))
                .thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DUPLICATE_SOURCE_QUESTION_ID")));

        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verify(mediator).send(any(UpsertQuestionCommand.class));
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should return 500 Internal Server Error for database errors")
    @Description("Verifies that database errors are mapped to HTTP 500 status code with proper error handling")
    void shouldReturn500InternalServerErrorForDatabaseErrors() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();
        var mappedInternalDto = createMockInternalRequestDto();
        var failureResult = Result.<QuestionResponseDto>failure("DATABASE_ERROR: Connection timeout");

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenReturn(mappedInternalDto);
        when(mediator.send(any(UpsertQuestionCommand.class)))
                .thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DATABASE_ERROR")));

        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verify(mediator).send(any(UpsertQuestionCommand.class));
    }

    @Test
    @Epic("Use Case Upsert Question with Relation-Main Path")
    @Story("story-009.http-api-integration")
    @DisplayName("Should handle unexpected runtime exceptions gracefully")
    @Description("Verifies that unexpected runtime exceptions are caught and return appropriate error responses")
    void shouldHandleUnexpectedRuntimeExceptionsGracefully() throws Exception {
        // Given
        var httpRequest = createValidHttpRequest();

        when(dtoMapper.mapToInternal(any(UpsertQuestionHttpRequestDto.class)))
                .thenThrow(new RuntimeException("Unexpected mapping error"));

        // When & Then
        mockMvc.perform(post("/api/users/1/questionbanks/123/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(httpRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INTERNAL_ERROR")));

        verify(dtoMapper).mapToInternal(any(UpsertQuestionHttpRequestDto.class));
        verifyNoInteractions(mediator);
    }

    private UpsertQuestionHttpRequestDto createValidHttpRequest() {
        var request = UpsertQuestionHttpRequestDto.builder()
                .sourceQuestionId("test-source-id")
                .questionType("mcq")
                .title("Test Question")
                .content("Test content")
                .status("draft")
                .build();

        // Add required taxonomy data to avoid validation errors
        var taxonomy = new TaxonomyHttpDto();
        var categories = new TaxonomyHttpDto.CategoriesHttpDto();
        var level1 = new TaxonomyHttpDto.CategoryHttpDto();
        level1.setId("tech");
        level1.setName("Technology");
        categories.setLevel1(level1);
        taxonomy.setCategories(categories);
        request.setTaxonomy(taxonomy);

        // Add required MCQ data for "mcq" question type
        var mcqData = new McqHttpDto();
        var option = new McqHttpDto.McqOptionHttpDto();
        option.setId(1);
        option.setText("Option 1");
        option.setIsCorrect(true);
        mcqData.setOptions(java.util.List.of(option));
        request.setMcqData(mcqData);

        return request;
    }

    private com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto createMockInternalRequestDto() {
        var dto = new com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto();
        dto.setSourceQuestionId("test-source-id");
        dto.setQuestionType("mcq");
        dto.setTitle("Test Question");
        dto.setContent("Test content");
        dto.setStatus("draft");
        return dto;
    }

    private Result<QuestionResponseDto> createSuccessResult(String operation) {
        var responseDto = QuestionResponseDto.builder()
                .questionId("507f1f77bcf86cd799439011")
                .sourceQuestionId("test-source-id")
                .operation(operation)
                .taxonomyRelationshipsCount(3)
                .build();

        return Result.success(responseDto);
    }
}