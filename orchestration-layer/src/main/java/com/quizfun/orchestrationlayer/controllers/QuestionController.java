package com.quizfun.orchestrationlayer.controllers;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.orchestrationlayer.dto.UpsertQuestionHttpRequestDto;
import com.quizfun.orchestrationlayer.mapper.UpsertQuestionDtoMapper;
import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * REST controller for question management operations.
 * Handles HTTP requests and delegates to CQRS command handlers through the mediator pattern.
 *
 * This controller maintains clean separation between HTTP concerns and business logic
 * by transforming HTTP DTOs to internal commands and handling HTTP-specific responses.
 */
@RestController
@RequestMapping("/api/users/{userId}/questionbanks/{questionbankId}")
@Validated
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final IMediator mediator;
    private final UpsertQuestionDtoMapper dtoMapper;

    /**
     * Constructor injection for dependencies.
     *
     * @param mediator The CQRS mediator for command processing
     * @param dtoMapper The mapper for transforming between HTTP and internal DTOs
     */
    public QuestionController(IMediator mediator, UpsertQuestionDtoMapper dtoMapper) {
        this.mediator = mediator;
        this.dtoMapper = dtoMapper;
        logger.info("QuestionController initialized with mediator and DTO mapper");
    }

    /**
     * Creates or updates a question based on the source question ID.
     *
     * @param userId The user ID from path parameter
     * @param questionbankId The question bank ID from path parameter
     * @param request The question data from request body
     * @param httpRequest The HTTP servlet request for logging
     * @return ResponseEntity with question response or error details
     */
    @PostMapping("/questions")
    public ResponseEntity<Result<QuestionResponseDto>> upsertQuestion(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long questionbankId,
            @Valid @RequestBody UpsertQuestionHttpRequestDto request,
            HttpServletRequest httpRequest) {

        logger.info("Received upsert question request for user: {} and question bank: {} from IP: {}",
                userId, questionbankId, getClientIpAddress(httpRequest));

        try {
            if (userId == null || userId <= 0 || questionbankId == null || questionbankId <= 0) {
                throw new IllegalArgumentException("Path parameters must be positive");
            }
            // Transform HTTP DTO to internal DTO
            var internalRequestDto = dtoMapper.mapToInternal(request);

            // Create command with path parameters and transformed request
            var command = new UpsertQuestionCommand(userId, questionbankId, internalRequestDto);

            // Send command through mediator
            var result = mediator.send(command);

            if (result.success()) {
                logger.info("Successfully processed upsert question request for source ID: {} with operation: {}",
                        request.getSourceQuestionId(), result.data().getOperation());

                return ResponseEntity.ok()
                    .header("X-Operation", result.data().getOperation())
                    .header("X-Question-Id", result.data().getQuestionId())
                    .body(result);
            } else {
                logger.warn("Failed to process upsert question request for source ID: {} with error: {}",
                        request.getSourceQuestionId(), result.message());

                return createErrorResponse(result);
            }

        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid request parameters for user: {} and question bank: {}: {}",
                    userId, questionbankId, ex.getMessage());

            var errorResult = Result.<QuestionResponseDto>failure(
                "INVALID_REQUEST: " + ex.getMessage());
            return ResponseEntity.badRequest().body(errorResult);

        } catch (Exception ex) {
            logger.error("Unexpected error processing upsert question request for user: {} and question bank: {}",
                     userId, questionbankId, ex);

            var errorResult = Result.<QuestionResponseDto>failure(
                "INTERNAL_ERROR: An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Creates appropriate HTTP error response based on business error codes.
     *
     * @param result The failed result from command processing
     * @return ResponseEntity with appropriate HTTP status code
     */
    private ResponseEntity<Result<QuestionResponseDto>> createErrorResponse(Result<QuestionResponseDto> result) {
        var error = result.message();

        // Map error codes to HTTP status codes
        if (error.startsWith("UNAUTHORIZED_ACCESS") || error.startsWith("QUESTION_BANK_NOT_FOUND")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        } else if (error.startsWith("TAXONOMY_REFERENCE_NOT_FOUND")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        } else if (error.startsWith("MISSING_REQUIRED_FIELD") ||
                   error.startsWith("TYPE_DATA_MISMATCH") ||
                   error.startsWith("INVALID_QUESTION_TYPE")) {
            return ResponseEntity.badRequest().body(result);
        } else if (error.startsWith("DUPLICATE_SOURCE_QUESTION_ID")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        } else if (error.startsWith("DATABASE_ERROR") ||
                   error.startsWith("TRANSACTION_FAILED")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            // Default to bad request for unknown errors
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Extracts client IP address from HTTP request, considering proxy headers.
     *
     * @param request The HTTP servlet request
     * @return The client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Global exception handler for validation errors.
     *
     * @param ex The method argument validation exception
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<QuestionResponseDto>> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Validation error in request: {}", ex.getMessage());

        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        var errorResult = Result.<QuestionResponseDto>failure(
            "VALIDATION_ERROR: Request validation failed: " + errors);

        return ResponseEntity.badRequest().body(errorResult);
    }

    /**
     * Global exception handler for constraint violation errors.
     *
     * @param ex The constraint violation exception
     * @return ResponseEntity with constraint violation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<QuestionResponseDto>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation in request: {}", ex.getMessage());

        var errorResult = Result.<QuestionResponseDto>failure(
            "CONSTRAINT_VIOLATION: Request constraint violation: " + ex.getMessage());

        return ResponseEntity.badRequest().body(errorResult);
    }

    /**
     * Health check endpoint to verify controller and mediator integration.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            validateControllerSetup();
            logger.info("Health check passed - controller and dependencies properly configured");
            return ResponseEntity.ok("OK - QuestionController ready");
        } catch (Exception ex) {
            logger.error("Health check failed - Controller configuration issue", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("ERROR - Service temporarily unavailable");
        }
    }

    /**
     * Validates that controller dependencies are properly initialized.
     * This helps with debugging configuration issues.
     */
    private void validateControllerSetup() {
        if (mediator == null) {
            throw new IllegalStateException("Mediator not properly injected");
        }
        if (dtoMapper == null) {
            throw new IllegalStateException("DTO Mapper not properly injected");
        }
        logger.debug("Controller validation passed - all dependencies properly configured");
    }
}