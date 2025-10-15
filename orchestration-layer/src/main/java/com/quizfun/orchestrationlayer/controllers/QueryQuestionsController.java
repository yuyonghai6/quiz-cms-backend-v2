package com.quizfun.orchestrationlayer.controllers;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.queries.QueryQuestions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * REST controller for querying questions from a question bank.
 * Handles HTTP GET requests and delegates to CQRS query handlers through the mediator pattern.
 *
 * This controller provides read-only operations with support for filtering, pagination, and sorting.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/question-banks/{questionBankId}/questions")
@Validated
public class QueryQuestionsController {

    private static final Logger logger = LoggerFactory.getLogger(QueryQuestionsController.class);

    private final IMediator mediator;

    /**
     * Constructor injection for dependencies.
     *
     * @param mediator The CQRS mediator for query processing
     */
    public QueryQuestionsController(IMediator mediator) {
        this.mediator = mediator;
        logger.info("QueryQuestionsController initialized with mediator");
    }

    /**
     * Queries questions from a question bank with optional filtering, pagination, and sorting.
     *
     * @param userId The user ID from path parameter
     * @param questionBankId The question bank ID from path parameter
     * @param categories Optional list of categories to filter by
     * @param tags Optional list of tags to filter by
     * @param quizzes Optional list of quizzes to filter by
     * @param searchText Optional text to search in question text
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (1-100, default: 20)
     * @param sortBy Field to sort by (default: createdAt)
     * @param sortDirection Sort direction (asc/desc, default: desc)
     * @return ResponseEntity with query results and pagination metadata
     */
    @GetMapping
    public ResponseEntity<?> queryQuestions(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long questionBankId,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> quizzes,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        logger.info("Received query questions request for user: {} and question bank: {}", userId, questionBankId);

        try {
            // Build query request
            QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                    .userId(userId)
                    .questionBankId(questionBankId)
                    .categories(categories)
                    .tags(tags)
                    .quizzes(quizzes)
                    .searchText(searchText)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();

            // Create and send query through mediator
            QueryQuestions query = new QueryQuestions(request);
            Result<QueryQuestionsResponse> result = mediator.send(query);

            if (result.success()) {
                logger.info("Successfully queried {} questions for user: {} and question bank: {}",
                        result.data().questions().size(), userId, questionBankId);
                return ResponseEntity.ok(result.data());
            } else {
                logger.warn("Failed to query questions for user: {} and question bank: {} with error: {}",
                        userId, questionBankId, result.message());
                return ResponseEntity.badRequest().body(result.message());
            }

        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid request parameters for user: {} and question bank: {}: {}",
                    userId, questionBankId, ex.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + ex.getMessage());

        } catch (Exception ex) {
            logger.error("Unexpected error querying questions for user: {} and question bank: {}",
                    userId, questionBankId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: An unexpected error occurred");
        }
    }

    /**
     * Exception handler for constraint violation errors (e.g., invalid page/size parameters).
     *
     * @param ex The constraint violation exception
     * @return ResponseEntity with 400 status and error message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation in query request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body("Validation error: " + ex.getMessage());
    }
}
