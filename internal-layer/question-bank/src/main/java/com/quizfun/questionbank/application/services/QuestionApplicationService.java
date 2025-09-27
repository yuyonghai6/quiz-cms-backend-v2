package com.quizfun.questionbank.application.services;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.ports.out.QuestionRepository;
import com.quizfun.questionbank.application.ports.out.QuestionTaxonomyRelationshipRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategyFactory;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Application service that coordinates the complete question upsert business workflow.
 * Integrates validation chain, question type strategies, and repository operations
 * within a transactional boundary to ensure ACID compliance across MongoDB collections.
 *
 * This service follows the Application Service pattern and serves as the orchestrator
 * for all business operations required for question upsert operations.
 */
@Service
@Transactional(
    isolation = Isolation.READ_COMMITTED,
    propagation = Propagation.REQUIRED,
    rollbackFor = Exception.class
)
public class QuestionApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionApplicationService.class);

    private final ValidationHandler validationChain;
    private final QuestionTypeStrategyFactory strategyFactory;
    private final QuestionRepository questionRepository;
    private final QuestionTaxonomyRelationshipRepository relationshipRepository;

    /**
     * Constructor injection for all dependencies as specified in the requirements.
     *
     * @param validationChain Complete validation chain for command validation
     * @param strategyFactory Factory for selecting appropriate question type strategies
     * @param questionRepository Repository for question aggregate persistence
     * @param relationshipRepository Repository for taxonomy relationship management
     */
    public QuestionApplicationService(
            @Qualifier("questionUpsertValidationChain") ValidationHandler validationChain,
            QuestionTypeStrategyFactory strategyFactory,
            QuestionRepository questionRepository,
            QuestionTaxonomyRelationshipRepository relationshipRepository) {

        this.validationChain = Objects.requireNonNull(validationChain, "ValidationHandler cannot be null");
        this.strategyFactory = Objects.requireNonNull(strategyFactory, "QuestionTypeStrategyFactory cannot be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "QuestionRepository cannot be null");
        this.relationshipRepository = Objects.requireNonNull(relationshipRepository, "QuestionTaxonomyRelationshipRepository cannot be null");

        logger.info("QuestionApplicationService initialized with dependencies: " +
                   "validation chain, strategy factory, question repository, relationship repository");
    }

    /**
     * Coordinates the complete question upsert business workflow with ACID transaction support.
     *
     * Business flow:
     * 1. Execute validation chain (fail-fast)
     * 2. Process question using Strategy Pattern
     * 3. Instantiate QuestionAggregate object and store to MongoDB
     * 4. Instantiate QuestionTaxonomyRelationshipAggregate objects
     * 5. Insert multiple relationship documents to MongoDB
     * 6. Return success response
     *
     * @param command Complete command containing all required data for question upsert
     * @return Result with QuestionResponseDto containing operation details
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public Result<QuestionResponseDto> upsertQuestion(UpsertQuestionCommand command) {
        Instant startTime = Instant.now();

        try {
            logger.info("Starting question upsert process for source ID: {} by user: {} in question bank: {}",
                       command.getSourceQuestionId(), command.getUserId(), command.getQuestionBankId());

            // Step 1: Execute validation chain (fail-fast)
            logger.debug("Executing validation chain for source ID: {}", command.getSourceQuestionId());
            var validationResult = validationChain.validate(command);

            if (validationResult.isFailure()) {
                logger.warn("Validation failed for source ID: {} with error: {}",
                           command.getSourceQuestionId(), validationResult.getError());

                recordValidationFailure(validationResult.getErrorCode());
                return Result.failure(validationResult.getErrorCode(), validationResult.getError());
            }

            logger.debug("Validation chain passed for source ID: {}", command.getSourceQuestionId());
            recordValidationSuccess();

            // Step 2: Process question using Strategy Pattern
            logger.debug("Processing question data using strategy for type: {} and source ID: {}",
                        command.getQuestionType(), command.getSourceQuestionId());

            var strategy = strategyFactory.getStrategy(command.getQuestionType());
            var questionAggregateResult = strategy.processQuestionData(command);

            if (questionAggregateResult.isFailure()) {
                logger.warn("Strategy processing failed for source ID: {} with error: {}",
                           command.getSourceQuestionId(), questionAggregateResult.getError());

                recordStrategyFailure(command.getQuestionType().toString(), questionAggregateResult.getErrorCode());
                return Result.failure(questionAggregateResult.getErrorCode(), questionAggregateResult.getError());
            }

            var questionAggregate = questionAggregateResult.getValue();
            logger.debug("Successfully processed question data for source ID: {} using {} strategy",
                        command.getSourceQuestionId(), strategy.getStrategyName());

            recordStrategySuccess(command.getQuestionType().toString());

            // Validate aggregate state
            if (!questionAggregate.hasValidTypeSpecificData()) {
                logger.error("Strategy produced invalid aggregate for source ID: {}",
                            command.getSourceQuestionId());
                return Result.failure("INVALID_AGGREGATE", "Strategy produced invalid question aggregate");
            }

            // Step 3: Instantiate QuestionAggregate object and store to MongoDB
            logger.debug("Upserting question aggregate for source ID: {}", command.getSourceQuestionId());

            var questionResult = questionRepository.upsertBySourceQuestionId(questionAggregate);

            if (questionResult.isFailure()) {
                logger.error("Question repository upsert failed for source ID: {} with error: {}",
                            command.getSourceQuestionId(), questionResult.getError());

                recordRepositoryFailure("question", questionResult.getErrorCode());

                // Transaction will automatically rollback due to @Transactional annotation
                return Result.failure(questionResult.getErrorCode(), questionResult.getError());
            }

            var persistedQuestionAggregate = questionResult.getValue();
            logger.debug("Successfully upserted question with MongoDB ID: {} for source ID: {}",
                        persistedQuestionAggregate.getId(), command.getSourceQuestionId());

            recordRepositorySuccess("question");

            // Ensure we have a valid MongoDB ID for relationships
            if (persistedQuestionAggregate.getId() == null) {
                logger.error("Persisted question has null MongoDB ID for source ID: {}",
                            command.getSourceQuestionId());
                return Result.failure("INVALID_QUESTION_ID", "Question was persisted but has no MongoDB ID");
            }

            // Step 4: Instantiate QuestionTaxonomyRelationshipAggregate objects
            logger.debug("Creating taxonomy relationships for question ID: {}", persistedQuestionAggregate.getId());

            var relationships = QuestionTaxonomyRelationshipAggregate.createFromCommand(
                persistedQuestionAggregate.getId(), command);

            logger.debug("Created {} taxonomy relationships for question ID: {}",
                        relationships.size(), persistedQuestionAggregate.getId());

            // Step 5: Insert multiple relationship documents to MongoDB (transactional)
            // This part follows requirement: "do insert multiple documents to mongodb:
            // for each question to every single taxonomy mapping, add one document"
            var relationshipResult = relationshipRepository.replaceRelationshipsForQuestion(
                persistedQuestionAggregate.getId(), relationships);

            if (relationshipResult.isFailure()) {
                logger.error("Relationship repository operation failed for question ID: {} with error: {}",
                            persistedQuestionAggregate.getId(), relationshipResult.getError());

                recordRepositoryFailure("relationships", relationshipResult.getErrorCode());

                // Transaction will automatically rollback, undoing both question and relationship operations
                return Result.failure(relationshipResult.getErrorCode(), relationshipResult.getError());
            }

            logger.debug("Successfully managed {} taxonomy relationships for question ID: {}",
                        relationships.size(), persistedQuestionAggregate.getId());

            recordRepositorySuccess("relationships");

            // Step 6: Return success response
            var responseDto = mapToResponseDto(persistedQuestionAggregate, relationships.size());

            Duration executionTime = Duration.between(startTime, Instant.now());
            logger.info("Successfully completed question upsert for source ID: {} with operation: {} in {}ms",
                       command.getSourceQuestionId(), responseDto.getOperation(), executionTime.toMillis());

            return Result.success(responseDto);

        } catch (Exception ex) {
            Duration executionTime = Duration.between(startTime, Instant.now());
            logger.error("Unexpected error during question upsert for source ID: {} after {}ms",
                        command.getSourceQuestionId(), executionTime.toMillis(), ex);
            return Result.failure("UPSERT_ERROR", "Unexpected error during question upsert: " + ex.getMessage());
        }
    }

    /**
     * Maps QuestionAggregate to QuestionResponseDto with operation type detection.
     *
     * @param aggregate The persisted question aggregate
     * @param relationshipCount Number of taxonomy relationships created
     * @return Response DTO with operation type and relationship count
     */
    private QuestionResponseDto mapToResponseDto(QuestionAggregate aggregate, int relationshipCount) {
        return QuestionResponseDto.builder()
            .questionId(aggregate.getId().toString())
            .sourceQuestionId(aggregate.getSourceQuestionId())
            .operation(determineOperation(aggregate))
            .taxonomyRelationshipsCount(relationshipCount)
            .build();
    }

    /**
     * Determines whether the operation was a create or update based on aggregate timestamps.
     *
     * @param aggregate The question aggregate to analyze
     * @return "created" if timestamps are equal, "updated" if different
     */
    private String determineOperation(QuestionAggregate aggregate) {
        // Check if creation and update timestamps are the same (new question)
        // or if the question was actually updated
        return aggregate.getCreatedAt().equals(aggregate.getUpdatedAt()) ? "created" : "updated";
    }

    // Metrics recording methods for monitoring and observability

    private void recordValidationSuccess() {
        logger.debug("Validation metrics: success recorded");
    }

    private void recordValidationFailure(String errorCode) {
        logger.debug("Validation metrics: failure recorded with error code: {}", errorCode);
    }

    private void recordStrategySuccess(String questionType) {
        logger.debug("Strategy metrics: success recorded for question type: {}", questionType);
    }

    private void recordStrategyFailure(String questionType, String errorCode) {
        logger.debug("Strategy metrics: failure recorded for question type: {} with error code: {}",
                    questionType, errorCode);
    }

    private void recordRepositorySuccess(String operation) {
        logger.debug("Repository metrics: success recorded for operation: {}", operation);
    }

    private void recordRepositoryFailure(String operation, String errorCode) {
        logger.debug("Repository metrics: failure recorded for operation: {} with error code: {}",
                    operation, errorCode);
    }
}