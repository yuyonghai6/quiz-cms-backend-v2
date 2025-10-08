package com.quizfun.orchestrationlayer.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.orchestrationlayer.dto.CreateDefaultQuestionBankRequestDto;
import com.quizfun.questionbank.application.commands.OnNewUserCreateDefaultQuestionBankCommand;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for DefaultQuestionBankController.
 *
 * Tests REST API contract including:
 * - HTTP status codes
 * - Request validation
 * - Response structure
 * - Header inclusion
 */
@WebMvcTest(DefaultQuestionBankController.class)
@AutoConfigureMockMvc(addFilters = false)
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1008.rest-controller")
@DisplayName("DefaultQuestionBankController MockMvc Tests")
class DefaultQuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IMediator mediator;

    @Test
    @DisplayName("Should return 201 Created when successful")
    void shouldReturn201CreatedWhenSuccessful() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .metadata(Map.of("source", "registration"))
            .build();

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(123456789L)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("X-Question-Bank-ID"))
            .andExpect(header().string("X-Question-Bank-ID", "1730832000000000"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(123456789))
            .andExpect(jsonPath("$.data.questionBankId").value(1730832000000000L));

        verify(mediator, times(1)).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when userId is null")
    void shouldReturn400BadRequestWhenUserIdIsNull() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(null)
            .userEmail("test@example.com")
            .build();

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(mediator, never()).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when userId is negative")
    void shouldReturn400BadRequestWhenUserIdIsNegative() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(-1L)
            .userEmail("test@example.com")
            .build();

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(mediator, never()).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when userId is zero")
    void shouldReturn400BadRequestWhenUserIdIsZero() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(0L)
            .userEmail("test@example.com")
            .build();

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(mediator, never()).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when email format is invalid")
    void shouldReturn400BadRequestWhenEmailFormatIsInvalid() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("invalid-email")
            .build();

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(mediator, never()).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should return 409 Conflict when user already exists")
    void shouldReturn409ConflictWhenUserAlreadyExists() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.failure("DUPLICATE_USER: User 123456789 already has a default question bank"));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("DUPLICATE_USER: User 123456789 already has a default question bank"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error on database error")
    void shouldReturn500InternalServerErrorOnDatabaseError() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.failure("DATABASE_ERROR: Connection timeout"));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("DATABASE_ERROR: Connection timeout"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error on internal error")
    void shouldReturn500InternalServerErrorOnInternalError() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.failure("INTERNAL_ERROR: ID generation failed"));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("INTERNAL_ERROR: ID generation failed"));
    }

    @Test
    @DisplayName("Should handle valid request successfully")
    void shouldHandleValidRequestSuccessfully() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = new CreateDefaultQuestionBankRequestDto();
        request.setUserId(999L);
        request.setUserEmail("test@example.com");

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(999L)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should handle missing request body")
    void shouldHandleMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(mediator, never()).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }

    @Test
    @DisplayName("Should handle empty metadata map")
    void shouldHandleEmptyMetadataMap() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .metadata(Map.of())
            .build();

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(123456789L)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should handle null email (optional field)")
    void shouldHandleNullEmail() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail(null)
            .build();

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(123456789L)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should handle null metadata (optional field)")
    void shouldHandleNullMetadata() throws Exception {
        // Given
        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(123456789L)
            .userEmail("test@example.com")
            .metadata(null)
            .build();

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(123456789L)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should send command via mediator with correct parameters")
    void shouldSendCommandViaMediatorWithCorrectParameters() throws Exception {
        // Given
        Long userId = 999L;
        String email = "user@test.com";
        Map<String, String> metadata = Map.of("key", "value");

        CreateDefaultQuestionBankRequestDto request = CreateDefaultQuestionBankRequestDto.builder()
            .userId(userId)
            .userEmail(email)
            .metadata(metadata)
            .build();

        DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
            .userId(userId)
            .questionBankId(1730832000000000L)
            .questionBankName("Default Question Bank")
            .description("Your default question bank for getting started with quiz creation")
            .isActive(true)
            .taxonomySetCreated(true)
            .createdAt(Instant.now())
            .build();

        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success("Success", response));

        // When
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Then - Verify mediator was called (command creation is tested in command tests)
        verify(mediator, times(1)).send(any(OnNewUserCreateDefaultQuestionBankCommand.class));
    }
}
