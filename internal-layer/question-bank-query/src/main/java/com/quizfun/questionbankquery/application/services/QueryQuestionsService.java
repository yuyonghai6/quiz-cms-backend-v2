package com.quizfun.questionbankquery.application.services;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.PaginationMetadata;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.ports.in.IQueryQuestionsService;
import com.quizfun.questionbankquery.application.ports.out.IQuestionQueryRepository;
import com.quizfun.questionbankquery.application.validation.QueryRequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for querying questions.
 *
 * <p>This service orchestrates the query operation without domain logic complexity.
 * It delegates to the repository and calculates pagination metadata.
 *
 * <p><strong>Why No Domain Layer?</strong></p>
 * <ul>
 *   <li>Query operations don't enforce business rules</li>
 *   <li>No state transitions or domain events</li>
 *   <li>Direct mapping from repository to DTOs is sufficient</li>
 *   <li>Simpler architecture improves maintainability</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryQuestionsService implements IQueryQuestionsService {

    private final IQuestionQueryRepository questionQueryRepository;

    @Override
    public Result<QueryQuestionsResponse> queryQuestions(QueryQuestionsRequest request) {
        try {
            // Validate request
            List<String> validationErrors = QueryRequestValidator.validate(request);
            if (!validationErrors.isEmpty()) {
                String errorMessage = String.join(", ", validationErrors);
                log.warn("Query request validation failed: {}", errorMessage);
                return Result.failure(errorMessage);
            }

            log.debug("Querying questions for user: {}, questionBank: {}, page: {}, size: {}",
                    request.getUserId(), request.getQuestionBankId(),
                    request.getPage(), request.getSize());

            // Query questions from repository
            List<QuestionDTO> questions = questionQueryRepository.queryQuestions(request);

            // Count total matching questions
            long totalItems = questionQueryRepository.countQuestions(
                    request.getUserId(),
                    request.getQuestionBankId(),
                    request
            );

            // Calculate pagination metadata
            PaginationMetadata pagination = PaginationMetadata.of(
                    request.getPage(),
                    request.getSize(),
                    totalItems
            );

            // Create response
            QueryQuestionsResponse response = new QueryQuestionsResponse(questions, pagination);

            log.debug("Query completed successfully. Found {} questions out of {} total",
                    questions.size(), totalItems);

            return Result.success(response);

        } catch (Exception e) {
            log.error("Error querying questions: {}", e.getMessage(), e);
            return Result.failure("Failed to query questions: " + e.getMessage());
        }
    }
}
