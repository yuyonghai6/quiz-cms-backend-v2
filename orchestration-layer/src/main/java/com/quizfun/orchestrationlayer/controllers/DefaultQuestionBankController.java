package com.quizfun.orchestrationlayer.controllers;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.orchestrationlayer.dto.CreateDefaultQuestionBankRequestDto;
import com.quizfun.questionbank.application.commands.OnNewUserCreateDefaultQuestionBankCommand;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for default question bank operations.
 *
 * This controller provides HTTP endpoints for external systems
 * (e.g., user management service) to trigger default question bank
 * provisioning for new users.
 *
 * API Contract:
 * - POST /api/users/default-question-bank
 * - Accepts: CreateDefaultQuestionBankRequestDto (JSON)
 * - Returns: 201 Created | 400 Bad Request | 409 Conflict | 500 Internal Server Error
 */
@RestController
@RequestMapping("/api/users/default-question-bank")
@Validated
public class DefaultQuestionBankController {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuestionBankController.class);

    private final IMediator mediator;

    /**
     * Constructor injection for mediator.
     *
     * @param mediator CQRS mediator for command routing
     */
    public DefaultQuestionBankController(IMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Creates a default question bank for a new user.
     *
     * HTTP Status Codes:
     * - 201 Created: Question bank successfully created
     * - 400 Bad Request: Invalid input (validation failure)
     * - 409 Conflict: User already has a default question bank
     * - 500 Internal Server Error: Database or system error
     *
     * @param request The request DTO containing userId, email, and metadata
     * @return ResponseEntity with Result containing DefaultQuestionBankResponseDto
     */
    @PostMapping
    public ResponseEntity<Result<DefaultQuestionBankResponseDto>> createDefaultQuestionBank(
            @Valid @RequestBody CreateDefaultQuestionBankRequestDto request) {

        logger.info("Received request to create default question bank for userId: {}", request.getUserId());

        try {
            // Create command from request
            OnNewUserCreateDefaultQuestionBankCommand command =
                new OnNewUserCreateDefaultQuestionBankCommand(
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getMetadata()
                );

            // Send command via mediator
            Result<DefaultQuestionBankResponseDto> result = mediator.send(command);

            if (result.success()) {
                logger.info("Successfully created default question bank for userId: {}, bankId: {}",
                    request.getUserId(), result.data().getQuestionBankId());

                return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("X-Question-Bank-ID", result.data().getQuestionBankId().toString())
                    .body(result);
            } else {
                logger.warn("Failed to create default question bank for userId: {}. Reason: {}",
                    request.getUserId(), result.message());

                return mapErrorToHttpStatus(result);
            }

        } catch (IllegalArgumentException ex) {
            // Command constructor validation failure
            logger.warn("Validation error for userId: {}. Error: {}",
                request.getUserId(), ex.getMessage());

            return ResponseEntity
                .badRequest()
                .body(Result.failure("VALIDATION_ERROR: " + ex.getMessage()));
        } catch (Exception ex) {
            // Unexpected errors
            logger.error("Unexpected error creating default question bank for userId: {}",
                request.getUserId(), ex);

            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure("INTERNAL_ERROR: " + ex.getMessage()));
        }
    }

    /**
     * Maps business error codes to HTTP status codes.
     *
     * Error Mapping:
     * - DUPLICATE_USER → 409 Conflict
     * - DATABASE_ERROR → 500 Internal Server Error
     * - INTERNAL_ERROR → 500 Internal Server Error
     * - Default → 400 Bad Request
     *
     * @param result The failed result from the application layer
     * @return ResponseEntity with appropriate HTTP status code
     */
    private ResponseEntity<Result<DefaultQuestionBankResponseDto>> mapErrorToHttpStatus(
            Result<DefaultQuestionBankResponseDto> result) {

        if (result.message().startsWith("DUPLICATE_USER")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        } else if (result.message().startsWith("DATABASE_ERROR") ||
                   result.message().startsWith("INTERNAL_ERROR")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
